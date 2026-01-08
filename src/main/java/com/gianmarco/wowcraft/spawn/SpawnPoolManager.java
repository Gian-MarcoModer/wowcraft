package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages the spawn point pool system.
 * Handles spawn point generation, activation/deactivation, and lazy spawning.
 */
public class SpawnPoolManager {

    // All spawn points (pool)
    private static final Map<UUID, SpawnPoint> spawnPointPool = new ConcurrentHashMap<>();

    // Currently active spawn points (75% of pool)
    private static final Set<UUID> activeSpawnPoints = ConcurrentHashMap.newKeySet();

    // Spatial index for fast lookups
    private static final Map<ChunkPos, List<SpawnPoint>> chunkSpawnMap = new ConcurrentHashMap<>();

    // Track which regions have been generated
    private static final Set<RegionPos> generatedRegions = ConcurrentHashMap.newKeySet();

    // Lazy spawning config
    public static final int SPAWN_ENTITY_DISTANCE = 128;  // 8 chunks
    public static final int UNLOAD_ENTITY_DISTANCE = 160;  // 10 chunks
    public static final int DEACTIVATE_POINT_DISTANCE = 192;  // 12 chunks
    public static final int CHECK_INTERVAL_TICKS = 100;  // 5 seconds

    // Rotation config
    private static final float BASE_ACTIVE_PERCENTAGE = 0.75f;  // 75% active

    /**
     * Check if a region has been generated.
     */
    public static boolean isRegionGenerated(BlockPos pos) {
        RegionPos region = RegionPos.fromBlockPos(pos, 300);
        return generatedRegions.contains(region);
    }

    /**
     * Mark a region as generated.
     */
    public static void markRegionGenerated(BlockPos pos) {
        RegionPos region = RegionPos.fromBlockPos(pos, 300);
        generatedRegions.add(region);
    }

    /**
     * Add a spawn point to the pool.
     */
    public static void addSpawnPoint(SpawnPoint point) {
        spawnPointPool.put(point.getId(), point);

        // Add to spatial index
        ChunkPos chunk = new ChunkPos(point.getPosition());
        chunkSpawnMap.computeIfAbsent(chunk, k -> new ArrayList<>()).add(point);

        WowCraft.LOGGER.debug("Added spawn point {} at {} (type: {}, hostility: {})",
            point.getId(), point.getPosition(), point.getType(), point.getHostility());
    }

    /**
     * Get a spawn point by ID.
     */
    public static SpawnPoint getSpawnPoint(UUID id) {
        return spawnPointPool.get(id);
    }

    /**
     * Get all spawn points.
     */
    public static Collection<SpawnPoint> getAllSpawnPoints() {
        return spawnPointPool.values();
    }

    /**
     * Get spawn points near a chunk.
     */
    public static List<SpawnPoint> getSpawnPointsNearChunk(ChunkPos chunk, int radiusChunks) {
        List<SpawnPoint> result = new ArrayList<>();

        for (int x = -radiusChunks; x <= radiusChunks; x++) {
            for (int z = -radiusChunks; z <= radiusChunks; z++) {
                ChunkPos nearbyChunk = new ChunkPos(chunk.x + x, chunk.z + z);
                List<SpawnPoint> points = chunkSpawnMap.get(nearbyChunk);
                if (points != null) {
                    result.addAll(points);
                }
            }
        }

        return result;
    }

    /**
     * Get spawn points in a region.
     */
    public static List<SpawnPoint> getSpawnPointsInRegion(RegionPos region) {
        List<SpawnPoint> result = new ArrayList<>();
        BlockPos center = region.getCenterBlockPos(300);

        WowCraft.LOGGER.info("Looking for spawn points in region {} - center: {}, checking {} total points",
            region, center, spawnPointPool.size());

        int halfRegion = 150;  // Region is 300x300, so ±150 from center
        int found = 0;
        int missed = 0;

        for (SpawnPoint point : spawnPointPool.values()) {
            int dx = Math.abs(point.getPosition().getX() - center.getX());
            int dz = Math.abs(point.getPosition().getZ() - center.getZ());

            // Use rectangular bounds check instead of circular distance
            if (dx <= halfRegion && dz <= halfRegion) {
                result.add(point);
                if (found < 3) {
                    WowCraft.LOGGER.info("  Found point at {} (dx: {}, dz: {})",
                        point.getPosition(), dx, dz);
                }
                found++;
            } else if (missed < 3 && (dx <= halfRegion * 2 || dz <= halfRegion * 2)) {
                WowCraft.LOGGER.info("  MISSED point at {} (dx: {}, dz: {}, outside bounds)",
                    point.getPosition(), dx, dz);
                missed++;
            }
        }

        WowCraft.LOGGER.info("Found {} spawn points in region {} within rectangular bounds", result.size(), region);
        return result;
    }

    /**
     * Activate spawn points in a region (called on generation).
     */
    public static void activateRegionSpawnPoints(RegionPos region, float percentage) {
        List<SpawnPoint> regionPoints = getSpawnPointsInRegion(region);

        WowCraft.LOGGER.info("Activating spawn points in region {} - found {} total points in pool, {} in region",
            region, spawnPointPool.size(), regionPoints.size());

        // Separate points into always-active and rotatable
        List<SpawnPoint> alwaysActive = new ArrayList<>();
        List<SpawnPoint> rotatable = new ArrayList<>();

        for (SpawnPoint point : regionPoints) {
            if (point.isAlwaysActive()) {
                alwaysActive.add(point);
            } else {
                rotatable.add(point);
            }
        }

        // Always activate POI points
        for (SpawnPoint point : alwaysActive) {
            activateSpawnPoint(point);
        }

        // Activate percentage of rotatable points
        int targetActive = (int)(rotatable.size() * percentage);
        Collections.shuffle(rotatable);

        for (int i = 0; i < targetActive && i < rotatable.size(); i++) {
            activateSpawnPoint(rotatable.get(i));
        }

        WowCraft.LOGGER.info("Activated {} spawn points in region {} ({} POI, {} scatter)",
            alwaysActive.size() + targetActive, region, alwaysActive.size(), targetActive);
    }

    /**
     * Activate a spawn point.
     */
    private static void activateSpawnPoint(SpawnPoint point) {
        activeSpawnPoints.add(point.getId());
        point.setActiveInRotation(true);

        // Roll which mob type to spawn at this point
        point.rollMobType();

        WowCraft.LOGGER.debug("Activated spawn point {} (type: {})", point.getId(), point.getType());
    }

    /**
     * Deactivate a spawn point (for rotation).
     */
    private static void deactivateSpawnPoint(SpawnPoint point) {
        activeSpawnPoints.remove(point.getId());
        point.setActiveInRotation(false);
        point.setRespawnEnabled(false);

        // Don't despawn entities, just stop respawning
        WowCraft.LOGGER.debug("Deactivated spawn point {} (entities remain)", point.getId());
    }

    /**
     * Rotate spawn points in a region (called periodically).
     */
    public static void rotateRegionSpawnPoints(RegionPos region) {
        List<SpawnPoint> regionPoints = getSpawnPointsInRegion(region);

        // Filter to rotatable points only
        List<SpawnPoint> rotatable = regionPoints.stream()
            .filter(p -> !p.isAlwaysActive())
            .toList();

        if (rotatable.isEmpty()) {
            return;
        }

        // Deactivate all current
        for (SpawnPoint point : rotatable) {
            if (point.isActiveInRotation()) {
                deactivateSpawnPoint(point);
            }
        }

        // Activate new random selection (75%)
        int targetActive = (int)(rotatable.size() * BASE_ACTIVE_PERCENTAGE);
        List<SpawnPoint> shuffled = new ArrayList<>(rotatable);
        Collections.shuffle(shuffled);

        for (int i = 0; i < targetActive && i < shuffled.size(); i++) {
            activateSpawnPoint(shuffled.get(i));
        }

        WowCraft.LOGGER.info("Rotated spawn points in region {} ({}/{} active)",
            region, targetActive, rotatable.size());
    }

    /**
     * Update spawn point states based on player distance (lazy spawning).
     * Called every 100 ticks (5 seconds).
     */
    public static void updateSpawnPointStates(ServerLevel level) {
        long currentTick = level.getGameTime();

        for (SpawnPoint point : spawnPointPool.values()) {
            // Get distance to nearest player
            double nearestDistSq = getNearestPlayerDistanceSq(level, point.getPosition());

            // Calculate desired state
            SpawnPointState desiredState = calculateDesiredState(point, nearestDistSq);

            // Transition if needed
            if (point.getState() != desiredState) {
                transitionSpawnPointState(point, desiredState, level, currentTick);
            }

            // Tick respawn timer for active states
            if (point.getState() != SpawnPointState.DORMANT &&
                point.getState() != SpawnPointState.INACTIVE) {
                tickRespawnTimer(point, level, currentTick);
            }
        }
    }

    public static void applyRoadSafeZoneForChunk(ServerLevel level, ChunkPos chunk) {
        List<SpawnPoint> points = getSpawnPointsNearChunk(chunk, 0);
        for (SpawnPoint point : points) {
            if (!SafeZoneDetector.canBeAffectedBySafeZone(point)) {
                continue;
            }

            if (SafeZoneDetector.isOnRoad(level, point.getPosition())) {
                suppressSpawnPoint(point, level);
                continue;
            }

            if (SafeZoneDetector.isNearRoad(level, point.getPosition())) {
                if (point.getHostility() == SpawnHostility.ALWAYS_HOSTILE) {
                    point.setHostility(SpawnHostility.NEUTRAL_DEFENSIVE);
                    List<MobOption> neutralMobs = MobOptionProvider.getNeutralMobs(point.getBiome());
                    point.setMobOptions(neutralMobs);
                }
            }
        }
    }

    /**
     * Get squared distance to nearest player.
     */
    private static double getNearestPlayerDistanceSq(ServerLevel level, BlockPos pos) {
        List<ServerPlayer> players = level.players();

        if (players.isEmpty()) {
            return Double.MAX_VALUE;
        }

        double minDistSq = Double.MAX_VALUE;

        for (ServerPlayer player : players) {
            double distSq = player.blockPosition().distSqr(pos);
            if (distSq < minDistSq) {
                minDistSq = distSq;
            }
        }

        return minDistSq;
    }

    /**
     * Calculate desired state based on distance.
     */
    private static SpawnPointState calculateDesiredState(SpawnPoint point, double nearestPlayerDistSq) {
        // If point is inactive in rotation, stay inactive
        if (!point.isActiveInRotation()) {
            return SpawnPointState.INACTIVE;
        }

        // Check distance thresholds
        int spawnDistSq = SPAWN_ENTITY_DISTANCE * SPAWN_ENTITY_DISTANCE;
        int dormantDistSq = DEACTIVATE_POINT_DISTANCE * DEACTIVATE_POINT_DISTANCE;

        if (nearestPlayerDistSq > dormantDistSq) {
            return SpawnPointState.DORMANT;
        } else if (nearestPlayerDistSq > spawnDistSq) {
            // Virtual: track state but no entities
            return point.hasMobsAlive() ?
                SpawnPointState.VIRTUAL_SPAWNED :
                SpawnPointState.VIRTUAL_DEAD;
        } else {
            // Close enough: spawn actual entities
            return SpawnPointState.ENTITY_SPAWNED;
        }
    }

    /**
     * Transition spawn point to new state.
     */
    private static void transitionSpawnPointState(
            SpawnPoint point,
            SpawnPointState newState,
            ServerLevel level,
            long currentTick) {

        SpawnPointState oldState = point.getState();

        switch (newState) {
            case DORMANT -> transitionToDormant(point, level);
            case VIRTUAL_SPAWNED -> transitionToVirtual(point, level, currentTick);
            case VIRTUAL_DEAD -> transitionToVirtualDead(point, level);
            case ENTITY_SPAWNED -> transitionToEntitySpawned(point, level, currentTick);
            case INACTIVE -> transitionToInactive(point, level);
        }

        point.setState(newState);

        WowCraft.LOGGER.debug("Spawn point {} transitioned {} -> {}",
            point.getId(), oldState, newState);
    }

    private static void transitionToDormant(SpawnPoint point, ServerLevel level) {
        if (point.hasSpawnedEntities()) {
            point.saveVirtualMobState(level);
            unloadEntities(point, level);
        }
        point.pauseRespawnTimer();
    }

    private static void transitionToVirtual(SpawnPoint point, ServerLevel level, long currentTick) {
        if (point.hasSpawnedEntities()) {
            point.saveVirtualMobState(level);
            unloadEntities(point, level);
        }

        if (point.isRespawnTimerPaused()) {
            point.resumeRespawnTimer(currentTick);
        }
    }

    private static void transitionToVirtualDead(SpawnPoint point, ServerLevel level) {
        if (point.hasSpawnedEntities()) {
            unloadEntities(point, level);
        }
    }

    private static void transitionToEntitySpawned(SpawnPoint point, ServerLevel level, long currentTick) {
        // Spawn entities if ready
        if (point.hasVirtualMobs() || point.isRespawnReady(currentTick)) {
            spawnEntitiesAtPoint(point, level);
        }
    }

    private static void transitionToInactive(SpawnPoint point, ServerLevel level) {
        // Don't unload entities, just stop respawning
        point.setRespawnEnabled(false);
    }

    /**
     * Unload entities from a spawn point.
     */
    private static void unloadEntities(SpawnPoint point, ServerLevel level) {
        for (UUID entityId : point.getSpawnedEntityIds()) {
            var entity = level.getEntity(entityId);
            if (entity != null) {
                entity.discard();
            }
        }
        point.clearSpawnedEntityIds();
    }

    private static void suppressSpawnPoint(SpawnPoint point, ServerLevel level) {
        activeSpawnPoints.remove(point.getId());
        point.setActiveInRotation(false);
        point.setRespawnEnabled(false);
        point.setState(SpawnPointState.INACTIVE);

        if (point.hasSpawnedEntities()) {
            unloadEntities(point, level);
        }
    }

    /**
     * Spawn entities at a spawn point.
     */
    private static void spawnEntitiesAtPoint(SpawnPoint point, ServerLevel level) {
        List<UUID> spawnedIds = new ArrayList<>();

        // Check if has virtual mobs (restore from saved state)
        if (point.hasVirtualMobs()) {
            // Restore from virtual state
            for (VirtualMobState virtual : point.getVirtualMobs()) {
                var mob = virtual.spawnEntity(level, point.getPackId());
                if (mob != null) {
                    makePackMobTerritorial(mob, virtual.getPosition());
                    registerMobWithSpawnPoint(mob.getUUID(), point.getId());
                    spawnedIds.add(mob.getUUID());
                }
            }
            point.clearVirtualMobs();
        } else {
            // Fresh spawn - create new mobs
            MobOption mobOption = point.rollMobType();
            int count = mobOption.rollCount(new Random());

            for (int i = 0; i < count; i++) {
                // Offset from spawn point center (6 block spread - tight WoW Classic camp spacing)
                BlockPos spawnPos = point.getPosition().offset(
                    level.random.nextInt(6) - 3,
                    0,
                    level.random.nextInt(6) - 3
                );

                // Adjust to surface
                spawnPos = findSurfaceNear(level, spawnPos);
                if (spawnPos == null) {
                    continue; // Skip if can't find surface
                }

                // Create virtual state and spawn entity
                VirtualMobState virtual = new VirtualMobState(
                    mobOption.mobType(),
                    spawnPos,
                    point.getTargetLevel() + point.getLevelBonus(),
                    null,
                    level.getGameTime()
                );

                var mob = virtual.spawnEntity(level, point.getPackId());
                if (mob != null) {
                    makePackMobTerritorial(mob, spawnPos);
                    registerMobWithSpawnPoint(mob.getUUID(), point.getId());
                    spawnedIds.add(mob.getUUID());
                }
            }
        }

        point.setSpawnedEntityIds(spawnedIds);

        if (spawnedIds.size() > 0) {
            WowCraft.LOGGER.debug("Spawned {} mobs at spawn point {} (type: {}, hostility: {})",
                spawnedIds.size(), point.getId(), point.getType(), point.getHostility());
        }
    }

    /**
     * Find surface position near target.
     */
    private static BlockPos findSurfaceNear(ServerLevel level, BlockPos target) {
        if (!level.isLoaded(target)) {
            return null;
        }

        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            target.getX(), target.getZ());

        if (y >= level.getMinY() && y <= level.getMaxY() - 2) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());
            var below = level.getBlockState(pos.below());
            var at = level.getBlockState(pos);

            if ((below.isSuffocating(level, pos.below()) || below.blocksMotion()) &&
                !at.isSuffocating(level, pos)) {
                return pos;
            }
        }

        return target; // Fallback to original position
    }

    /**
     * Make a pack mob territorial by setting home position and leash.
     */
    private static void makePackMobTerritorial(net.minecraft.world.entity.Mob mob, BlockPos homePos) {
        // Set home position with 12 block leash - WoW Classic style tight camps
        mob.restrictTo(homePos, 12);

        // If this is a custom pack mob, set its home position
        if (mob instanceof com.gianmarco.wowcraft.entity.pack.IPackMob packMob) {
            packMob.setHomePosition(homePos);
        }
    }

    /**
     * Register a mob with its spawn point for tracking.
     */
    private static final java.util.Map<UUID, UUID> mobToSpawnPointMap = new java.util.concurrent.ConcurrentHashMap<>();

    private static void registerMobWithSpawnPoint(UUID mobId, UUID spawnPointId) {
        mobToSpawnPointMap.put(mobId, spawnPointId);
    }

    /**
     * Get spawn point for a mob.
     */
    public static UUID getSpawnPointForMob(UUID mobId) {
        return mobToSpawnPointMap.get(mobId);
    }

    /**
     * Called when a mob dies - update spawn point.
     */
    public static void onMobDeath(UUID mobId, long currentTick) {
        UUID spawnPointId = mobToSpawnPointMap.remove(mobId);
        if (spawnPointId != null) {
            SpawnPoint point = getSpawnPoint(spawnPointId);
            if (point != null) {
                point.onMobKilled(mobId, currentTick);
                WowCraft.LOGGER.debug("Mob {} died from spawn point {}, respawn in {} seconds",
                    mobId, spawnPointId, point.calculateRespawnDelay(0.5f) / 20);
            }
        }
    }

    /**
     * Tick respawn timer.
     */
    private static void tickRespawnTimer(SpawnPoint point, ServerLevel level, long currentTick) {
        if (!point.hasMobsAlive() && point.isRespawnReady(currentTick)) {
            if (point.getState() == SpawnPointState.ENTITY_SPAWNED) {
                // Player nearby: spawn actual entities
                spawnEntitiesAtPoint(point, level);
            } else if (point.getState() == SpawnPointState.VIRTUAL_DEAD) {
                // No player nearby: create virtual mobs (don't spawn entities)
                // Just mark as having virtual mobs
                point.setState(SpawnPointState.VIRTUAL_SPAWNED);
            }
        }
    }

    /**
     * Clear all data (world unload).
     */
    public static void clear() {
        spawnPointPool.clear();
        activeSpawnPoints.clear();
        chunkSpawnMap.clear();
        generatedRegions.clear();
        mobToSpawnPointMap.clear();
        WowCraft.LOGGER.info("Cleared SpawnPoolManager data");
    }

    /**
     * Region position helper.
     */
    public static class RegionPos {
        private final int regionX;
        private final int regionZ;

        public RegionPos(int regionX, int regionZ) {
            this.regionX = regionX;
            this.regionZ = regionZ;
        }

        public static RegionPos fromBlockPos(BlockPos pos, int regionSize) {
            return new RegionPos(
                Math.floorDiv(pos.getX(), regionSize),
                Math.floorDiv(pos.getZ(), regionSize)
            );
        }

        public BlockPos getCenterBlockPos(int regionSize) {
            return new BlockPos(
                regionX * regionSize + regionSize / 2,
                64,
                regionZ * regionSize + regionSize / 2
            );
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RegionPos regionPos = (RegionPos) o;
            return regionX == regionPos.regionX && regionZ == regionPos.regionZ;
        }

        @Override
        public int hashCode() {
            return Objects.hash(regionX, regionZ);
        }

        @Override
        public String toString() {
            return "Region[" + regionX + ", " + regionZ + "]";
        }
    }
}

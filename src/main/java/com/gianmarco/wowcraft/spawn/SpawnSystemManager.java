package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.poi.POIManager;
import com.gianmarco.wowcraft.poi.POISaveData;
import com.gianmarco.wowcraft.poi.PointOfInterest;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import com.gianmarco.wowcraft.zone.ZoneRegion;
import com.gianmarco.wowcraft.zone.ZoneSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Main coordinator for the new spawn system.
 * Uses async/queued generation to prevent world load freezes.
 */
public class SpawnSystemManager {

    private static final Set<Long> processedChunks = ConcurrentHashMap.newKeySet();
    private static final Queue<ChunkPos> pendingChunks = new ConcurrentLinkedQueue<>();
    private static int tickCounter = 0;
    private static boolean initialSpawnGenerated = false;

    /**
     * Called when a chunk loads.
     * QUEUES the chunk for background processing instead of generating immediately.
     * This prevents world load freezes.
     */
    public static void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        // Only in overworld
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        long chunkKey = chunkPos.toLong();
        if (processedChunks.contains(chunkKey)) {
            return; // Already processed or queued
        }

        // Mark as processed immediately to prevent duplicate queueing
        processedChunks.add(chunkKey);

        // Queue for background processing (non-blocking)
        pendingChunks.offer(chunkPos);
    }

    /**
     * Generate spawn points for a region.
     */
    private static void generateRegionSpawnPoints(ServerLevel level, BlockPos regionCenter, BiomeGroup biome) {
        SpawnPoolManager.RegionPos regionPos = SpawnPoolManager.RegionPos.fromBlockPos(regionCenter, 300);

        // Mark region as generated IMMEDIATELY to prevent duplicate generation
        SpawnPoolManager.markRegionGenerated(regionCenter);

        Random random = new Random(regionPos.hashCode() ^ level.getSeed());

        WowCraft.LOGGER.info("Generating spawn points for region {} (biome: {})", regionPos, biome);

        WowCraft.LOGGER.debug("Step 1: Getting zone data");
        // Get zone data for level calculation
        ZoneSaveData zoneSaveData = ZoneSaveData.get(level);
        ZoneRegion zone = zoneSaveData.getZone(biome);
        int baseLevel = zone != null ? zone.suggestedLevelMin() : 1;

        WowCraft.LOGGER.debug("Step 2: Getting POI system");
        // Get POI system
        POISaveData poiSaveData = POISaveData.get(level);
        POIManager poiManager = poiSaveData.getManager();

        WowCraft.LOGGER.debug("Step 3: Checking existing POIs");
        // Get or generate POIs
        ChunkPos regionChunk = new ChunkPos(regionCenter);
        List<PointOfInterest> pois = poiManager.getPOIsNearChunk(regionChunk);
        if (pois.isEmpty()) {
            WowCraft.LOGGER.debug("Step 4: Generating new POIs");
            // No POIs yet, generate them (existing POI generation)
            pois = com.gianmarco.wowcraft.poi.POIGenerator.generatePOIsForRegion(
                level, regionCenter, level.getSeed(), biome);

            WowCraft.LOGGER.debug("Step 5: Adding {} POIs to manager", pois.size());
            for (PointOfInterest poi : pois) {
                poiManager.addPOI(poi);
            }
            poiManager.markRegionGenerated(regionCenter);
        }

        WowCraft.LOGGER.debug("Step 6: Converting {} POIs to spawn points", pois.size());
        // Convert POIs to spawn points
        for (PointOfInterest poi : pois) {
            List<SpawnPoint> poiSpawnPoints = POISpawnPointGenerator.generateSpawnPointsForPOI(
                poi, biome, baseLevel, level);

            for (SpawnPoint point : poiSpawnPoints) {
                SpawnPoolManager.addSpawnPoint(point);
            }
        }

        // Visualize POIs if debug enabled
        POIDebugVisualizer.visualizePOIs(level, pois);

        WowCraft.LOGGER.debug("Step 7: Generating scatter spawn points");
        // Generate scatter spawn points
        boolean isNearSpawn = regionCenter.distSqr(level.getSharedSpawnPos()) < 500 * 500;
        List<SpawnPoint> scatterPoints = ScatterSpawnGenerator.generateScatterSpawns(
            level, regionCenter, 300, biome, isNearSpawn, random);

        WowCraft.LOGGER.debug("Step 8: Generated {} scatter points", scatterPoints.size());

        WowCraft.LOGGER.debug("Step 9: Applying safe zone modifiers");
        // Apply safe zone modifiers
        for (SpawnPoint point : scatterPoints) {
            SafeZoneDetector.applySafeZoneModifiers(point, level);
        }

        WowCraft.LOGGER.debug("Step 10: Adding scatter points to pool");
        // Add scatter points to pool
        for (SpawnPoint point : scatterPoints) {
            SpawnPoolManager.addSpawnPoint(point);
        }

        WowCraft.LOGGER.debug("Step 11: Activating spawn points");
        // Activate spawn points (75% of scatter points)
        SpawnPoolManager.activateRegionSpawnPoints(regionPos, 0.75f);

        WowCraft.LOGGER.info("Generated spawn system for region {} - {} POI spawns, {} scatter spawns",
            regionPos, pois.size() * 3, scatterPoints.size());
    }

    /**
     * Called every server tick.
     * Processes queued chunks in background and updates spawn point states.
     */
    public static void onServerTick(ServerLevel level) {
        tickCounter++;

        // Process pending chunks gradually (1-2 regions per tick to avoid lag)
        processPendingChunks(level);

        // Generate spawn points for spawn area after world finishes loading (200 ticks = 10 seconds)
        if (!initialSpawnGenerated && tickCounter > 200) {
            initialSpawnGenerated = true;
            generateInitialSpawnArea(level);
        }

        // Update spawn point states every 100 ticks (5 seconds)
        if (tickCounter % SpawnPoolManager.CHECK_INTERVAL_TICKS == 0) {
            SpawnPoolManager.updateSpawnPointStates(level);
        }

        // TODO: Implement rotation timer (every 5-10 minutes)
        // TODO: Implement hyperspawn (based on player activity)
    }

    /**
     * Process pending chunks in background (non-blocking).
     * Processes up to 2 regions per tick to avoid lag spikes.
     */
    private static void processPendingChunks(ServerLevel level) {
        int processed = 0;
        int maxPerTick = 2; // Process max 2 regions per tick

        while (processed < maxPerTick && !pendingChunks.isEmpty()) {
            ChunkPos chunkPos = pendingChunks.poll();
            if (chunkPos == null) break;

            // Only process if chunk is still loaded
            BlockPos chunkCenter = chunkPos.getMiddleBlockPosition(64);
            if (!level.isLoaded(chunkCenter)) {
                continue; // Skip unloaded chunks
            }

            // Get biome and check if valid
            Holder<Biome> biomeHolder = level.getBiome(chunkCenter);
            ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
            BiomeGroup group = BiomeGroup.fromBiome(biomeKey);

            if (group == null || !group.isNameable()) {
                continue; // Skip non-nameable zones
            }

            // Check if region already has spawn points
            if (SpawnPoolManager.isRegionGenerated(chunkCenter)) {
                continue; // Already generated
            }

            // Generate spawn points for this region (async, in server tick)
            generateRegionSpawnPoints(level, chunkCenter, group);
            processed++;
        }
    }

    /**
     * Generate spawn points for the initial spawn area after world load completes.
     */
    private static void generateInitialSpawnArea(ServerLevel level) {
        BlockPos spawnPos = level.getSharedSpawnPos();

        // Generate spawn points for a 3x3 region grid around spawn (900x900 blocks)
        for (int rx = -1; rx <= 1; rx++) {
            for (int rz = -1; rz <= 1; rz++) {
                BlockPos regionCenter = spawnPos.offset(rx * 300, 0, rz * 300);

                // Only generate if chunk is loaded
                if (level.isLoaded(regionCenter)) {
                    Holder<Biome> biomeHolder = level.getBiome(regionCenter);
                    ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
                    BiomeGroup group = BiomeGroup.fromBiome(biomeKey);

                    if (group != null && group.isNameable() && !SpawnPoolManager.isRegionGenerated(regionCenter)) {
                        generateRegionSpawnPoints(level, regionCenter, group);
                    }
                }
            }
        }

        WowCraft.LOGGER.info("Initial spawn area generation complete");
    }

    /**
     * Called when a mob dies.
     * Update spawn point respawn timer.
     */
    public static void onMobDeath(net.minecraft.world.entity.LivingEntity mob, ServerLevel level) {
        if (mob instanceof net.minecraft.world.entity.Mob) {
            SpawnPoolManager.onMobDeath(mob.getUUID(), level.getGameTime());
        }
    }

    /**
     * Clear all data (world unload).
     */
    public static void clear() {
        processedChunks.clear();
        pendingChunks.clear();
        SpawnPoolManager.clear();
        tickCounter = 0;
        initialSpawnGenerated = false;
        tickCounter = 0;
        WowCraft.LOGGER.info("Cleared SpawnSystemManager data");
    }
}

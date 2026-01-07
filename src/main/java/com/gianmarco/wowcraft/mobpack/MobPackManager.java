package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import com.gianmarco.wowcraft.poi.POIGenerator;
import com.gianmarco.wowcraft.poi.POIManager;
import com.gianmarco.wowcraft.poi.POISaveData;
import com.gianmarco.wowcraft.poi.PointOfInterest;
import com.gianmarco.wowcraft.spawn.SpawnSystemManager;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import com.gianmarco.wowcraft.zone.ZoneRegion;
import com.gianmarco.wowcraft.zone.ZoneSaveData;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Core manager for mob pack lifecycle.
 * Handles pack generation, respawning, persistence, and social aggro lookups.
 */
public class MobPackManager {

    /** Probability of a pack spawning in a chunk (1 in N) */
    private static final int PACK_SPAWN_CHANCE = 20; // ~5% chance per chunk (reduced to prevent overcrowding)

    /** Minimum distance between pack centers (blocks) */
    private static final int MIN_PACK_DISTANCE = 24; // Increased spacing to reduce density

    /** Maximum distance between pack centers to form a cluster */
    private static final int CLUSTER_DISTANCE = 48; // Packs within 48 blocks = same cluster (increased range)

    /** Bonus chance for pack to spawn near existing pack (cluster formation) */
    private static final float CLUSTER_BONUS_CHANCE = 0.4f; // 40% bonus if near existing pack (reduced from 60%)

    /** Tracks which chunks have been processed for pack spawning */
    private static final Set<Long> processedChunks = ConcurrentHashMap.newKeySet();

    /** All spawned packs by pack ID */
    private static final Map<UUID, SpawnedMobPack> allPacks = new ConcurrentHashMap<>();

    /** Pack lookup by mob entity ID (for social aggro) */
    private static final Map<UUID, UUID> mobToPackMap = new ConcurrentHashMap<>();

    /** Spatial map for fast collision checks (ChunkPos long -> List of packs) */
    private static final Map<Long, List<SpawnedMobPack>> chunkPackMap = new ConcurrentHashMap<>();

    /** World key for cleanup */
    private static String currentWorldKey = null;

    /** Buffer for chunks to be processed gradually */
    private static final Set<Long> pendingChunks = ConcurrentHashMap.newKeySet();

    /**
     * Called on chunk load to potentially spawn a pack.
     * Now just buffers chunks for background processing to avoid lag.
     */
    public static void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
        // NEW SPAWN SYSTEM: Queue chunks for async processing (non-blocking)
        SpawnSystemManager.onChunkLoad(level, chunkPos);

        // Only in overworld
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        // Always buffer chunks - they'll be processed gradually in the background
        long chunkKey = chunkPos.toLong();
        if (!processedChunks.contains(chunkKey)) {
            pendingChunks.add(chunkKey);
        }
    }

    /**
     * Process pending chunks gradually to avoid lag spikes.
     * Called from server tick.
     */
    private static void processPendingChunks(ServerLevel level) {
        if (pendingChunks.isEmpty()) {
            return;
        }

        // Only spawn packs if players are in the world (defer during initial world gen)
        if (level.players().isEmpty()) {
            return;
        }

        // Process up to 1 chunk per tick to avoid lag (reduced from 2)
        int processedThisTick = 0;
        Iterator<Long> iterator = pendingChunks.iterator();

        while (iterator.hasNext() && processedThisTick < 1) {
            long chunkKey = iterator.next();
            iterator.remove();

            // Skip if already processed
            if (processedChunks.contains(chunkKey)) {
                continue;
            }

            processChunkForPackSpawn(level, new ChunkPos(chunkKey));
            processedThisTick++;
        }
    }

    /**
     * Actually processes a chunk for pack spawning.
     * Now uses POI system for structured spawning.
     */
    private static void processChunkForPackSpawn(ServerLevel level, ChunkPos chunkPos) {
        // Check if already processed
        long chunkKey = chunkPos.toLong();

        // Get biome group at chunk center
        BlockPos chunkCenter = chunkPos.getMiddleBlockPosition(64);
        Holder<Biome> biomeHolder = level.getBiome(chunkCenter);
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
        BiomeGroup group = BiomeGroup.fromBiome(biomeKey);

        if (group == null || !group.isNameable()) {
            processedChunks.add(chunkKey);
            return; // Not a nameable zone
        }

        // Check if zone is discovered
        ZoneSaveData zoneSaveData = ZoneSaveData.get(level);
        ZoneRegion zone = zoneSaveData.getZone(group);

        // Get POI system
        POISaveData poiSaveData = POISaveData.get(level);
        POIManager poiManager = poiSaveData.getManager();

        // Check if POIs have been generated for this region
        if (!poiManager.isRegionGenerated(chunkCenter)) {
            // Generate POIs for this region
            POIManager.RegionPos regionPos = POIManager.RegionPos.fromBlockPos(chunkCenter, 300);
            BlockPos regionCenter = regionPos.getCenterBlockPos(300);

            WowCraft.LOGGER.debug("Generating POIs for region at {} (biome: {})", regionCenter, group);

            List<PointOfInterest> newPOIs = POIGenerator.generatePOIsForRegion(
                    level, regionCenter, level.getSeed(), group);

            // Add POIs to manager
            for (PointOfInterest poi : newPOIs) {
                poiManager.addPOI(poi);
            }

            // Mark region as generated
            poiManager.markRegionGenerated(regionCenter);

            // Save POI data (async to avoid blocking)
            poiSaveData.save();

            WowCraft.LOGGER.debug("Generated and saved {} POIs for region {}", newPOIs.size(), regionPos);
        }

        // Get POIs near this chunk
        List<PointOfInterest> nearbyPOIs = poiManager.getPOIsNearChunk(chunkPos);

        // Mark as processed
        processedChunks.add(chunkKey);

        // OLD MOB PACK SPAWNING - DISABLED IN FAVOR OF NEW SPAWN SYSTEM
        // The new SpawnSystemManager now handles all mob spawning
        // This old code was causing double-spawning and excessive mob density
        /*
        // Spawn packs at POI locations (limit to 1 pack per chunk processing to avoid lag)
        Random random = new Random(chunkKey ^ level.getSeed());
        int spawnsThisChunk = 0;
        final int MAX_SPAWNS_PER_CHUNK = 1;

        for (PointOfInterest poi : nearbyPOIs) {
            if (spawnsThisChunk >= MAX_SPAWNS_PER_CHUNK) {
                break; // Limit spawns per chunk processing
            }

            // Check if this POI already has packs spawned
            if (!poi.getAssignedPackIds().isEmpty()) {
                continue; // Already has packs
            }

            // Get spawn positions from POI
            List<BlockPos> spawnPositions = poi.getSpawnPositions();

            for (BlockPos spawnPos : spawnPositions) {
                if (spawnsThisChunk >= MAX_SPAWNS_PER_CHUNK) {
                    break; // Limit spawns
                }

                // Get a template for this zone
                MobPackTemplate template = MobPackTemplateLoader.getRandomTemplateForZone(group, random);
                if (template == null) {
                    WowCraft.LOGGER.debug("No pack templates for zone {}", group);
                    continue;
                }

                // Adjust Y coordinate to surface
                BlockPos surfacePos = findSurfaceNear(level, spawnPos);
                if (surfacePos == null) {
                    WowCraft.LOGGER.debug("Could not find surface near POI spawn position {}", spawnPos);
                    continue;
                }

                // Generate the pack
                SpawnedMobPack pack = generatePack(level, template, surfacePos, zone, random);
                if (pack != null) {
                    registerPack(pack);
                    pack.spawnReadyMobs(level, level.getGameTime());

                    // Link pack to POI
                    poi.assignPack(pack.getPackId());

                    String zoneName = zone != null ? zone.assignedName() : "Unknown (" + group.name() + ")";
                    WowCraft.LOGGER.info("Spawned {} pack at {} from {} POI (zone: {}, level: {})",
                            template.id(), surfacePos, poi.getType(), zoneName, pack.getTargetLevel());

                    spawnsThisChunk++;
                }
            }
        }
        */
    }

    /**
     * Find surface position near a target position (for POI spawning).
     */
    @Nullable
    private static BlockPos findSurfaceNear(ServerLevel level, BlockPos target) {
        // Try the exact position first
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, target.getX(), target.getZ());

        // Validate Y is within world bounds
        if (y >= level.getMinY() && y <= level.getMaxY() - 2) {
            BlockPos pos = new BlockPos(target.getX(), y, target.getZ());

            // Quick check: is there a solid block below and air above?
            BlockState below = level.getBlockState(pos.below());
            BlockState at = level.getBlockState(pos);

            if ((below.isSuffocating(level, pos.below()) || below.blocksMotion()) &&
                !at.isSuffocating(level, pos)) {
                return pos;
            }
        }

        // If exact position fails, try nearby positions
        Random random = new Random(target.asLong());
        for (int attempt = 0; attempt < 5; attempt++) {
            int offsetX = random.nextInt(10) - 5;
            int offsetZ = random.nextInt(10) - 5;

            int x = target.getX() + offsetX;
            int z = target.getZ() + offsetZ;
            y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            if (y >= level.getMinY() && y <= level.getMaxY() - 2) {
                BlockPos pos = new BlockPos(x, y, z);
                BlockState below = level.getBlockState(pos.below());
                BlockState at = level.getBlockState(pos);

                if ((below.isSuffocating(level, pos.below()) || below.blocksMotion()) &&
                    !at.isSuffocating(level, pos)) {
                    return pos;
                }
            }
        }

        return null;
    }

    /**
     * Find a valid surface position in the chunk for a pack (legacy, for fallback).
     */
    @Nullable
    private static BlockPos findPackSpawnPosition(ServerLevel level, ChunkPos chunkPos, Random random) {
        // Check if chunk is loaded
        if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
            WowCraft.LOGGER.debug("Chunk {} not loaded, skipping pack spawn", chunkPos);
            return null;
        }

        // Try a few random positions in the chunk
        for (int attempt = 0; attempt < 10; attempt++) {
            int x = chunkPos.getMinBlockX() + random.nextInt(16);
            int z = chunkPos.getMinBlockZ() + random.nextInt(16);

            // Get surface Y
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);

            // Validate Y is within world bounds
            if (y >= level.getMinY() && y <= level.getMaxY() - 2) {
                BlockPos pos = new BlockPos(x, y, z);

                // Quick check: is there a solid block below and air above?
                BlockState below = level.getBlockState(pos.below());
                BlockState at = level.getBlockState(pos);

                if ((below.isSuffocating(level, pos.below()) || below.blocksMotion()) &&
                    !at.isSuffocating(level, pos)) {
                    return pos;
                }
            }
        }

        WowCraft.LOGGER.debug("Could not find valid pack center in chunk {} after 10 attempts", chunkPos);
        return null;
    }

    /**
     * Check if there's a pack within a certain distance of a chunk.
     * Used for clustering - spawns more packs near existing ones.
     */
    private static boolean hasPackWithinDistance(ChunkPos chunkPos, int distance) {
        BlockPos chunkCenter = chunkPos.getMiddleBlockPosition(64);
        double distSq = distance * distance;

        // Check chunks in a radius
        int chunkRadius = (distance / 16) + 1;
        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                long chunkKey = ChunkPos.asLong(chunkPos.x + x, chunkPos.z + z);
                List<SpawnedMobPack> packsInChunk = chunkPackMap.get(chunkKey);

                if (packsInChunk != null) {
                    for (SpawnedMobPack pack : packsInChunk) {
                        if (pack.getCenterPos().distSqr(chunkCenter) < distSq) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    /**
     * Check if a pack location is valid (minimum distance from other packs).
     * Uses spatial map to check only relevant chunks.
     */
    private static boolean isValidPackLocation(BlockPos center) {
        double minDistSq = MIN_PACK_DISTANCE * MIN_PACK_DISTANCE;
        ChunkPos centerChunk = new ChunkPos(center);

        // Check 3x3 chunks around the center
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                long chunkKey = ChunkPos.asLong(centerChunk.x + x, centerChunk.z + z);
                List<SpawnedMobPack> packsInChunk = chunkPackMap.get(chunkKey);

                if (packsInChunk != null) {
                    for (SpawnedMobPack pack : packsInChunk) {
                        if (pack.getCenterPos().distSqr(center) < minDistSq) {
                            return false;
                        }
                    }
                }
            }
        }
        return true;
    }

    /**
     * Generate a new pack from a template.
     */
    private static SpawnedMobPack generatePack(ServerLevel level, MobPackTemplate template,
            BlockPos center, @Nullable ZoneRegion zone, Random random) {
        // Roll target level
        int targetLevel;
        if (zone != null) {
            // Use zone range
            int levelMin = zone.suggestedLevelMin();
            int levelMax = zone.suggestedLevelMax();
            targetLevel = levelMin + random.nextInt(Math.max(1, levelMax - levelMin + 1));
        } else {
            // Fallback: Distance based leveling (approx. 150 blocks per level)
            double dist = Math.sqrt(center.distSqr(level.getSharedSpawnPos()));
            targetLevel = 1 + (int) (dist / 150.0);
            targetLevel = Math.min(60, Math.max(1, targetLevel));
        }

        SpawnedMobPack pack = new SpawnedMobPack(
                UUID.randomUUID(),
                template.id(),
                center,
                targetLevel,
                template.socialAggroRadius(),
                template.respawnDelaySeconds(),
                zone != null ? zone.biomeGroup() : BiomeGroup.PLAINS); // Fallback biome group if null (shouldn't be,
                                                                       // but safe)

        // Roll pack size
        int packSize = template.rollPackSize(random);

        // Generate mob slots using weighted selection
        int totalWeight = template.mobPool().stream().mapToInt(MobEntry::weight).sum();

        for (int i = 0; i < packSize; i++) {
            // Weighted random selection from mob pool
            int roll = random.nextInt(totalWeight);
            int cumulative = 0;
            MobEntry selected = template.mobPool().get(0);

            for (MobEntry entry : template.mobPool()) {
                cumulative += entry.weight();
                if (roll < cumulative) {
                    selected = entry;
                    break;
                }
            }

            // Random offset from pack center (within 8 blocks for better spread)
            int offsetX = random.nextInt(17) - 8;
            int offsetZ = random.nextInt(17) - 8;
            BlockPos mobPos = center.offset(offsetX, 0, offsetZ);

            // Adjust to surface
            int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    mobPos.getX(), mobPos.getZ());
            mobPos = new BlockPos(mobPos.getX(), surfaceY, mobPos.getZ());

            pack.addMob(new SpawnedMob(selected.mobType(), mobPos));
        }

        return pack;
    }

    /**
     * Register a pack in the manager.
     */
    public static void registerPack(SpawnedMobPack pack) {
        allPacks.put(pack.getPackId(), pack);

        // Add to spatial map
        ChunkPos chunkPos = new ChunkPos(pack.getCenterPos());
        chunkPackMap.computeIfAbsent(chunkPos.toLong(), k -> new java.util.concurrent.CopyOnWriteArrayList<>())
                .add(pack);
    }

    /**
     * Called on server tick to handle respawns and process pending chunks.
     */
    public static void onServerTick(ServerLevel level) {
        long currentTick = level.getGameTime();

        // NEW SPAWN SYSTEM: Process queued spawn generation
        SpawnSystemManager.onServerTick(level);

        // Process pending chunks gradually (every tick, up to 5 chunks)
        processPendingChunks(level);

        // Only check respawns every 20 ticks (1 second)
        if (currentTick % 20 != 0) {
            return;
        }

        for (SpawnedMobPack pack : allPacks.values()) {
            pack.spawnReadyMobs(level, currentTick);
        }
    }

    /**
     * Called when a mob dies to track respawn.
     */
    public static void onMobDeath(LivingEntity mob, ServerLevel level) {
        // NEW SPAWN SYSTEM: Track mob death for respawns
        SpawnSystemManager.onMobDeath(mob, level);

        MobData data = mob.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data == null || data.packId() == null) {
            return; // Not a pack mob
        }

        UUID packId = data.packId();
        SpawnedMobPack pack = allPacks.get(packId);
        if (pack != null) {
            pack.onMobDeath(mob.getUUID(), level.getGameTime());
            mobToPackMap.remove(mob.getUUID());
        }
    }

    /**
     * Called when a pack mob spawns to register in lookup map.
     */
    public static void onPackMobSpawned(UUID mobId, UUID packId) {
        mobToPackMap.put(mobId, packId);
    }

    /**
     * Get the pack a mob belongs to.
     */
    @Nullable
    public static SpawnedMobPack getPackForMob(UUID mobId) {
        UUID packId = mobToPackMap.get(mobId);
        if (packId == null)
            return null;
        return allPacks.get(packId);
    }

    /**
     * Get a pack by ID.
     */
    @Nullable
    public static SpawnedMobPack getPack(UUID packId) {
        return allPacks.get(packId);
    }

    /**
     * Clear all data (on world unload).
     */
    public static void clear() {
        // NEW SPAWN SYSTEM: Clear spawn data
        SpawnSystemManager.clear();

        processedChunks.clear();
        allPacks.clear();
        mobToPackMap.clear();
        chunkPackMap.clear();
        pendingChunks.clear();
        currentWorldKey = null;
        WowCraft.LOGGER.info("Cleared MobPackManager data");
    }

    /**
     * Get all packs (for persistence).
     */
    public static Collection<SpawnedMobPack> getAllPacks() {
        return allPacks.values();
    }

    /**
     * Load packs from save data.
     */
    public static void loadPacks(List<SpawnedMobPack> packs) {
        // Clear processed chunks when loading saved packs
        // This allows chunks to be re-evaluated for spawning
        processedChunks.clear();

        for (SpawnedMobPack pack : packs) {
            registerPack(pack); // Handles both allPacks and chunkPackMap

            // Mark this pack's chunk as processed to prevent duplicate spawning
            ChunkPos chunkPos = new ChunkPos(pack.getCenterPos());
            processedChunks.add(chunkPos.toLong());

            // Rebuild mob lookup map
            for (SpawnedMob mob : pack.getMobs()) {
                if (mob.isAlive() && mob.getEntityId() != null) {
                    mobToPackMap.put(mob.getEntityId(), pack.getPackId());
                }
            }
        }
        WowCraft.LOGGER.info("Loaded {} packs from save data", packs.size());
    }

    // === Serialization helpers ===

    public static JsonArray toJson() {
        JsonArray array = new JsonArray();
        for (SpawnedMobPack pack : allPacks.values()) {
            array.add(pack.toJson());
        }
        return array;
    }

    public static void fromJson(JsonArray array) {
        List<SpawnedMobPack> packs = new ArrayList<>();
        for (var element : array) {
            try {
                packs.add(SpawnedMobPack.fromJson(element.getAsJsonObject()));
            } catch (Exception e) {
                WowCraft.LOGGER.warn("Failed to load pack: {}", e.getMessage());
            }
        }
        loadPacks(packs);
    }
}

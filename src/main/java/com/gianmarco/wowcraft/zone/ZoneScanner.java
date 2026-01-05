package com.gianmarco.wowcraft.zone;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.status.ChunkStatus;

import java.util.*;

/**
 * Scans the world to detect zone regions.
 * Uses flood-fill algorithm to group contiguous biome chunks into regions.
 */
public class ZoneScanner {

    /** Scan chunk step size (every N chunks to optimize performance) */
    private static final int SCAN_STEP = 4;

    /**
     * Scans the world around spawn to detect and register zone regions.
     * Should be called on world load.
     */
    public static void scanWorld(ServerLevel level) {
        WowCraft.LOGGER.info("Starting zone scan around spawn...");
        long startTime = System.currentTimeMillis();

        BlockPos spawn = level.getSharedSpawnPos();
        int scanRadius = ZoneRegion.MAX_NAMED_DISTANCE;
        int scanRadiusChunks = scanRadius / 16;

        // Track which chunks have been visited
        Set<Long> visited = new HashSet<>();

        // Discovered regions
        List<ZoneRegion> regions = new ArrayList<>();

        // Scan in a grid pattern
        int spawnChunkX = spawn.getX() >> 4;
        int spawnChunkZ = spawn.getZ() >> 4;

        for (int dx = -scanRadiusChunks; dx <= scanRadiusChunks; dx += SCAN_STEP) {
            for (int dz = -scanRadiusChunks; dz <= scanRadiusChunks; dz += SCAN_STEP) {
                int chunkX = spawnChunkX + dx;
                int chunkZ = spawnChunkZ + dz;
                long chunkKey = chunkKey(chunkX, chunkZ);

                if (visited.contains(chunkKey))
                    continue;

                // Get biome at this chunk
                BiomeGroup group = getBiomeGroupAt(level, chunkX, chunkZ);
                if (group == null || !group.isNameable()) {
                    visited.add(chunkKey);
                    continue;
                }

                // Flood-fill to find entire region
                ZoneRegion region = floodFillRegion(level, chunkX, chunkZ, group, visited, spawn);
                if (region != null && region.isLargeEnough()) {
                    regions.add(region);
                }
            }
        }

        // Sort regions by distance from spawn
        regions.sort(Comparator.comparingInt(ZoneRegion::distanceFromSpawn));

        // Assign names based on distance ranking
        Map<BiomeGroup, Integer> groupCounters = new EnumMap<>(BiomeGroup.class);
        for (ZoneRegion region : regions) {
            int index = groupCounters.getOrDefault(region.biomeGroup(), 0);
            groupCounters.put(region.biomeGroup(), index + 1);

            // Calculate level range based on distance
            int levelMin = calculateLevelMin(region.distanceFromSpawn());
            int levelMax = levelMin + 5;
            ZoneRegion withLevels = region.withLevelRange(levelMin, levelMax);

            // Assign name from pool
            ZoneRegion namedRegion = ZoneRegistry.assignName(withLevels);
            ZoneRegistry.registerZone(namedRegion);

            WowCraft.LOGGER.info("Registered zone: {} ({}) at distance {} - Level {}-{}",
                    namedRegion.assignedName(),
                    namedRegion.biomeGroup(),
                    namedRegion.distanceFromSpawn(),
                    namedRegion.suggestedLevelMin(),
                    namedRegion.suggestedLevelMax());
        }

        long elapsed = System.currentTimeMillis() - startTime;
        WowCraft.LOGGER.info("Zone scan complete. Found {} zones in {}ms", regions.size(), elapsed);
    }

    /**
     * Flood-fills from a starting chunk to find all contiguous chunks of the same
     * biome group.
     */
    private static ZoneRegion floodFillRegion(ServerLevel level, int startX, int startZ,
            BiomeGroup targetGroup, Set<Long> visited, BlockPos spawn) {
        Queue<long[]> queue = new LinkedList<>();
        queue.add(new long[] { startX, startZ });

        List<int[]> regionChunks = new ArrayList<>();
        int minX = startX, maxX = startX, minZ = startZ, maxZ = startZ;

        while (!queue.isEmpty()) {
            long[] pos = queue.poll();
            int cx = (int) pos[0];
            int cz = (int) pos[1];
            long key = chunkKey(cx, cz);

            if (visited.contains(key))
                continue;
            visited.add(key);

            BiomeGroup group = getBiomeGroupAt(level, cx, cz);
            if (group != targetGroup)
                continue;

            regionChunks.add(new int[] { cx, cz });
            minX = Math.min(minX, cx);
            maxX = Math.max(maxX, cx);
            minZ = Math.min(minZ, cz);
            maxZ = Math.max(maxZ, cz);

            // Add neighbors (limit search distance to prevent runaway)
            if (regionChunks.size() < 10000) {
                queue.add(new long[] { cx + 1, cz });
                queue.add(new long[] { cx - 1, cz });
                queue.add(new long[] { cx, cz + 1 });
                queue.add(new long[] { cx, cz - 1 });
            }
        }

        if (regionChunks.isEmpty())
            return null;

        // Calculate center
        int centerX = ((minX + maxX) / 2) * 16 + 8;
        int centerZ = ((minZ + maxZ) / 2) * 16 + 8;
        BlockPos center = new BlockPos(centerX, level.getSeaLevel(), centerZ);

        // Calculate distance from spawn
        int distance = (int) Math.sqrt(center.distSqr(spawn));

        return new ZoneRegion(
                UUID.randomUUID(),
                targetGroup,
                center,
                regionChunks.size(),
                distance,
                null,
                null,
                1,
                10);
    }

    /**
     * Gets the BiomeGroup at a chunk position.
     */
    private static BiomeGroup getBiomeGroupAt(ServerLevel level, int chunkX, int chunkZ) {
        try {
            // Use chunk access to get biome without generating new chunks
            ChunkAccess chunk = level.getChunk(chunkX, chunkZ, ChunkStatus.BIOMES, false);
            if (chunk == null)
                return null;

            // Sample biome at chunk center
            int x = chunkX * 16 + 8;
            int z = chunkZ * 16 + 8;
            int y = level.getSeaLevel();

            Holder<Biome> biomeHolder = level.getBiome(new BlockPos(x, y, z));
            ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);

            return BiomeGroup.fromBiome(biomeKey);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Calculates suggested minimum level based on distance from spawn.
     */
    private static int calculateLevelMin(int distance) {
        // Level 1-10: 0-1000 blocks
        // Level 10-20: 1000-2500 blocks
        // Level 20-30: 2500-4000 blocks
        // Level 30-40: 4000-6000 blocks
        // Level 40-50: 6000-8000 blocks
        if (distance < 1000)
            return 1;
        if (distance < 2500)
            return 10;
        if (distance < 4000)
            return 20;
        if (distance < 6000)
            return 30;
        return 40;
    }

    private static long chunkKey(int x, int z) {
        return ((long) x << 32) | (z & 0xFFFFFFFFL);
    }
}

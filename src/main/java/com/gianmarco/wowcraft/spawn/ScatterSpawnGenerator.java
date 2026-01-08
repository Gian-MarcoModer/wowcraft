package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Generates scatter spawn points to fill gaps between POIs.
 * Creates ambient density for the world.
 */
public class ScatterSpawnGenerator {

    private static final int SCATTER_SPACING_MIN = 80;  // Minimum distance between scatter points (wider spread)
    private static final int SCATTER_SPACING_MAX = 120;  // Target spacing (much more spread out)
    private static final int MIN_DISTANCE_FROM_POI = 80;  // Don't overlap camps

    /**
     * Generate scatter spawn points for a region.
     */
    public static List<SpawnPoint> generateScatterSpawns(
            ServerLevel level,
            BlockPos regionCenter,
            int regionSize,
            BiomeGroup biome,
            boolean isNearPlayerBase,
            Random random) {

        List<SpawnPoint> scatterPoints = new ArrayList<>();

        // Target density: 10-15 spawn points per 300x300 region (matches WoW Classic density)
        int targetPoints = 10 + random.nextInt(6);
        int attempts = targetPoints * 3;  // Try 3x to hit target

        WowCraft.LOGGER.debug("Generating scatter spawns for region at {} (target: {})",
            regionCenter, targetPoints);

        for (int i = 0; i < attempts && scatterPoints.size() < targetPoints; i++) {
            // Random position in region
            int offsetX = random.nextInt(regionSize) - regionSize / 2;
            int offsetZ = random.nextInt(regionSize) - regionSize / 2;
            BlockPos testPos = regionCenter.offset(offsetX, 0, offsetZ);

            // Find surface
            BlockPos surfacePos = findSurfacePos(level, testPos);
            if (surfacePos == null) {
                continue;
            }

            // Check spacing from other scatter points
            if (!isValidScatterLocation(level, surfacePos, scatterPoints)) {
                continue;
            }

            // Determine hostility for this spawn point
            SpawnHostility hostility = rollHostility(isNearPlayerBase, random);

            // Get mob options for this hostility type
            List<MobOption> mobOptions = MobOptionProvider.getMobOptions(biome, hostility);

            // Roll how many mobs at this scatter point (1-3, weighted toward 1-2)
            int mobCount = rollScatterMobCount(random);

            // Create scatter spawn point
            SpawnPoint scatter = new SpawnPoint(
                UUID.randomUUID(),
                surfacePos,
                SpawnPointType.SCATTER,
                biome,
                hostility,
                mobOptions,
                1,
                mobCount
            );

            // Set level based on region (will be calculated properly later)
            scatter.setTargetLevel(calculateRegionLevel(regionCenter));

            scatterPoints.add(scatter);
        }

        WowCraft.LOGGER.info("Generated {} scatter spawn points for region at {}",
            scatterPoints.size(), regionCenter);

        return scatterPoints;
    }

    /**
     * Roll hostility type based on location.
     */
    private static SpawnHostility rollHostility(boolean nearPlayerBase, Random random) {
        if (nearPlayerBase) {
            // Near player bases: more neutral (no passive - vanilla handles that)
            int roll = random.nextInt(100);
            if (roll < 30) return SpawnHostility.ALWAYS_HOSTILE;      // 30%
            return SpawnHostility.NEUTRAL_DEFENSIVE;                   // 70%
        } else {
            // Wilderness: more hostile (no passive - vanilla handles that)
            int roll = random.nextInt(100);
            if (roll < 60) return SpawnHostility.ALWAYS_HOSTILE;      // 60%
            return SpawnHostility.NEUTRAL_DEFENSIVE;                   // 40%
        }
    }

    /**
     * Roll how many mobs spawn at this scatter point.
     */
    private static int rollScatterMobCount(Random random) {
        int roll = random.nextInt(100);
        if (roll < 70) return 1;      // 70% chance: solo mob
        if (roll < 95) return 2;      // 25% chance: pair
        return 3;                     // 5% chance: trio
    }

    /**
     * Check if position is valid for scatter spawn.
     */
    private static boolean isValidScatterLocation(
            ServerLevel level,
            BlockPos pos,
            List<SpawnPoint> existing) {

        if (SafeZoneDetector.isOnRoad(level, pos)) {
            return false;
        }

        // Check distance from other scatter points
        int minDistSq = SCATTER_SPACING_MIN * SCATTER_SPACING_MIN;

        for (SpawnPoint point : existing) {
            double distSq = point.getPosition().distSqr(pos);
            if (distSq < minDistSq) {
                return false;  // Too close to another scatter spawn
            }
        }

        return true;
    }

    /**
     * Check if position is too close to POI.
     * POIs will be checked separately when integrating.
     */
    public static boolean isTooCloseToAnyPOI(BlockPos pos, List<BlockPos> poiPositions) {
        int minDistSq = MIN_DISTANCE_FROM_POI * MIN_DISTANCE_FROM_POI;

        for (BlockPos poiPos : poiPositions) {
            if (pos.distSqr(poiPos) < minDistSq) {
                return true;
            }
        }

        return false;
    }

    /**
     * Find surface position at XZ coordinates.
     */
    private static BlockPos findSurfacePos(ServerLevel level, BlockPos pos) {
        if (!level.isLoaded(pos)) {
            return null;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            pos.getX(), pos.getZ());

        // Validate Y is within world bounds
        if (y >= level.getMinY() && y <= level.getMaxY() - 2) {
            BlockPos surfacePos = new BlockPos(pos.getX(), y, pos.getZ());

            // Quick validation
            BlockState below = level.getBlockState(surfacePos.below());
            BlockState at = level.getBlockState(surfacePos);

            if ((below.isSuffocating(level, surfacePos.below()) || below.blocksMotion()) &&
                !at.isSuffocating(level, surfacePos)) {
                return surfacePos;
            }
        }

        return null;
    }

    /**
     * Calculate base level for a region based on distance from spawn.
     */
    private static int calculateRegionLevel(BlockPos regionCenter) {
        // Simple distance-based leveling for now
        // Will be replaced by zone system integration
        double distFromSpawn = Math.sqrt(regionCenter.distSqr(BlockPos.ZERO));
        int level = 1 + (int)(distFromSpawn / 150.0);
        return Math.min(60, Math.max(1, level));
    }
}

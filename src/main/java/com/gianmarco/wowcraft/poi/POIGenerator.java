package com.gianmarco.wowcraft.poi;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * Generates POIs procedurally for regions.
 * Uses deterministic seeding based on world seed + region position.
 */
public class POIGenerator {

    private static final int REGION_SIZE = 300;  // Reduced from 500 for denser POIs
    private static final int MIN_POI_SPACING = 80;  // Reduced from 150 for closer camps

    // Terrain analysis thresholds
    private static final double FLATNESS_THRESHOLD = 0.7; // For camps
    private static final int TERRAIN_SAMPLE_RADIUS = 10;

    /**
     * Generate POIs for a region.
     */
    public static List<PointOfInterest> generatePOIsForRegion(ServerLevel level, BlockPos regionCenter,
            long worldSeed, BiomeGroup biomeGroup) {

        List<PointOfInterest> pois = new ArrayList<>();

        // Create deterministic random for this region
        long regionSeed = worldSeed + (regionCenter.getX() * 31L) + (regionCenter.getZ() * 17L);
        Random random = new Random(regionSeed);

        WowCraft.LOGGER.debug("Generating POIs for region at {} (biome: {})", regionCenter, biomeGroup);

        // Generate different POI types based on biome
        // For now, use simple defaults - will be replaced by config system later

        // 8-12 individual Camps per region (single 3-5 mob spawn points)
        int campCount = 8 + random.nextInt(5);
        for (int i = 0; i < campCount; i++) {
            BlockPos campPos = findValidCampLocation(level, regionCenter, REGION_SIZE, random, pois);
            if (campPos != null) {
                CampPOI camp = new CampPOI(UUID.randomUUID(), campPos);
                pois.add(camp);
                WowCraft.LOGGER.debug("Generated CAMP at {}", campPos);
            }
        }

        // 1-2 Compounds per region (multi-camp clusters - rare special encounters)
        int compoundCount = 1 + random.nextInt(2);
        for (int i = 0; i < compoundCount; i++) {
            BlockPos compoundPos = findValidCampLocation(level, regionCenter, REGION_SIZE, random, pois);
            if (compoundPos != null) {
                CompoundPOI compound = new CompoundPOI(
                        UUID.randomUUID(),
                        compoundPos,
                        40, // radius
                        2, // min camps
                        4, // max camps
                        20 // camp spacing
                );
                pois.add(compound);
                WowCraft.LOGGER.debug("Generated COMPOUND at {}", compoundPos);
            }
        }

        // 1-2 Wildlife areas per region (rare peaceful encounters)
        int wildlifeCount = 1 + random.nextInt(2);
        for (int i = 0; i < wildlifeCount; i++) {
            BlockPos wildlifePos = findRandomLocation(level, regionCenter, REGION_SIZE, random, pois);
            if (wildlifePos != null) {
                WildlifePOI wildlife = new WildlifePOI(
                        UUID.randomUUID(),
                        wildlifePos,
                        60, // larger radius for roaming
                        1, // min packs
                        2 // max packs
                );
                pois.add(wildlife);
                WowCraft.LOGGER.debug("Generated WILDLIFE at {}", wildlifePos);
            }
        }

        // 0-1 Patrol routes per region
        if (random.nextFloat() < 0.5f && pois.size() >= 2) {
            PatrolRoutePOI patrol = generatePatrolRoute(level, pois, random);
            if (patrol != null) {
                pois.add(patrol);
                WowCraft.LOGGER.debug("Generated PATROL_ROUTE with {} waypoints", patrol.getWaypoints().size());
            }
        }

        // 0-1 Lair per region (rare)
        if (random.nextFloat() < 0.3f) {
            BlockPos lairPos = findSecludedLocation(level, regionCenter, REGION_SIZE, random, pois);
            if (lairPos != null) {
                LairPOI lair = new LairPOI(
                        UUID.randomUUID(),
                        lairPos,
                        25, // smaller radius, focused area
                        3, // +3 levels for elite
                        300 // 5 minute respawn
                );
                pois.add(lair);
                WowCraft.LOGGER.debug("Generated LAIR at {}", lairPos);
            }
        }

        WowCraft.LOGGER.info("Generated {} POIs for region at {}", pois.size(), regionCenter);
        return pois;
    }

    /**
     * Find a valid location for a camp (prefers flat areas).
     */
    private static BlockPos findValidCampLocation(ServerLevel level, BlockPos regionCenter, int regionSize,
            Random random, List<PointOfInterest> existingPOIs) {

        int attempts = 20;
        for (int i = 0; i < attempts; i++) {
            int offsetX = random.nextInt(regionSize) - regionSize / 2;
            int offsetZ = random.nextInt(regionSize) - regionSize / 2;

            BlockPos testPos = regionCenter.offset(offsetX, 0, offsetZ);
            BlockPos surfacePos = findSurfacePos(level, testPos);

            if (surfacePos != null && isFlatEnough(level, surfacePos) && !isTooCloseToExisting(surfacePos, existingPOIs)) {
                return surfacePos;
            }
        }

        // Fallback: just find any surface (with distance check)
        return findRandomLocation(level, regionCenter, regionSize, random, existingPOIs);
    }

    /**
     * Find a secluded location (prefers areas away from other POIs).
     */
    private static BlockPos findSecludedLocation(ServerLevel level, BlockPos regionCenter, int regionSize,
            Random random, List<PointOfInterest> existingPOIs) {

        int attempts = 15;
        for (int i = 0; i < attempts; i++) {
            BlockPos testPos = findRandomLocation(level, regionCenter, regionSize, random, existingPOIs);

            if (testPos != null) {
                // Check if far enough from other POIs
                boolean tooClose = false;
                for (PointOfInterest poi : existingPOIs) {
                    if (poi.getPosition().distSqr(testPos) < 200 * 200) {
                        tooClose = true;
                        break;
                    }
                }

                if (!tooClose) {
                    return testPos;
                }
            }
        }

        return null;
    }

    /**
     * Find a random valid location in the region.
     */
    private static BlockPos findRandomLocation(ServerLevel level, BlockPos regionCenter, int regionSize,
            Random random, List<PointOfInterest> existingPOIs) {

        int attempts = 10;
        for (int i = 0; i < attempts; i++) {
            int offsetX = random.nextInt(regionSize) - regionSize / 2;
            int offsetZ = random.nextInt(regionSize) - regionSize / 2;

            BlockPos testPos = regionCenter.offset(offsetX, 0, offsetZ);
            BlockPos surfacePos = findSurfacePos(level, testPos);

            if (surfacePos != null && !isTooCloseToExisting(surfacePos, existingPOIs)) {
                return surfacePos;
            }
        }

        return null;
    }

    /**
     * Generate a patrol route between existing POIs.
     */
    private static PatrolRoutePOI generatePatrolRoute(ServerLevel level, List<PointOfInterest> existingPOIs,
            Random random) {

        if (existingPOIs.size() < 2) {
            return null;
        }

        // Strategy 1: Connect two camps
        List<PointOfInterest> camps = existingPOIs.stream()
                .filter(p -> p.getType() == POIType.CAMP)
                .toList();

        if (camps.size() >= 2) {
            PointOfInterest start = camps.get(random.nextInt(camps.size()));
            PointOfInterest end = findNearestPOI(start, camps, 100, 250);

            if (end != null) {
                List<BlockPos> waypoints = createWaypoints(start.getPosition(), end.getPosition(), 3);
                BlockPos center = waypoints.get(0);

                return new PatrolRoutePOI(
                        UUID.randomUUID(),
                        center,
                        waypoints,
                        10 // wait 10 seconds at each waypoint
                );
            }
        }

        // Strategy 2: Circular patrol around a single POI
        if (!existingPOIs.isEmpty()) {
            PointOfInterest center = existingPOIs.get(random.nextInt(existingPOIs.size()));
            List<BlockPos> waypoints = createCircularWaypoints(center.getPosition(), 50, 4);

            return new PatrolRoutePOI(
                    UUID.randomUUID(),
                    center.getPosition(),
                    waypoints,
                    5 // wait 5 seconds at each waypoint
            );
        }

        return null;
    }

    /**
     * Create waypoints between two points.
     */
    private static List<BlockPos> createWaypoints(BlockPos start, BlockPos end, int waypointCount) {
        List<BlockPos> waypoints = new ArrayList<>();
        waypoints.add(start);

        for (int i = 1; i < waypointCount - 1; i++) {
            double t = (double) i / (waypointCount - 1);
            int x = (int) (start.getX() + (end.getX() - start.getX()) * t);
            int z = (int) (start.getZ() + (end.getZ() - start.getZ()) * t);
            waypoints.add(new BlockPos(x, start.getY(), z));
        }

        waypoints.add(end);
        return waypoints;
    }

    /**
     * Create circular waypoints around a center point.
     */
    private static List<BlockPos> createCircularWaypoints(BlockPos center, int radius, int count) {
        List<BlockPos> waypoints = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            double angle = (2 * Math.PI * i) / count;
            int x = (int) (center.getX() + Math.cos(angle) * radius);
            int z = (int) (center.getZ() + Math.sin(angle) * radius);
            waypoints.add(new BlockPos(x, center.getY(), z));
        }

        return waypoints;
    }

    /**
     * Find nearest POI within distance range.
     */
    private static PointOfInterest findNearestPOI(PointOfInterest from, List<PointOfInterest> candidates,
            int minDist, int maxDist) {

        PointOfInterest nearest = null;
        double nearestDist = Double.MAX_VALUE;

        for (PointOfInterest candidate : candidates) {
            if (candidate == from) {
                continue;
            }

            double dist = Math.sqrt(from.getPosition().distSqr(candidate.getPosition()));
            if (dist >= minDist && dist <= maxDist && dist < nearestDist) {
                nearest = candidate;
                nearestDist = dist;
            }
        }

        return nearest;
    }

    /**
     * Find surface position at XZ coordinates.
     */
    private static BlockPos findSurfacePos(ServerLevel level, BlockPos pos) {
        // Check if chunk is loaded first to prevent cascading chunk loads during world generation
        if (!level.isLoaded(pos)) {
            return null;
        }

        // Use heightmap instead of Y-loop (eliminates 192 chunk accesses per call)
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
            pos.getX(), pos.getZ());

        // Validate Y is within world bounds
        if (y >= level.getMinY() && y <= level.getMaxY() - 2) {
            BlockPos surfacePos = new BlockPos(pos.getX(), y, pos.getZ());

            // Quick validation
            BlockState below = level.getBlockState(surfacePos.below());
            BlockState at = level.getBlockState(surfacePos);

            if ((below.isSuffocating(level, surfacePos.below()) || below.blocksMotion()) &&
                !at.isSuffocating(level, surfacePos) && at.getBlock() != Blocks.WATER) {
                return surfacePos;
            }
        }

        return null;
    }

    /**
     * Check if terrain is flat enough for a camp.
     */
    private static boolean isFlatEnough(ServerLevel level, BlockPos center) {
        int flatCount = 0;
        int totalSamples = 0;

        for (int dx = -TERRAIN_SAMPLE_RADIUS; dx <= TERRAIN_SAMPLE_RADIUS; dx += 5) {
            for (int dz = -TERRAIN_SAMPLE_RADIUS; dz <= TERRAIN_SAMPLE_RADIUS; dz += 5) {
                BlockPos samplePos = findSurfacePos(level, center.offset(dx, 0, dz));
                if (samplePos != null && Math.abs(samplePos.getY() - center.getY()) <= 3) {
                    flatCount++;
                }
                totalSamples++;
            }
        }

        return totalSamples > 0 && ((double) flatCount / totalSamples) >= FLATNESS_THRESHOLD;
    }

    /**
     * Check if position is too close to existing POIs.
     */
    private static boolean isTooCloseToExisting(BlockPos pos, List<PointOfInterest> existingPOIs) {
        for (PointOfInterest poi : existingPOIs) {
            if (poi.getPosition().distSqr(pos) < MIN_POI_SPACING * MIN_POI_SPACING) {
                return true;
            }
        }
        return false;
    }
}

package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.poi.*;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Generates SpawnPoints from POIs.
 * Converts POI definitions into the new spawn system.
 */
public class POISpawnPointGenerator {

    /**
     * Generate spawn points for a Camp POI.
     */
    public static List<SpawnPoint> generateCampSpawnPoints(
            CampPOI poi,
            BiomeGroup biome,
            int level,
            Random random) {

        List<SpawnPoint> spawnPoints = new ArrayList<>();
        List<BlockPos> spawnPositions = poi.getSpawnPositions();

        // Each camp spawn position gets a spawn point with 3-5 hostile mobs (camp encounter)
        for (BlockPos pos : spawnPositions) {
            List<MobOption> hostileMobs = MobOptionProvider.getHostileMobs(biome);

            SpawnPoint point = new SpawnPoint(
                UUID.randomUUID(),
                pos,
                SpawnPointType.POI_CAMP,
                biome,
                SpawnHostility.ALWAYS_HOSTILE,  // Camps are always hostile
                hostileMobs,
                3,
                5
            );

            point.setTargetLevel(level);
            point.setProtected(true);  // POI spawns never suppressed

            spawnPoints.add(point);
        }

        return spawnPoints;
    }

    /**
     * Generate spawn points for a Compound POI (multiple camps clustered together).
     */
    public static List<SpawnPoint> generateCompoundSpawnPoints(
            CompoundPOI poi,
            BiomeGroup biome,
            int level,
            Random random) {

        List<SpawnPoint> spawnPoints = new ArrayList<>();
        List<BlockPos> spawnPositions = poi.getSpawnPositions();

        // Each camp position in the compound gets a spawn point with 3-5 hostile mobs
        for (BlockPos pos : spawnPositions) {
            List<MobOption> hostileMobs = MobOptionProvider.getHostileMobs(biome);

            SpawnPoint point = new SpawnPoint(
                UUID.randomUUID(),
                pos,
                SpawnPointType.POI_COMPOUND,
                biome,
                SpawnHostility.ALWAYS_HOSTILE,  // Compound camps are always hostile
                hostileMobs,
                3,
                5
            );

            point.setTargetLevel(level);
            point.setProtected(true);  // POI spawns never suppressed

            spawnPoints.add(point);
        }

        return spawnPoints;
    }

    /**
     * Generate spawn points for a Wildlife POI.
     */
    public static List<SpawnPoint> generateWildlifeSpawnPoints(
            WildlifePOI poi,
            BiomeGroup biome,
            int level,
            Random random) {

        List<SpawnPoint> spawnPoints = new ArrayList<>();
        List<BlockPos> spawnPositions = poi.getSpawnPositions();

        // Wildlife areas spawn neutral animals
        for (BlockPos pos : spawnPositions) {
            List<MobOption> neutralMobs = MobOptionProvider.getNeutralMobs(biome);

            SpawnPoint point = new SpawnPoint(
                UUID.randomUUID(),
                pos,
                SpawnPointType.POI_WILDLIFE,
                biome,
                SpawnHostility.NEUTRAL_DEFENSIVE,  // Only attack if provoked
                neutralMobs,
                2,
                4
            );

            point.setTargetLevel(level);
            point.setProtected(true);

            spawnPoints.add(point);
        }

        return spawnPoints;
    }

    /**
     * Generate spawn points for a Patrol Route POI.
     */
    public static List<SpawnPoint> generatePatrolSpawnPoints(
            PatrolRoutePOI poi,
            BiomeGroup biome,
            int level,
            Random random) {

        List<SpawnPoint> spawnPoints = new ArrayList<>();

        // Patrol creates one moving spawn point
        // For now, we'll create spawn points at each waypoint
        // TODO: Implement actual patrol movement later
        List<BlockPos> waypoints = poi.getWaypoints();

        if (!waypoints.isEmpty()) {
            // Create spawn point at first waypoint
            List<MobOption> hostileMobs = MobOptionProvider.getHostileMobs(biome);

            SpawnPoint point = new SpawnPoint(
                UUID.randomUUID(),
                waypoints.get(0),
                SpawnPointType.POI_PATROL,
                biome,
                SpawnHostility.ALWAYS_HOSTILE,
                hostileMobs,
                2,
                4
            );

            point.setTargetLevel(level);
            point.setProtected(true);

            spawnPoints.add(point);
        }

        return spawnPoints;
    }

    /**
     * Generate spawn points for a Lair POI (boss + guards).
     */
    public static List<SpawnPoint> generateLairSpawnPoints(
            LairPOI poi,
            BiomeGroup biome,
            int level,
            Random random) {

        List<SpawnPoint> spawnPoints = new ArrayList<>();

        // Boss spawn point (single boss mob with level bonus)
        List<MobOption> bossMobs = MobOptionProvider.getHostileMobs(biome);

        SpawnPoint bossPoint = new SpawnPoint(
            UUID.randomUUID(),
            poi.getPosition(),
            SpawnPointType.POI_LAIR,
            biome,
            SpawnHostility.NEUTRAL_TERRITORIAL,  // Boss doesn't aggro until close
            bossMobs,
            1,
            1
        );

        bossPoint.setTargetLevel(level);
        bossPoint.setLevelBonus(poi.getLevelBonus());
        bossPoint.setRespawnDelay(poi.getRespawnDelaySeconds() * 20);  // Convert to ticks
        bossPoint.setProtected(true);
        bossPoint.setNamedMob(biome.name() + " Boss");

        spawnPoints.add(bossPoint);

        // Guard spawn points around lair (3-4 guards)
        int guardCount = 3 + random.nextInt(2);
        List<MobOption> guardMobs = MobOptionProvider.getHostileMobs(biome);

        for (int i = 0; i < guardCount; i++) {
            BlockPos guardPos = poi.getPosition().offset(
                random.nextInt(30) - 15,
                0,
                random.nextInt(30) - 15
            );

            SpawnPoint guardPoint = new SpawnPoint(
                UUID.randomUUID(),
                guardPos,
                SpawnPointType.POI_LAIR,
                biome,
                SpawnHostility.ALWAYS_HOSTILE,
                guardMobs,
                2,
                3
            );

            guardPoint.setTargetLevel(level);
            guardPoint.setLevelBonus(1);  // Guards +1 level
            guardPoint.setProtected(true);

            spawnPoints.add(guardPoint);
        }

        return spawnPoints;
    }

    /**
     * Generate spawn points for a Resource Area POI.
     */
    public static List<SpawnPoint> generateResourceAreaSpawnPoints(
            ResourceAreaPOI poi,
            BiomeGroup biome,
            int level,
            Random random) {

        List<SpawnPoint> spawnPoints = new ArrayList<>();
        List<BlockPos> spawnPositions = poi.getSpawnPositions();

        // Resource areas have neutral/territorial guards
        for (BlockPos pos : spawnPositions) {
            List<MobOption> territorialMobs = MobOptionProvider.getTerritorialMobs(biome);

            SpawnPoint point = new SpawnPoint(
                UUID.randomUUID(),
                pos,
                SpawnPointType.POI_WILDLIFE,
                biome,
                SpawnHostility.NEUTRAL_TERRITORIAL,
                territorialMobs,
                2,
                3
            );

            point.setTargetLevel(level);
            point.setProtected(true);

            spawnPoints.add(point);
        }

        return spawnPoints;
    }

    /**
     * Generate spawn points for any POI type.
     */
    public static List<SpawnPoint> generateSpawnPointsForPOI(
            PointOfInterest poi,
            BiomeGroup biome,
            int level,
            ServerLevel serverLevel) {

        Random random = new Random(poi.getPoiId().hashCode());

        return switch (poi.getType()) {
            case CAMP -> generateCampSpawnPoints((CampPOI) poi, biome, level, random);
            case COMPOUND -> generateCompoundSpawnPoints((CompoundPOI) poi, biome, level, random);
            case WILDLIFE -> generateWildlifeSpawnPoints((WildlifePOI) poi, biome, level, random);
            case PATROL_ROUTE -> generatePatrolSpawnPoints((PatrolRoutePOI) poi, biome, level, random);
            case LAIR -> generateLairSpawnPoints((LairPOI) poi, biome, level, random);
            case RESOURCE_AREA -> generateResourceAreaSpawnPoints((ResourceAreaPOI) poi, biome, level, random);
        };
    }
}

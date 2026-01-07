package com.gianmarco.wowcraft.spawn;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.util.List;

/**
 * Detects player safe zones to reduce hostile spawns near bases.
 * Simple implementation for Phase 1.
 */
public class SafeZoneDetector {

    /**
     * Check if a position is in a safe zone.
     * For Phase 1, just check distance from spawn.
     * Full implementation with block detection will come later.
     */
    public static boolean isInSafeZone(ServerLevel level, BlockPos pos) {
        // Check distance from world spawn (spawn area = safer)
        BlockPos spawn = level.getSharedSpawnPos();
        double distSq = pos.distSqr(spawn);

        // 500 blocks from spawn = safe zone
        if (distSq < 500 * 500) {
            return true;
        }

        // TODO: Add player structure detection
        // TODO: Add light level checks
        // TODO: Add base age tracking

        return false;
    }

    /**
     * Get safe zone multiplier for spawn rate reduction.
     * Returns 1.0 for normal, <1.0 for reduced spawns.
     */
    public static float getSafeZoneMultiplier(ServerLevel level, BlockPos pos) {
        if (isInSafeZone(level, pos)) {
            return 0.5f;  // 50% less hostile spawns in safe zones
        }

        return 1.0f;  // Normal spawn rate
    }

    /**
     * Check if a spawn point should be affected by safe zones.
     * Protected spawns (POIs, named mobs) ignore safe zones.
     */
    public static boolean canBeAffectedBySafeZone(SpawnPoint point) {
        // Protected spawns never affected
        if (point.isProtected()) {
            return false;
        }

        // POI spawns never affected
        if (point.isAlwaysActive()) {
            return false;
        }

        // Only scatter spawns can be affected
        return point.getType() == SpawnPointType.SCATTER;
    }

    /**
     * Apply safe zone modifiers to a spawn point.
     * Converts hostile to neutral in safe zones.
     */
    public static void applySafeZoneModifiers(SpawnPoint point, ServerLevel level) {
        if (!canBeAffectedBySafeZone(point)) {
            return;  // Can't be modified
        }

        if (!isInSafeZone(level, point.getPosition())) {
            return;  // Not in safe zone
        }

        // Convert hostile to neutral (60% chance)
        if (point.getHostility() == SpawnHostility.ALWAYS_HOSTILE) {
            if (Math.random() < 0.6) {
                point.setHostility(SpawnHostility.NEUTRAL_DEFENSIVE);
                // Update mob options to neutral types
                List<MobOption> neutralMobs = MobOptionProvider.getNeutralMobs(point.getBiome());
                point.setMobOptions(neutralMobs);
            }
        }
    }
}

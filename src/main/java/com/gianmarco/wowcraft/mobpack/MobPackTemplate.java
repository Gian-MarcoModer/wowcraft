package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.zone.BiomeGroup;

import java.util.List;
import java.util.Set;

/**
 * Defines a pack template that can spawn in zones.
 * Loaded from JSON data files.
 */
public record MobPackTemplate(
        String id, // "zombie_pack", "skeleton_patrol"
        Set<BiomeGroup> validZones, // Which zones this pack can spawn in
        List<MobEntry> mobPool, // Available mob types
        int minPackSize,
        int maxPackSize,
        float socialAggroRadius, // blocks - default ~8
        int respawnDelaySeconds // per-mob respawn timer
) {
    public static final float DEFAULT_SOCIAL_AGGRO_RADIUS = 8.0f;
    public static final int DEFAULT_RESPAWN_DELAY = 120;

    /**
     * Rolls a random pack size within the min-max range.
     */
    public int rollPackSize(java.util.Random random) {
        if (minPackSize >= maxPackSize) {
            return minPackSize;
        }
        return minPackSize + random.nextInt(maxPackSize - minPackSize + 1);
    }

    /**
     * Checks if this template can spawn in the given zone.
     */
    public boolean canSpawnInZone(BiomeGroup zone) {
        return validZones.contains(zone);
    }
}

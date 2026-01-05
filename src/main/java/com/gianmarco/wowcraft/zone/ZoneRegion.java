package com.gianmarco.wowcraft.zone;

import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a discovered zone region in the world.
 * Each region is a contiguous area of the same biome group.
 */
public record ZoneRegion(
        UUID id,
        BiomeGroup biomeGroup,
        BlockPos center,
        int chunkCount,
        int distanceFromSpawn,
        @Nullable String assignedName,
        @Nullable String subtitle,
        int suggestedLevelMin,
        int suggestedLevelMax) {
    /** Minimum chunk count for a region to qualify as a named zone */
    public static final int MIN_ZONE_SIZE = 30;

    /** Distance beyond which zones are considered "Unexplored Wilds" */
    public static final int MAX_NAMED_DISTANCE = 8000;

    /** Distance threshold for merging small regions into larger ones */
    public static final int MERGE_THRESHOLD = 500;

    /**
     * Returns true if this region is large enough to be a named zone.
     */
    public boolean isLargeEnough() {
        return chunkCount >= MIN_ZONE_SIZE;
    }

    /**
     * Returns true if this region is beyond the named zone radius.
     */
    public boolean isUnexplored() {
        return distanceFromSpawn > MAX_NAMED_DISTANCE;
    }

    /**
     * Creates a copy of this region with an assigned name.
     */
    public ZoneRegion withName(String name, String subtitle) {
        return new ZoneRegion(id, biomeGroup, center, chunkCount, distanceFromSpawn,
                name, subtitle, suggestedLevelMin, suggestedLevelMax);
    }

    /**
     * Creates a copy of this region with updated level range.
     */
    public ZoneRegion withLevelRange(int min, int max) {
        return new ZoneRegion(id, biomeGroup, center, chunkCount, distanceFromSpawn,
                assignedName, subtitle, min, max);
    }
}

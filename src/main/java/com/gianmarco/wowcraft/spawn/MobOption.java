package com.gianmarco.wowcraft.spawn;

import net.minecraft.resources.ResourceLocation;

/**
 * Represents one possible mob type for a spawn point.
 * Spawn points can have multiple options (shared spawns).
 */
public record MobOption(
    ResourceLocation mobType,
    int weight,
    int minCount,
    int maxCount
) {
    public MobOption {
        if (weight <= 0) {
            throw new IllegalArgumentException("Weight must be positive");
        }
        if (minCount < 1 || maxCount < minCount) {
            throw new IllegalArgumentException("Invalid count range: " + minCount + "-" + maxCount);
        }
    }

    /**
     * Roll how many mobs to spawn from this option.
     */
    public int rollCount(java.util.Random random) {
        if (minCount == maxCount) {
            return minCount;
        }
        return minCount + random.nextInt(maxCount - minCount + 1);
    }
}

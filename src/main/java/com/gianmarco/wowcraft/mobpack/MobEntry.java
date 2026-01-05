package com.gianmarco.wowcraft.mobpack;

import net.minecraft.resources.ResourceLocation;

/**
 * Defines a single mob type entry in a pack template.
 * Used to specify which mobs can spawn and in what quantities.
 */
public record MobEntry(
        ResourceLocation mobType, // e.g., "minecraft:zombie"
        int minCount,
        int maxCount,
        int weight // spawn weight relative to other entries
) {
    /**
     * Rolls a random count within the min-max range.
     */
    public int rollCount(java.util.Random random) {
        if (minCount >= maxCount) {
            return minCount;
        }
        return minCount + random.nextInt(maxCount - minCount + 1);
    }
}

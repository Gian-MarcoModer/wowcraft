package com.gianmarco.wowcraft.roads;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BiomeTags;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * Determines road block materials.
 * Currently uses dirt path everywhere for consistent safe-zone gameplay.
 */
public class BiomeRoadMaterial {

    /**
     * Get the primary road surface block.
     * Always returns dirt path for consistent roads and safe-zone detection.
     */
    public static Block getRoadBlock(ServerLevel level, BlockPos pos) {
        return Blocks.DIRT_PATH;
    }

    /**
     * Get the edge/border block for roads.
     * Using coarse dirt for a subtle border effect.
     */
    public static Block getEdgeBlock(ServerLevel level, BlockPos pos) {
        return Blocks.COARSE_DIRT;
    }

    /**
     * Get decoration post block (for lantern posts).
     * Still biome-adaptive for visual variety.
     */
    public static Block getFenceBlock(ServerLevel level, BlockPos pos) {
        Holder<Biome> biomeHolder = level.getBiome(pos);

        if (biomeHolder.is(BiomeTags.IS_JUNGLE)) {
            return Blocks.JUNGLE_FENCE;
        }

        if (biomeHolder.is(BiomeTags.IS_TAIGA)) {
            return Blocks.SPRUCE_FENCE;
        }

        Biome biome = biomeHolder.value();
        if (biome.coldEnoughToSnow(pos, pos.getY())) {
            return Blocks.SPRUCE_FENCE;
        }

        if (biome.getBaseTemperature() > 1.5f || biomeHolder.is(BiomeTags.IS_BADLANDS)) {
            return Blocks.BIRCH_FENCE;
        }

        if (biomeHolder.is(BiomeTags.IS_SAVANNA)) {
            return Blocks.ACACIA_FENCE;
        }

        return Blocks.OAK_FENCE;
    }

    /**
     * Check if this biome is an ocean (skip road generation).
     */
    public static boolean isOceanBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(BiomeTags.IS_OCEAN) || biomeHolder.is(BiomeTags.IS_DEEP_OCEAN);
    }

    /**
     * Check if this is a river biome.
     */
    public static boolean isRiverBiome(Holder<Biome> biomeHolder) {
        return biomeHolder.is(BiomeTags.IS_RIVER);
    }
}

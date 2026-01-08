package com.gianmarco.wowcraft.roads;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;

import java.util.Optional;

public class RoadLandmarkGenerator {
    private static final int CLEARING_RADIUS = 5;
    private static final int CLEARING_ATTEMPTS = 18;
    private static final int MAX_LEAF_COUNT = 6;

    public static Optional<BlockPos> findClearing(ServerLevel level, BlockPos center, int radius, RandomSource random) {
        for (int i = 0; i < CLEARING_ATTEMPTS; i++) {
            int dx = random.nextInt(radius * 2 + 1) - radius;
            int dz = random.nextInt(radius * 2 + 1) - radius;
            int x = center.getX() + dx;
            int z = center.getZ() + dz;

            if (!level.hasChunk(x >> 4, z >> 4)) {
                continue;
            }

            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockState groundState = level.getBlockState(ground);
            if (!groundState.getFluidState().isEmpty()) {
                continue;
            }

            if (countLeaves(level, ground.above()) <= MAX_LEAF_COUNT) {
                return Optional.of(ground.above());
            }
        }

        return Optional.empty();
    }

    private static int countLeaves(ServerLevel level, BlockPos center) {
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        for (int dx = -CLEARING_RADIUS; dx <= CLEARING_RADIUS; dx += 2) {
            for (int dz = -CLEARING_RADIUS; dz <= CLEARING_RADIUS; dz += 2) {
                pos.set(center.getX() + dx, center.getY(), center.getZ() + dz);
                BlockState state = level.getBlockState(pos);
                if (state.is(BlockTags.LEAVES)) {
                    count++;
                }
            }
        }

        return count;
    }
}

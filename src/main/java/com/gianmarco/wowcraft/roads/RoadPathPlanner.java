package com.gianmarco.wowcraft.roads;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

import java.util.*;

public class RoadPathPlanner {
    private static final int BASE_CELL_SIZE = 12;
    private static final int MAX_GRID_CELLS = 160;
    private static final int PADDING = 48;
    private static final int MAX_SLOPE = 3;
    private static final double SLOPE_COST = 2.5;
    private static final double ROUGHNESS_COST = 3.0;
    private static final int TREE_SCAN_HEIGHT = 6;
    private static final double WATER_COST = 18.0;
    private static final double LEAF_COST = 4.0;
    private static final double NOISE_COST = 0.6;
    private static final double SAMPLE_STEP = 2.0;
    private static final int SMOOTH_ITERATIONS = 2;

    private static final int[] DIR_X = {1, -1, 0, 0, 1, 1, -1, -1};
    private static final int[] DIR_Z = {0, 0, 1, -1, 1, -1, 1, -1};
    private static final double[] DIR_COST = {1.0, 1.0, 1.0, 1.0, 1.42, 1.42, 1.42, 1.42};

    public List<BlockPos> plan(ServerLevel level, BlockPos start, BlockPos end, long seed) {
        Grid grid = Grid.build(level, start, end, seed);
        if (grid == null) {
            return fallbackPath(level, start, end);
        }

        int startIndex = grid.indexForWorld(start.getX(), start.getZ());
        int endIndex = grid.indexForWorld(end.getX(), end.getZ());
        if (startIndex < 0 || endIndex < 0) {
            return fallbackPath(level, start, end);
        }
        if (grid.blocked[startIndex]) {
            grid.blocked[startIndex] = false;
        }
        if (grid.blocked[endIndex]) {
            grid.blocked[endIndex] = false;
        }

        int size = grid.width * grid.height;
        double[] gScore = new double[size];
        Arrays.fill(gScore, Double.POSITIVE_INFINITY);
        int[] cameFrom = new int[size];
        Arrays.fill(cameFrom, -1);

        PriorityQueue<Node> open = new PriorityQueue<>(Comparator.comparingDouble(n -> n.fScore));
        gScore[startIndex] = 0.0;
        open.add(new Node(startIndex, heuristic(grid, startIndex, endIndex)));

        while (!open.isEmpty()) {
            Node current = open.poll();
            if (current.index == endIndex) {
                return buildPath(level, grid, cameFrom, endIndex, seed);
            }

            if (current.fScore - heuristic(grid, current.index, endIndex) > gScore[current.index] + 0.001) {
                continue;
            }

            int cx = current.index % grid.width;
            int cz = current.index / grid.width;

            for (int i = 0; i < DIR_X.length; i++) {
                int nx = cx + DIR_X[i];
                int nz = cz + DIR_Z[i];
                if (!grid.inBounds(nx, nz)) {
                    continue;
                }

                int neighbor = grid.index(nx, nz);
                if (grid.blocked[neighbor]) {
                    continue;
                }

                int heightDelta = Math.abs(grid.heights[neighbor] - grid.heights[current.index]);
                if (heightDelta > MAX_SLOPE) {
                    continue;
                }

                double cost = DIR_COST[i];
                cost += heightDelta * SLOPE_COST;
                cost += grid.roughness[neighbor] * ROUGHNESS_COST;
                if (grid.water[neighbor]) {
                    cost += WATER_COST;
                }
                if (grid.leaves[neighbor]) {
                    cost += LEAF_COST;
                }
                cost += noisePenalty(grid.worldX(nx), grid.worldZ(nz), seed) * NOISE_COST;

                double tentative = gScore[current.index] + cost;
                if (tentative < gScore[neighbor]) {
                    cameFrom[neighbor] = current.index;
                    gScore[neighbor] = tentative;
                    double fScore = tentative + heuristic(grid, neighbor, endIndex);
                    open.add(new Node(neighbor, fScore));
                }
            }
        }

        return fallbackPath(level, start, end);
    }

    private List<BlockPos> buildPath(ServerLevel level, Grid grid, int[] cameFrom, int endIndex, long seed) {
        List<Integer> indices = new ArrayList<>();
        int current = endIndex;
        while (current != -1) {
            indices.add(current);
            current = cameFrom[current];
        }
        Collections.reverse(indices);

        List<Vec2> raw = new ArrayList<>();
        for (int index : indices) {
            int gx = index % grid.width;
            int gz = index / grid.width;
            raw.add(new Vec2(grid.worldX(gx), grid.worldZ(gz)));
        }

        List<Vec2> smoothed = smooth(raw, seed);
        return sampleToBlocks(level, smoothed);
    }

    private List<Vec2> smooth(List<Vec2> input, long seed) {
        if (input.size() < 3) {
            return input;
        }

        List<Vec2> current = input;
        for (int iter = 0; iter < SMOOTH_ITERATIONS; iter++) {
            List<Vec2> next = new ArrayList<>();
            next.add(current.get(0));
            for (int i = 0; i < current.size() - 1; i++) {
                Vec2 p0 = current.get(i);
                Vec2 p1 = current.get(i + 1);
                Vec2 q = new Vec2(p0.x * 0.75 + p1.x * 0.25, p0.z * 0.75 + p1.z * 0.25);
                Vec2 r = new Vec2(p0.x * 0.25 + p1.x * 0.75, p0.z * 0.25 + p1.z * 0.75);
                next.add(q);
                next.add(r);
            }
            next.add(current.get(current.size() - 1));
            current = applyMeander(next, seed, iter);
        }
        return current;
    }

    private List<Vec2> applyMeander(List<Vec2> points, long seed, int iter) {
        RandomSource random = RandomSource.create(seed ^ (iter * 131L));
        List<Vec2> result = new ArrayList<>(points.size());
        result.add(points.get(0));
        for (int i = 1; i < points.size() - 1; i++) {
            Vec2 p = points.get(i);
            double offset = (random.nextDouble() - 0.5) * 2.0;
            double jitter = offset * 1.5;
            result.add(new Vec2(p.x + jitter, p.z - jitter));
        }
        result.add(points.get(points.size() - 1));
        return result;
    }

    private List<BlockPos> sampleToBlocks(ServerLevel level, List<Vec2> points) {
        List<BlockPos> path = new ArrayList<>();
        BlockPos last = null;

        for (int i = 0; i < points.size() - 1; i++) {
            Vec2 a = points.get(i);
            Vec2 b = points.get(i + 1);
            double dx = b.x - a.x;
            double dz = b.z - a.z;
            double dist = Math.sqrt(dx * dx + dz * dz);
            int steps = Math.max(1, (int) Math.ceil(dist / SAMPLE_STEP));

            for (int step = 0; step <= steps; step++) {
                double t = steps == 0 ? 0.0 : (double) step / (double) steps;
                int x = (int) Math.round(a.x + dx * t);
                int z = (int) Math.round(a.z + dz * t);
                int y = level.hasChunk(x >> 4, z >> 4)
                        ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                        : level.getMinY();
                BlockPos pos = new BlockPos(x, y, z);
                if (last == null || !last.equals(pos)) {
                    path.add(pos);
                    last = pos;
                }
            }
        }

        return path;
    }

    private List<BlockPos> fallbackPath(ServerLevel level, BlockPos start, BlockPos end) {
        List<BlockPos> path = new ArrayList<>();
        int dx = end.getX() - start.getX();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.abs(dx), Math.abs(dz));

        for (int i = 0; i <= steps; i++) {
            double t = steps == 0 ? 0.0 : (double) i / (double) steps;
            int x = (int) Math.round(start.getX() + dx * t);
            int z = (int) Math.round(start.getZ() + dz * t);
            int y = level.hasChunk(x >> 4, z >> 4)
                    ? level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z)
                    : level.getMinY();
            path.add(new BlockPos(x, y, z));
        }
        return path;
    }

    private double heuristic(Grid grid, int index, int endIndex) {
        int x = index % grid.width;
        int z = index / grid.width;
        int ex = endIndex % grid.width;
        int ez = endIndex / grid.width;
        int dx = ex - x;
        int dz = ez - z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    private double noisePenalty(int x, int z, long seed) {
        long h = seed ^ (x * 341873128712L + z * 132897987541L);
        h ^= (h >> 13);
        h *= 1274126177L;
        h ^= (h >> 16);
        return (double) (h & 0xffff) / 65535.0;
    }

    private record Node(int index, double fScore) {
    }

    private record Vec2(double x, double z) {
    }

    private static class Grid {
        private final int minX;
        private final int minZ;
        private final int width;
        private final int height;
        private final int cellSize;
        private final int[] heights;
        private final boolean[] water;
        private final boolean[] leaves;
        private final boolean[] blocked;
        private final int[] roughness;
        private final long seed;

        private Grid(int minX, int minZ, int width, int height, int cellSize, long seed) {
            this.minX = minX;
            this.minZ = minZ;
            this.width = width;
            this.height = height;
            this.cellSize = cellSize;
            this.heights = new int[width * height];
            this.water = new boolean[width * height];
            this.leaves = new boolean[width * height];
            this.blocked = new boolean[width * height];
            this.roughness = new int[width * height];
            this.seed = seed;
        }

        static Grid build(ServerLevel level, BlockPos start, BlockPos end, long seed) {
            int minX = Math.min(start.getX(), end.getX()) - PADDING;
            int minZ = Math.min(start.getZ(), end.getZ()) - PADDING;
            int maxX = Math.max(start.getX(), end.getX()) + PADDING;
            int maxZ = Math.max(start.getZ(), end.getZ()) + PADDING;

            int cellSize = BASE_CELL_SIZE;
            int width = Math.max(1, (maxX - minX) / cellSize + 1);
            int height = Math.max(1, (maxZ - minZ) / cellSize + 1);

            if (width > MAX_GRID_CELLS || height > MAX_GRID_CELLS) {
                int spanX = Math.max(1, maxX - minX);
                int spanZ = Math.max(1, maxZ - minZ);
                cellSize = Math.max(cellSize, Math.max(spanX / MAX_GRID_CELLS, spanZ / MAX_GRID_CELLS) + 1);
                width = Math.max(1, spanX / cellSize + 1);
                height = Math.max(1, spanZ / cellSize + 1);
            }

            Grid grid = new Grid(minX, minZ, width, height, cellSize, seed);
            grid.populate(level);
            return grid;
        }

        void populate(ServerLevel level) {
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
            for (int z = 0; z < height; z++) {
                for (int x = 0; x < width; x++) {
                    int worldX = worldX(x);
                    int worldZ = worldZ(z);
                    int idx = index(x, z);

                    if (!level.hasChunk(worldX >> 4, worldZ >> 4)) {
                        blocked[idx] = true;
                        heights[idx] = 0;
                        water[idx] = false;
                        leaves[idx] = false;
                        continue;
                    }

                    int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, worldX, worldZ);
                    int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, worldX, worldZ);

                    pos.set(worldX, surfaceY - 1, worldZ);
                    BlockState surfaceState = level.getBlockState(pos);

                    heights[idx] = groundY;
                    water[idx] = surfaceState.getFluidState().is(FluidTags.WATER);

                    boolean hasLog = false;
                    boolean hasLeaf = false;
                    for (int y = groundY; y <= groundY + TREE_SCAN_HEIGHT; y++) {
                        pos.set(worldX, y, worldZ);
                        BlockState state = level.getBlockState(pos);
                        if (state.is(BlockTags.LOGS)) {
                            hasLog = true;
                            break;
                        }
                        if (state.is(BlockTags.LEAVES)) {
                            hasLeaf = true;
                        }
                    }
                    blocked[idx] = hasLog;
                    leaves[idx] = hasLeaf;
                }
            }

            computeRoughness();
        }

        void computeRoughness() {
            for (int z = 0; z < height; z++) {
                for (int x = 0; x < width; x++) {
                    int idx = index(x, z);
                    if (blocked[idx]) {
                        roughness[idx] = MAX_SLOPE + 1;
                        continue;
                    }

                    int base = heights[idx];
                    int maxDiff = 0;
                    for (int i = 0; i < 4; i++) {
                        int nx = x + DIR_X[i];
                        int nz = z + DIR_Z[i];
                        if (!inBounds(nx, nz)) {
                            continue;
                        }
                        int nIdx = index(nx, nz);
                        if (blocked[nIdx]) {
                            continue;
                        }
                        int diff = Math.abs(heights[nIdx] - base);
                        if (diff > maxDiff) {
                            maxDiff = diff;
                        }
                    }
                    roughness[idx] = maxDiff;
                }
            }
        }

        int index(int x, int z) {
            return x + z * width;
        }

        int indexForWorld(int worldX, int worldZ) {
            int gx = (worldX - minX) / cellSize;
            int gz = (worldZ - minZ) / cellSize;
            if (!inBounds(gx, gz)) {
                return -1;
            }
            return index(gx, gz);
        }

        boolean inBounds(int x, int z) {
            return x >= 0 && z >= 0 && x < width && z < height;
        }

        int worldX(int x) {
            return minX + x * cellSize + cellSize / 2;
        }

        int worldZ(int z) {
            return minZ + z * cellSize + cellSize / 2;
        }
    }
}

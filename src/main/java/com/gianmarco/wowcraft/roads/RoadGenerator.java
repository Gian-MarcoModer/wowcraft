package com.gianmarco.wowcraft.roads;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.util.RandomSource;

import java.util.*;

/**
 * Generates roads between structures instantly.
 * Roads are built immediately when planned, not tick-by-tick.
 */
public class RoadGenerator {

    private static RoadGenerator instance;

    // Structures we've discovered
    private final List<BlockPos> foundStructures = new ArrayList<>();

    // Track which structure pairs already have roads
    private final Set<String> builtRoadPairs = new HashSet<>();

    // Configuration
    private int roadWidth = 3; // Width of roads (odd numbers work best)
    private int decorationChance = 60; // 1 in N chance for lantern post

    // Stats
    private int totalRoadsBuilt = 0;
    private int totalBlocksPlaced = 0;

    // Auto-initialization
    private boolean initialized = false;
    private int initDelayTicks = 0;
    private static final int INIT_DELAY = 100; // Wait 5 seconds after world load
    private static final int AUTO_SEARCH_RADIUS = 2000;
    private static final int AUTO_MAX_STRUCTURES = 10;

    // Player expansion tracking
    private final Map<UUID, Long> lastExpansionCheck = new HashMap<>();
    private static final int EXPANSION_CHECK_INTERVAL = 600; // 30 seconds (was 5 minutes)
    private static final int PLAYER_EXPANSION_RADIUS = 500; // Search within 500 blocks of player

    public static RoadGenerator getInstance() {
        if (instance == null) {
            instance = new RoadGenerator();
        }
        return instance;
    }

    private RoadGenerator() {
    }

    /**
     * Add a discovered structure position.
     * Only adds structures in chunks that are actually generated.
     */
    public void addStructure(BlockPos pos, ServerLevel level) {
        // Only add if chunk is generated (not just seed-predicted)
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) {
            WowCraft.LOGGER.debug("Skipping structure at {} - chunk not generated", pos);
            return;
        }

        // Avoid duplicates (check within 50 block radius)
        for (BlockPos existing : foundStructures) {
            if (existing.distSqr(pos) < 50 * 50) {
                return;
            }
        }
        foundStructures.add(pos);
        WowCraft.LOGGER.info("Road generator discovered structure at {}", pos);
    }

    /**
     * Add a discovered structure position (legacy method without level check).
     */
    public void addStructure(BlockPos pos) {
        // Avoid duplicates (check within 50 block radius)
        for (BlockPos existing : foundStructures) {
            if (existing.distSqr(pos) < 50 * 50) {
                return;
            }
        }
        foundStructures.add(pos);
        WowCraft.LOGGER.info("Road generator discovered structure at {} (unverified)", pos);
    }

    public int getStructureCount() {
        return foundStructures.size();
    }

    /**
     * Build roads between all discovered structures within range.
     * Roads are built INSTANTLY - all blocks placed immediately.
     */
    public void buildRoads(ServerLevel level) {
        if (foundStructures.size() < 2) {
            WowCraft.LOGGER.warn("Need at least 2 structures to build roads, have {}", foundStructures.size());
            return;
        }

        int newRoads = 0;

        // Connect all structures within 500 blocks of each other
        for (int i = 0; i < foundStructures.size(); i++) {
            BlockPos structureA = foundStructures.get(i);

            for (int j = i + 1; j < foundStructures.size(); j++) {
                BlockPos structureB = foundStructures.get(j);

                double distance = Math.sqrt(structureA.distSqr(structureB));
                if (distance > 500) {
                    continue;
                }

                String pairKey = createPairKey(structureA, structureB);
                if (builtRoadPairs.contains(pairKey)) {
                    continue;
                }

                // Build road instantly
                buildRoadBetween(level, structureA, structureB);
                builtRoadPairs.add(pairKey);
                newRoads++;
                totalRoadsBuilt++;

                WowCraft.LOGGER.info("Built road: {} -> {} (distance: {})",
                        structureA.toShortString(), structureB.toShortString(), (int) distance);
            }
        }

        if (newRoads > 0) {
            WowCraft.LOGGER.info("Built {} new roads (total: {}, blocks placed: {})",
                    newRoads, totalRoadsBuilt, totalBlocksPlaced);
        }
    }

    /**
     * Build a road between two points immediately.
     */
    private void buildRoadBetween(ServerLevel level, BlockPos start, BlockPos end) {
        int x = start.getX();
        int z = start.getZ();
        int targetX = end.getX();
        int targetZ = end.getZ();

        int dx = Integer.compare(targetX, x);
        int dz = Integer.compare(targetZ, z);

        int xRemaining = Math.abs(targetX - x);
        int zRemaining = Math.abs(targetZ - z);

        RandomSource random = level.getRandom();

        // Walk from start to end, placing road sections
        while (xRemaining > 0 || zRemaining > 0) {
            // Move along whichever axis has more distance remaining
            if (xRemaining >= zRemaining && xRemaining > 0) {
                x += dx;
                xRemaining--;
            } else if (zRemaining > 0) {
                z += dz;
                zRemaining--;
            }

            BlockPos roadPos = new BlockPos(x, 0, z);
            placeRoadSection(level, roadPos, random);
        }
    }

    /**
     * Build a manual road between two points.
     */
    public void buildManualRoad(ServerLevel level, BlockPos start, BlockPos end) {
        buildRoadBetween(level, start, end);
        WowCraft.LOGGER.info("Built manual road: {} -> {}", start, end);
    }

    /**
     * Place a road section at the given XZ position.
     */
    private void placeRoadSection(ServerLevel level, BlockPos centerPos, RandomSource random) {
        // Only place in loaded chunks
        int chunkX = centerPos.getX() >> 4;
        int chunkZ = centerPos.getZ() >> 4;
        if (!level.hasChunk(chunkX, chunkZ)) {
            return; // Chunk not loaded, skip this section
        }

        int surfaceY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                centerPos.getX(), centerPos.getZ());

        BlockPos surfacePos = new BlockPos(centerPos.getX(), surfaceY - 1, centerPos.getZ());

        // Skip oceans
        Holder<Biome> biome = level.getBiome(surfacePos);
        if (BiomeRoadMaterial.isOceanBiome(biome)) {
            return;
        }

        Block roadBlock = BiomeRoadMaterial.getRoadBlock(level, surfacePos);
        Block edgeBlock = BiomeRoadMaterial.getEdgeBlock(level, surfacePos);

        int halfWidth = roadWidth / 2;

        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                int distance = Math.abs(dx) + Math.abs(dz);

                int placeX = centerPos.getX() + dx;
                int placeZ = centerPos.getZ() + dz;
                int placeY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, placeX, placeZ) - 1;

                BlockPos placePos = new BlockPos(placeX, placeY, placeZ);

                BlockState currentState = level.getBlockState(placePos);
                if (currentState.isAir() || currentState.getBlock() == Blocks.WATER) {
                    continue;
                }

                Block blockToPlace = (distance >= halfWidth) ? edgeBlock : roadBlock;

                // Dirt paths need air above
                if (roadBlock == Blocks.DIRT_PATH) {
                    BlockPos above = placePos.above();
                    if (!level.getBlockState(above).isAir()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                    }
                }

                level.setBlock(placePos, blockToPlace.defaultBlockState(), 3);
                totalBlocksPlaced++;
            }
        }

        // Random decoration
        if (random.nextInt(decorationChance) == 0) {
            placeDecoration(level, surfacePos, random);
        }
    }

    /**
     * Place a lantern post decoration.
     */
    private void placeDecoration(ServerLevel level, BlockPos roadPos, RandomSource random) {
        int side = random.nextBoolean() ? (roadWidth / 2 + 2) : -(roadWidth / 2 + 2);
        BlockPos decorPos = roadPos.offset(side, 0, 0);

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                decorPos.getX(), decorPos.getZ());
        decorPos = new BlockPos(decorPos.getX(), y, decorPos.getZ());

        if (level.getBlockState(decorPos.below()).getBlock() == Blocks.WATER) {
            return;
        }

        Block fenceBlock = BiomeRoadMaterial.getFenceBlock(level, decorPos);

        for (int i = 0; i < 3; i++) {
            level.setBlock(decorPos.above(i), fenceBlock.defaultBlockState(), 3);
        }
        level.setBlock(decorPos.above(3), Blocks.LANTERN.defaultBlockState(), 3);
        totalBlocksPlaced += 4;
    }

    /**
     * Create a unique key for a structure pair.
     */
    private String createPairKey(BlockPos a, BlockPos b) {
        if (a.getX() < b.getX() || (a.getX() == b.getX() && a.getZ() < b.getZ())) {
            return a.toShortString() + "-" + b.toShortString();
        } else {
            return b.toShortString() + "-" + a.toShortString();
        }
    }

    // === Server Tick (for auto-init and expansion only) ===

    public void onServerTick(ServerLevel level) {
        // Auto-initialization on world start
        if (!initialized) {
            initDelayTicks++;
            if (initDelayTicks >= INIT_DELAY) {
                initialized = true;
                autoInitialize(level);
            }
        }
    }

    private void autoInitialize(ServerLevel level) {
        BlockPos spawnPos = level.getSharedSpawnPos();
        WowCraft.LOGGER.info("Auto-initializing road generator near spawn {}", spawnPos);

        StructureFinder.findStructuresAsync(level, spawnPos, AUTO_SEARCH_RADIUS, AUTO_MAX_STRUCTURES)
                .thenAccept(structures -> {
                    level.getServer().execute(() -> {
                        int added = 0;
                        for (BlockPos pos : structures) {
                            int before = foundStructures.size();
                            addStructure(pos, level); // Use level-aware version
                            if (foundStructures.size() > before) added++;
                        }

                        WowCraft.LOGGER.info("Found {} seed-predicted structures, {} in generated chunks",
                                structures.size(), added);

                        if (foundStructures.size() >= 2) {
                            WowCraft.LOGGER.info("Building roads between {} structures...", foundStructures.size());
                            buildRoads(level);
                        } else {
                            WowCraft.LOGGER.info("Need 2+ structures in generated chunks for roads (have {})",
                                    foundStructures.size());
                        }
                    });
                });
    }

    /**
     * Force a structure search and road build around a position.
     * Useful for testing or manual triggering.
     */
    public void forceSearchAndBuild(ServerLevel level, BlockPos center, int radius) {
        WowCraft.LOGGER.info("Force searching for structures within {} blocks of {}", radius, center);

        StructureFinder.findStructuresAsync(level, center, radius, 10)
                .thenAccept(structures -> {
                    level.getServer().execute(() -> {
                        int added = 0;
                        for (BlockPos pos : structures) {
                            int before = foundStructures.size();
                            addStructure(pos, level);
                            if (foundStructures.size() > before) {
                                added++;
                                WowCraft.LOGGER.info("  Added structure at {} (in loaded chunk)", pos);
                            } else {
                                WowCraft.LOGGER.info("  Skipped structure at {} (chunk not loaded or duplicate)", pos);
                            }
                        }

                        WowCraft.LOGGER.info("Force search: {} predicted, {} added, {} total structures",
                                structures.size(), added, foundStructures.size());

                        if (foundStructures.size() >= 2) {
                            buildRoads(level);
                        }
                    });
                });
    }

    /**
     * Check for new structures near players and expand road network.
     */
    public void checkExpansion(ServerLevel level) {
        long currentTick = level.getGameTime();

        for (net.minecraft.server.level.ServerPlayer player : level.players()) {
            UUID playerId = player.getUUID();
            long lastCheck = lastExpansionCheck.getOrDefault(playerId, 0L);

            if (currentTick - lastCheck < EXPANSION_CHECK_INTERVAL) {
                continue;
            }

            lastExpansionCheck.put(playerId, currentTick);
            BlockPos playerPos = player.blockPosition();

            StructureFinder.findStructuresAsync(level, playerPos, PLAYER_EXPANSION_RADIUS, 3)
                    .thenAccept(structures -> {
                        level.getServer().execute(() -> {
                            int newCount = 0;
                            for (BlockPos pos : structures) {
                                int beforeCount = foundStructures.size();
                                addStructure(pos, level); // Use level-aware version
                                if (foundStructures.size() > beforeCount) {
                                    newCount++;
                                }
                            }

                            if (newCount > 0 && foundStructures.size() >= 2) {
                                WowCraft.LOGGER.info("Player {} discovered {} new structures, building roads",
                                        player.getName().getString(), newCount);
                                buildRoads(level);
                            }
                        });
                    });
        }
    }

    // === Getters ===

    public int getTotalRoadsBuilt() {
        return totalRoadsBuilt;
    }

    public int getTotalBlocksPlaced() {
        return totalBlocksPlaced;
    }

    public boolean isInitialized() {
        return initialized;
    }

    /**
     * Clear all data and reset generator.
     */
    public void reset() {
        foundStructures.clear();
        builtRoadPairs.clear();
        totalRoadsBuilt = 0;
        totalBlocksPlaced = 0;
        initialized = false;
        initDelayTicks = 0;
        lastExpansionCheck.clear();
    }
}

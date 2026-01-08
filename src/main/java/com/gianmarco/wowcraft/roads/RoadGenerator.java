package com.gianmarco.wowcraft.roads;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.spawn.SpawnPoolManager;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import com.gianmarco.wowcraft.zone.ZoneRegion;
import com.gianmarco.wowcraft.zone.ZoneSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.core.Holder;

import java.util.*;

public class RoadGenerator {

    private static RoadGenerator instance;

    private static final int INIT_DELAY = 100;
    private static final int AUTO_SEARCH_RADIUS = 2000;
    private static final int AUTO_MAX_STRUCTURES = 10;

    private static final int MAX_CONNECTION_DISTANCE = 1400;
    private static final int HUB_LONG_DISTANCE = 8000;
    private static final int HUB_OUTWARD_MIN_DELTA = 400;
    private static final int STRUCTURE_HUB_DISTANCE = 3000;
    private static final int DIRECT_STRUCTURE_DISTANCE = 900;
    private static final int MAX_CONNECTIONS_PER_NODE = 4;
    private static final int LANDMARK_RADIUS = 200;
    private static final int CLEARING_CONNECT_DISTANCE = 240;
    private static final int CLEARING_MIN_NODE_DISTANCE = 96;
    private static final int REGIONAL_HUB_SIZE = 3000;
    private static final int TRAIL_SPOKE_COUNT = 3;
    private static final int TRAIL_SHORT_MIN_DISTANCE = 140;
    private static final int TRAIL_SHORT_MAX_DISTANCE = 260;
    private static final int TRAIL_LONG_MIN_DISTANCE = 420;
    private static final int TRAIL_LONG_MAX_DISTANCE = 900;
    private static final int TRAIL_SCAN_STEP = 24;
    private static final int TRAIL_CONNECT_DISTANCE = 1400;
    private static final int TRAIL_MIN_NODE_DISTANCE = 120;
    private static final int STRUCTURE_TRAIL_CONNECTIONS = 2;

    private static final int MAX_GRADE_STEP = 1;
    private static final int MAX_TERRAFORM_HEIGHT = 1;
    private static final int TREE_SCAN_HEIGHT = 6;
    private static final int ZONE_HUB_CHECK_INTERVAL = 200;

    private static final int PROCESS_STEPS_PER_TICK = 120;
    private static final int NODES_PLANNED_PER_TICK = 2;
    private static final int PLAN_REQUESTS_PER_TICK = 1;
    private static final int SAFE_ZONE_CHUNKS_PER_TICK = 6;
    private static final int ROAD_TICK_INTERVAL = 2;
    private static final long MAX_NANOS_PER_TICK = 2_500_000L;
    private static final int CHUNK_BUILD_GRACE_TICKS = 40;
    private static final int RECENT_CHUNK_CLEANUP_INTERVAL = 200;
    private static final int BRIDGE_MAX_WATER_WIDTH = 12;
    private static final int BRIDGE_MAX_Y = 80;
    private static final int DOCK_LENGTH = 4;

    private static final Set<String> EXTRA_STRUCTURES = Set.of(
            "pillager_outpost",
            "ruined_portal",
            "swamp_hut");

    private final RoadRegistry registry = new RoadRegistry();
    private final RoadPathPlanner planner = new RoadPathPlanner();
    private final List<RoadPlan> activePlans = new ArrayList<>();
    private final Deque<RoadNode> pendingNodes = new ArrayDeque<>();
    private final Set<UUID> pendingNodeIds = new HashSet<>();
    private final Deque<RoadPlanRequest> planRequests = new ArrayDeque<>();
    private final Deque<Long> pendingSafeZoneChunks = new ArrayDeque<>();
    private final Set<Long> pendingSafeZoneChunkSet = new HashSet<>();
    private final Map<Long, Long> recentChunkLoads = new HashMap<>();

    private boolean initialized = false;
    private boolean needsPlanning = false;
    private int initDelayTicks = 0;
    private int roadTickCounter = 0;
    private long tickStartTime = 0L;
    private int recentChunkCleanupCounter = 0;

    private RoadSaveData saveData;

    private int roadWidth = 3;
    private int decorationChance = 60;

    private int totalRoadsBuilt = 0;
    private int totalBlocksPlaced = 0;
    private int zoneHubTickCounter = 0;

    public static RoadGenerator getInstance() {
        if (instance == null) {
            instance = new RoadGenerator();
        }
        return instance;
    }

    private RoadGenerator() {
    }

    public void onChunkLoad(ServerLevel level, LevelChunk chunk) {
        ensureLoaded(level);
        recordChunkLoad(level, chunk);
        registerStructuresFromChunk(level, chunk);
    }

    public void onServerTick(ServerLevel level) {
        if (!initialized) {
            initDelayTicks++;
            if (initDelayTicks >= INIT_DELAY) {
                initialized = true;
                ensureLoaded(level);
                autoInitialize(level);
            }
        }

        if (needsPlanning && registry.getNodeCount() >= 2) {
            needsPlanning = false;
            enqueueAllNodes();
        }

        zoneHubTickCounter++;
        if (zoneHubTickCounter >= ZONE_HUB_CHECK_INTERVAL) {
            zoneHubTickCounter = 0;
            updateZoneHubs(level);
        }

        cleanupRecentChunks(level.getGameTime());

        roadTickCounter++;
        if (roadTickCounter < ROAD_TICK_INTERVAL) {
            return;
        }
        roadTickCounter = 0;

        tickStartTime = System.nanoTime();
        processPendingNodes(level, NODES_PLANNED_PER_TICK);
        if (isBudgetExceeded()) {
            return;
        }
        processPlanRequests(level, PLAN_REQUESTS_PER_TICK);
        if (isBudgetExceeded()) {
            return;
        }
        processPlans(level, PROCESS_STEPS_PER_TICK);
        if (isBudgetExceeded()) {
            return;
        }
        processSafeZoneChunks(level, SAFE_ZONE_CHUNKS_PER_TICK);
    }

    private void ensureLoaded(ServerLevel level) {
        if (saveData == null) {
            saveData = RoadSaveData.get(level);
            saveData.loadInto(registry);
            if (registry.getNodeCount() >= 2) {
                needsPlanning = true;
            }
        }
    }

    public void save(ServerLevel level) {
        if (saveData == null) {
            saveData = RoadSaveData.get(level);
        }
        saveData.save(registry);
    }

    private void autoInitialize(ServerLevel level) {
        if (registry.getNodeCount() > 0) {
            return;
        }

        BlockPos spawnPos = level.getSharedSpawnPos();
        WowCraft.LOGGER.info("Auto-initializing road generator near spawn {}", spawnPos);

        StructureFinder.findStructuresAsync(level, spawnPos, AUTO_SEARCH_RADIUS, AUTO_MAX_STRUCTURES)
                .thenAccept(structures -> {
                    level.getServer().execute(() -> {
                        int added = 0;
                        for (BlockPos pos : structures) {
                            if (addStructure(level, pos, "auto")) {
                                added++;
                            }
                        }

                        WowCraft.LOGGER.info("Auto search found {} structures, added {}", structures.size(), added);
                    });
                });
    }

    private void updateZoneHubs(ServerLevel level) {
        ZoneSaveData zoneData = ZoneSaveData.get(level);

        for (ServerPlayer player : level.players()) {
            BiomeGroup group = BiomeGroup.fromBiome(
                    level.getBiome(player.blockPosition()).unwrapKey().orElse(null));
            if (group == null || !group.isNameable()) {
                continue;
            }

            ZoneRegion zone = zoneData.getZone(group);
            if (zone == null) {
                continue;
            }

            ensureZoneHub(level, group, zone, player.blockPosition());
        }
    }

    private void ensureZoneHub(ServerLevel level, BiomeGroup group, ZoneRegion zone, BlockPos playerPos) {
        UUID hubId = RoadNode.makeDeterministicId(RoadNodeType.HUB, group.name(), BlockPos.ZERO);
        if (registry.getNode(hubId) != null) {
            return;
        }

        BlockPos hubPos = pickHubPosition(level, zone.center(), playerPos);
        RoadNode hub = new RoadNode(hubId, hubPos, RoadNodeType.HUB);
        if (registry.addNode(hub)) {
            WowCraft.LOGGER.info("Road hub added for zone {} at {}", group, hubPos);
            enqueueNode(hub);
        }
    }

    private void ensureRegionalHub(ServerLevel level, BlockPos anchor) {
        int regionX = Math.floorDiv(anchor.getX(), REGIONAL_HUB_SIZE);
        int regionZ = Math.floorDiv(anchor.getZ(), REGIONAL_HUB_SIZE);
        String key = "region-" + regionX + "-" + regionZ;
        UUID hubId = RoadNode.makeDeterministicId(RoadNodeType.HUB, key, BlockPos.ZERO);
        if (registry.getNode(hubId) != null) {
            return;
        }

        int centerX = regionX * REGIONAL_HUB_SIZE + REGIONAL_HUB_SIZE / 2;
        int centerZ = regionZ * REGIONAL_HUB_SIZE + REGIONAL_HUB_SIZE / 2;
        BlockPos center = new BlockPos(centerX, anchor.getY(), centerZ);
        BlockPos hubPos = pickHubPosition(level, center, anchor);
        RoadNode hub = new RoadNode(hubId, hubPos, RoadNodeType.HUB);
        if (registry.addNode(hub)) {
            WowCraft.LOGGER.info("Road hub added for region {} at {}", key, hubPos);
            enqueueNode(hub);
        }
    }

    private BlockPos pickHubPosition(ServerLevel level, BlockPos zoneCenter, BlockPos fallback) {
        BlockPos chosen = zoneCenter;
        if (!level.hasChunk(chosen.getX() >> 4, chosen.getZ() >> 4)) {
            chosen = fallback;
        }

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                chosen.getX(), chosen.getZ());
        BlockPos result = new BlockPos(chosen.getX(), y, chosen.getZ());
        if (BiomeRoadMaterial.isOceanBiome(level.getBiome(result))) {
            int fallbackY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                    fallback.getX(), fallback.getZ());
            return new BlockPos(fallback.getX(), fallbackY, fallback.getZ());
        }
        return result;
    }

    private void registerStructuresFromChunk(ServerLevel level, LevelChunk chunk) {
        for (Map.Entry<Structure, StructureStart> entry : chunk.getAllStarts().entrySet()) {
            StructureStart start = entry.getValue();
            if (start == null || !start.isValid()) {
                continue;
            }

            Structure structure = entry.getKey();
            // Get structure key from the level's registry access (structures are datapack
            // registries)
            var structureRegistry = level.registryAccess().lookupOrThrow(Registries.STRUCTURE);
            ResourceLocation id = structureRegistry.getKey(structure);
            if (id == null || !isAllowedStructure(id)) {
                continue;
            }

            BlockPos center = start.getBoundingBox().getCenter();
            if (!addStructure(level, center, id.toString())) {
                ensureStructureExtras(level, center, id.toString());
            }
        }
    }

    private boolean isAllowedStructure(ResourceLocation id) {
        String path = id.getPath();
        if (path.startsWith("village")) {
            return true;
        }
        if (path.startsWith("ruined_portal")) {
            return true;
        }
        return EXTRA_STRUCTURES.contains(path);
    }

    private boolean addStructure(ServerLevel level, BlockPos pos, String key) {
        UUID id = RoadNode.makeDeterministicId(RoadNodeType.STRUCTURE, key, pos);
        RoadNode node = new RoadNode(id, pos, RoadNodeType.STRUCTURE);
        boolean added = registry.addNode(node);
        if (added) {
            WowCraft.LOGGER.info("Road node added for structure at {}", pos);
            ensureStructureExtras(level, pos, key);
            enqueueNode(node);
        }
        return added;
    }

    private void ensureStructureExtras(ServerLevel level, BlockPos pos, String key) {
        ensureRegionalHub(level, pos);
        maybeAddLandmarks(level, pos, key);
        maybeAddTrailheads(level, pos, key);
    }

    private void maybeAddLandmarks(ServerLevel level, BlockPos center, String key) {
        RandomSource random = RandomSource.create(level.getSeed() ^ center.asLong());
        RoadLandmarkGenerator.findClearing(level, center, LANDMARK_RADIUS, random)
                .ifPresent(pos -> addLandmark(pos, RoadNodeType.CLEARING, "clearing-" + key));
    }

    private void maybeAddTrailheads(ServerLevel level, BlockPos center, String key) {
        RandomSource random = RandomSource.create(level.getSeed() ^ center.asLong() ^ 0x5f4d);
        double baseAngle = random.nextDouble() * Math.PI * 2.0;

        for (int i = 0; i < TRAIL_SPOKE_COUNT; i++) {
            double angle = baseAngle + (Math.PI * 2.0 / TRAIL_SPOKE_COUNT) * i;
            angle += (random.nextDouble() - 0.5) * 0.6;

            int minDist = (i == 0) ? TRAIL_SHORT_MIN_DISTANCE : TRAIL_LONG_MIN_DISTANCE;
            int maxDist = (i == 0) ? TRAIL_SHORT_MAX_DISTANCE : TRAIL_LONG_MAX_DISTANCE;
            int desired = minDist + random.nextInt(Math.max(1, maxDist - minDist + 1));

            BlockPos endpoint = findTrailEndpoint(level, center, angle, minDist, desired);
            if (endpoint != null) {
                addTrail(endpoint, "trail-" + key + "-" + i);
            }
        }
    }

    private void addLandmark(BlockPos pos, RoadNodeType type, String key) {
        if (isNodeNearby(pos, CLEARING_MIN_NODE_DISTANCE)) {
            return;
        }

        UUID id = RoadNode.makeDeterministicId(type, key, pos);
        RoadNode node = new RoadNode(id, pos, type);
        if (registry.addNode(node)) {
            WowCraft.LOGGER.info("Road landmark added: {} at {}", type, pos);
            enqueueNode(node);
        }
    }

    private void addTrail(BlockPos pos, String key) {
        if (isNodeNearby(pos, TRAIL_MIN_NODE_DISTANCE)) {
            return;
        }

        UUID id = RoadNode.makeDeterministicId(RoadNodeType.TRAIL, key, pos);
        RoadNode node = new RoadNode(id, pos, RoadNodeType.TRAIL);
        if (registry.addNode(node)) {
            WowCraft.LOGGER.info("Road trail added at {}", pos);
            enqueueNode(node);
        }
    }

    private BlockPos findTrailEndpoint(ServerLevel level, BlockPos center, double angle, int minDist, int maxDist) {
        BlockPos fallback = null;

        for (int dist = maxDist; dist >= minDist; dist -= TRAIL_SCAN_STEP) {
            int x = center.getX() + (int) Math.round(Math.cos(angle) * dist);
            int z = center.getZ() + (int) Math.round(Math.sin(angle) * dist);

            if (!level.hasChunk(x >> 4, z >> 4)) {
                if (fallback == null) {
                    fallback = new BlockPos(x, center.getY(), z);
                }
                continue;
            }

            SurfaceInfo info = getSurfaceInfo(level, new BlockPos(x, 0, z));
            if (info.isWater) {
                continue;
            }
            if (BiomeRoadMaterial.isOceanBiome(level.getBiome(info.groundPos))) {
                continue;
            }
            if (hasTreeColumn(level, x, info.groundPos.getY(), z)) {
                continue;
            }

            return new BlockPos(x, center.getY(), z);
        }

        return fallback;
    }

    private boolean isNodeNearby(BlockPos pos, int radius) {
        int radiusSq = radius * radius;
        for (RoadNode node : registry.getNodes()) {
            if (node.getPosition().distSqr(pos) <= radiusSq) {
                return true;
            }
        }
        return false;
    }

    private void enqueueAllNodes() {
        for (RoadNode node : registry.getNodes()) {
            enqueueNode(node);
        }
    }

    private void enqueueNode(RoadNode node) {
        if (pendingNodeIds.add(node.getId())) {
            pendingNodes.add(node);
        }
    }

    private void processPendingNodes(ServerLevel level, int maxNodes) {
        if (pendingNodes.isEmpty()) {
            return;
        }

        List<RoadNode> nodes = new ArrayList<>(registry.getNodes());
        int processed = 0;

        while (processed < maxNodes && !pendingNodes.isEmpty()) {
            RoadNode node = pendingNodes.poll();
            pendingNodeIds.remove(node.getId());

            List<RoadNode> nearest = findNearestNodes(level, node, nodes);
            for (RoadNode neighbor : nearest) {
                queuePlanRequest(level, node, neighbor, true);
            }
            processed++;
            if (isBudgetExceeded()) {
                break;
            }
        }
    }

    private List<RoadNode> findNearestNodes(ServerLevel level, RoadNode node, List<RoadNode> nodes) {
        List<RoadNode> candidates = new ArrayList<>();
        for (RoadNode other : nodes) {
            if (other.getId().equals(node.getId())) {
                continue;
            }
            double distSq = node.getPosition().distSqr(other.getPosition());
            if (node.getType() == RoadNodeType.HUB) {
                if (distSq <= (double) HUB_LONG_DISTANCE * HUB_LONG_DISTANCE) {
                    candidates.add(other);
                }
                continue;
            }

            if (node.getType() == RoadNodeType.TRAIL) {
                if (distSq <= (double) TRAIL_CONNECT_DISTANCE * TRAIL_CONNECT_DISTANCE) {
                    candidates.add(other);
                }
                continue;
            }

            if (other.getType() == RoadNodeType.TRAIL) {
                if (node.getType() == RoadNodeType.STRUCTURE
                        && distSq <= (double) TRAIL_CONNECT_DISTANCE * TRAIL_CONNECT_DISTANCE) {
                    candidates.add(other);
                }
                continue;
            }

            if (node.getType() == RoadNodeType.STRUCTURE && other.getType() == RoadNodeType.HUB) {
                if (distSq <= (double) STRUCTURE_HUB_DISTANCE * STRUCTURE_HUB_DISTANCE) {
                    candidates.add(other);
                }
            } else if (distSq <= (double) MAX_CONNECTION_DISTANCE * MAX_CONNECTION_DISTANCE) {
                candidates.add(other);
            }
        }

        candidates.sort(Comparator.comparingDouble(a -> a.getPosition().distSqr(node.getPosition())));
        if (node.getType() == RoadNodeType.CLEARING) {
            return selectClearingNeighbors(node, candidates);
        }
        if (node.getType() == RoadNodeType.HUB) {
            return selectHubNeighbors(level, node, candidates);
        }
        if (node.getType() == RoadNodeType.TRAIL) {
            return selectTrailNeighbors(node, candidates);
        }

        return selectStructureNeighbors(node, candidates);
    }

    private List<RoadNode> selectClearingNeighbors(RoadNode node, List<RoadNode> candidates) {
        RoadNode nearestStructure = null;
        double nearestStructureDist = Double.MAX_VALUE;

        for (RoadNode other : candidates) {
            if (other.getType() != RoadNodeType.STRUCTURE) {
                continue;
            }
            double distSq = node.getPosition().distSqr(other.getPosition());
            if (distSq < nearestStructureDist && distSq <= CLEARING_CONNECT_DISTANCE * CLEARING_CONNECT_DISTANCE) {
                nearestStructureDist = distSq;
                nearestStructure = other;
            }
        }

        if (nearestStructure != null) {
            return List.of(nearestStructure);
        }

        for (RoadNode other : candidates) {
            if (other.getType() == RoadNodeType.HUB) {
                return List.of(other);
            }
        }

        return Collections.emptyList();
    }

    private List<RoadNode> selectStructureNeighbors(RoadNode node, List<RoadNode> candidates) {
        RoadNode nearestHub = null;
        RoadNode nearestStructure = null;
        double hubDist = Double.MAX_VALUE;
        double structureDist = Double.MAX_VALUE;
        List<RoadNode> trails = new ArrayList<>();

        for (RoadNode other : candidates) {
            double distSq = node.getPosition().distSqr(other.getPosition());
            if (other.getType() == RoadNodeType.HUB && distSq < hubDist) {
                nearestHub = other;
                hubDist = distSq;
            } else if (other.getType() == RoadNodeType.STRUCTURE && distSq < structureDist) {
                nearestStructure = other;
                structureDist = distSq;
            } else if (other.getType() == RoadNodeType.TRAIL) {
                trails.add(other);
            }
        }

        trails.sort(Comparator.comparingDouble(a -> a.getPosition().distSqr(node.getPosition())));

        List<RoadNode> result = new ArrayList<>();
        if (nearestHub != null) {
            result.add(nearestHub);
        }

        int trailCount = 0;
        for (RoadNode trail : trails) {
            if (trailCount >= STRUCTURE_TRAIL_CONNECTIONS || result.size() >= MAX_CONNECTIONS_PER_NODE) {
                break;
            }
            result.add(trail);
            trailCount++;
        }

        boolean addedDirect = false;
        if (nearestStructure != null
                && structureDist <= (double) DIRECT_STRUCTURE_DISTANCE * DIRECT_STRUCTURE_DISTANCE
                && result.size() < MAX_CONNECTIONS_PER_NODE) {
            result.add(nearestStructure);
            addedDirect = true;
        }

        if (!addedDirect && nearestHub == null && nearestStructure != null
                && result.size() < MAX_CONNECTIONS_PER_NODE) {
            result.add(nearestStructure);
        }

        if (result.isEmpty() && !candidates.isEmpty()) {
            result.add(candidates.get(0));
        }

        return result;
    }

    private List<RoadNode> selectTrailNeighbors(RoadNode node, List<RoadNode> candidates) {
        RoadNode nearestStructure = null;
        RoadNode nearestHub = null;
        double structureDist = Double.MAX_VALUE;
        double hubDist = Double.MAX_VALUE;

        for (RoadNode other : candidates) {
            double distSq = node.getPosition().distSqr(other.getPosition());
            if (other.getType() == RoadNodeType.STRUCTURE && distSq < structureDist) {
                nearestStructure = other;
                structureDist = distSq;
            } else if (other.getType() == RoadNodeType.HUB && distSq < hubDist) {
                nearestHub = other;
                hubDist = distSq;
            }
        }

        if (nearestStructure != null) {
            return List.of(nearestStructure);
        }
        if (nearestHub != null) {
            return List.of(nearestHub);
        }

        return Collections.emptyList();
    }

    private List<RoadNode> selectHubNeighbors(ServerLevel level, RoadNode node, List<RoadNode> candidates) {
        BlockPos spawn = level.getSharedSpawnPos();
        double nodeDist = node.getPosition().distSqr(spawn);
        double minOutwardDist = nodeDist + (double) HUB_OUTWARD_MIN_DELTA * HUB_OUTWARD_MIN_DELTA;

        RoadNode nearestHub = null;
        double nearestHubDist = Double.MAX_VALUE;
        RoadNode outwardHub = null;
        double outwardDist = Double.MAX_VALUE;

        for (RoadNode other : candidates) {
            if (other.getType() != RoadNodeType.HUB) {
                continue;
            }

            double distSq = node.getPosition().distSqr(other.getPosition());
            if (distSq < nearestHubDist) {
                nearestHubDist = distSq;
                nearestHub = other;
            }

            double otherDist = other.getPosition().distSqr(spawn);
            if (otherDist > minOutwardDist) {
                if (otherDist < outwardDist) {
                    outwardDist = otherDist;
                    outwardHub = other;
                }
            }
        }

        List<RoadNode> result = new ArrayList<>();
        if (nearestHub != null) {
            result.add(nearestHub);
        }

        if (outwardHub != null && outwardHub != nearestHub) {
            result.add(outwardHub);
        }

        return result;
    }

    private void queuePlanRequest(ServerLevel level, RoadNode start, RoadNode end, boolean recordPair) {
        if (recordPair && registry.hasBuiltPair(start.getId(), end.getId())) {
            return;
        }

        long seed = level.getSeed() ^ start.getId().getMostSignificantBits() ^ end.getId().getLeastSignificantBits();
        planRequests.add(new RoadPlanRequest(start.getId(), end.getId(), start.getPosition(), end.getPosition(), seed));

        if (recordPair) {
            registry.addBuiltPair(start.getId(), end.getId());
        }
    }

    private void processPlanRequests(ServerLevel level, int maxRequests) {
        int processed = 0;
        while (processed < maxRequests && !planRequests.isEmpty()) {
            RoadPlanRequest request = planRequests.poll();
            if (request == null) {
                break;
            }

            List<BlockPos> path = planner.plan(level, request.startPos, request.endPos, request.seed);
            if (!path.isEmpty()) {
                activePlans.add(new RoadPlan(request.startId, request.endId, path, request.seed));
            }
            processed++;
            if (isBudgetExceeded()) {
                break;
            }
        }
    }

    private void processPlans(ServerLevel level, int maxSteps) {
        if (activePlans.isEmpty()) {
            return;
        }

        int stepsRemaining = maxSteps;
        Iterator<RoadPlan> iterator = activePlans.iterator();

        while (iterator.hasNext() && stepsRemaining > 0) {
            RoadPlan plan = iterator.next();
            int used = buildPlan(level, plan, stepsRemaining);
            stepsRemaining -= used;

            if (!plan.touchedChunks.isEmpty()) {
                queueSafeZoneChunks(plan.touchedChunks);
                plan.touchedChunks.clear();
            }

            if (plan.complete) {
                totalRoadsBuilt++;
                iterator.remove();
            }

            if (stepsRemaining <= 0 || isBudgetExceeded()) {
                break;
            }
        }
    }

    private int buildPlan(ServerLevel level, RoadPlan plan, int maxSteps) {
        int steps = 0;
        long nowTick = level.getGameTime();
        while (plan.nextIndex < plan.path.size() && steps < maxSteps) {
            if (isBudgetExceeded()) {
                break;
            }
            BlockPos centerPos = plan.path.get(plan.nextIndex);
            ChunkPos chunkPos = new ChunkPos(centerPos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                break;
            }
            if (isChunkInGrace(chunkPos, nowTick)) {
                break;
            }

            SurfaceInfo info = getSurfaceInfo(level, centerPos);
            if (info.isWater) {
                WaterRun run = scanWaterRun(level, plan.path, plan.nextIndex);
                if (run == null) {
                    break;
                }

                if (run.isHigh()) {
                    plan.nextIndex = run.endIndex + 1;
                    plan.hasLastY = false;
                    continue;
                }

                if (run.isWide()) {
                    placeDock(level, plan, run);
                    plan.complete = true;
                    plan.nextIndex = plan.path.size();
                    break;
                }

                placeBridge(level, plan, run);
                BlockPos bridgeEnd = plan.path.get(run.endIndex);
                SurfaceInfo bridgeInfo = getSurfaceInfo(level, bridgeEnd);
                plan.lastRoadY = bridgeInfo.surfaceY - 1;
                plan.hasLastY = true;
                plan.nextIndex = run.endIndex + 1;
                continue;
            }

            int desiredY = info.groundPos.getY();
            if (plan.hasLastY) {
                int delta = desiredY - plan.lastRoadY;
                if (delta > MAX_GRADE_STEP) {
                    desiredY = plan.lastRoadY + MAX_GRADE_STEP;
                } else if (delta < -MAX_GRADE_STEP) {
                    desiredY = plan.lastRoadY - MAX_GRADE_STEP;
                }
            }

            Integer placedY = placeRoadSection(level, plan, centerPos, desiredY);
            if (placedY != null) {
                plan.lastRoadY = placedY;
                plan.hasLastY = true;
            }
            plan.nextIndex++;
            steps++;
        }

        if (plan.nextIndex >= plan.path.size()) {
            plan.complete = true;
        }

        return steps;
    }

    private SurfaceInfo getSurfaceInfo(ServerLevel level, BlockPos centerPos) {
        int surfaceY = level.getHeight(Heightmap.Types.WORLD_SURFACE, centerPos.getX(), centerPos.getZ());
        BlockPos surfacePos = new BlockPos(centerPos.getX(), surfaceY - 1, centerPos.getZ());
        BlockState surfaceState = level.getBlockState(surfacePos);
        boolean water = surfaceState.getFluidState().is(FluidTags.WATER);

        int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, centerPos.getX(), centerPos.getZ());
        BlockPos groundPos = new BlockPos(centerPos.getX(), groundY - 1, centerPos.getZ());

        return new SurfaceInfo(surfacePos, groundPos, surfaceY, groundY, water);
    }

    private WaterRun scanWaterRun(ServerLevel level, List<BlockPos> path, int startIndex) {
        int endIndex = startIndex;
        int sumY = 0;
        int count = 0;

        while (endIndex < path.size()) {
            BlockPos pos = path.get(endIndex);
            ChunkPos chunkPos = new ChunkPos(pos);
            if (!level.hasChunk(chunkPos.x, chunkPos.z)) {
                return null;
            }

            SurfaceInfo info = getSurfaceInfo(level, pos);
            if (!info.isWater) {
                break;
            }

            sumY += info.surfaceY;
            count++;
            endIndex++;
        }

        if (count == 0) {
            return null;
        }

        return new WaterRun(startIndex, endIndex - 1, sumY / Math.max(1, count));
    }

    private Integer placeRoadSection(ServerLevel level, RoadPlan plan, BlockPos centerPos, int targetY) {
        SurfaceInfo info = getSurfaceInfo(level, centerPos);
        if (info.groundPos == null) {
            return null;
        }

        Holder<Biome> biome = level.getBiome(info.groundPos);
        if (BiomeRoadMaterial.isOceanBiome(biome)) {
            return null;
        }

        Block roadBlock = BiomeRoadMaterial.getRoadBlock(level, info.groundPos);
        Block edgeBlock = BiomeRoadMaterial.getEdgeBlock(level, info.groundPos);

        int centerGroundY = info.groundPos.getY();
        if (hasTreeColumn(level, centerPos.getX(), centerGroundY, centerPos.getZ())) {
            return null;
        }

        int halfWidth = roadWidth / 2;
        int edgeThickness = roadWidth >= 5 ? 1 : 0;
        int innerHalf = Math.max(0, halfWidth - edgeThickness);
        Integer placedCenterY = null;

        for (int dx = -halfWidth; dx <= halfWidth; dx++) {
            for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                int placeX = centerPos.getX() + dx;
                int placeZ = centerPos.getZ() + dz;
                int groundY = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, placeX, placeZ) - 1;
                if (hasTreeColumn(level, placeX, groundY, placeZ)) {
                    continue;
                }

                int desiredY = targetY;
                if (Math.abs(desiredY - groundY) > MAX_TERRAFORM_HEIGHT) {
                    desiredY = groundY;
                }

                if (desiredY > groundY) {
                    BlockPos fillPos = new BlockPos(placeX, groundY + 1, placeZ);
                    BlockState fillState = level.getBlockState(fillPos);
                    if (fillState.isAir() || fillState.getFluidState().is(FluidTags.WATER)) {
                        level.setBlock(fillPos, Blocks.DIRT.defaultBlockState(), 3);
                        totalBlocksPlaced++;
                    } else {
                        desiredY = groundY;
                    }
                } else if (desiredY < groundY) {
                    BlockPos cutPos = new BlockPos(placeX, groundY, placeZ);
                    BlockState cutState = level.getBlockState(cutPos);
                    if (isSoftGround(cutState)) {
                        level.setBlock(cutPos, Blocks.AIR.defaultBlockState(), 3);
                    } else {
                        desiredY = groundY;
                    }
                }

                BlockPos placePos = new BlockPos(placeX, desiredY, placeZ);
                BlockState currentState = level.getBlockState(placePos);
                if (currentState.isAir()
                        || currentState.getFluidState().is(FluidTags.WATER)
                        || currentState.is(BlockTags.LOGS)
                        || currentState.is(BlockTags.LEAVES)) {
                    continue;
                }

                boolean isEdge = Math.abs(dx) > innerHalf || Math.abs(dz) > innerHalf;
                Block blockToPlace = isEdge ? edgeBlock : roadBlock;

                if (blockToPlace == Blocks.DIRT_PATH) {
                    BlockPos above = placePos.above();
                    BlockState aboveState = level.getBlockState(above);
                    if (aboveState.is(BlockTags.LOGS)) {
                        continue;
                    }
                    if (!aboveState.isAir()) {
                        level.setBlock(above, Blocks.AIR.defaultBlockState(), 3);
                    }
                }

                level.setBlock(placePos, blockToPlace.defaultBlockState(), 3);
                totalBlocksPlaced++;
                plan.touchedChunks.add(new ChunkPos(placePos).toLong());

                if (dx == 0 && dz == 0) {
                    placedCenterY = desiredY;
                }
            }
        }

        RandomSource localRandom = RandomSource.create(plan.seed ^ centerPos.asLong());
        if (localRandom.nextInt(decorationChance) == 0) {
            placeDecoration(level, info.groundPos, localRandom);
        }

        return placedCenterY;
    }

    private void placeBridge(ServerLevel level, RoadPlan plan, WaterRun run) {
        int halfWidth = roadWidth / 2;
        Block bridgeBlock = Blocks.OAK_PLANKS;

        for (int i = run.startIndex; i <= run.endIndex; i++) {
            BlockPos centerPos = plan.path.get(i);
            SurfaceInfo info = getSurfaceInfo(level, centerPos);
            int deckY = info.surfaceY - 1;

            for (int dx = -halfWidth; dx <= halfWidth; dx++) {
                for (int dz = -halfWidth; dz <= halfWidth; dz++) {
                    int placeX = centerPos.getX() + dx;
                    int placeZ = centerPos.getZ() + dz;
                    BlockPos placePos = new BlockPos(placeX, deckY, placeZ);

                    level.setBlock(placePos, bridgeBlock.defaultBlockState(), 3);
                    totalBlocksPlaced++;
                    plan.touchedChunks.add(new ChunkPos(placePos).toLong());
                }
            }
        }
    }

    private void placeDock(ServerLevel level, RoadPlan plan, WaterRun run) {
        if (run.startIndex == 0) {
            return;
        }

        BlockPos landPos = plan.path.get(run.startIndex - 1);
        BlockPos waterPos = plan.path.get(run.startIndex);
        int dx = Integer.compare(waterPos.getX(), landPos.getX());
        int dz = Integer.compare(waterPos.getZ(), landPos.getZ());
        if (dx == 0 && dz == 0) {
            return;
        }

        for (int i = 0; i <= DOCK_LENGTH; i++) {
            BlockPos stepPos = landPos.offset(dx * i, 0, dz * i);
            SurfaceInfo info = getSurfaceInfo(level, stepPos);
            int deckY = info.surfaceY - 1;
            BlockPos deckPos = new BlockPos(stepPos.getX(), deckY, stepPos.getZ());

            BlockState current = level.getBlockState(deckPos);
            if (current.isAir() || current.getFluidState().is(FluidTags.WATER)) {
                level.setBlock(deckPos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
                totalBlocksPlaced++;
                plan.touchedChunks.add(new ChunkPos(deckPos).toLong());
            }
        }
    }

    private void placeDecoration(ServerLevel level, BlockPos roadPos, RandomSource random) {
        int side = random.nextBoolean() ? (roadWidth / 2 + 2) : -(roadWidth / 2 + 2);
        BlockPos decorPos = roadPos.offset(side, 0, 0);

        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                decorPos.getX(), decorPos.getZ());
        decorPos = new BlockPos(decorPos.getX(), y, decorPos.getZ());

        if (level.getBlockState(decorPos.below()).getFluidState().is(FluidTags.WATER)) {
            return;
        }

        Block fenceBlock = BiomeRoadMaterial.getFenceBlock(level, decorPos);

        for (int i = 0; i < 3; i++) {
            level.setBlock(decorPos.above(i), fenceBlock.defaultBlockState(), 3);
        }
        level.setBlock(decorPos.above(3), Blocks.LANTERN.defaultBlockState(), 3);
        totalBlocksPlaced += 4;
    }

    private void queueSafeZoneChunks(Set<Long> chunkKeys) {
        for (long key : chunkKeys) {
            if (pendingSafeZoneChunkSet.add(key)) {
                pendingSafeZoneChunks.add(key);
            }
        }
    }

    private void processSafeZoneChunks(ServerLevel level, int maxChunks) {
        int processed = 0;
        while (processed < maxChunks && !pendingSafeZoneChunks.isEmpty()) {
            long key = pendingSafeZoneChunks.poll();
            pendingSafeZoneChunkSet.remove(key);
            ChunkPos chunkPos = new ChunkPos(key);
            SpawnPoolManager.applyRoadSafeZoneForChunk(level, chunkPos);
            processed++;
            if (isBudgetExceeded()) {
                break;
            }
        }
    }

    private boolean isBudgetExceeded() {
        return System.nanoTime() - tickStartTime >= MAX_NANOS_PER_TICK;
    }

    private void recordChunkLoad(ServerLevel level, LevelChunk chunk) {
        recentChunkLoads.put(chunk.getPos().toLong(), level.getGameTime());
    }

    private boolean isChunkInGrace(ChunkPos chunkPos, long nowTick) {
        Long loadedAt = recentChunkLoads.get(chunkPos.toLong());
        return loadedAt != null && nowTick - loadedAt < CHUNK_BUILD_GRACE_TICKS;
    }

    private void cleanupRecentChunks(long nowTick) {
        recentChunkCleanupCounter++;
        if (recentChunkCleanupCounter < RECENT_CHUNK_CLEANUP_INTERVAL) {
            return;
        }
        recentChunkCleanupCounter = 0;

        Iterator<Map.Entry<Long, Long>> iterator = recentChunkLoads.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<Long, Long> entry = iterator.next();
            if (nowTick - entry.getValue() > CHUNK_BUILD_GRACE_TICKS) {
                iterator.remove();
            }
        }
    }

    private boolean hasTreeColumn(ServerLevel level, int x, int baseY, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = baseY; y <= baseY + TREE_SCAN_HEIGHT; y++) {
            pos.set(x, y, z);
            BlockState state = level.getBlockState(pos);
            if (state.is(BlockTags.LOGS)) {
                return true;
            }
        }
        return false;
    }

    private boolean isSoftGround(BlockState state) {
        if (state.is(BlockTags.LOGS) || state.is(BlockTags.LEAVES)) {
            return false;
        }
        return state.getBlock() == Blocks.DIRT
                || state.getBlock() == Blocks.GRASS_BLOCK
                || state.getBlock() == Blocks.COARSE_DIRT
                || state.getBlock() == Blocks.PODZOL
                || state.getBlock() == Blocks.MYCELIUM
                || state.getBlock() == Blocks.SAND
                || state.getBlock() == Blocks.RED_SAND
                || state.getBlock() == Blocks.GRAVEL;
    }

    public void buildManualRoad(ServerLevel level, BlockPos start, BlockPos end) {
        RoadNode tempStart = new RoadNode(UUID.randomUUID(), start, RoadNodeType.STRUCTURE);
        RoadNode tempEnd = new RoadNode(UUID.randomUUID(), end, RoadNodeType.STRUCTURE);
        queuePlanRequest(level, tempStart, tempEnd, false);
    }

    public void planAllRoads(ServerLevel level) {
        ensureLoaded(level);
        if (registry.getNodeCount() < 2) {
            return;
        }
        enqueueAllNodes();
    }

    public void forceSearchAndBuild(ServerLevel level, BlockPos center, int radius) {
        WowCraft.LOGGER.info("Force searching for structures within {} blocks of {}", radius, center);

        StructureFinder.findStructuresAsync(level, center, radius, 10)
                .thenAccept(structures -> {
                    level.getServer().execute(() -> {
                        int added = 0;
                        for (BlockPos pos : structures) {
                            if (addStructure(level, pos, "force")) {
                                added++;
                            } else {
                                ensureStructureExtras(level, pos, "force");
                            }
                        }

                        WowCraft.LOGGER.info("Force search: {} predicted, {} added, {} total nodes",
                                structures.size(), added, registry.getNodeCount());
                    });
                });
    }

    public int getStructureCount() {
        int count = 0;
        for (RoadNode node : registry.getNodes()) {
            if (node.getType() == RoadNodeType.STRUCTURE) {
                count++;
            }
        }
        return count;
    }

    public int getTotalRoadsBuilt() {
        return totalRoadsBuilt;
    }

    public int getTotalBlocksPlaced() {
        return totalBlocksPlaced;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public int getActivePlanCount() {
        return activePlans.size();
    }

    public void reset() {
        registry.clear();
        activePlans.clear();
        pendingNodes.clear();
        pendingNodeIds.clear();
        planRequests.clear();
        pendingSafeZoneChunks.clear();
        pendingSafeZoneChunkSet.clear();
        recentChunkLoads.clear();
        totalRoadsBuilt = 0;
        totalBlocksPlaced = 0;
        initialized = false;
        initDelayTicks = 0;
        needsPlanning = false;
        roadTickCounter = 0;
        tickStartTime = 0L;
        recentChunkCleanupCounter = 0;
    }

    private static class RoadPlan {
        private final UUID startId;
        private final UUID endId;
        private final List<BlockPos> path;
        private final long seed;
        private int nextIndex = 0;
        private boolean complete = false;
        private final Set<Long> touchedChunks = new HashSet<>();
        private int lastRoadY = 0;
        private boolean hasLastY = false;

        private RoadPlan(UUID startId, UUID endId, List<BlockPos> path, long seed) {
            this.startId = startId;
            this.endId = endId;
            this.path = path;
            this.seed = seed;
        }
    }

    private static class RoadPlanRequest {
        private final UUID startId;
        private final UUID endId;
        private final BlockPos startPos;
        private final BlockPos endPos;
        private final long seed;

        private RoadPlanRequest(UUID startId, UUID endId, BlockPos startPos, BlockPos endPos, long seed) {
            this.startId = startId;
            this.endId = endId;
            this.startPos = startPos;
            this.endPos = endPos;
            this.seed = seed;
        }
    }

    private record SurfaceInfo(BlockPos surfacePos, BlockPos groundPos, int surfaceY, int groundY, boolean isWater) {
    }

    private static class WaterRun {
        private final int startIndex;
        private final int endIndex;
        private final int avgSurfaceY;

        private WaterRun(int startIndex, int endIndex, int avgSurfaceY) {
            this.startIndex = startIndex;
            this.endIndex = endIndex;
            this.avgSurfaceY = avgSurfaceY;
        }

        private boolean isWide() {
            return (endIndex - startIndex + 1) > BRIDGE_MAX_WATER_WIDTH;
        }

        private boolean isHigh() {
            return avgSurfaceY > BRIDGE_MAX_Y;
        }
    }
}

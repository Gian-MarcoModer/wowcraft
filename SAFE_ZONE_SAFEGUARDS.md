# Safe Zone Safeguards - Anti-Exploit & Quest Protection

## Problem 1: Block Spam Exploits

### Exploit Scenarios
```java
// Malicious player could:
1. Place beds everywhere to create "safe zones"
2. Place torches around enemy camps to suppress spawns
3. Grief other players by blocking their combat zones
4. Place minimal blocks to claim huge areas
```

## Solution: Density-Based Safe Zone Detection

### Requirements for Safe Zone Status

```java
/**
 * Safe zones require SIGNIFICANT player investment, not just spam.
 * Must have multiple indicators of actual base building.
 */

public class SafeZoneDetector {

    // Thresholds for safe zone qualification
    private static final int MIN_PLAYER_BLOCKS = 50;        // Minimum unique blocks
    private static final int MIN_DIFFERENT_TYPES = 5;       // Must use variety of blocks
    private static final int DETECTION_RADIUS = 48;         // Check 48 block radius
    private static final int SCAN_SAMPLE_SIZE = 100;        // Sample points to check
    private static final float DENSITY_THRESHOLD = 0.15f;   // 15% of samples must be player blocks

    /**
     * Check if position qualifies as a safe zone.
     * Returns true only if there's a substantial player base.
     */
    public static boolean isInSafeZone(ServerLevel level, BlockPos pos) {
        // Quick checks first (cheap)
        if (!hasMinimumPlayerActivity(level, pos)) {
            return false;
        }

        // Detailed check (expensive, cached)
        SafeZoneCache cache = getSafeZoneCache(level);
        ChunkPos chunk = new ChunkPos(pos);

        // Check cache first
        SafeZoneStatus cached = cache.get(chunk);
        if (cached != null && !cached.isExpired(level.getGameTime())) {
            return cached.isSafe();
        }

        // Perform full check
        boolean isSafe = performDetailedSafeZoneCheck(level, pos);

        // Cache result for 5 minutes
        cache.put(chunk, new SafeZoneStatus(isSafe, level.getGameTime() + 6000));

        return isSafe;
    }

    /**
     * Quick preliminary check - reject obvious non-bases.
     */
    private static boolean hasMinimumPlayerActivity(ServerLevel level, BlockPos center) {
        // Check spawn distance first (always safe near spawn)
        if (center.distSqr(level.getSharedSpawnPos()) < 500 * 500) {
            return true;
        }

        // Quick scan: any player blocks at all?
        int found = 0;
        for (int i = 0; i < 10; i++) {
            BlockPos sample = center.offset(
                level.random.nextInt(32) - 16,
                0,
                level.random.nextInt(32) - 16
            );

            if (isPlayerBlock(level.getBlockState(sample).getBlock())) {
                found++;
                if (found >= 3) return true;  // Early exit if we find some
            }
        }

        return false;
    }

    /**
     * Detailed safe zone check - requires substantial building.
     */
    private static boolean performDetailedSafeZoneCheck(ServerLevel level, BlockPos center) {
        Map<Block, Integer> blockCounts = new HashMap<>();
        int totalPlayerBlocks = 0;
        int samplesChecked = 0;

        // Random sampling in radius
        Random random = new Random(center.asLong());

        for (int i = 0; i < SCAN_SAMPLE_SIZE; i++) {
            int offsetX = random.nextInt(DETECTION_RADIUS * 2) - DETECTION_RADIUS;
            int offsetZ = random.nextInt(DETECTION_RADIUS * 2) - DETECTION_RADIUS;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING,
                center.getX() + offsetX, center.getZ() + offsetZ);

            BlockPos sample = new BlockPos(center.getX() + offsetX, y, center.getZ() + offsetZ);

            // Check this position and a few blocks around it (vertical scan)
            for (int dy = -2; dy <= 3; dy++) {
                BlockPos check = sample.offset(0, dy, 0);
                Block block = level.getBlockState(check).getBlock();

                if (isPlayerBlock(block)) {
                    totalPlayerBlocks++;
                    blockCounts.put(block, blockCounts.getOrDefault(block, 0) + 1);
                }
                samplesChecked++;
            }
        }

        // Check thresholds
        boolean hasEnoughBlocks = totalPlayerBlocks >= MIN_PLAYER_BLOCKS;
        boolean hasVariety = blockCounts.size() >= MIN_DIFFERENT_TYPES;
        boolean hasDensity = (float) totalPlayerBlocks / samplesChecked >= DENSITY_THRESHOLD;

        // Reject spam: if one block type is >70% of total, it's spam
        boolean isSpam = false;
        for (int count : blockCounts.values()) {
            if ((float) count / totalPlayerBlocks > 0.7f) {
                isSpam = true;
                break;
            }
        }

        return hasEnoughBlocks && hasVariety && hasDensity && !isSpam;
    }

    /**
     * Check if a block is "player placed" indicator.
     * Only blocks that indicate actual base building.
     */
    private static boolean isPlayerBlock(Block block) {
        // High-value blocks (strong indicators)
        if (block == Blocks.CRAFTING_TABLE ||
            block == Blocks.FURNACE ||
            block == Blocks.BLAST_FURNACE ||
            block == Blocks.SMOKER ||
            block == Blocks.CHEST ||
            block == Blocks.BARREL ||
            block == Blocks.ENCHANTING_TABLE ||
            block == Blocks.ANVIL ||
            block == Blocks.BREWING_STAND) {
            return true;
        }

        // Building blocks (moderate indicators)
        if (block == Blocks.COBBLESTONE ||
            block == Blocks.STONE_BRICKS ||
            block == Blocks.BRICKS ||
            block == Blocks.PLANKS ||
            block == Blocks.OAK_PLANKS ||
            block == Blocks.SPRUCE_PLANKS ||
            block == Blocks.GLASS ||
            block == Blocks.GLASS_PANE) {
            return true;
        }

        // Exclude spam-able blocks
        // NOTE: Beds and torches NOT counted to prevent spam
        // Light level check is separate (see below)

        return false;
    }
}
```

### Light Level Check (Separate from Block Check)

```java
/**
 * Light level provides BONUS safe zone chance, but not automatic.
 * Prevents torch spam exploit.
 */
public static float getSafeZoneMultiplier(ServerLevel level, BlockPos pos) {
    float multiplier = 1.0f;

    // Light level adds safety, but doesn't create safe zone alone
    int lightLevel = level.getMaxLocalRawBrightness(pos);
    if (lightLevel >= 12) {
        multiplier *= 0.7f;  // 30% less hostile spawns in well-lit areas
    } else if (lightLevel >= 8) {
        multiplier *= 0.85f;  // 15% less hostile spawns in moderately lit
    }

    // If also has player structures, compound the effect
    if (performDetailedSafeZoneCheck(level, pos)) {
        multiplier *= 0.5f;  // Additional 50% reduction
    }

    return multiplier;
}
```

## Problem 2: Named/Quest Mob Suppression

### POI Protection System

```java
/**
 * POI spawns are NEVER suppressed by safe zones or player activity.
 * They represent story content and must always be available.
 */

public class POISpawnProtection {

    /**
     * Check if a spawn point is protected from safe zone suppression.
     */
    public static boolean isProtectedSpawn(SpawnPoint point) {
        // All POI spawns are protected
        if (point.getType() == SpawnPointType.POI_CAMP ||
            point.getType() == SpawnPointType.POI_LAIR ||
            point.getType() == SpawnPointType.POI_PATROL ||
            point.getType() == SpawnPointType.POI_RESOURCE) {
            return true;
        }

        // Named/quest mobs are protected
        if (point.hasNamedMob() || point.isQuestMob()) {
            return true;
        }

        // Elite spawns are protected
        if (point.getLevelBonus() > 0) {
            return true;
        }

        return false;
    }

    /**
     * Apply safe zone modifiers, but skip protected spawns.
     */
    public static void applySafeZoneToSpawnPoint(SpawnPoint point, boolean isInSafeZone) {
        // Protected spawns ignore safe zones
        if (isProtectedSpawn(point)) {
            return;  // No modification
        }

        // Only scatter spawns can be modified
        if (point.getType() != SpawnPointType.SCATTER) {
            return;
        }

        // Apply safe zone effects
        if (isInSafeZone) {
            // Convert hostile to neutral (60% chance)
            if (point.getHostility() == SpawnHostility.ALWAYS_HOSTILE) {
                if (Math.random() < 0.6) {
                    point.setHostility(SpawnHostility.NEUTRAL_DEFENSIVE);
                    point.setMobOptions(getNeutralMobOptions(point.getBiome()));
                }
            }
        }
    }
}
```

### Named Mob System

```java
/**
 * Named mobs are special spawns that appear at specific locations.
 * Used for quests, lore, and unique encounters.
 */
public class NamedMobSpawn extends SpawnPoint {
    private final String mobName;
    private final boolean isQuestMob;
    private final boolean isUnique;  // Only one can exist at a time
    private final long respawnDelayTicks;

    public NamedMobSpawn(
            UUID id,
            BlockPos position,
            String mobName,
            ResourceLocation mobType,
            int level,
            boolean isQuestMob,
            long respawnDelayTicks) {

        super(id, position, SpawnPointType.NAMED_MOB,
            SpawnHostility.ALWAYS_HOSTILE,
            List.of(new MobOption(mobType, 100, 1, 1)),
            1, 1);

        this.mobName = mobName;
        this.isQuestMob = isQuestMob;
        this.isUnique = true;
        this.respawnDelayTicks = respawnDelayTicks;

        // Named mobs are ALWAYS active, never suppressed
        this.setAlwaysActive(true);
        this.setProtected(true);
    }

    @Override
    public boolean canBeAffectedBySafeZone() {
        return false;  // Named mobs ignore safe zones
    }

    @Override
    public boolean canBeDeactivatedByRotation() {
        return false;  // Named mobs never deactivate
    }

    @Override
    public void spawnEntity(ServerLevel level) {
        // Check if unique mob already exists
        if (isUnique && isAlreadySpawned(level)) {
            return;
        }

        super.spawnEntity(level);

        // Apply custom name
        if (currentEntity != null && currentEntity instanceof Mob mob) {
            mob.setCustomName(Component.literal("§6" + mobName));
            mob.setCustomNameVisible(true);

            // Mark as quest mob if applicable
            if (isQuestMob) {
                // Add quest marker data
                mob.setAttached(QuestMobMarker.KEY, new QuestMobMarker(this.id));
            }
        }
    }
}
```

### POI Generation with Named Mobs

```java
/**
 * Generate POIs with named mob spawns.
 */
public class POIGenerator {

    public static List<PointOfInterest> generatePOIsForRegion(
            ServerLevel level,
            BlockPos regionCenter,
            long worldSeed,
            BiomeGroup biomeGroup) {

        List<PointOfInterest> pois = new ArrayList<>();
        Random random = new Random(getSeed(regionCenter, worldSeed));

        // Regular POIs (camps, patrols, etc.)
        pois.addAll(generateRegularPOIs(level, regionCenter, biomeGroup, random));

        // Named mob spawns (5% chance per region)
        if (random.nextFloat() < 0.05f) {
            NamedMobSpawn namedMob = generateNamedMob(level, regionCenter, biomeGroup, random);
            if (namedMob != null) {
                pois.add(namedMob);
                WowCraft.LOGGER.info("Generated named mob '{}' at {}",
                    namedMob.getMobName(), namedMob.getPosition());
            }
        }

        return pois;
    }

    private static NamedMobSpawn generateNamedMob(
            ServerLevel level,
            BlockPos regionCenter,
            BiomeGroup biome,
            Random random) {

        // Find suitable location (away from other POIs)
        BlockPos pos = findIsolatedLocation(level, regionCenter, 300, random);
        if (pos == null) return null;

        // Generate name and stats
        String name = MobNameGenerator.generateName(biome, random);
        ResourceLocation mobType = selectEliteMobType(biome, random);
        int level = calculateRegionLevel(regionCenter) + 2;  // +2 levels for named

        return new NamedMobSpawn(
            UUID.randomUUID(),
            pos,
            name,
            mobType,
            level,
            false,  // Not a quest mob (those are added by quest system)
            6000    // 5 minute respawn (100 ticks/sec × 60 sec × 5)
        );
    }
}
```

## Quest Mob Protection

```java
/**
 * Quest system can register specific spawns as quest objectives.
 * These are absolutely protected from any suppression.
 */
public class QuestMobRegistry {

    private static final Map<UUID, QuestMobData> questMobs = new ConcurrentHashMap<>();

    /**
     * Register a spawn point as a quest objective.
     * This prevents ANY modification to the spawn.
     */
    public static void registerQuestMob(UUID spawnPointId, String questId, String objectiveName) {
        questMobs.put(spawnPointId, new QuestMobData(questId, objectiveName));

        // Mark the spawn point as protected
        SpawnPoint point = SpawnPoolManager.getSpawnPoint(spawnPointId);
        if (point != null) {
            point.setProtected(true);
            point.setAlwaysActive(true);
            point.setQuestMob(true);

            WowCraft.LOGGER.info("Registered quest mob '{}' for quest '{}'",
                objectiveName, questId);
        }
    }

    /**
     * Check if a spawn point is a quest objective.
     */
    public static boolean isQuestMob(UUID spawnPointId) {
        return questMobs.containsKey(spawnPointId);
    }

    /**
     * Get quest data for a spawn point.
     */
    public static QuestMobData getQuestData(UUID spawnPointId) {
        return questMobs.get(spawnPointId);
    }
}

public record QuestMobData(String questId, String objectiveName) {}
```

## Anti-Griefing Measures

### Time-Based Base Detection

```java
/**
 * Require time investment to establish safe zone.
 * Prevents instant griefing.
 */
public class BaseAgeTracker {

    private static final Map<ChunkPos, Long> baseEstablishedTime = new ConcurrentHashMap<>();
    private static final long MIN_BASE_AGE_TICKS = 72000;  // 1 hour (20 ticks/sec × 3600 sec)

    /**
     * Track when a chunk first had player structures.
     */
    public static void onPlayerBlockPlaced(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);

        // Record first placement time
        baseEstablishedTime.putIfAbsent(chunk, level.getGameTime());
    }

    /**
     * Check if a base is old enough to grant safe zone status.
     */
    public static boolean isEstablishedBase(ServerLevel level, BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        Long establishedTime = baseEstablishedTime.get(chunk);

        if (establishedTime == null) {
            return false;
        }

        long age = level.getGameTime() - establishedTime;
        return age >= MIN_BASE_AGE_TICKS;
    }

    /**
     * Modified safe zone check with age requirement.
     */
    public static boolean isInSafeZone(ServerLevel level, BlockPos pos) {
        // Must be established base (1+ hour old)
        if (!isEstablishedBase(level, pos)) {
            return false;
        }

        // Must meet density/variety requirements
        return SafeZoneDetector.performDetailedSafeZoneCheck(level, pos);
    }
}
```

### Admin Override System

```java
/**
 * Server admins can manually protect or un-protect areas.
 */
public class SpawnZoneOverride {

    private static final Map<ChunkPos, ZoneOverride> overrides = new ConcurrentHashMap<>();

    public enum OverrideType {
        FORCE_SAFE,      // Always safe zone, ignore blocks
        FORCE_HOSTILE,   // Always hostile, ignore player structures
        FORCE_PROTECTED, // POI-level protection (named mobs always spawn)
        NONE            // No override, normal rules
    }

    /**
     * Admin command: /spawnzone protect <radius>
     * Protects an area from safe zone suppression.
     */
    public static void setZoneOverride(BlockPos center, int radius, OverrideType type) {
        int chunkRadius = (radius / 16) + 1;
        ChunkPos centerChunk = new ChunkPos(center);

        for (int x = -chunkRadius; x <= chunkRadius; x++) {
            for (int z = -chunkRadius; z <= chunkRadius; z++) {
                ChunkPos chunk = new ChunkPos(centerChunk.x + x, centerChunk.z + z);
                overrides.put(chunk, new ZoneOverride(type, center, radius));
            }
        }

        WowCraft.LOGGER.info("Set zone override '{}' at {} with radius {}",
            type, center, radius);
    }

    /**
     * Check for admin overrides before applying safe zone logic.
     */
    public static OverrideType getOverride(BlockPos pos) {
        ChunkPos chunk = new ChunkPos(pos);
        ZoneOverride override = overrides.get(chunk);

        if (override != null && override.contains(pos)) {
            return override.type();
        }

        return OverrideType.NONE;
    }
}

record ZoneOverride(OverrideType type, BlockPos center, int radius) {
    public boolean contains(BlockPos pos) {
        return pos.distSqr(center) <= radius * radius;
    }
}
```

## Final Safe Zone Logic

```java
/**
 * Complete safe zone check with all protections.
 */
public static boolean shouldReduceHostileSpawns(ServerLevel level, BlockPos pos, SpawnPoint point) {
    // 1. Check if spawn is protected (POI, quest, named mob)
    if (POISpawnProtection.isProtectedSpawn(point)) {
        return false;  // Never reduce protected spawns
    }

    // 2. Check admin override
    OverrideType override = SpawnZoneOverride.getOverride(pos);
    if (override == OverrideType.FORCE_HOSTILE || override == OverrideType.FORCE_PROTECTED) {
        return false;  // Admin says hostile spawns stay
    }
    if (override == OverrideType.FORCE_SAFE) {
        return true;  // Admin says it's safe
    }

    // 3. Check if near spawn point (always safe)
    if (pos.distSqr(level.getSharedSpawnPos()) < 500 * 500) {
        return true;
    }

    // 4. Check if established base (1+ hour old + density requirements)
    if (!BaseAgeTracker.isEstablishedBase(level, pos)) {
        return false;  // Too new, no safe zone yet
    }

    // 5. Check density/variety requirements
    if (!SafeZoneDetector.performDetailedSafeZoneCheck(level, pos)) {
        return false;  // Not enough building
    }

    // All checks passed - this is a legitimate safe zone
    return true;
}
```

## Summary of Protections

### Against Block Spam:
✅ Requires 50+ player blocks
✅ Requires 5+ different block types
✅ Requires 15% density in 48 block radius
✅ Rejects single-block spam (>70% same block)
✅ Excludes easily-spammed blocks (beds, torches)
✅ Requires 1 hour base age
✅ Results cached for 5 minutes

### Against Quest Suppression:
✅ All POI spawns are protected
✅ Named mobs always spawn
✅ Quest mobs marked and protected
✅ Elite spawns never suppressed
✅ Admin override system
✅ Protected spawns ignore safe zones

### Performance:
✅ Quick rejection checks first
✅ Expensive checks cached
✅ Cache expires after 5 minutes
✅ Only scatter spawns affected
✅ POIs always active

This should prevent exploits while protecting gameplay content!

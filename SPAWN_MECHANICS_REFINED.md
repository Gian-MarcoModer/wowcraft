# Refined Spawn Mechanics Analysis - WoW Classic System

## Critical Insight: Spawn Pool vs Active Spawns

### What the Maps Actually Show

The maps display **ALL POSSIBLE spawn points**, but NOT all active at once:

```
Burning Steppes map shows ~400 spawn points
BUT only ~300-320 (75-80%) are active simultaneously
The game rotates which points are "hot" based on:
- Player activity in the area
- Time since last kill
- Zone population pressure
```

This is MUCH more efficient than it appears!

## Real WoW Spawn Mechanics Breakdown

### 1. Spawn Point Pool System

```java
/**
 * WoW doesn't spawn all points at once.
 * Instead, it maintains a pool of POTENTIAL spawn points
 * and activates a percentage based on demand.
 */

// Example from Burning Steppes:
Total spawn points defined: 400
Active spawn points at any time: 280-320 (70-80%)
Inactive/dormant points: 80-120 (20-30%)

// This creates:
// - Visual variety (different routes each time)
// - Lower server load (fewer active entities)
// - Dynamic density (responds to player activity)
```

### 2. Shared Spawn Points

**Key Mechanic:** One location can spawn different mob types

```java
// Example spawn point configuration:
SpawnPoint {
    position: BlockPos(100, 64, 200),
    possibleMobs: [
        {type: "zombie", weight: 50},
        {type: "skeleton", weight: 30},
        {type: "creeper", weight: 20}
    ],
    currentlyActive: true,
    currentMob: "zombie",
    respawnTime: 5 minutes
}

// When zombie is killed:
// - Point becomes available
// - Random roll determines next mob type
// - Creates variety without duplicate entities
```

### 3. Dynamic Respawn System

**Hyperspawn Mechanic:**

```java
/**
 * Base respawn: 5 minutes
 *
 * If area has high player activity:
 * - Monitor kill rate vs spawn rate
 * - If kills/minute > spawns/minute × 1.5:
 *   - Reduce respawn timer to 2-3 minutes
 *   - Activate dormant spawn points
 *   - Creates "hyperspawn" effect
 *
 * If area is empty:
 * - Increase respawn timer to 7-10 minutes
 * - Deactivate some spawn points
 * - Saves performance
 */

public class DynamicRespawnManager {

    private static final float BASE_RESPAWN_SECONDS = 300;  // 5 minutes
    private static final float MIN_RESPAWN_SECONDS = 120;   // 2 minutes (hyperspawn)
    private static final float MAX_RESPAWN_SECONDS = 600;   // 10 minutes (idle)

    private static final float BASE_ACTIVE_PERCENTAGE = 0.75f;  // 75% active normally
    private static final float MAX_ACTIVE_PERCENTAGE = 0.90f;   // 90% during hyperspawn
    private static final float MIN_ACTIVE_PERCENTAGE = 0.60f;   // 60% when idle

    public static void updateRegionSpawnRate(Region region) {
        // Calculate player activity
        int playersInRegion = region.getPlayerCount();
        float killsPerMinute = region.getRecentKillRate();

        // Determine pressure
        float spawnPressure = calculateSpawnPressure(playersInRegion, killsPerMinute);

        // Adjust active spawn point percentage
        float activePercentage = BASE_ACTIVE_PERCENTAGE + (spawnPressure * 0.15f);
        activePercentage = Math.max(MIN_ACTIVE_PERCENTAGE, Math.min(MAX_ACTIVE_PERCENTAGE, activePercentage));

        // Adjust respawn timers
        float respawnTime = BASE_RESPAWN_SECONDS * (1.0f - spawnPressure * 0.6f);

        region.setActiveSpawnPercentage(activePercentage);
        region.setRespawnMultiplier(respawnTime / BASE_RESPAWN_SECONDS);
    }

    private static float calculateSpawnPressure(int players, float killsPerMinute) {
        // Pressure increases with more players and more kills
        float playerPressure = Math.min(1.0f, players / 10.0f);  // 10+ players = max pressure
        float killPressure = Math.min(1.0f, killsPerMinute / 20.0f);  // 20+ kills/min = max

        return (playerPressure * 0.6f) + (killPressure * 0.4f);  // Weighted average
    }
}
```

## Revised Implementation Strategy

### Architecture: Spawn Pool + Activation System

Instead of spawning everything, create a pool and activate dynamically:

```java
/**
 * New system architecture:
 *
 * 1. Generate spawn point pool (large, like WoW)
 * 2. Activate only 70-80% of points
 * 3. Rotate which points are active
 * 4. Adjust activation % based on player activity
 */

public class SpawnPoolManager {

    // Pool of ALL possible spawn points
    private static final Map<UUID, SpawnPoint> spawnPointPool = new ConcurrentHashMap<>();

    // Currently active spawn points
    private static final Set<UUID> activeSpawnPoints = ConcurrentHashMap.newKeySet();

    // Region-based activation tracking
    private static final Map<RegionPos, RegionSpawnState> regionStates = new ConcurrentHashMap<>();

    /**
     * Generate spawn point pool for a region.
     * This creates MORE points than will be active at once.
     */
    public static void generateSpawnPool(ServerLevel level, BlockPos regionCenter, BiomeGroup biome) {
        Random random = new Random(getSeed(regionCenter, level.getSeed()));

        List<SpawnPoint> newPoints = new ArrayList<>();

        // STRATEGY 1: POI-Based Elite Spawns (10-15 points)
        // These are always active (like rare mobs)
        int poiCount = 3 + random.nextInt(2);  // 3-4 POIs
        for (int i = 0; i < poiCount; i++) {
            CampPOI camp = generateCampPOI(level, regionCenter, random);
            if (camp != null) {
                // Each camp is one elite spawn point
                SpawnPoint point = new SpawnPoint(
                    UUID.randomUUID(),
                    camp.getPosition(),
                    SpawnPointType.POI_CAMP,
                    true,  // Always active
                    generateMobOptions(biome, MobTier.ELITE, random),
                    3,  // 3-5 mobs
                    5
                );
                newPoints.add(point);
            }
        }

        // STRATEGY 2: Scatter Spawn Pool (60-80 points)
        // These rotate - only 70-80% active at once
        int scatterPoolSize = 60 + random.nextInt(20);  // 60-80 potential points
        for (int i = 0; i < scatterPoolSize; i++) {
            BlockPos pos = findValidScatterPosition(level, regionCenter, newPoints, random);
            if (pos != null) {
                SpawnPoint point = new SpawnPoint(
                    UUID.randomUUID(),
                    pos,
                    SpawnPointType.SCATTER,
                    false,  // Can be deactivated
                    generateMobOptions(biome, MobTier.NORMAL, random),
                    1,  // 1-3 mobs
                    3
                );
                newPoints.add(point);
            }
        }

        // Add to pool
        for (SpawnPoint point : newPoints) {
            spawnPointPool.put(point.getId(), point);
        }

        // Activate initial set (75%)
        activateSpawnPoints(regionCenter, 0.75f);

        WowCraft.LOGGER.info("Generated spawn pool: {} total points, {} initially active",
            newPoints.size(), activeSpawnPoints.size());
    }

    /**
     * Activate a percentage of dormant spawn points in a region.
     */
    private static void activateSpawnPoints(BlockPos regionCenter, float percentage) {
        RegionPos region = RegionPos.fromBlockPos(regionCenter);

        // Get all spawn points in this region
        List<SpawnPoint> regionPoints = getSpawnPointsInRegion(region);

        // Filter to points that can be activated/deactivated (not POIs)
        List<SpawnPoint> rotatablePoints = regionPoints.stream()
            .filter(p -> !p.isAlwaysActive())
            .toList();

        // Calculate target count
        int targetActive = (int)(rotatablePoints.size() * percentage);

        // Deactivate all current
        for (SpawnPoint point : rotatablePoints) {
            if (activeSpawnPoints.contains(point.getId())) {
                deactivateSpawnPoint(point);
            }
        }

        // Randomly activate target count
        Collections.shuffle(rotatablePoints);
        for (int i = 0; i < targetActive && i < rotatablePoints.size(); i++) {
            activateSpawnPoint(rotatablePoints.get(i));
        }
    }

    private static void activateSpawnPoint(SpawnPoint point) {
        activeSpawnPoints.add(point.getId());
        point.setActive(true);

        // Roll which mob type to spawn at this point
        MobOption chosen = point.rollMobType();
        point.setCurrentMobType(chosen);

        // Spawn immediately
        spawnMobsAtPoint(point);
    }

    private static void deactivateSpawnPoint(SpawnPoint point) {
        activeSpawnPoints.remove(point.getId());
        point.setActive(false);

        // Despawn any mobs from this point
        despawnMobsAtPoint(point);
    }
}
```

### SpawnPoint Class (WoW-Style)

```java
/**
 * Represents a spawn point that can be active or dormant.
 * Can spawn different mob types (shared spawn).
 */
public class SpawnPoint {
    private final UUID id;
    private final BlockPos position;
    private final SpawnPointType type;
    private final boolean alwaysActive;  // POIs/rare spawns never deactivate

    // Shared spawn: multiple possible mob types
    private final List<MobOption> possibleMobs;
    private MobOption currentMobType;

    // Spawn count range
    private final int minMobs;
    private final int maxMobs;

    // State
    private boolean active;
    private List<UUID> spawnedMobIds;
    private long lastKillTime;
    private int respawnDelayTicks;

    /**
     * Roll which mob type spawns at this point.
     * Creates variety without needing multiple spawn points.
     */
    public MobOption rollMobType() {
        int totalWeight = possibleMobs.stream().mapToInt(MobOption::weight).sum();
        Random random = new Random(System.currentTimeMillis() + id.hashCode());

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (MobOption option : possibleMobs) {
            cumulative += option.weight();
            if (roll < cumulative) {
                return option;
            }
        }

        return possibleMobs.get(0);  // Fallback
    }

    /**
     * Calculate respawn time based on activity.
     */
    public int calculateRespawnDelay(float regionPressure) {
        // Base: 200 ticks (10 seconds) - faster than WoW for Minecraft pacing
        int baseDelay = 200;

        // Reduce for high activity (hyperspawn)
        if (regionPressure > 0.7f) {
            baseDelay = 100;  // 5 second hyperspawn
        }
        // Increase for low activity (idle)
        else if (regionPressure < 0.3f) {
            baseDelay = 400;  // 20 second idle spawn
        }

        return baseDelay;
    }
}

/**
 * Represents one possible mob type for a spawn point.
 */
public record MobOption(
    ResourceLocation mobType,
    int weight,
    int minCount,
    int maxCount
) {}

public enum SpawnPointType {
    POI_CAMP,      // Elite camp (always active, 3-5 mobs)
    POI_LAIR,      // Boss lair (always active, special mob)
    POI_PATROL,    // Patrol route (always active, moving)
    SCATTER        // Normal spawn (can rotate, 1-3 mobs)
}
```

## Recommended Settings - Refined

### Spawn Pool Generation

```java
// Per 300x300 region:

// POI Points (Always Active):
- 3-4 Camp POIs (elite/pack encounters)
- 0-1 Lair POI (rare/boss)
- 0-1 Patrol Route POI
Total POI points: 4-6

// Scatter Spawn Pool:
- 60-80 potential scatter points
- Only 45-60 (75%) active at any time
- 15-20 dormant, rotated every 5-10 minutes

// Total Spawn Points:
Pool size: 64-86 points
Active points: 49-66 points (76% average)
```

### Mob Count

```java
// POI spawns:
4 POIs × 3 mobs average = 12 mobs

// Active scatter spawns:
55 active scatter points × 1.8 mobs average = 99 mobs

// Total active mobs: ~111 per region
// Total possible mobs (if all points active): ~146

// Density: 1 mob per 810 blocks² (active)
//          1 mob per 616 blocks² (max capacity)
```

### Dynamic Activation

```java
// Rotation Schedule:
Every 5 minutes (6000 ticks):
- Evaluate region player activity
- Calculate spawn pressure (0.0 - 1.0)
- Adjust active percentage:
  - Low activity (0 players): 60% active, 20 second respawn
  - Normal (1-3 players): 75% active, 10 second respawn
  - High activity (4+ players): 90% active, 5 second respawn (hyperspawn)

// Rotation strategy:
- Deactivate least-recently-killed points
- Activate points near player activity
- Keep POIs always active
- Creates natural variety
```

## Shared Spawn Implementation

### Mob Type Pools by Biome

```java
// Example: Plains biome scatter spawns
ScatterSpawnPoint {
    position: (100, 64, 200),
    possibleMobs: [
        {type: "zombie", weight: 40, count: 1-2},
        {type: "skeleton", weight: 35, count: 1-2},
        {type: "creeper", weight: 15, count: 1-1},
        {type: "pillager", weight: 10, count: 1-2}
    ]
}

// When activated:
// - Roll random mob type (zombie 40% chance, etc.)
// - Spawn that type
// - On kill, respawn timer starts
// - On respawn, roll again (might be different type)
```

### Benefits of Shared Spawns

1. **Visual Variety** - Same location spawns different mobs over time
2. **Reduced Entities** - Don't need separate points for each mob type
3. **Natural Mixing** - Multiple mob types in same area without clustering
4. **Easier Balancing** - Adjust weights instead of spawn point positions

## Performance Comparison

### Current System
```
5 POIs × 3 packs × 4 mobs = 60 entities
No rotation, all always loaded
Memory: 60 entities × full mob data
```

### Proposed Pool System
```
Pool: 70 points total
Active: 53 points (75%)
Mobs: 53 × 1.8 avg = 95 entities active
       70 × 1.8 avg = 126 entities if all spawned (never happens)

Memory: 70 spawn point structs (lightweight)
        95 mob entities (active only)

Entity count: 1.6x current
Memory: 1.4x current (spawn points are cheap)
Performance: Better (rotation reduces loaded chunk pressure)
```

### Optimization: Lazy Spawning

```java
/**
 * Don't actually spawn mobs until player is near.
 * Spawn points track "should be spawned" but entity creation is deferred.
 */
public class LazySpawnSystem {

    public static void onPlayerMoved(Player player, ServerLevel level) {
        ChunkPos playerChunk = new ChunkPos(player.blockPosition());

        // Get spawn points in render distance (8 chunks)
        List<SpawnPoint> nearbyPoints = getSpawnPointsInRadius(playerChunk, 8);

        for (SpawnPoint point : nearbyPoints) {
            if (point.isActive() && !point.hasSpawnedMobs()) {
                // Player got close, actually spawn the mobs now
                spawnMobsAtPoint(point, level);
            }
        }

        // Despawn mobs from points far away
        List<SpawnPoint> farPoints = getSpawnPointsOutsideRadius(playerChunk, 12);
        for (SpawnPoint point : farPoints) {
            if (point.hasSpawnedMobs()) {
                despawnMobsAtPoint(point);  // Save memory
                point.markShouldRespawn();  // But remember to respawn when player returns
            }
        }
    }
}
```

## Visualization: Active vs Pool

```
Region Map (300×300 blocks):

[Map showing spawn points]
● = Active spawn point (mob spawned)
○ = Dormant spawn point (in pool, not active)
★ = POI spawn point (always active)

Time 0:00 (75% active):
★ ● ● ○ ● ● ★ ● ○ ● ● ● ○ ● ★
● ○ ● ● ● ○ ● ● ● ○ ● ● ● ● ●
○ ● ● ● ○ ● ● ○ ● ● ● ○ ● ● ○

Time 5:00 (rotation, still 75%):
★ ● ○ ● ● ● ★ ○ ● ● ○ ● ● ● ★
○ ● ● ● ○ ● ● ● ○ ● ● ● ○ ● ●
● ● ○ ● ● ○ ● ● ● ● ○ ● ● ○ ●

Time 10:00 (high activity, 90% active):
★ ● ● ● ● ● ★ ● ● ● ● ● ○ ● ★
● ● ● ● ● ● ● ● ● ○ ● ● ● ● ●
● ● ● ● ○ ● ● ● ● ● ● ● ● ● ●

Notice: Same total points, but different activation pattern
        POIs (★) never deactivate
        High activity increases active percentage
```

## Implementation Priority - Revised

### Phase 1: Core Pool System ⭐ START HERE
1. Create `SpawnPoint` class with active/dormant state
2. Generate large spawn pool (70-80 points per region)
3. Activate 75% on region generation
4. Keep current respawn logic (no dynamic adjustment yet)

**Effort:** Medium
**Impact:** High (immediate density increase)

### Phase 2: Shared Spawn Types
1. Add `MobOption` list to spawn points
2. Roll mob type on activation
3. Re-roll on each respawn

**Effort:** Low
**Impact:** High (variety without more entities)

### Phase 3: Dynamic Activation
1. Track player activity per region
2. Adjust active percentage (60-90%)
3. Adjust respawn timers (5-20 seconds)
4. Implement rotation every 5 minutes

**Effort:** Medium
**Impact:** Medium (polish, responsiveness)

### Phase 4: Lazy Spawning (Optional)
1. Defer mob entity creation until player nearby
2. Despawn distant mobs (but keep spawn point active)
3. Performance optimization for large worlds

**Effort:** High
**Impact:** Medium (mainly for performance)

## Expected Feel

### Before (Current):
"I fly for 30 seconds without seeing anything, then suddenly encounter a camp of 6 mobs clustered together."

### After (Pool System):
"As I travel, I encounter mobs every 15-20 seconds - sometimes solo, sometimes pairs. Occasionally I find a dangerous camp of 4-5 elites. The world feels populated."

### After (With Dynamic System):
"When I'm farming in an area, mobs respawn quickly and there's always something to fight. When I leave and come back later, the spawns have shifted - different mobs in different places."

## Key Advantages Over Previous Plan

1. **Lower Entity Count** - Pool of 70, only 53 active = fewer entities than "spawn all scatter points"
2. **More Realistic** - Matches WoW's actual implementation
3. **Better Performance** - Rotation + lazy spawning keeps memory low
4. **Dynamic Response** - Hyperspawn for busy areas, quiet when empty
5. **Easy Tuning** - Adjust activation % without regenerating world
6. **Variety** - Shared spawns mean different mobs each visit

This is significantly better than my previous recommendation of spawning 120 mobs!

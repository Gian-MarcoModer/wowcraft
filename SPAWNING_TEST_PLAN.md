# Spawning System Test Plan

## Current Status

✅ **Custom entities working** - All hostile pack mobs use custom AI
✅ **Passive mobs fixed** - Using vanilla spawning only
🔄 **Spawn density** - Needs testing and tuning

## What to Test

### 1. POI Density
**Goal:** Verify there are enough camps/spawns per region

**Current settings (POIGenerator.java):**
- Region size: 300 blocks
- Min POI spacing: 80 blocks
- Camps per region: 4-6
- Wildlife areas: 2-3
- Patrol routes: 1-2

**Test:**
1. Fly around for 5 minutes in plains/forest
2. Count number of camps encountered
3. Measure distance between camps
4. **Expected:** Find a camp every ~100-150 blocks

### 2. Mob Pack Sizes
**Goal:** Verify packs have enough mobs

**Current settings (MobPackTemplateLoader.java):**
```
zombie_pack: 2-4 pack size, 1-3 zombies + 0-1 husks per pack
skeleton_pack: 2-4 pack size, 2-3 skeletons + 0-1 strays per pack
spider_nest: 3-5 pack size, 2-4 spiders + 0-2 cave spiders per pack
```

**Test:**
1. Find 10 different camps
2. Count mobs in each camp
3. **Expected:** 3-8 mobs per camp on average

### 3. Pack Variety
**Goal:** Verify different pack types spawn appropriately

**Test biomes:**
- **Plains:** Should have zombie packs, pillager camps, creeper packs
- **Forest:** Should have skeleton packs, spider nests
- **Swamp:** Should have drowned packs, witch camps, slime pools
- **Desert:** Should have husk packs
- **Taiga:** Should have stray packs

### 4. Respawn Timing
**Goal:** Verify mobs respawn appropriately

**Current setting:** 200 ticks (10 seconds) default respawn

**Test:**
1. Kill all mobs in a camp
2. Wait and observe
3. **Expected:** Mobs respawn after ~10 seconds

### 5. Level Scaling
**Goal:** Verify mobs scale with distance from spawn

**Test:**
1. Check mob levels at spawn (0, 0)
2. Check mob levels 500 blocks away
3. Check mob levels 1000 blocks away
4. **Expected:** Gradual level increase

## Known Settings to Adjust

### If Spawning Too Sparse:

**Option 1: Increase camps per region**
```java
// POIGenerator.java:46
int campCount = 6 + random.nextInt(4); // Was: 4 + random.nextInt(3)
```

**Option 2: Reduce min spacing**
```java
// POIGenerator.java:22
private static final int MIN_POI_SPACING = 60; // Was: 80
```

**Option 3: Increase mobs per pack**
```java
// MobPackTemplateLoader.java
new MobEntry(ResourceLocation.parse("minecraft:zombie"), 2, 5, 70), // Was: 1, 3
```

**Option 4: Increase pack spawn chance**
```java
// MobPackManager.java:38
private static final int PACK_SPAWN_CHANCE = 15; // Was: 20 (~7% vs 5%)
```

### If Spawning Too Dense:

Do the opposite of above adjustments.

## Testing Commands

```bash
# Teleport to test biomes
/locate biome minecraft:plains
/locate biome minecraft:forest
/locate biome minecraft:desert

# Check POI data
# (Check logs for "Generated X POIs for region")

# Count active mobs
/execute as @e[type=wowcraft:pack_zombie] run say Z
/execute as @e[type=wowcraft:pack_skeleton] run say S
/execute as @e[type=wowcraft:pack_spider] run say Sp

# Check mob levels
# (Look at mob nameplates - should show level)

# Clear mobs to test respawn
/kill @e[type=wowcraft:pack_zombie]

# Give speed to travel faster
/effect give @p minecraft:speed 999 3
/effect give @p minecraft:night_vision 999 0
```

## What User Reported

**"Spawning is really not dense enough. had looong flights between camps most of the time."**

This suggests we should:
1. ✅ Increase camps per region (4-6 → 6-8)
2. ✅ Reduce min spacing (80 → 60-70 blocks)
3. ✅ Add more mobs per pack
4. ❓ Check if POI generation is working correctly

## Debug Logging

Enable these logs to diagnose:
```java
// Check POI generation
WowCraft.LOGGER.info("Generated {} POIs for region at {}", pois.size(), regionCenter);

// Check pack spawning
WowCraft.LOGGER.info("Spawned {} pack at {} from {} POI", packId, pos, poiType);

// Check mob spawning
WowCraft.LOGGER.info("Spawned {} (Level {}) in pack {}", mobType, level, packId);
```

## Success Criteria

✅ Find a camp every 100-150 blocks on average
✅ Each camp has 4-10 mobs
✅ Mix of different pack types in each biome
✅ Mobs respawn within 10-15 seconds
✅ Levels scale appropriately with distance
✅ No long empty stretches (>200 blocks without mobs)
✅ Performance stays good (>15 TPS with 10 loaded chunks)

## Next Steps After Testing

1. **If density is good:** Test other aspects (combat, leveling, loot)
2. **If density is low:** Adjust settings and rebuild
3. **If density is inconsistent:** Check POI generation logic
4. **If performance issues:** Optimize tick/spawn logic

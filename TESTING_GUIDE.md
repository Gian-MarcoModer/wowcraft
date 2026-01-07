# Testing Guide: Custom Pack Entities

## What Changed

We refactored pack mobs to use custom entity types for better control:
- **Zombies** → `PackZombie` (custom entity with built-in AI)
- **Skeletons** → `PackSkeleton` (custom entity with built-in AI)
- **Spiders** → `PackSpider` (custom entity with built-in AI)
- **Everything else** → Still uses vanilla entities with mixin-based AI

## Testing Checklist

### 1. Verify Custom Entities Spawn Correctly

**Look for these log messages on world generation:**
```
[WowCraft] Set pack mob home position at BlockPos{x=..., y=..., z=...} with custom AI
```

**In-game checks:**
- Find a Plains, Forest, Dark Forest, Jungle, or Badlands biome
- Zombies, skeletons, and spiders should spawn in packs
- Use F3+B to see hitboxes - verify they're the right entity type

### 2. Test Evade Mechanics (Custom Entities)

**Test on Zombies/Skeletons/Spiders:**
1. Aggro a pack zombie/skeleton/spider
2. Kite it 24+ blocks from spawn (check with F3)
3. **Wait 5 seconds** beyond leash range
4. Watch logs for: `"[MobName] entering EVADE mode!"`
5. Mob should **force run back** to spawn, ignoring you
6. Health should regenerate during return
7. Check logs for: `"[MobName] stopped evading"` when home

**Expected behavior:**
- Mob moves at 1.5x speed back to spawn
- Cannot re-aggro during evade
- Health regenerates over 5 seconds
- Stops exactly at home position

### 3. Test Kiting Mechanic

**Test damage reset:**
1. Aggro a zombie/skeleton/spider
2. Kite beyond 24 blocks
3. Wait 4 seconds (don't let timer expire)
4. **Hit the mob** with any damage
5. Timer should reset to 0 (check logs every 20 ticks)
6. Repeat - you can kite indefinitely if you damage it

### 4. Test Vanilla Pack Mobs (Backwards Compatibility)

**Test on Wolves, Husks, Zombie Villagers, etc.:**
1. Find packs with non-zombie/skeleton/spider mobs
2. Check logs for: `"Added vanilla pack goals to [MobName]"`
3. Test that they still:
   - Stay stationary when idle
   - Aggro at correct range
   - Social aggro works
   - Leash mechanics work (may be less reliable - old system)

### 5. Test Social Aggro

**Test both custom and vanilla:**
1. Find a mixed pack (e.g., zombies + husks in Badlands)
2. Aggro ONE mob
3. All pack members within social aggro radius should aggro
4. Verify custom entities aggro properly
5. Verify vanilla mobs in same pack also aggro

### 6. Performance Testing

**Monitor with debug logging enabled:**
```
tail -f run/logs/latest.log | grep "EVADE\|evading\|timer"
```

**Stress test:**
1. Spawn 10+ pack zombies/skeletons/spiders
2. Aggro them all simultaneously
3. Watch for lag spikes
4. Check TPS with `/forge tps` or similar
5. Monitor tick time in F3 debug screen

### 7. Edge Cases

**Test these scenarios:**
- Mob reaches exactly 24.0 blocks (should not evade yet)
- Mob reaches 24.1 blocks (timer starts)
- Player kills mob during evade (should die normally)
- Multiple players kiting same mob
- Mob stuck on terrain during evade (pathfinding)

## Expected Log Output

### Successful Custom Entity Spawn:
```
[WowCraft] Spawned pack mob minecraft:zombie at [x, y, z] (Level 5)
[WowCraft] Set pack mob home position at BlockPos{...} with custom AI
```

### Evade Sequence:
```
[WowCraft] PackZombie at distance 25.3 from home, timer: 20/100
[WowCraft] PackZombie at distance 26.1 from home, timer: 40/100
[WowCraft] PackZombie at distance 27.5 from home, timer: 60/100
[WowCraft] PackZombie at distance 28.2 from home, timer: 80/100
[WowCraft] PackZombie at distance 29.0 from home, timer: 100/100
[WowCraft] PackZombie entering EVADE mode!
[WowCraft] PackZombie stopped evading
```

### Vanilla Pack Mob:
```
[WowCraft] Added vanilla pack goals to Wolf
[WowCraft] Made vanilla mob territorial at BlockPos{...} with 24 block leash
```

## Troubleshooting

**Mobs not evading:**
- Check logs - is timer incrementing?
- Verify mob is beyond 24 blocks (F3 coordinates)
- Ensure mob is a custom entity (zombie/skeleton/spider)

**Mobs evading too early:**
- Check if taking damage (should reset timer)
- Verify LEASH_RANGE = 24.0 in PackMobBehavior.java

**Performance issues:**
- Count active pack mobs: `/execute as @e[type=wowcraft:pack_zombie] run say hi`
- Check if too many packs spawned
- Monitor tick time in F3 debug

**Vanilla mobs broken:**
- Check if mixin is applying (look for "Added vanilla pack goals")
- Verify PackMobGoalMixin instanceof check works
- Test with simple mob like wolf in plains

## Quick Test Commands

```
# Teleport to plains biome
/locate biome minecraft:plains

# Clear all mobs
/kill @e[type=!player]

# Count pack zombies
/execute as @e[type=wowcraft:pack_zombie] run say Pack Zombie Found

# Give speed to test kiting
/effect give @p minecraft:speed 999 2

# Set to night for mob spawning
/time set night
```

## What to Report

If something's wrong, provide:
1. Which mob type (custom vs vanilla)
2. What behavior you expected
3. What actually happened
4. Relevant log snippets
5. F3 debug info (coordinates, entity ID)

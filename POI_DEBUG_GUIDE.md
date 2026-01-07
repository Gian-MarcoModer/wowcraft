# POI Debug Visualization Guide

## Overview

The spawn system now includes visual debugging to help you see POIs (Points of Interest) in-game.

## Command Usage

```
/debugspawn         - Toggle debug mode on/off
/debugspawn on      - Enable debug visualization
/debugspawn off     - Disable debug visualization
```

**Requires OP permissions (level 2)**

## What You'll See

When debug mode is enabled, POIs will be marked with:

1. **Text Displays** - Floating text above each POI showing:
   - POI type and icon
   - Radius
   - Number of spawn positions
   - Special properties (level bonus, respawn time)

2. **Particle Effects** - Colored particles showing POI boundaries:
   - **Red/Orange Flames** - Hostile Camps
   - **Blue Soul Fire** - Boss Lairs
   - **Green Sparkles** - Wildlife Areas
   - **Yellow Wax** - Resource Areas
   - **Purple Enchant** - Patrol Routes

## POI Types Explained

### ⚔ CAMP (Hostile)
- **Spawns**: 3-5 hostile mobs per spawn point
- **Hostility**: Always aggressive
- **Color**: Red flames
- **Purpose**: Main hostile encounters, like bandit camps
- **Example**: 2-3 camps per region with 2-4 spawn points each

### ☠ LAIR (Boss)
- **Spawns**: 1 boss + 3-4 guards
- **Hostility**: Boss is territorial (aggros when close), guards are always hostile
- **Color**: Blue soul fire
- **Special**:
  - Boss has level bonus (e.g., +3 levels)
  - Extended respawn timer (e.g., 300 seconds)
  - Custom name (e.g., "FOREST Boss")
- **Purpose**: Elite encounters with better loot
- **Example**: 0-1 lairs per region

### 🦌 WILDLIFE (Neutral)
- **Spawns**: 2-4 neutral animals
- **Hostility**: Defensive (only attack if provoked)
- **Color**: Green sparkles
- **Purpose**: Peaceful ambient life
- **Example**: 1-2 wildlife areas per region

### ⛏ RESOURCE (Territorial)
- **Spawns**: 2-3 territorial mobs
- **Hostility**: Territorial (attack if you get too close)
- **Color**: Yellow wax
- **Purpose**: Guard valuable resources
- **Example**: Less common, special locations

### 👁 PATROL (Moving)
- **Spawns**: 2-4 hostile mobs
- **Hostility**: Always aggressive
- **Color**: Purple enchant
- **Purpose**: Moving patrols between waypoints
- **Status**: Currently static (movement TODO)

## How Bosses Work

Bosses are defined in [POISpawnPointGenerator.java:142-162](src/main/java/com/gianmarco/wowcraft/spawn/POISpawnPointGenerator.java#L142-L162):

### Boss Properties

1. **Level Bonus**: Set in [LairPOI.java:17](src/main/java/com/gianmarco/wowcraft/poi/LairPOI.java#L17)
   - Example: `levelBonus = 3` means boss is 3 levels higher than region
   - Applied when spawning: `targetLevel + levelBonus`

2. **Custom Name**: Set via `setNamedMob()`
   - Format: `"{Biome} Boss"` (e.g., "FOREST Boss", "DESERT Boss")
   - Previously had "Elite" prefix, now removed

3. **Extended Respawn**: Set in [LairPOI.java:18](src/main/java/com/gianmarco/wowcraft/poi/LairPOI.java#L18)
   - Example: `respawnDelaySeconds = 300` (5 minutes)
   - Much longer than normal spawns (30 seconds)

4. **Territorial Hostility**: `SpawnHostility.NEUTRAL_TERRITORIAL`
   - Boss doesn't aggro until you get close
   - Creates "lair" feel where you can approach cautiously

5. **Protected Spawn**: `setProtected(true)`
   - Never suppressed by spawn pool rotation
   - Always available for players to fight

### Boss Guards

Bosses spawn with 3-4 guards around them:
- Guards are always hostile
- Positioned randomly within 15 blocks of boss
- Guards have +1 level bonus
- Also protected from suppression

### Boss Spawning Code

```java
// Boss spawn point (single boss mob with level bonus)
SpawnPoint bossPoint = new SpawnPoint(
    UUID.randomUUID(),
    poi.getPosition(),
    SpawnPointType.POI_LAIR,
    biome,
    SpawnHostility.NEUTRAL_TERRITORIAL,  // Boss doesn't aggro until close
    bossMobs,
    1,  // Min mobs: 1
    1   // Max mobs: 1
);

bossPoint.setTargetLevel(level);
bossPoint.setLevelBonus(poi.getLevelBonus());  // +3 levels typically
bossPoint.setRespawnDelay(poi.getRespawnDelaySeconds() * 20);  // Convert to ticks
bossPoint.setProtected(true);
bossPoint.setNamedMob(biome.name() + " Boss");  // Custom name
```

The level bonus is applied in [SpawnPoolManager.java:405](src/main/java/com/gianmarco/wowcraft/spawn/SpawnPoolManager.java#L405):
```java
point.getTargetLevel() + point.getLevelBonus()
```

## Testing Tips

1. **Enable debug before loading new regions**:
   ```
   /debugspawn on
   ```

2. **Fly around to load new regions** - POI markers appear when regions generate

3. **Look for particle pillars** - Vertical columns of particles mark POI centers

4. **Read the floating text** - Hover over text displays to see POI details

5. **Check logs** - Server logs show POI generation:
   ```
   [Server thread/INFO] (wowcraft) Generating spawn points for region Region[1, 2] (biome: FOREST)
   [Server thread/INFO] (wowcraft) Generated spawn system for region Region[1, 2] - 12 POI spawns, 14 scatter spawns
   ```

6. **Disable when done** - Reduces visual clutter:
   ```
   /debugspawn off
   ```

## Current Spawn Density

Per 300x300 region:
- **POI Camps**: 2-3 camps (each with 2-4 spawn points)
- **POI Wildlife**: 1-2 areas
- **POI Lairs**: 0-1 (rare)
- **Scatter Spawns**: 10-15 spawn points
- **Total Spawn Points**: 60-80 per region

Each spawn point can spawn 1-5 mobs depending on type.

## Files Modified

- [POIDebugVisualizer.java](src/main/java/com/gianmarco/wowcraft/spawn/POIDebugVisualizer.java) - New debug visualization system
- [DebugSpawnCommand.java](src/main/java/com/gianmarco/wowcraft/command/DebugSpawnCommand.java) - New `/debugspawn` command
- [SpawnSystemManager.java:109](src/main/java/com/gianmarco/wowcraft/spawn/SpawnSystemManager.java#L109) - Integrated visualization
- [WowCommands.java:33](src/main/java/com/gianmarco/wowcraft/command/WowCommands.java#L33) - Registered command

## Next Steps

1. Test the debug visualization in-game
2. Verify POI types are visually distinct
3. Check boss properties (level bonus, name, respawn)
4. Adjust spawn density if needed
5. Implement patrol movement (currently TODO)

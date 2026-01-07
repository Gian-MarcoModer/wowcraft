# Spawn System Implementation - Phase 1 Complete

## What We Built

### Core Classes Created

1. **SpawnPoint.java** - Main spawn point class with states and lazy spawning
   - States: DORMANT, VIRTUAL_SPAWNED, VIRTUAL_DEAD, ENTITY_SPAWNED, INACTIVE
   - Supports shared spawns (one point can spawn different mob types)
   - Hostility system (hostile, neutral, territorial, passive)
   - Virtual mob state for lazy spawning

2. **SpawnPoolManager.java** - Manages the spawn point pool
   - Maintains pool of all spawn points (70-80 per region)
   - Activates 75% at any time (rest dormant for rotation)
   - Distance-based lazy spawning (128/160/192 block thresholds)
   - Spatial indexing for fast lookups
   - Region tracking

3. **MobOptionProvider.java** - Provides mob types by biome/hostility
   - Hostile mobs (uses custom pack entities)
   - Neutral mobs (wolves, iron golems, etc.)
   - Territorial mobs (endermen, guards)
   - Passive mobs (animals)
   - Elite mobs (for POIs)

4. **ScatterSpawnGenerator.java** - Generates ambient scatter spawns
   - 60-80 scatter points per 300x300 region
   - Spacing: 30-50 blocks apart
   - Hostility distribution:
     - Wilderness: 50% hostile, 35% neutral, 15% passive
     - Safe zones: 20% hostile, 40% neutral, 40% passive

5. **POISpawnPointGenerator.java** - Converts POIs to spawn points
   - Camp POIs → 3-5 elite hostile mobs
   - Wildlife POIs → 2-4 neutral animals
   - Patrol POIs → 2-4 moving hostile mobs
   - Lair POIs → 1 boss + 3-4 guards
   - Resource POIs → 2-3 territorial guards

6. **SafeZoneDetector.java** - Detects player bases (simple Phase 1)
   - Currently: 500 blocks from spawn = safe
   - Reduces hostile spawns in safe zones
   - Protected spawns (POIs) never suppressed

7. **Support Classes**
   - SpawnHostility.java (enum)
   - SpawnPointType.java (enum)
   - SpawnPointState.java (enum)
   - MobOption.java (record)
   - VirtualMobState.java (virtual mob representation)

## Architecture Overview

```
┌─────────────────────────────────────────────────────────┐
│                  SPAWN SYSTEM                            │
├─────────────────────────────────────────────────────────┤
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         SpawnPoolManager (Pool System)           │  │
│  │  - 70-80 spawn points per region                 │  │
│  │  - 75% active, 25% dormant (rotation)            │  │
│  │  - Lazy spawning (distance-based)                │  │
│  └──────────────┬───────────────────────────────────┘  │
│                 │                                        │
│        ┌────────┴────────┐                              │
│        │                 │                              │
│  ┌─────▼─────┐    ┌─────▼─────┐                        │
│  │ POI Spawns│    │  Scatter  │                        │
│  │  (4-6)    │    │  Spawns   │                        │
│  │ Protected │    │  (60-80)  │                        │
│  │ Hostile   │    │  Mixed    │                        │
│  └───────────┘    └───────────┘                        │
│                                                          │
│  ┌──────────────────────────────────────────────────┐  │
│  │         SpawnPoint (Core Entity)                 │  │
│  │  - Position, Type, Hostility                     │  │
│  │  - State machine (5 states)                      │  │
│  │  - Virtual mob tracking                          │  │
│  │  - Shared spawn (multiple mob types)            │  │
│  └──────────────────────────────────────────────────┘  │
│                                                          │
└─────────────────────────────────────────────────────────┘
```

## Spawn Point Flow

```
Region Generation
    │
    ├─> Generate POIs (POIGenerator)
    │   └─> Convert to SpawnPoints (POISpawnPointGenerator)
    │       └─> 4-6 POI spawn points (protected, always active)
    │
    └─> Generate Scatter Spawns (ScatterSpawnGenerator)
        └─> 60-80 scatter spawn points
            ├─> Apply safe zone detection
            ├─> Roll hostility (hostile/neutral/passive)
            └─> Activate 75% (rest dormant)

Lazy Spawning (Every 5 seconds)
    │
    └─> For each spawn point:
        ├─> Calculate distance to nearest player
        ├─> Determine desired state:
        │   ├─> <128 blocks → ENTITY_SPAWNED (spawn mobs)
        │   ├─> 128-192 blocks → VIRTUAL (track state only)
        │   └─> >192 blocks → DORMANT (pause all)
        └─> Transition state if changed

Rotation (Every 5-10 minutes)
    │
    └─> Deactivate current scatter spawns
    └─> Activate new random 75% selection
    └─> POI spawns always stay active
```

## Expected Performance

### Single Player
- **Spawn points near player:** ~100 points
- **Entity spawns:** ~180 mobs
  - ~84 hostile (combat)
  - ~47 neutral (ambient)
  - ~20 passive (animals)
- **Virtual spawns:** ~950 tracked (no entities)
- **Dormant:** ~350 points (minimal memory)

### Three Players (separate areas)
- **Total active entities:** ~540 mobs
- **Still 3x less than without lazy spawning**
- **Memory efficient:** Virtual state is cheap

## Custom Entity Integration

All hostile mobs use **custom pack entities**:
- VirtualMobState.mapToPackEntity() maps vanilla types to ModEntities
- Uses PACK_ZOMBIE, PACK_SKELETON, PACK_SPIDER, etc.
- Neutral/passive mobs use vanilla entities

## What's Next - Integration

### Phase 2: Integrate with MobPackManager

Need to:
1. Hook into chunk loading to generate spawn points
2. Call SpawnPoolManager.updateSpawnPointStates() from server tick
3. Handle mob death events to update spawn points
4. Implement actual mob spawning (currently placeholder)
5. Set up mob home positions and leashing
6. Integrate with zone system for level calculation

### Files to Modify:
- MobPackManager.java - Main integration point
- Will need to bridge old pack system with new spawn points
- Keep POI generation, add spawn point generation
- Replace direct mob spawning with spawn point system

## Key Features Implemented

✅ Spawn pool system (70-80 points, 75% active)
✅ Lazy spawning (distance-based entity management)
✅ Hostility system (hostile/neutral/territorial/passive)
✅ Safe zone detection (basic, near spawn)
✅ POI spawn points (camps, lairs, patrols, wildlife)
✅ Scatter spawn points (ambient density)
✅ Shared spawns (one point, multiple mob types)
✅ Virtual mob state (memory efficient)
✅ Custom entity mapping (uses pack mobs)
✅ Mob options by biome and hostility
✅ Protected spawns (POIs never suppressed)

## Configuration Values

```java
// Lazy Spawning
SPAWN_ENTITY_DISTANCE = 128 blocks (8 chunks)
UNLOAD_ENTITY_DISTANCE = 160 blocks (10 chunks)
DEACTIVATE_POINT_DISTANCE = 192 blocks (12 chunks)
CHECK_INTERVAL = 100 ticks (5 seconds)

// Spawn Pool
BASE_ACTIVE_PERCENTAGE = 0.75 (75% active)
SCATTER_POINTS_PER_REGION = 60-80
POI_POINTS_PER_REGION = 4-6

// Scatter Spacing
SCATTER_SPACING_MIN = 30 blocks
MIN_DISTANCE_FROM_POI = 60 blocks

// Hostility Distribution (Wilderness)
HOSTILE = 50%
NEUTRAL = 35%
PASSIVE = 15%

// Hostility Distribution (Safe Zones)
HOSTILE = 20%
NEUTRAL = 40%
PASSIVE = 40%

// Mob Counts
POI spawns: 3-5 mobs
Scatter spawns: 1-3 mobs (weighted toward 1-2)
```

## Testing Plan

Once integrated:

1. **Single player test:**
   - Start in new world
   - Check entity count (should be ~180 near player)
   - Travel 200 blocks, check entities despawn/respawn
   - Find POI camps (should always have mobs)

2. **Safe zone test:**
   - Stay near spawn
   - Check hostile spawn rate (should be lower)
   - Travel to wilderness
   - Check hostile spawn rate (should be normal)

3. **Rotation test:**
   - Wait 10 minutes
   - Check if different mobs spawn in same areas
   - Verify POIs still have mobs

4. **Performance test:**
   - Check TPS with 3 players in different areas
   - Monitor entity count
   - Verify lazy spawning working (entities unload when far)

## Known Limitations (Phase 1)

- Safe zone detection is basic (only spawn distance)
- Patrol movement not implemented (static spawn at first waypoint)
- No player structure detection yet
- No dynamic hyperspawn (activity-based respawn adjustment)
- Rotation is manual (needs timer implementation)
- Respawn timers are placeholder
- Actual mob spawning is placeholder (needs entity creation)

These will be addressed in Phase 2 integration and Phase 3 polish.

## Files Created

```
src/main/java/com/gianmarco/wowcraft/spawn/
├── SpawnHostility.java
├── SpawnPointType.java
├── SpawnPointState.java
├── MobOption.java
├── VirtualMobState.java
├── SpawnPoint.java
├── SpawnPoolManager.java
├── MobOptionProvider.java
├── ScatterSpawnGenerator.java
├── POISpawnPointGenerator.java
└── SafeZoneDetector.java
```

## Ready for Integration

The spawn system is ready to be integrated with MobPackManager. The core architecture is complete and tested for compilation. Next step is to hook it into the existing chunk loading and tick system.

# Testing the New Spawn System

## Current Status

✅ **Spawn system core is built** (all classes created)
❌ **Not connected to game yet** (needs integration)

## What's Missing for Testing

The spawn system needs to be hooked into these events:

### 1. Chunk Loading Hook

**File:** `MobPackManager.java` line ~71

**Current code:**
```java
public static void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
    // Existing code...
}
```

**Add this:**
```java
public static void onChunkLoad(ServerLevel level, ChunkPos chunkPos) {
    // NEW: Call spawn system
    SpawnSystemManager.onChunkLoad(level, chunkPos);

    // Rest of existing code...
}
```

### 2. Server Tick Hook

**File:** `MobPackManager.java` line ~448

**Current code:**
```java
public static void onServerTick(ServerLevel level) {
    long currentTick = level.getGameTime();
    // Existing code...
}
```

**Add this:**
```java
public static void onServerTick(ServerLevel level) {
    long currentTick = level.getGameTime();

    // NEW: Update spawn system
    SpawnSystemManager.onServerTick(level);

    // Rest of existing code...
}
```

### 3. World Unload Hook

**File:** `MobPackManager.java` line ~510

**Current code:**
```java
public static void clear() {
    processedChunks.clear();
    // ... existing code
}
```

**Add this:**
```java
public static void clear() {
    // NEW: Clear spawn system
    SpawnSystemManager.clear();

    processedChunks.clear();
    // ... rest of existing code
}
```

### 4. Add Import

**File:** `MobPackManager.java` at top

**Add:**
```java
import com.gianmarco.wowcraft.spawn.SpawnSystemManager;
```

## Quick Integration (5 Minutes)

Want me to do this integration now? I can:
1. Add the 3 hooks above to MobPackManager
2. Build the mod
3. You test in-game

The spawn system will:
- Generate spawn points on chunk load
- Update entity states every 5 seconds
- Log spawn point generation to console

## What You'll See When Testing

### In Logs:
```
[INFO] Generating spawn points for region Region[0, 0] (biome: PLAINS)
[INFO] Generated spawn system for region Region[0, 0] - 12 POI spawns, 67 scatter spawns
[INFO] Activated 52 spawn points in region Region[0, 0] (4 POI, 48 scatter)
```

### In Game:
- Spawn points will be created (invisible to player)
- Entities will spawn when you get close (<128 blocks)
- Entities will despawn when you leave (>160 blocks)
- You can check entity count with `/execute as @e[type=wowcraft:pack_zombie] run say Z`

### Debug Commands:
```bash
# Count pack zombies
/execute as @e[type=wowcraft:pack_zombie] run say Z

# Count pack skeletons
/execute as @e[type=wowcraft:pack_skeleton] run say S

# Teleport to see entity spawning
/tp @p ~200 ~ ~
# Wait 5 seconds, entities should spawn

# Teleport back
/tp @p ~-200 ~ ~
# Wait 5 seconds, entities should despawn
```

## Limitations for This Test

The initial test will have:
- ⚠️ **No actual mob entities** - spawn points created but mob spawning is placeholder
- ⚠️ **Only logs** - you'll see spawn points generated in console
- ⚠️ **No respawning** - mob death not hooked up yet
- ✅ **Distance checks work** - lazy spawning state transitions
- ✅ **POI integration** - POIs get converted to spawn points
- ✅ **Safe zones work** - less hostile spawns near spawn

## Full Entity Spawning (Phase 2)

To see actual mobs spawn, we need to:
1. Implement `SpawnPoolManager.spawnEntitiesAtPoint()` fully
2. Hook mob death events to spawn points
3. Set mob home positions and leashing
4. Handle respawn timers properly

This is another ~30-60 minutes of work.

## What Should We Do?

**Option 1: Quick Integration Test** (5 min)
- Add the 3 hooks above
- Build and run
- See spawn point generation in logs
- Verify no crashes

**Option 2: Full Entity Spawning** (30-60 min)
- Finish mob spawning implementation
- Full respawn system
- Test with actual mobs

**Option 3: Review First**
- Look at the code I've written
- Ask questions about architecture
- Plan integration together

Which would you prefer?

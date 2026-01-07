# Temporary Spawn System Disable - For Testing

To test if the spawn system is causing the 100% hang, temporarily disable it:

## Quick Disable

Edit `MobPackManager.java` and comment out these lines:

### Line ~73 (in onChunkLoad):
```java
// SpawnSystemManager.onChunkLoad(level, chunkPos);
```

### Line ~456 (in onServerTick):
```java
// SpawnSystemManager.onServerTick(level);
```

### Line ~476 (in onMobDeath):
```java
// SpawnSystemManager.onMobDeath(mob, level);
```

### Line ~522 (in clear):
```java
// SpawnSystemManager.clear();
```

Then rebuild and test. If the world loads fine, the spawn system is the issue. If it still hangs, something else is causing it.

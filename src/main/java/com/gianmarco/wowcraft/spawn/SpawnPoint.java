package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Represents a spawn point that can be active or dormant.
 * Can spawn different mob types (shared spawn).
 * Supports lazy spawning (virtual mobs when player far away).
 */
public class SpawnPoint {
    private final UUID id;
    private final BlockPos position;
    private final SpawnPointType type;
    private final BiomeGroup biome;
    private final boolean alwaysActive;  // POIs/rare spawns never deactivate

    // Shared spawn: multiple possible mob types
    private List<MobOption> possibleMobs;
    private MobOption currentMobType;

    // Spawn count range
    private final int minMobs;
    private final int maxMobs;

    // Hostility behavior
    private SpawnHostility hostility;

    // State management
    private SpawnPointState state;
    private boolean activeInRotation;
    private boolean isProtected;  // Quest/named mobs, never suppressed

    // Virtual mob state
    private final List<VirtualMobState> virtualMobs;
    private final List<UUID> spawnedEntityIds;

    // Respawn timing
    private long lastDeathTime;
    private int respawnDelayTicks;
    private boolean respawnTimerPaused;
    private boolean respawnEnabled;

    // Level/pack info
    private int targetLevel;
    private int levelBonus;  // For elite mobs
    private UUID packId;

    // Special flags
    private boolean isQuestMob;
    private boolean hasNamedMob;
    private String namedMobName;

    public SpawnPoint(
            UUID id,
            BlockPos position,
            SpawnPointType type,
            BiomeGroup biome,
            SpawnHostility hostility,
            List<MobOption> possibleMobs,
            int minMobs,
            int maxMobs) {
        this.id = id;
        this.position = position;
        this.type = type;
        this.biome = biome;
        this.hostility = hostility;
        this.possibleMobs = new ArrayList<>(possibleMobs);
        this.minMobs = minMobs;
        this.maxMobs = maxMobs;

        // POIs are always active
        this.alwaysActive = type != SpawnPointType.SCATTER;

        // Initial state
        this.state = SpawnPointState.DORMANT;
        this.activeInRotation = true;
        this.isProtected = false;

        // Collections
        this.virtualMobs = new ArrayList<>();
        this.spawnedEntityIds = new ArrayList<>();

        // Respawn
        this.respawnDelayTicks = 3600;  // 3 minutes default (180 seconds * 20 ticks)
        this.respawnTimerPaused = false;
        this.respawnEnabled = true;
        this.lastDeathTime = -1;  // -1 = never spawned yet (spawn immediately)

        // Level
        this.targetLevel = 1;
        this.levelBonus = 0;
        this.packId = UUID.randomUUID();

        // Special flags
        this.isQuestMob = false;
        this.hasNamedMob = false;
    }

    // === Getters ===

    public UUID getId() {
        return id;
    }

    public BlockPos getPosition() {
        return position;
    }

    public SpawnPointType getType() {
        return type;
    }

    public BiomeGroup getBiome() {
        return biome;
    }

    public SpawnHostility getHostility() {
        return hostility;
    }

    public SpawnPointState getState() {
        return state;
    }

    public boolean isAlwaysActive() {
        return alwaysActive;
    }

    public boolean isActiveInRotation() {
        return activeInRotation;
    }

    public boolean isProtected() {
        return isProtected;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public int getLevelBonus() {
        return levelBonus;
    }

    public UUID getPackId() {
        return packId;
    }

    public boolean isQuestMob() {
        return isQuestMob;
    }

    public boolean hasNamedMob() {
        return hasNamedMob;
    }

    public List<UUID> getSpawnedEntityIds() {
        return new ArrayList<>(spawnedEntityIds);
    }

    public List<VirtualMobState> getVirtualMobs() {
        return new ArrayList<>(virtualMobs);
    }

    // === Setters ===

    public void setState(SpawnPointState state) {
        this.state = state;
    }

    public void setActiveInRotation(boolean active) {
        this.activeInRotation = active;
    }

    public void setProtected(boolean isProtected) {
        this.isProtected = isProtected;
    }

    public void setHostility(SpawnHostility hostility) {
        this.hostility = hostility;
    }

    public void setMobOptions(List<MobOption> options) {
        this.possibleMobs = new ArrayList<>(options);
    }

    public void setTargetLevel(int level) {
        this.targetLevel = level;
    }

    public void setLevelBonus(int bonus) {
        this.levelBonus = bonus;
    }

    public void setRespawnDelay(int ticks) {
        this.respawnDelayTicks = ticks;
    }

    public void setRespawnEnabled(boolean enabled) {
        this.respawnEnabled = enabled;
    }

    public void setQuestMob(boolean isQuest) {
        this.isQuestMob = isQuest;
        if (isQuest) {
            this.isProtected = true;
        }
    }

    public void setNamedMob(String name) {
        this.hasNamedMob = true;
        this.namedMobName = name;
        this.isProtected = true;
    }

    // === State Checks ===

    public boolean hasSpawnedEntities() {
        return !spawnedEntityIds.isEmpty();
    }

    public boolean hasVirtualMobs() {
        return !virtualMobs.isEmpty();
    }

    public boolean hasMobsAlive() {
        return hasSpawnedEntities() || hasVirtualMobs();
    }

    public boolean isRespawnTimerPaused() {
        return respawnTimerPaused;
    }

    public boolean isRespawnReady(long currentTick) {
        if (!respawnEnabled) return false;
        if (respawnTimerPaused) return false;
        if (hasMobsAlive()) return false;

        // Never spawned yet - spawn immediately
        if (lastDeathTime == -1) return true;

        // Respawn timer check
        return currentTick >= lastDeathTime + respawnDelayTicks;
    }

    // === Mob Type Rolling ===

    /**
     * Roll which mob type spawns at this point.
     * Creates variety without needing multiple spawn points.
     */
    public MobOption rollMobType() {
        if (possibleMobs.isEmpty()) {
            throw new IllegalStateException("No mob options available for spawn point " + id);
        }

        int totalWeight = possibleMobs.stream().mapToInt(MobOption::weight).sum();
        Random random = new Random(System.currentTimeMillis() + id.hashCode());

        int roll = random.nextInt(totalWeight);
        int cumulative = 0;

        for (MobOption option : possibleMobs) {
            cumulative += option.weight();
            if (roll < cumulative) {
                currentMobType = option;
                return option;
            }
        }

        currentMobType = possibleMobs.get(0);
        return currentMobType;
    }

    // === Virtual Mob Management ===

    /**
     * Save current entities to virtual state before unloading.
     */
    public void saveVirtualMobState(ServerLevel level) {
        virtualMobs.clear();

        for (UUID entityId : spawnedEntityIds) {
            Entity entity = level.getEntity(entityId);
            if (entity instanceof Mob mob) {
                MobData mobData = mob.getAttached(PlayerDataRegistry.MOB_DATA);

                ResourceLocation mobTypeLocation = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
                    .getKey(mob.getType());

                VirtualMobState virtual = new VirtualMobState(
                    mobTypeLocation,
                    mob.blockPosition(),
                    mobData != null ? mobData.level() : targetLevel,
                    mob.getUUID(),
                    level.getGameTime()
                );

                virtualMobs.add(virtual);
            }
        }

        WowCraft.LOGGER.debug("Saved {} mobs to virtual state for spawn point {}",
            virtualMobs.size(), id);
    }

    public void clearVirtualMobs() {
        virtualMobs.clear();
    }

    public void clearSpawnedEntityIds() {
        spawnedEntityIds.clear();
    }

    public void setSpawnedEntityIds(List<UUID> ids) {
        spawnedEntityIds.clear();
        spawnedEntityIds.addAll(ids);
    }

    public void addSpawnedEntityId(UUID entityId) {
        spawnedEntityIds.add(entityId);
    }

    public void removeSpawnedEntityId(UUID entityId) {
        spawnedEntityIds.remove(entityId);
    }

    // === Respawn Timer Management ===

    public void pauseRespawnTimer() {
        this.respawnTimerPaused = true;
    }

    public void resumeRespawnTimer(long currentTick) {
        this.respawnTimerPaused = false;
        // Adjust last death time to account for paused period
        // This prevents instant respawn when resuming
    }

    public void markMobDead(long currentTick) {
        this.lastDeathTime = currentTick;
    }

    /**
     * Calculate respawn time based on activity.
     */
    public int calculateRespawnDelay(float regionPressure) {
        // Base: 3600 ticks (3 minutes / 180 seconds) - WoW Classic style respawn
        int baseDelay = 3600;

        // Reduce for high activity (hyperspawn)
        if (regionPressure > 0.7f) {
            baseDelay = 1800;  // 90 second hyperspawn (1.5 minutes)
        }
        // Increase for low activity (idle)
        else if (regionPressure < 0.3f) {
            baseDelay = 6000;  // 300 second idle spawn (5 minutes)
        }

        return baseDelay;
    }

    // === Spawn Actions ===

    /**
     * Called when all mobs at this point are dead.
     */
    public void onAllMobsDead(long currentTick) {
        markMobDead(currentTick);
        clearSpawnedEntityIds();
        clearVirtualMobs();

        if (state == SpawnPointState.ENTITY_SPAWNED) {
            state = SpawnPointState.VIRTUAL_DEAD;
        }
    }

    /**
     * Called when a mob from this point is killed.
     */
    public void onMobKilled(UUID mobId, long currentTick) {
        removeSpawnedEntityId(mobId);

        // If all mobs dead, mark for respawn
        if (!hasMobsAlive()) {
            onAllMobsDead(currentTick);
        }
    }

    @Override
    public String toString() {
        return "SpawnPoint{" +
            "id=" + id +
            ", type=" + type +
            ", state=" + state +
            ", hostility=" + hostility +
            ", position=" + position +
            ", level=" + targetLevel +
            ", entities=" + spawnedEntityIds.size() +
            ", virtual=" + virtualMobs.size() +
            '}';
    }
}

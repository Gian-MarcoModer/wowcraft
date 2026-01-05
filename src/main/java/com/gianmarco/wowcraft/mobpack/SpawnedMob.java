package com.gianmarco.wowcraft.mobpack;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Tracks an individual mob's state within a spawned pack.
 * Handles respawn timing when the mob dies.
 */
public class SpawnedMob {
    private final ResourceLocation mobType;
    private final BlockPos spawnPos;

    @Nullable
    private UUID entityId; // null if dead/not yet spawned
    private long deathTick; // game tick when mob died (for respawn timing)
    private boolean alive;

    public SpawnedMob(ResourceLocation mobType, BlockPos spawnPos) {
        this.mobType = mobType;
        this.spawnPos = spawnPos;
        this.entityId = null;
        this.deathTick = 0;
        this.alive = false;
    }

    // === Getters ===

    public ResourceLocation getMobType() {
        return mobType;
    }

    public BlockPos getSpawnPos() {
        return spawnPos;
    }

    @Nullable
    public UUID getEntityId() {
        return entityId;
    }

    public long getDeathTick() {
        return deathTick;
    }

    public boolean isAlive() {
        return alive;
    }

    // === State Management ===

    /**
     * Called when the mob is spawned into the world.
     */
    public void onSpawned(UUID entityId) {
        this.entityId = entityId;
        this.alive = true;
        this.deathTick = 0;
    }

    /**
     * Called when the mob dies.
     */
    public void onDeath(long currentTick) {
        this.alive = false;
        this.deathTick = currentTick;
        this.entityId = null;
    }

    /**
     * Checks if this mob is ready to respawn.
     */
    public boolean isReadyToRespawn(long currentTick, int respawnDelayTicks) {
        if (alive) {
            return false; // Already alive
        }
        if (deathTick == 0) {
            return true; // Never spawned yet
        }
        return (currentTick - deathTick) >= respawnDelayTicks;
    }

    // === Serialization (for persistence) ===

    public com.google.gson.JsonObject toJson() {
        var json = new com.google.gson.JsonObject();
        json.addProperty("mobType", mobType.toString());
        json.addProperty("x", spawnPos.getX());
        json.addProperty("y", spawnPos.getY());
        json.addProperty("z", spawnPos.getZ());
        json.addProperty("alive", alive);
        json.addProperty("deathTick", deathTick);
        if (entityId != null) {
            json.addProperty("entityId", entityId.toString());
        }
        return json;
    }

    public static SpawnedMob fromJson(com.google.gson.JsonObject json) {
        ResourceLocation type = ResourceLocation.parse(json.get("mobType").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        SpawnedMob mob = new SpawnedMob(type, pos);
        mob.alive = json.get("alive").getAsBoolean();
        mob.deathTick = json.get("deathTick").getAsLong();
        if (json.has("entityId")) {
            mob.entityId = UUID.fromString(json.get("entityId").getAsString());
        }
        return mob;
    }
}

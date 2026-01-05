package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Entity-based ground decal for WoW-style spell effects.
 * Renders a flat texture on the ground that expands and fades.
 */
public class SpellEffectEntity extends Entity {

    /**
     * Available effect types with their visual properties.
     */
    public enum EffectType {
        FROST_NOVA(0, "frost_nova", 8.0f, 20, true),
        ARCANE_EXPLOSION(1, "arcane_explosion", 10.0f, 15, true),
        CONSECRATION(2, "consecration", 8.0f, 200, false); // Long-lasting, no expand

        private final int id;
        private final String textureName;
        private final float defaultRadius;
        private final int defaultLifetime;
        private final boolean expands;

        EffectType(int id, String textureName, float defaultRadius, int defaultLifetime, boolean expands) {
            this.id = id;
            this.textureName = textureName;
            this.defaultRadius = defaultRadius;
            this.defaultLifetime = defaultLifetime;
            this.expands = expands;
        }

        public int getId() { return id; }
        public String getTextureName() { return textureName; }
        public float getDefaultRadius() { return defaultRadius; }
        public int getDefaultLifetime() { return defaultLifetime; }
        public boolean expands() { return expands; }

        public static EffectType fromId(int id) {
            for (EffectType type : values()) {
                if (type.id == id) return type;
            }
            return FROST_NOVA;
        }
    }

    // Synced data accessors
    private static final EntityDataAccessor<Integer> EFFECT_TYPE_ID = SynchedEntityData.defineId(
            SpellEffectEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Float> MAX_RADIUS = SynchedEntityData.defineId(
            SpellEffectEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> LIFETIME = SynchedEntityData.defineId(
            SpellEffectEntity.class, EntityDataSerializers.INT);

    public SpellEffectEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public SpellEffectEntity(Level level, Vec3 position, EffectType effectType) {
        this(ModEntities.SPELL_EFFECT, level);
        this.setPos(position.x, position.y + 0.02, position.z); // Slight Y offset to avoid z-fighting
        setEffectType(effectType);
        setMaxRadius(effectType.getDefaultRadius());
        setLifetime(effectType.getDefaultLifetime());
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(EFFECT_TYPE_ID, 0);
        builder.define(MAX_RADIUS, 8.0f);
        builder.define(LIFETIME, 20);
    }

    @Override
    public void tick() {
        super.tick();

        // Auto-remove when lifetime expires
        if (this.tickCount >= getLifetime()) {
            this.discard();
        }
    }

    // Required by Entity in 1.21.5
    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false; // Visual effect cannot be damaged
    }

    // === Getters and Setters ===

    public void setEffectType(EffectType type) {
        this.entityData.set(EFFECT_TYPE_ID, type.getId());
    }

    public EffectType getEffectType() {
        return EffectType.fromId(this.entityData.get(EFFECT_TYPE_ID));
    }

    public void setMaxRadius(float radius) {
        this.entityData.set(MAX_RADIUS, radius);
    }

    public float getMaxRadius() {
        return this.entityData.get(MAX_RADIUS);
    }

    public void setLifetime(int ticks) {
        this.entityData.set(LIFETIME, ticks);
    }

    public int getLifetime() {
        return this.entityData.get(LIFETIME);
    }

    // === Animation Helpers (for renderer) ===

    /**
     * Get animation progress from 0.0 (start) to 1.0 (end).
     */
    public float getProgress(float partialTick) {
        return Math.min(1.0f, (this.tickCount + partialTick) / getLifetime());
    }

    /**
     * Get current radius based on animation progress.
     * For expanding effects, starts small and grows to max.
     * For static effects, returns max radius immediately.
     */
    public float getCurrentRadius(float partialTick) {
        EffectType type = getEffectType();
        float maxRadius = getMaxRadius();

        if (!type.expands()) {
            return maxRadius;
        }

        // Expand quickly at start (ease-out curve)
        float progress = getProgress(partialTick);
        float expandProgress = Math.min(1.0f, progress * 4.0f); // Expand in first 25% of lifetime
        return maxRadius * easeOutQuad(expandProgress);
    }

    /**
     * Get alpha for fade-out effect.
     * Full opacity until 70% of lifetime, then fades out.
     */
    public float getAlpha(float partialTick) {
        float progress = getProgress(partialTick);

        if (progress < 0.7f) {
            return 1.0f;
        }

        // Fade out in last 30%
        return 1.0f - ((progress - 0.7f) / 0.3f);
    }

    /**
     * Get rotation angle for visual interest.
     */
    public float getRotation(float partialTick) {
        return (this.tickCount + partialTick) * 0.5f; // Slow rotation
    }

    // Easing function for smooth expansion
    private float easeOutQuad(float t) {
        return 1 - (1 - t) * (1 - t);
    }

    // === NBT Serialization ===

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        // MC 1.21.5 uses Optional-returning methods
        tag.getInt("EffectType").ifPresent(val -> setEffectType(EffectType.fromId(val)));
        tag.getFloat("MaxRadius").ifPresent(this::setMaxRadius);
        tag.getInt("Lifetime").ifPresent(this::setLifetime);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("EffectType", getEffectType().getId());
        tag.putFloat("MaxRadius", getMaxRadius());
        tag.putInt("Lifetime", getLifetime());
    }

    // Don't render shadow
    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 4096; // Visible up to 64 blocks away
    }
}

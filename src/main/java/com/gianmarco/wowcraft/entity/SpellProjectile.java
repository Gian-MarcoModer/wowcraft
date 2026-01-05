package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableProjectile;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Base class for all WowCraft spell projectiles.
 * Handles damage application on hit with stat scaling.
 */
public abstract class SpellProjectile extends ThrowableProjectile {

    protected float damage;
    protected boolean canCrit;
    protected boolean isCrit;

    protected SpellProjectile(EntityType<? extends ThrowableProjectile> entityType, Level level) {
        super(entityType, level);
    }

    protected SpellProjectile(EntityType<? extends ThrowableProjectile> entityType,
            LivingEntity owner, Level level) {
        super(entityType, level);
        this.setOwner(owner);
        this.setPos(owner.getX(), owner.getEyeY() - 0.1, owner.getZ());
    }

    /**
     * Set the damage this projectile will deal
     */
    public void setDamage(float damage) {
        this.damage = damage;
    }

    /**
     * Set whether this projectile can crit
     */
    public void setCanCrit(boolean canCrit) {
        this.canCrit = canCrit;
    }

    /**
     * Set whether this is a critical hit
     */
    public void setIsCrit(boolean isCrit) {
        this.isCrit = isCrit;
    }

    public boolean isCrit() {
        return this.isCrit;
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);

        if (!this.level().isClientSide) {
            // Play impact sound
            this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
                    getImpactSound(), SoundSource.PLAYERS, 1.0f, 1.0f);

            // Spawn impact particles
            if (this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(getImpactParticle(),
                        this.getX(), this.getY(), this.getZ(),
                        getImpactParticleCount(), 0.3, 0.3, 0.3, 0.1);
            }

            this.discard();
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);

        if (!this.level().isClientSide) {
            Entity target = result.getEntity();

            if (target instanceof LivingEntity livingTarget && target != this.getOwner()) {
                // Use DamagePipeline for centralized damage handling
                // The pipeline handles crit calculation, damage events, and FCT
                LivingEntity owner = (LivingEntity) this.getOwner();

                com.gianmarco.wowcraft.combat.WowDamageSource source = com.gianmarco.wowcraft.combat.WowDamageSource
                        .spellDirect(
                                owner,
                                null, // No ability ID for now
                                getDamageSchool());

                // Route through the damage pipeline
                // Note: Pipeline handles crit chance calculation based on player stats
                com.gianmarco.wowcraft.combat.DamageResult damageResult = com.gianmarco.wowcraft.combat.DamagePipeline
                        .deal(source, livingTarget, this.damage);

                WowCraft.LOGGER.debug("Spell Hit via Pipeline: Target={} Damage={} IsCrit={}",
                        livingTarget.getName().getString(),
                        damageResult.finalDamage(),
                        damageResult.isCritical());

                // Spawn hit particles on target
                if (this.level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(getHitParticle(),
                            livingTarget.getX(),
                            livingTarget.getY() + livingTarget.getBbHeight() / 2,
                            livingTarget.getZ(),
                            getHitParticleCount(), 0.3, 0.3, 0.3, 0.05);
                }
            }
        }
    }

    /**
     * Get the damage school for this spell (for WowDamageSource).
     * Override in subclasses for FROST, ARCANE, etc.
     */
    protected com.gianmarco.wowcraft.combat.WowDamageSource.DamageSchool getDamageSchool() {
        return com.gianmarco.wowcraft.combat.WowDamageSource.DamageSchool.ARCANE;
    }

    @Override
    public void tick() {
        super.tick();

        // Spawn trail particles
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(getTrailParticle(),
                    this.getX(), this.getY(), this.getZ(),
                    getTrailParticleCount(), 0.05, 0.05, 0.05, 0);
        }

        // Discard if exists too long
        if (this.tickCount > getMaxLifetimeTicks()) {
            this.discard();
        }
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        // No synced data needed for now
    }

    // Abstract methods for specific projectiles
    protected abstract ParticleOptions getTrailParticle();

    protected abstract ParticleOptions getImpactParticle();

    protected abstract ParticleOptions getHitParticle();

    protected abstract SoundEvent getImpactSound();

    protected abstract net.minecraft.world.damagesource.DamageSource getDamageSource();

    protected abstract String getDamageType();

    protected abstract String getDamageColor();

    // Defaults
    protected float getCritMultiplier() {
        return 1.5f;
    }

    protected int getTrailParticleCount() {
        return 2;
    }

    protected int getImpactParticleCount() {
        return 15;
    }

    protected int getHitParticleCount() {
        return 20;
    }

    protected int getMaxLifetimeTicks() {
        return 100;
    }
}

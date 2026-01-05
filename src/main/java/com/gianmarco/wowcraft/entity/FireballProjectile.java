package com.gianmarco.wowcraft.entity;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

/**
 * Fireball projectile - deals fire damage on impact.
 */
public class FireballProjectile extends SpellProjectile {

    public FireballProjectile(EntityType<? extends FireballProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public FireballProjectile(LivingEntity owner, Level level) {
        super(ModEntities.FIREBALL_PROJECTILE, owner, level);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected ParticleOptions getImpactParticle() {
        return ParticleTypes.LAVA;
    }

    @Override
    protected ParticleOptions getHitParticle() {
        return ParticleTypes.FLAME;
    }

    @Override
    protected SoundEvent getImpactSound() {
        return SoundEvents.BLAZE_SHOOT;
    }

    @Override
    protected DamageSource getDamageSource() {
        if (this.getOwner() instanceof LivingEntity owner) {
            return this.damageSources().indirectMagic(this, owner);
        }
        return this.damageSources().magic();
    }

    @Override
    protected String getDamageType() {
        return "fire";
    }

    @Override
    protected String getDamageColor() {
        return "§6"; // Gold/orange for fire
    }

    @Override
    protected float getCritMultiplier() {
        return 1.5f;
    }

    @Override
    public boolean isNoGravity() {
        return true; // Fireball flies straight - no gravity
    }

    @Override
    protected int getTrailParticleCount() {
        return 3;
    }

    @Override
    protected com.gianmarco.wowcraft.combat.WowDamageSource.DamageSchool getDamageSchool() {
        return com.gianmarco.wowcraft.combat.WowDamageSource.DamageSchool.FIRE;
    }
}

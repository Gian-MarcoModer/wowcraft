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
 * Arcane Missile projectile - deals arcane damage on impact.
 */
public class ArcaneMissileProjectile extends SpellProjectile {

    public ArcaneMissileProjectile(EntityType<? extends ArcaneMissileProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public ArcaneMissileProjectile(LivingEntity owner, Level level) {
        super(ModEntities.ARCANE_MISSILE_PROJECTILE, owner, level);
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.END_ROD; // Purple/arcane trail
    }

    @Override
    protected ParticleOptions getImpactParticle() {
        return ParticleTypes.ENCHANT;
    }

    @Override
    protected ParticleOptions getHitParticle() {
        return ParticleTypes.ENCHANT;
    }

    @Override
    protected SoundEvent getImpactSound() {
        return SoundEvents.EVOKER_CAST_SPELL;
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
        return "arcane";
    }

    @Override
    protected String getDamageColor() {
        return "§d"; // Purple/magenta for arcane
    }

    @Override
    protected float getCritMultiplier() {
        return 1.5f;
    }

    @Override
    public boolean isNoGravity() {
        return true; // Arcane missiles fly straight - no gravity
    }

    @Override
    protected int getTrailParticleCount() {
        return 2;
    }
}

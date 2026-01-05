package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.ability.mage.IceLance;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

/**
 * Ice Lance projectile - deals frost damage and applies slow.
 * Deals triple damage to already slowed/frozen targets.
 */
public class IceLanceProjectile extends SpellProjectile {

    private static final float FROZEN_DAMAGE_MULTIPLIER = 3.0f;
    private static final int SLOW_DURATION_TICKS = 40; // 2 seconds
    private static final int SLOW_AMPLIFIER = 0; // Slowness I

    public IceLanceProjectile(EntityType<? extends IceLanceProjectile> entityType, Level level) {
        super(entityType, level);
    }

    public IceLanceProjectile(LivingEntity owner, Level level) {
        super(ModEntities.ICE_LANCE_PROJECTILE, owner, level);
    }

    @Override
    @SuppressWarnings("deprecation")
    protected void onHitEntity(EntityHitResult result) {
        if (this.level().isClientSide) {
            return;
        }

        Entity target = result.getEntity();

        if (target instanceof LivingEntity livingTarget && target != this.getOwner()) {
            // Check if target is already slowed/frozen for bonus damage
            boolean wasSlowed = IceLance.isTargetSlowed(livingTarget);

            // Calculate final damage with frozen multiplier
            float finalDamage = this.damage;
            if (wasSlowed) {
                finalDamage *= FROZEN_DAMAGE_MULTIPLIER;
            }

            // Use DamagePipeline for proper FCT integration
            LivingEntity owner = (LivingEntity) this.getOwner();

            com.gianmarco.wowcraft.combat.WowDamageSource source = com.gianmarco.wowcraft.combat.WowDamageSource
                    .spellDirect(
                            owner,
                            net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wowcraft", "ice_lance"),
                            getDamageSchool());

            // DamagePipeline handles crit calculation based on player stats
            com.gianmarco.wowcraft.combat.DamagePipeline
                    .deal(source, livingTarget, finalDamage);

            // Apply slow effect
            livingTarget.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    SLOW_DURATION_TICKS,
                    SLOW_AMPLIFIER,
                    false,
                    true));

            // Spawn hit particles on target
            if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                serverLevel.sendParticles(getHitParticle(),
                        livingTarget.getX(),
                        livingTarget.getY() + livingTarget.getBbHeight() / 2,
                        livingTarget.getZ(),
                        wasSlowed ? 30 : getHitParticleCount(), 0.3, 0.3, 0.3, 0.05);

                // Extra shatter effect if target was frozen
                if (wasSlowed) {
                    serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                            livingTarget.getX(),
                            livingTarget.getY() + livingTarget.getBbHeight() / 2,
                            livingTarget.getZ(),
                            20, 0.4, 0.4, 0.4, 0.1);
                }
            }
        }
    }

    @Override
    protected ParticleOptions getTrailParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    protected ParticleOptions getImpactParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    protected ParticleOptions getHitParticle() {
        return ParticleTypes.SNOWFLAKE;
    }

    @Override
    protected SoundEvent getImpactSound() {
        return SoundEvents.GLASS_BREAK;
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
        return "frost";
    }

    @Override
    protected String getDamageColor() {
        return "§b"; // Aqua/cyan for frost
    }

    @Override
    protected float getCritMultiplier() {
        return 1.5f;
    }

    @Override
    public boolean isNoGravity() {
        return true; // Ice lance flies straight - no gravity
    }

    @Override
    protected int getTrailParticleCount() {
        return 4;
    }
}

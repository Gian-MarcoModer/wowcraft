package com.gianmarco.wowcraft.combat.events;

import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.gianmarco.wowcraft.core.event.CancellableEvent;
import net.minecraft.world.entity.LivingEntity;

/**
 * Event fired before damage is applied.
 * Handlers can modify the damage or cancel the event.
 */
public class PreDamageEvent extends CancellableEvent {

    private final WowDamageSource source;
    private final LivingEntity target;
    private float damage;

    public PreDamageEvent(WowDamageSource source, LivingEntity target, float damage) {
        this.source = source;
        this.target = target;
        this.damage = damage;
    }

    public WowDamageSource getSource() {
        return source;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public float getDamage() {
        return damage;
    }

    /**
     * Modify the damage amount.
     * 
     * @param damage New damage value
     */
    public void setDamage(float damage) {
        this.damage = Math.max(0, damage);
    }

    /**
     * Multiply damage by a factor.
     * 
     * @param multiplier Factor to multiply by
     */
    public void multiplyDamage(float multiplier) {
        this.damage *= multiplier;
    }

    /**
     * Add to the damage.
     * 
     * @param amount Amount to add (can be negative)
     */
    public void addDamage(float amount) {
        this.damage = Math.max(0, this.damage + amount);
    }
}

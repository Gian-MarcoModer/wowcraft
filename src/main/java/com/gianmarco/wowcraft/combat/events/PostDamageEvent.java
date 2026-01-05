package com.gianmarco.wowcraft.combat.events;

import com.gianmarco.wowcraft.combat.DamageResult;
import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.gianmarco.wowcraft.core.event.WowEvent;
import net.minecraft.world.entity.LivingEntity;

/**
 * Event fired after damage has been applied.
 * Used for reactions like floating combat text, threat, resource generation.
 * This event cannot be cancelled.
 */
public class PostDamageEvent implements WowEvent {

    private final WowDamageSource source;
    private final LivingEntity target;
    private final DamageResult result;

    public PostDamageEvent(WowDamageSource source, LivingEntity target, DamageResult result) {
        this.source = source;
        this.target = target;
        this.result = result;
    }

    public WowDamageSource getSource() {
        return source;
    }

    public LivingEntity getTarget() {
        return target;
    }

    public DamageResult getResult() {
        return result;
    }

    /**
     * Convenience: get the final damage dealt.
     */
    public float getFinalDamage() {
        return result.finalDamage();
    }

    /**
     * Convenience: check if it was a critical hit.
     */
    public boolean isCritical() {
        return result.isCritical();
    }

    /**
     * Convenience: check if damage was dealt.
     */
    public boolean didDamage() {
        return result.didDamage();
    }
}

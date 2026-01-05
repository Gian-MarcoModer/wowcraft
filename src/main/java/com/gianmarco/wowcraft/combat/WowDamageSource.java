package com.gianmarco.wowcraft.combat;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a source of damage in the WowCraft combat system.
 * Wraps vanilla DamageSource with additional WoW-specific info.
 */
public record WowDamageSource(
        DamageSchool school,
        DamageType type,
        @Nullable LivingEntity attacker,
        @Nullable ResourceLocation abilityId,
        boolean canCrit,
        boolean ignoresArmor,
        boolean isTrueDamage) {

    /**
     * Damage schools (for resistances).
     */
    public enum DamageSchool {
        PHYSICAL,
        FIRE,
        FROST,
        ARCANE,
        NATURE,
        SHADOW,
        HOLY
    }

    /**
     * Damage types (for handling logic).
     */
    public enum DamageType {
        MELEE_AUTO, // Auto-attack
        MELEE_ABILITY, // Heroic Strike, etc.
        RANGED_AUTO, // Bow auto-attack
        RANGED_ABILITY, // Aimed Shot, etc.
        SPELL_DIRECT, // Fireball, etc.
        SPELL_DOT, // Damage over time
        SPELL_AOE, // Area of effect
        REFLECT, // Reflected damage
        ENVIRONMENTAL // Fall, fire, etc.
    }

    // ========== Factory Methods ==========

    /**
     * Create a melee auto-attack source.
     */
    public static WowDamageSource meleeAuto(LivingEntity attacker) {
        return new WowDamageSource(
                DamageSchool.PHYSICAL,
                DamageType.MELEE_AUTO,
                attacker,
                null,
                true, // Can crit
                false, // Doesn't ignore armor
                false // Not true damage
        );
    }

    /**
     * Create a melee ability source.
     */
    public static WowDamageSource meleeAbility(LivingEntity attacker, ResourceLocation abilityId) {
        return new WowDamageSource(
                DamageSchool.PHYSICAL,
                DamageType.MELEE_ABILITY,
                attacker,
                abilityId,
                true,
                false,
                false);
    }

    /**
     * Create a direct spell damage source.
     */
    public static WowDamageSource spellDirect(LivingEntity caster, ResourceLocation abilityId, DamageSchool school) {
        return new WowDamageSource(
                school,
                DamageType.SPELL_DIRECT,
                caster,
                abilityId,
                true, // Spells can crit
                true, // Spells ignore armor
                false);
    }

    /**
     * Create a DoT damage source.
     */
    public static WowDamageSource spellDot(LivingEntity caster, ResourceLocation abilityId, DamageSchool school) {
        return new WowDamageSource(
                school,
                DamageType.SPELL_DOT,
                caster,
                abilityId,
                false, // DoTs usually don't crit (classic)
                true,
                false);
    }

    /**
     * Create an AOE damage source.
     */
    public static WowDamageSource spellAoe(LivingEntity caster, ResourceLocation abilityId, DamageSchool school) {
        return new WowDamageSource(
                school,
                DamageType.SPELL_AOE,
                caster,
                abilityId,
                true,
                true,
                false);
    }

    /**
     * Create true damage (bypasses everything).
     */
    public static WowDamageSource trueDamage(@Nullable LivingEntity source) {
        return new WowDamageSource(
                DamageSchool.PHYSICAL,
                DamageType.ENVIRONMENTAL,
                source,
                null,
                false,
                true,
                true);
    }

    // ========== Utility ==========

    /**
     * Check if the attacker is a player.
     */
    public boolean isPlayerAttack() {
        return attacker instanceof Player;
    }

    /**
     * Check if this is a melee attack.
     */
    public boolean isMelee() {
        return type == DamageType.MELEE_AUTO || type == DamageType.MELEE_ABILITY;
    }

    /**
     * Check if this is a spell.
     */
    public boolean isSpell() {
        return type == DamageType.SPELL_DIRECT ||
                type == DamageType.SPELL_DOT ||
                type == DamageType.SPELL_AOE;
    }

    /**
     * Check if this is an ability (not auto-attack).
     */
    public boolean isAbility() {
        return abilityId != null;
    }
}

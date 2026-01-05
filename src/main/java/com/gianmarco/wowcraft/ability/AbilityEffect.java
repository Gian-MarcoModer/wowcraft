package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;

/**
 * Represents a single effect within an ability.
 * Effects are composable building blocks that define what an ability does.
 */
public record AbilityEffect(
        EffectType type,
        @Nullable DamageSchool damageSchool,
        @Nullable String amountFormula,
        @Nullable ResourceLocation projectile,
        @Nullable Float duration,
        @Nullable Float speed,
        boolean canCrit) {

    /**
     * Types of ability effects.
     */
    public enum EffectType {
        DAMAGE, // Deal damage to target
        HEAL, // Heal target
        PROJECTILE, // Launch a projectile
        TELEPORT, // Teleport caster (Blink)
        DASH_TO_TARGET, // Dash to target (Charge)
        STUN, // Stun target
        SLOW, // Slow target
        BUFF, // Apply a buff
        DEBUFF, // Apply a debuff
        AOE_DAMAGE, // Area damage around caster/target
        DOT, // Damage over time
        HOT // Heal over time
    }

    /**
     * Damage schools (matches WowDamageSource.DamageSchool).
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

    public static AbilityEffect fromJson(JsonObject json) {
        String typeStr = json.get("type").getAsString().toUpperCase();
        EffectType type = switch (typeStr) {
            case "DAMAGE" -> EffectType.DAMAGE;
            case "HEAL" -> EffectType.HEAL;
            case "PROJECTILE" -> EffectType.PROJECTILE;
            case "TELEPORT" -> EffectType.TELEPORT;
            case "DASH_TO_TARGET" -> EffectType.DASH_TO_TARGET;
            case "STUN" -> EffectType.STUN;
            case "SLOW" -> EffectType.SLOW;
            case "BUFF" -> EffectType.BUFF;
            case "DEBUFF" -> EffectType.DEBUFF;
            case "AOE_DAMAGE" -> EffectType.AOE_DAMAGE;
            case "DOT" -> EffectType.DOT;
            case "HOT" -> EffectType.HOT;
            default -> throw new IllegalArgumentException("Unknown effect type: " + typeStr);
        };

        DamageSchool school = null;
        if (json.has("school")) {
            String schoolStr = json.get("school").getAsString().toUpperCase();
            school = switch (schoolStr) {
                case "PHYSICAL" -> DamageSchool.PHYSICAL;
                case "FIRE" -> DamageSchool.FIRE;
                case "FROST" -> DamageSchool.FROST;
                case "ARCANE" -> DamageSchool.ARCANE;
                case "NATURE" -> DamageSchool.NATURE;
                case "SHADOW" -> DamageSchool.SHADOW;
                case "HOLY" -> DamageSchool.HOLY;
                default -> DamageSchool.PHYSICAL;
            };
        }

        String amountFormula = json.has("amount") ? json.get("amount").getAsString() : null;

        ResourceLocation projectile = json.has("projectile")
                ? ResourceLocation.parse(json.get("projectile").getAsString())
                : null;

        Float duration = json.has("duration") ? json.get("duration").getAsFloat() : null;
        Float speed = json.has("speed") ? json.get("speed").getAsFloat() : null;
        boolean canCrit = !json.has("can_crit") || json.get("can_crit").getAsBoolean();

        return new AbilityEffect(type, school, amountFormula, projectile, duration, speed, canCrit);
    }

    /**
     * Convert to WowDamageSource.DamageSchool for use in DamagePipeline.
     */
    public WowDamageSource.DamageSchool toWowDamageSchool() {
        if (damageSchool == null)
            return WowDamageSource.DamageSchool.PHYSICAL;

        return switch (damageSchool) {
            case PHYSICAL -> WowDamageSource.DamageSchool.PHYSICAL;
            case FIRE -> WowDamageSource.DamageSchool.FIRE;
            case FROST -> WowDamageSource.DamageSchool.FROST;
            case ARCANE -> WowDamageSource.DamageSchool.ARCANE;
            case NATURE -> WowDamageSource.DamageSchool.NATURE;
            case SHADOW -> WowDamageSource.DamageSchool.SHADOW;
            case HOLY -> WowDamageSource.DamageSchool.HOLY;
        };
    }

    /**
     * Check if this effect deals damage.
     */
    public boolean isDamageEffect() {
        return type == EffectType.DAMAGE ||
                type == EffectType.AOE_DAMAGE ||
                type == EffectType.DOT;
    }

    /**
     * Check if this effect is a heal.
     */
    public boolean isHealEffect() {
        return type == EffectType.HEAL || type == EffectType.HOT;
    }
}

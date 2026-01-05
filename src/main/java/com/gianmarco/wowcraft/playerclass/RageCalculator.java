package com.gianmarco.wowcraft.playerclass;

/**
 * Calculates rage generation using WoW Classic (Vanilla 1.12) formulas.
 *
 * Formula sources:
 * - https://vanilla-wow-archive.fandom.com/wiki/Rage
 * - Rage conversion values are level-dependent
 * - Rage is generated from both dealing damage and taking damage
 */
public class RageCalculator {

    /**
     * Calculate rage conversion value for a given level.
     * Formula (up to level 70): c = 0.0091107836 * Level² + 3.225598133 * Level + 4.2652911
     *
     * @param level Player level
     * @return Rage conversion value
     */
    public static double getRageConversionValue(int level) {
        return 0.0091107836 * level * level + 3.225598133 * level + 4.2652911;
    }

    /**
     * Calculate rage gained from dealing damage with auto-attacks (white damage).
     *
     * WoW Classic Formula: R = 15 * d / (4 * c) + f * s / 2
     * Capped at: 15 * d / c
     *
     * Where:
     * - R = rage generated
     * - d = damage dealt
     * - c = rage conversion value (level-dependent)
     * - f = hit factor (3.5 for normal main-hand, 7.0 for crit main-hand)
     * - s = weapon speed in seconds
     *
     * @param damage Damage dealt
     * @param level Player level
     * @param weaponSpeed Weapon speed in seconds
     * @param isCritical Whether the hit was a critical strike
     * @return Rage generated (rounded to nearest integer)
     */
    public static int calculateRageFromDamageDealt(float damage, int level, float weaponSpeed, boolean isCritical) {
        double c = getRageConversionValue(level);
        double d = damage;
        double s = weaponSpeed;
        double f = isCritical ? 7.0 : 3.5; // Hit factor

        // Main formula: R = 15 * d / (4 * c) + f * s / 2
        double rage = (15.0 * d) / (4.0 * c) + (f * s) / 2.0;

        // Cap: Cannot exceed 15 * d / c
        double cap = (15.0 * d) / c;
        rage = Math.min(rage, cap);

        // Round to nearest integer and ensure non-negative
        return Math.max(0, (int) Math.round(rage));
    }

    /**
     * Calculate rage gained from taking damage.
     *
     * WoW Classic Formula: R = 2.5 * d / c
     *
     * Where:
     * - R = rage generated
     * - d = damage taken
     * - c = rage conversion value (level-dependent)
     *
     * Note: In WoW Classic, even blocked, parried, or absorbed damage generates rage.
     *
     * @param damageTaken Damage taken
     * @param level Player level
     * @return Rage generated (rounded to nearest integer)
     */
    public static int calculateRageFromDamageTaken(float damageTaken, int level) {
        double c = getRageConversionValue(level);
        double d = damageTaken;

        // Formula: R = 2.5 * d / c
        double rage = (2.5 * d) / c;

        // Round to nearest integer and ensure non-negative
        return Math.max(0, (int) Math.round(rage));
    }

    /**
     * Get the default weapon speed for unarmed/bare-handed attacks.
     * In Minecraft, this represents attacking without a weapon.
     *
     * @return Default weapon speed in seconds
     */
    public static float getDefaultWeaponSpeed() {
        return 2.0f; // Standard unarmed attack speed
    }

    /**
     * Get weapon speed from an item's attack speed attribute.
     * Falls back to default if no weapon speed is found.
     *
     * @param attackSpeed Attack speed from item attributes (attacks per second)
     * @return Weapon speed in seconds
     */
    public static float getWeaponSpeedFromAttackSpeed(double attackSpeed) {
        if (attackSpeed <= 0) {
            return getDefaultWeaponSpeed();
        }
        // Convert attacks per second to seconds per attack
        return (float) (1.0 / attackSpeed);
    }
}

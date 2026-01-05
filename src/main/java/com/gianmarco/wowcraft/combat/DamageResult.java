package com.gianmarco.wowcraft.combat;

/**
 * Result of a damage calculation through the pipeline.
 */
public record DamageResult(
        float finalDamage,
        boolean isCritical,
        boolean wasBlocked,
        boolean wasDodged,
        boolean wasParried,
        boolean wasAbsorbed,
        float absorbedAmount) {

    public static final DamageResult CANCELLED = new DamageResult(0, false, false, false, false, false, 0);
    public static final DamageResult DODGED = new DamageResult(0, false, false, true, false, false, 0);
    public static final DamageResult PARRIED = new DamageResult(0, false, false, false, true, false, 0);
    public static final DamageResult BLOCKED = new DamageResult(0, false, true, false, false, false, 0);

    /**
     * Create a simple hit result.
     */
    public static DamageResult hit(float damage, boolean crit) {
        return new DamageResult(damage, crit, false, false, false, false, 0);
    }

    /**
     * Create a partially absorbed result.
     */
    public static DamageResult absorbed(float finalDamage, float absorbedAmount, boolean crit) {
        return new DamageResult(finalDamage, crit, false, false, false, true, absorbedAmount);
    }

    /**
     * Did damage actually occur?
     */
    public boolean didDamage() {
        return finalDamage > 0;
    }

    /**
     * Was the attack completely avoided?
     */
    public boolean wasAvoided() {
        return wasDodged || wasParried;
    }
}

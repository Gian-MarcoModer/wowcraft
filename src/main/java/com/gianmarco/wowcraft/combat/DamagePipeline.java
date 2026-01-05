package com.gianmarco.wowcraft.combat;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.combat.events.PostDamageEvent;
import com.gianmarco.wowcraft.combat.events.PreDamageEvent;
import com.gianmarco.wowcraft.core.event.WowEventBus;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.playerclass.RageCalculator;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsManager;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Central damage pipeline for all WowCraft damage.
 * ALL damage should go through this class to ensure correct handling.
 * 
 * This is the ONLY place where damage is actually applied.
 * 
 * Usage:
 * 
 * <pre>
 * // From an ability
 * DamageResult result = DamagePipeline.deal(
 *         WowDamageSource.spellDirect(caster, FIREBALL_ID, DamageSchool.FIRE),
 *         target,
 *         baseDamage);
 * 
 * // From auto-attack
 * DamageResult result = DamagePipeline.deal(
 *         WowDamageSource.meleeAuto(attacker),
 *         target,
 *         weaponDamage);
 * </pre>
 */
public final class DamagePipeline {

    private DamagePipeline() {
    } // Prevent instantiation

    /**
     * Deal damage through the pipeline.
     * This is the ONLY way damage should be dealt in WowCraft.
     * 
     * @param source     The damage source with all metadata
     * @param target     The entity receiving damage
     * @param baseDamage The base damage before modifiers
     * @return The result of the damage calculation
     */
    public static DamageResult deal(WowDamageSource source, LivingEntity target, float baseDamage) {
        // ===== 1. Pre-Damage Event (can modify or cancel) =====
        PreDamageEvent preEvent = new PreDamageEvent(source, target, baseDamage);
        WowEventBus.post(preEvent);

        if (preEvent.isCancelled()) {
            WowCraft.LOGGER.debug("Damage cancelled by PreDamageEvent");
            return DamageResult.CANCELLED;
        }

        float damage = preEvent.getDamage();

        // ===== 2. Damage Variance (±10% randomization) =====
        // WoW Classic-style damage variance: each hit does 90-110% of base damage
        float variance = 0.9f + (float)(Math.random() * 0.2); // 0.9 to 1.1
        damage *= variance;

        // ===== 3. Avoidance Checks (dodge/parry/block) =====
        // Only for physical attacks against players or mobs that can dodge
        if (source.school() == WowDamageSource.DamageSchool.PHYSICAL && source.isMelee()) {
            // TODO: Implement dodge/parry checks based on target stats
            // For now, skip avoidance
        }

        // ===== 4. Armor/Resistance Reduction =====
        if (!source.ignoresArmor() && !source.isTrueDamage()) {
            damage = applyArmorReduction(damage, source, target);
        }

        // ===== 5. Critical Hit Check =====
        boolean isCrit = false;
        if (source.canCrit()) {
            float critChance = getCritChance(source);
            if (Math.random() < critChance) {
                damage *= getCritMultiplier(source);
                isCrit = true;
            }
        }

        // ===== 6. Apply Damage =====
        float finalDamage = Math.max(0, damage);

        if (finalDamage > 0) {
            // Create vanilla damage source for the actual hurt call
            DamageSource vanillaSource = createVanillaDamageSource(source, target);
            target.hurt(vanillaSource, finalDamage);

            // ===== Combat State Triggers =====
            // Attacker enters combat when dealing damage
            if (source.attacker() instanceof Player attacker) {
                CombatStateManager.enterCombat(attacker);
            }
            // Target enters combat when taking damage (if player)
            if (target instanceof Player playerTarget) {
                CombatStateManager.onDamageTaken(playerTarget);

                // Generate rage for Warriors taking damage (WoW Classic mechanic)
                if (PlayerDataManager.getPlayerClass(playerTarget) == PlayerClass.WARRIOR) {
                    int level = PlayerDataManager.getLevel(playerTarget);
                    int rageGain = RageCalculator.calculateRageFromDamageTaken(finalDamage, level);

                    if (rageGain > 0) {
                        PlayerDataManager.modifyResource(playerTarget, rageGain);
                        WowCraft.LOGGER.debug("Rage gained from taking damage: {} (damage={})",
                            rageGain, finalDamage);
                    }
                }
            }

            WowCraft.LOGGER.info("[DamagePipeline] Dealt {} damage ({}) to {} [crit: {}]",
                    finalDamage, source.school(), target.getName().getString(), isCrit);
        }

        // ===== 7. Create Result =====
        DamageResult result = DamageResult.hit(finalDamage, isCrit);

        // ===== 8. Post-Damage Event (for FCT, threat, etc.) =====
        WowCraft.LOGGER.info("[DamagePipeline] Firing PostDamageEvent for {} damage", finalDamage);
        WowEventBus.post(new PostDamageEvent(source, target, result));

        return result;
    }

    /**
     * Apply armor reduction to physical damage.
     * Uses WoW Classic-style armor formula with better scaling.
     */
    private static float applyArmorReduction(float damage, WowDamageSource source, LivingEntity target) {
        if (source.school() != WowDamageSource.DamageSchool.PHYSICAL) {
            // Spell damage uses resistance instead
            return applyResistanceReduction(damage, source, target);
        }

        // Get armor from WoW character stats if target is a player
        float armor;
        int defenderLevel;

        if (target instanceof Player player) {
            CharacterStats stats = StatsManager.getStats(player);
            armor = stats.getArmor();
            defenderLevel = Math.max(1, stats.getMaxHealth() / 100); // Estimate from HP
        } else {
            // For non-players, use vanilla armor value (mobs)
            armor = (float) target.getArmorValue();
            defenderLevel = 60; // Default mob level
        }

        // WoW Classic armor formula (CORRECTED - uses DEFENDER level, not attacker):
        // Damage Reduction % = Armor / (Armor + 400 + 85 × Defender Level)
        // Cap at 75% reduction maximum
        //
        // This formula provides diminishing returns and level scaling:
        // Level 1 (485 constant): 100 armor = 20.6%, 200 armor = 35.0%, 400 armor = 52.2%
        // Level 10 (1250 constant): 300 armor = 24.0%, 600 armor = 38.7%, 1200 armor = 56.1%
        // Level 30 (2950 constant): 600 armor = 20.3%, 1200 armor = 33.3%, 2400 armor = 50.0%
        //
        // This means you need MORE armor as you level to maintain the same % reduction
        float armorConstant = 400 + 85 * defenderLevel;
        float reduction = armor / (armor + armorConstant);
        reduction = Math.min(0.75f, reduction); // Cap at 75% reduction

        return damage * (1 - reduction);
    }

    /**
     * Apply magic resistance reduction.
     */
    private static float applyResistanceReduction(float damage, WowDamageSource source, LivingEntity target) {
        // TODO: Implement resistance system
        // For now, no magic resistance
        return damage;
    }

    /**
     * Get crit chance for an attack.
     */
    private static float getCritChance(WowDamageSource source) {
        if (source.attacker() instanceof Player player) {
            CharacterStats stats = StatsManager.getStats(player);

            if (source.isSpell()) {
                // Spell crit from intellect: 30 int = 1% crit
                return stats.getIntellect() / 30.0f / 100.0f + 0.01f; // Base 1%
            } else {
                // Melee crit from agility
                return stats.getCritChance(); // Already includes base
            }
        }

        // Default 5% crit chance for non-players
        return 0.05f;
    }

    /**
     * Get crit damage multiplier.
     */
    private static float getCritMultiplier(WowDamageSource source) {
        // Spells: 150% damage
        // Melee: 200% damage
        return source.isSpell() ? 1.5f : 2.0f;
    }

    /**
     * Create a vanilla DamageSource for the hurt() call.
     */
    private static DamageSource createVanillaDamageSource(WowDamageSource source, LivingEntity target) {
        // Use the appropriate vanilla damage type based on our source
        if (source.attacker() != null) {
            if (source.isSpell()) {
                return target.damageSources().indirectMagic(source.attacker(), source.attacker());
            } else {
                return target.damageSources().mobAttack(source.attacker());
            }
        }

        // Fallback to generic damage
        return target.damageSources().magic();
    }
}

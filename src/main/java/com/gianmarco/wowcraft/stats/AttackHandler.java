package com.gianmarco.wowcraft.stats;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.combat.DamagePipeline;
import com.gianmarco.wowcraft.combat.DamageResult;
import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.playerclass.RageCalculator;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Handles melee auto-attack damage using the v2 DamagePipeline.
 * Bonus damage from Attack Power is applied here.
 * Note: Dual wielding has been removed in v2.
 */
public class AttackHandler {

    // Track last attack time per player to prevent spam
    private static final Map<UUID, Long> lastAttackTimes = new ConcurrentHashMap<>();

    /**
     * Register attack event handler
     */
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            // Only process on server side
            if (world.isClientSide()) {
                return InteractionResult.PASS;
            }

            // Only handle MAIN_HAND events
            if (hand != net.minecraft.world.InteractionHand.MAIN_HAND) {
                return InteractionResult.PASS;
            }

            // Only process if player has a class
            if (!PlayerDataManager.hasSelectedClass(player)) {
                return InteractionResult.PASS;
            }

            // Only process for living entities
            if (!(entity instanceof LivingEntity target)) {
                return InteractionResult.PASS;
            }

            // Cooldown check to prevent spam
            long currentTime = System.currentTimeMillis();
            Long lastTime = lastAttackTimes.get(player.getUUID());
            if (lastTime != null && (currentTime - lastTime) < 50) {
                return InteractionResult.FAIL;
            }
            lastAttackTimes.put(player.getUUID(), currentTime);

            // -- MELEE AUTO-ATTACK via DamagePipeline --

            // 1. Get Base Damage (weapon + vanilla attributes)
            double baseDamage = player
                    .getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

            // 2. Get Bonus Damage from stats (Attack Power)
            float bonusDamage = StatsManager.getBonusMeleeDamage(player);
            float totalDamage = (float) baseDamage + bonusDamage;

            // 3. Create WowDamageSource for melee auto-attack
            WowDamageSource source = WowDamageSource.meleeAuto(player);

            // 4. Route through DamagePipeline (handles crit, events, FCT)
            DamageResult result = DamagePipeline.deal(source, target, totalDamage);

            WowCraft.LOGGER.debug("Melee Auto-Attack: Base={} Bonus={} Final={} Crit={}",
                    baseDamage, bonusDamage, result.finalDamage(), result.isCritical());

            // 5. Generate Rage (Warrior only) - WoW Classic formula
            if (PlayerDataManager.getPlayerClass(player) == com.gianmarco.wowcraft.playerclass.PlayerClass.WARRIOR) {
                int level = PlayerDataManager.getLevel(player);

                // Get weapon speed (attack speed attribute in Minecraft)
                double attackSpeed = player.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_SPEED);
                float weaponSpeed = RageCalculator.getWeaponSpeedFromAttackSpeed(attackSpeed);

                // Calculate rage using WoW Classic formula (damage dealt)
                int rageGain = RageCalculator.calculateRageFromDamageDealt(
                    result.finalDamage(),
                    level,
                    weaponSpeed,
                    result.isCritical()
                );

                if (rageGain > 0) {
                    PlayerDataManager.modifyResource(player, rageGain);
                    WowCraft.LOGGER.debug("Rage gained from damage: {} (damage={}, weaponSpeed={}, crit={})",
                        rageGain, result.finalDamage(), weaponSpeed, result.isCritical());
                }
            }

            // 6. Consume Event (Cancel Vanilla Attack - we handle damage ourselves)
            return InteractionResult.SUCCESS;
        });

        WowCraft.LOGGER.info("Registered WowCraft melee attack handler (v2 DamagePipeline)");
    }

    /**
     * Clear attack cooldown for a player (e.g., on login or respawn)
     */
    public static void clearCooldown(Player player) {
        lastAttackTimes.remove(player.getUUID());
    }
}

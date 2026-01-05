package com.gianmarco.wowcraft.damage;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.network.NetworkHandler;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import com.gianmarco.wowcraft.entity.SpellProjectile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Handles damage events for floating combat text display and mob aggro.
 * Uses AFTER_DAMAGE to get actual damage dealt after armor reduction.
 * Also forces mobs to aggro when damaged from outside their normal detection range.
 */
public class DamageEventHandler {

    /**
     * Register the damage event listener
     */
    public static void register() {
        // Use AFTER_DAMAGE to get actual damage dealt
        ServerLivingEntityEvents.AFTER_DAMAGE.register((entity, source, baseDamage, damageTaken, blocked) -> {
            // Only process for non-player living entities
            if (entity instanceof Player) {
                return;
            }

            // Find the player who caused the damage
            ServerPlayer attacker = null;
            boolean isCrit = false;

            Entity trueSource = source.getEntity();
            Entity directSource = source.getDirectEntity();

            // Case 1: Direct attack (Melee) or Vanilla Projectile
            if (trueSource instanceof ServerPlayer player) {
                attacker = player;

                // Case 2: WowCraft Spell Projectile
                if (directSource instanceof SpellProjectile spellProjectile) {
                    isCrit = spellProjectile.isCrit();
                }
                // Case 3: Standard Melee (Direct source is the player)
                else if (directSource == player) {
                    // Check for vanilla crit conditions (jump crit)
                    isCrit = attacker.fallDistance > 0.0F
                            && !attacker.onGround()
                            && !attacker.onClimbable()
                            && !attacker.isInWater()
                            && !attacker.hasEffect(net.minecraft.world.effect.MobEffects.BLINDNESS)
                            && !attacker.isPassenger();
                }
            }

            // Skip if no player attacker found or no damage
            if (attacker == null || damageTaken <= 0) {
                return;
            }

            // Force mob to aggro on attacker (fixes ranged attack aggro issue)
            if (entity instanceof Mob mob && trueSource instanceof LivingEntity livingAttacker) {
                // Set target if mob doesn't have one, or if attacked from outside detection range
                if (mob.getTarget() == null) {
                    mob.setTarget(livingAttacker);
                    WowCraft.LOGGER.debug("Mob {} aggro'd on {} due to damage",
                        mob.getName().getString(),
                        livingAttacker.getName().getString());
                }
            }

            // Verify detection
            WowCraft.LOGGER.info("Damage Event: Target={} Attacker={} DirectSource={} IsCrit={} Damage={}",
                    entity.getName().getString(),
                    attacker.getName().getString(),
                    directSource != null ? directSource.getName().getString() : "null",
                    isCrit,
                    damageTaken);

            // Send damage display
            NetworkHandler.sendDamageDisplay(
                    attacker,
                    entity.getId(),
                    damageTaken,
                    isCrit,
                    false, // isSpell - defaulting to false for generic damage handler
                    entity.getX(),
                    entity.getY() + entity.getBbHeight() + 0.5,
                    entity.getZ());
        });

        WowCraft.LOGGER.info("Registered WowCraft damage event handler");
    }
}

package com.gianmarco.wowcraft.combat.handlers;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.combat.events.PostDamageEvent;
import com.gianmarco.wowcraft.core.event.EventPriority;
import com.gianmarco.wowcraft.core.event.WowEventBus;
import com.gianmarco.wowcraft.network.NetworkHandler;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

/**
 * Handles PostDamageEvent to display floating combat text and force mob aggro.
 * This is the v2 approach - listening to our custom event bus.
 * Also forces mobs to aggro when damaged from outside their normal detection range.
 */
public final class FloatingTextHandler {

    private FloatingTextHandler() {
    }

    /**
     * Register the FCT event listener.
     */
    public static void register() {
        WowEventBus.register(PostDamageEvent.class, EventPriority.NORMAL, FloatingTextHandler::onPostDamage);
        WowCraft.LOGGER.info("Registered Floating Text Handler (v2 event bus)");
    }

    private static void onPostDamage(PostDamageEvent event) {
        // Only show FCT for damage to non-players
        LivingEntity target = event.getTarget();
        if (target instanceof Player) {
            return; // Don't show FCT for damage to players
        }

        // Find the player attacker
        ServerPlayer attacker = null;
        if (event.getSource().attacker() instanceof ServerPlayer player) {
            attacker = player;
        }

        if (attacker == null) {
            return; // No player attacker
        }

        float damage = event.getResult().finalDamage();
        if (damage <= 0) {
            return; // No damage dealt
        }

        // Force mob to aggro on attacker (fixes ranged attack aggro issue)
        if (target instanceof Mob mob && event.getSource().attacker() instanceof LivingEntity livingAttacker) {
            // Set target if mob doesn't have one, or if attacked from outside detection range
            if (mob.getTarget() == null) {
                mob.setTarget(livingAttacker);
                WowCraft.LOGGER.debug("Mob {} aggro'd on {} due to damage",
                    mob.getName().getString(),
                    livingAttacker.getName().getString());
            }
        }

        boolean isCrit = event.getResult().isCritical();

        WowCraft.LOGGER.info("[FCT v2] PostDamageEvent received: {} dealt {} to {} [crit: {}]",
                attacker.getName().getString(),
                damage,
                target.getName().getString(),
                isCrit);

        // Send FCT to the attacker's client
        boolean isSpell = event.getSource().isSpell();
        NetworkHandler.sendDamageDisplay(
                attacker,
                target.getId(),
                damage,
                isCrit,
                isSpell,
                target.getX(),
                target.getY() + target.getBbHeight() + 0.5,
                target.getZ());
    }
}

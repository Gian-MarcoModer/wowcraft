package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.combat.CombatStateManager;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.playerclass.ResourceType;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages ability cooldowns and provides utility methods for ability execution.
 */
public class AbilityManager {
    // Track cooldowns per player per ability
    private static final Map<UUID, Map<String, Long>> playerCooldowns = new HashMap<>();

    /**
     * Check if an ability is on cooldown for a player
     */
    public static boolean isOnCooldown(Player player, Ability ability) {
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUUID());
        if (cooldowns == null)
            return false;

        Long cooldownEnd = cooldowns.get(ability.getId());
        if (cooldownEnd == null)
            return false;

        return System.currentTimeMillis() < cooldownEnd;
    }

    /**
     * Get remaining cooldown in seconds
     */
    public static float getRemainingCooldown(Player player, Ability ability) {
        Map<String, Long> cooldowns = playerCooldowns.get(player.getUUID());
        if (cooldowns == null)
            return 0;

        Long cooldownEnd = cooldowns.get(ability.getId());
        if (cooldownEnd == null)
            return 0;

        long remaining = cooldownEnd - System.currentTimeMillis();
        return remaining > 0 ? remaining / 1000f : 0;
    }

    /**
     * Start the cooldown for an ability
     */
    public static void startCooldown(Player player, Ability ability) {
        playerCooldowns.computeIfAbsent(player.getUUID(), k -> new HashMap<>())
                .put(ability.getId(), System.currentTimeMillis() + (ability.getCooldownTicks() * 50L));
    }

    /**
     * Try to use an ability, checking all requirements
     * 
     * @return true if ability was used successfully
     */
    public static boolean tryUseAbility(Player player, Ability ability) {
        // Check class requirement
        if (PlayerDataManager.getPlayerClass(player) != ability.getRequiredClass()) {
            return false;
        }

        // Check cooldown
        if (isOnCooldown(player, ability)) {
            return false;
        }

        // Check resource
        if (!PlayerDataManager.hasResource(player, ability.getResourceCost())) {
            return false;
        }

        // Check ability-specific requirements
        if (!ability.canUse(player)) {
            return false;
        }

        // Use the ability (may be instant or start a cast)
        ability.use(player);

        // Consume resource
        PlayerDataManager.spendResource(player, ability.getResourceCost());

        // Start cooldown
        startCooldown(player, ability);

        // Track spell cast for Five Second Rule (mana regen)
        // Only track if the ability costs mana (for mana-using classes)
        PlayerClass playerClass = PlayerDataManager.getPlayerClass(player);
        if (playerClass.getResourceType() == ResourceType.MANA && ability.getResourceCost() > 0) {
            CombatStateManager.onSpellCast(player);
        }

        return true;
    }

    /**
     * Get the direction the player is looking
     */
    public static Vec3 getLookDirection(Player player) {
        return player.getLookAngle();
    }

    /**
     * Clean up cooldowns for a player (call when they disconnect)
     */
    public static void clearCooldowns(Player player) {
        playerCooldowns.remove(player.getUUID());
    }
}

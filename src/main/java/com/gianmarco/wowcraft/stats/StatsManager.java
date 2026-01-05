package com.gianmarco.wowcraft.stats;

import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages cached character stats for all players.
 */
public class StatsManager {
    private static final Map<UUID, CharacterStats> playerStats = new HashMap<>();

    /**
     * Get stats for a player, calculating if needed
     */
    public static CharacterStats getStats(Player player) {
        UUID playerId = player.getUUID();

        // Check cache first
        CharacterStats cached = playerStats.get(playerId);
        if (cached != null) {
            return cached;
        }

        // Calculate and cache
        return recalculateStats(player);
    }

    /**
     * Force recalculate stats (call when equipment changes or level up)
     */
    public static CharacterStats recalculateStats(Player player) {
        PlayerData data = PlayerDataManager.getData(player);
        CharacterStats stats = StatsCalculator.calculate(
                player,
                data.playerClass(),
                data.level());

        playerStats.put(player.getUUID(), stats);

        // Apply attributes to the entity so mechanics work
        applyAttributes(player, stats);

        // Update max mana in PlayerData for mana users
        if (data.playerClass().getResourceType().hasDynamicMax()) {
            int newMaxMana = stats.getMaxMana();
            if (data.maxResource() != newMaxMana) {
                PlayerDataManager.setData(player, data.withMaxResource(newMaxMana));
                System.out.println("WOWCRAFT DEBUG: Updated max mana to " + newMaxMana);
            }
        }

        return stats;
    }

    private static void applyAttributes(Player player, CharacterStats stats) {
        // Apply Max Health
        // Minecraft default is 20. Our formula gives ~200+.
        var healthAttr = player.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        if (healthAttr != null) {
            double oldMax = healthAttr.getValue();
            healthAttr.setBaseValue(stats.getMaxHealth());
            double newMax = healthAttr.getValue();
            
            System.out.println("WOWCRAFT DEBUG: Updated Max Health. Old: " + oldMax + ", New: " + newMax);

            // If current health is higher than new max (e.g. gear removal), clamp it
            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    /**
     * Clear cached stats for a player
     */
    public static void clearStats(Player player) {
        playerStats.remove(player.getUUID());
    }

    /**
     * Clear all cached stats (server shutdown)
     */
    public static void clearAll() {
        playerStats.clear();
    }

    /**
     * Get attack power for damage calculations
     */
    public static int getAttackPower(Player player) {
        return getStats(player).getAttackPower();
    }

    /**
     * Get bonus melee damage
     */
    public static float getBonusMeleeDamage(Player player) {
        return getStats(player).getBonusMeleeDamage();
    }

    /**
     * Roll for crit
     */
    public static boolean rollCrit(Player player) {
        return getStats(player).rollCrit();
    }

    /**
     * Get max health based on stats
     */
    public static int getMaxHealth(Player player) {
        return getStats(player).getMaxHealth();
    }

    /**
     * Get max mana based on stats
     */
    public static int getMaxMana(Player player) {
        return getStats(player).getMaxMana();
    }

    /**
     * Get mana regen per 5 seconds
     */
    public static int getManaRegenPer5(Player player) {
        return getStats(player).getManaRegenPer5();
    }
}

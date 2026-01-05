package com.gianmarco.wowcraft.playerclass;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.combat.CombatStateManager;
import com.gianmarco.wowcraft.network.NetworkHandler;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles resource and health regeneration for players.
 * - Health regenerates based on Spirit (ticks every 2 seconds, WoW Classic style)
 * - Mana regenerates passively (MP5 - mana per 5 seconds)
 * - Rage decays out of combat
 * 
 * WoW Classic health regen formulas (per 2 second tick):
 * - Warriors: (Spirit * 1.3) - 23
 * - Mages: (Spirit * 0.1) + 1.2
 */
public class ResourceRegeneration {

    // Regen rates
    private static final int RAGE_DECAY_PER_5_SECONDS = 5; // Rage loss out of combat
    private static final int ENERGY_REGEN_PER_5_SECONDS = 50; // Energy regen (100 max, 10 per second = 50 per 5 seconds)
    private static final int COMBO_POINTS_DECAY_SECONDS = 15; // Combo points reset after 15 seconds out of combat

    // Health regeneration ticks every 2 seconds (40 ticks)
    private static final int HEALTH_REGEN_INTERVAL_TICKS = 40;
    // Resource regen ticks every 5 seconds (100 ticks)
    private static final int RESOURCE_REGEN_INTERVAL_TICKS = 100;
    
    private static int healthTickCounter = 0;
    private static int resourceTickCounter = 0;

    /**
     * Register the server tick event for resource regeneration
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(ResourceRegeneration::onServerTick);
        WowCraft.LOGGER.info("Registered resource regeneration system");
    }

    private static void onServerTick(MinecraftServer server) {
        healthTickCounter++;
        resourceTickCounter++;

        boolean doHealthRegen = healthTickCounter >= HEALTH_REGEN_INTERVAL_TICKS;
        boolean doResourceRegen = resourceTickCounter >= RESOURCE_REGEN_INTERVAL_TICKS;

        if (doHealthRegen) {
            healthTickCounter = 0;
        }
        if (doResourceRegen) {
            resourceTickCounter = 0;
        }

        // Only process if at least one regen type is due
        if (!doHealthRegen && !doResourceRegen) {
            return;
        }

        // Process all online players
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (doHealthRegen) {
                processHealthRegen(player);
            }
            if (doResourceRegen) {
                processResourceRegen(player);
            }
        }
    }

    /**
     * Process health regeneration based on Spirit (every 2 seconds)
     * Uses WoW Classic class-specific formulas
     * Only regenerates when OUT OF COMBAT
     */
    private static void processHealthRegen(ServerPlayer player) {
        PlayerData data = PlayerDataManager.getData(player);

        // Skip if no class selected
        if (!data.hasSelectedClass()) {
            return;
        }

        // Only regenerate health when OUT OF COMBAT
        if (CombatStateManager.isInCombat(player)) {
            return;
        }

        // Only regenerate if not at full health
        float currentHealth = player.getHealth();
        float maxHealth = player.getMaxHealth();
        if (currentHealth >= maxHealth) {
            return;
        }

        // Get Spirit stat
        CharacterStats stats = StatsManager.getStats(player);
        int spirit = stats.getSpirit();

        // Calculate health regen based on class (WoW Classic formulas)
        float regenAmount = calculateHealthRegen(data.playerClass(), spirit);

        // Apply regeneration
        if (regenAmount > 0) {
            float newHealth = Math.min(maxHealth, currentHealth + regenAmount);
            player.setHealth(newHealth);
        }
    }

    /**
     * Calculate health regeneration per 2-second tick based on class and Spirit
     * WoW Classic formulas:
     * - Warriors: (Spirit * 1.3) - 23
     * - Mages/Casters: (Spirit * 0.1) + 1.2
     * - Rogues: (Spirit * 0.8) - 8 (moderate regen, between Warrior and Mage)
     */
    private static float calculateHealthRegen(PlayerClass playerClass, int spirit) {
        return switch (playerClass) {
            case WARRIOR -> Math.max(0, (spirit * 1.3f) - 23f);
            case MAGE -> Math.max(0, (spirit * 0.1f) + 1.2f);
            case ROGUE -> Math.max(0, (spirit * 0.8f) - 8f);
            default -> Math.max(0, (spirit * 0.5f)); // Default: moderate regen
        };
    }

    /**
     * Process mana/rage regeneration (every 5 seconds)
     */
    private static void processResourceRegen(ServerPlayer player) {
        PlayerData data = PlayerDataManager.getData(player);

        // Skip if no class selected
        if (!data.hasSelectedClass()) {
            return;
        }

        ResourceType resourceType = data.playerClass().getResourceType();
        int currentResource = data.currentResource();
        int maxResource = data.maxResource();

        switch (resourceType) {
            case MANA -> {
                // Mana regenerates based on Spirit (MP5)
                // WoW Classic rules:
                // - No regen while in combat
                // - No regen for 5 seconds after casting (Five Second Rule)
                if (currentResource < maxResource) {
                    // Only regen if OUT of combat
                    if (CombatStateManager.isOutOfCombat(player)) {
                        // Only regen if 5 seconds have passed since last spell cast
                        if (CombatStateManager.canFullManaRegen(player)) {
                            int regenAmount = StatsManager.getManaRegenPer5(player);
                            int newMana = Math.min(maxResource, currentResource + regenAmount);
                            if (newMana != currentResource) {
                                PlayerDataManager.setResource(player, newMana);
                                NetworkHandler.syncPlayerData(player);
                            }
                        }
                    }
                }
            }
            case RAGE -> {
                // Rage only decays when OUT OF COMBAT
                if (currentResource > 0 && CombatStateManager.isOutOfCombat(player)) {
                    int newRage = Math.max(0, currentResource - RAGE_DECAY_PER_5_SECONDS);
                    if (newRage != currentResource) {
                        PlayerDataManager.setResource(player, newRage);
                        NetworkHandler.syncPlayerData(player);
                    }
                }
            }
            case ENERGY -> {
                // Energy regenerates quickly (10 per second = 50 per 5 seconds)
                if (currentResource < maxResource) {
                    int newEnergy = Math.min(maxResource, currentResource + ENERGY_REGEN_PER_5_SECONDS);
                    if (newEnergy != currentResource) {
                        PlayerDataManager.setResource(player, newEnergy);
                        NetworkHandler.syncPlayerData(player);
                    }
                }

                // Decay combo points if out of combat for too long
                int comboPoints = data.comboPoints();
                if (comboPoints > 0 && CombatStateManager.isOutOfCombat(player)) {
                    // Check if player has been out of combat for COMBO_POINTS_DECAY_SECONDS
                    long outOfCombatTicks = CombatStateManager.getTicksOutOfCombat(player);
                    if (outOfCombatTicks >= COMBO_POINTS_DECAY_SECONDS * 20) {
                        PlayerData newData = data.withComboPoints(0);
                        PlayerDataManager.setData(player, newData);
                        NetworkHandler.syncPlayerData(player);
                    }
                }
            }
            case NONE -> {
                // No resource to regenerate
            }
        }
    }
}


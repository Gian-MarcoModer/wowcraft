package com.gianmarco.wowcraft.stats;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.network.NetworkHandler;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * Manages experience gain and leveling.
 * Handles XP calculations, mob XP values, and level-up logic.
 */
public class ExperienceManager {

    // Maximum level cap
    public static final int MAX_LEVEL = 30;

    // XP values per mob type
    private static final Map<EntityType<?>, Integer> MOB_XP_TABLE = new HashMap<>();

    static {
        // Passive mobs - 5 XP
        MOB_XP_TABLE.put(EntityType.CHICKEN, 5);
        MOB_XP_TABLE.put(EntityType.PIG, 5);
        MOB_XP_TABLE.put(EntityType.COW, 5);
        MOB_XP_TABLE.put(EntityType.SHEEP, 5);
        MOB_XP_TABLE.put(EntityType.RABBIT, 5);

        // Common hostiles - 20 XP
        MOB_XP_TABLE.put(EntityType.ZOMBIE, 20);
        MOB_XP_TABLE.put(EntityType.SKELETON, 20);
        MOB_XP_TABLE.put(EntityType.SPIDER, 20);
        MOB_XP_TABLE.put(EntityType.CAVE_SPIDER, 20);
        MOB_XP_TABLE.put(EntityType.DROWNED, 20);
        MOB_XP_TABLE.put(EntityType.HUSK, 20);
        MOB_XP_TABLE.put(EntityType.STRAY, 20);
        MOB_XP_TABLE.put(EntityType.SLIME, 15);
        MOB_XP_TABLE.put(EntityType.SILVERFISH, 10);

        // Dangerous hostiles - 35 XP
        MOB_XP_TABLE.put(EntityType.CREEPER, 35);
        MOB_XP_TABLE.put(EntityType.WITCH, 35);
        MOB_XP_TABLE.put(EntityType.PHANTOM, 35);
        MOB_XP_TABLE.put(EntityType.PILLAGER, 35);
        MOB_XP_TABLE.put(EntityType.VINDICATOR, 40);

        // Advanced hostiles - 50 XP
        MOB_XP_TABLE.put(EntityType.ENDERMAN, 50);
        MOB_XP_TABLE.put(EntityType.BLAZE, 50);
        MOB_XP_TABLE.put(EntityType.GHAST, 50);
        MOB_XP_TABLE.put(EntityType.PIGLIN_BRUTE, 50);
        MOB_XP_TABLE.put(EntityType.RAVAGER, 75);

        // Elite mobs - 75 XP
        MOB_XP_TABLE.put(EntityType.WITHER_SKELETON, 75);
        MOB_XP_TABLE.put(EntityType.EVOKER, 75);

        // Mini-bosses - 200 XP
        MOB_XP_TABLE.put(EntityType.ELDER_GUARDIAN, 200);
        MOB_XP_TABLE.put(EntityType.GUARDIAN, 40);
        MOB_XP_TABLE.put(EntityType.WARDEN, 300);

        // Bosses - 1000 XP
        MOB_XP_TABLE.put(EntityType.WITHER, 1000);
        MOB_XP_TABLE.put(EntityType.ENDER_DRAGON, 1000);
    }

    /**
     * Get base XP value for a mob type
     */
    public static int getBaseXpForMob(LivingEntity mob) {
        return MOB_XP_TABLE.getOrDefault(mob.getType(), 10); // Default 10 XP for unknown mobs
    }

    /**
     * Calculate final XP with level difference modifier.
     * Formula: baseXp × (1 + (mobLevel - playerLevel) × 0.02)
     */
    public static int calculateXp(Player player, LivingEntity mob) {
        int baseXp = getBaseXpForMob(mob);
        int playerLevel = PlayerDataManager.getLevel(player);

        // Use actual mob level (default 1 if not a WowCraft mob)
        int mobLevel = com.gianmarco.wowcraft.entity.MobLevelManager.getMobLevel(mob);

        // Level difference modifier: 2% per level difference
        float modifier = 1.0f + (mobLevel - playerLevel) * 0.02f;

        // Clamp modifier to reasonable range (50% - 150%)
        modifier = Math.max(0.5f, Math.min(1.5f, modifier));

        return Math.max(1, Math.round(baseXp * modifier));
    }

    /**
     * Get XP required to reach the next level.
     * Formula: 100 + (level - 1) * 25
     */
    public static int getXpRequiredForLevel(int level) {
        return 100 + (level - 1) * 25;
    }

    /**
     * Get total XP required from level 1 to reach a target level
     */
    public static int getTotalXpForLevel(int targetLevel) {
        int total = 0;
        for (int lvl = 1; lvl < targetLevel; lvl++) {
            total += getXpRequiredForLevel(lvl);
        }
        return total;
    }

    /**
     * Award XP to a player for killing a mob.
     * Handles level-up logic and notifications.
     */
    public static void awardXpForKill(Player player, LivingEntity mob) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return; // Only process on server
        }

        if (!PlayerDataManager.hasSelectedClass(player)) {
            return; // Only award XP if player has a class
        }

        int xpGain = calculateXp(player, mob);
        int oldLevel = PlayerDataManager.getLevel(player);

        // Add experience
        PlayerData current = PlayerDataManager.getData(player);
        PlayerData updated = addExperienceWithLevelUp(current, xpGain);
        PlayerDataManager.setData(player, updated);

        int newLevel = updated.level();

        // Check for level up
        if (newLevel > oldLevel) {
            onLevelUp(serverPlayer, oldLevel, newLevel);
        }

        // Sync to client
        NetworkHandler.syncPlayerData(serverPlayer);

        WowCraft.LOGGER.debug("Player {} gained {} XP (now {}/{})",
                player.getName().getString(), xpGain, updated.experience(),
                getXpRequiredForLevel(updated.level()));
    }

    /**
     * Add experience with proper level-up handling using the custom formula
     */
    private static PlayerData addExperienceWithLevelUp(PlayerData data, int xpGain) {
        int newXp = data.experience() + xpGain;
        int newLevel = data.level();

        // Check for level ups
        int xpNeeded = getXpRequiredForLevel(newLevel);
        while (newXp >= xpNeeded && newLevel < MAX_LEVEL) {
            newXp -= xpNeeded;
            newLevel++;
            xpNeeded = getXpRequiredForLevel(newLevel);
        }

        // Cap XP at max level
        if (newLevel >= MAX_LEVEL) {
            newLevel = MAX_LEVEL;
            newXp = 0; // No overflow at max level
        }

        return new PlayerData(data.playerClass(), newLevel, newXp,
                data.currentResource(), data.maxResource(), data.comboPoints(), data.moneyPouch(), data.actionBar());
    }

    /**
     * Handle level up - recalculate stats and notify player
     */
    private static void onLevelUp(ServerPlayer player, int oldLevel, int newLevel) {
        // Notify player
        player.displayClientMessage(Component.literal(
                "§6✦ LEVEL UP! §e" + oldLevel + " → " + newLevel), false);

        // Recalculate stats with new level
        StatsManager.recalculateStats(player);

        WowCraft.LOGGER.info("Player {} leveled up: {} -> {}",
                player.getName().getString(), oldLevel, newLevel);
    }
}

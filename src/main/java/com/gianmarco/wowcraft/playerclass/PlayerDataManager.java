package com.gianmarco.wowcraft.playerclass;

import com.gianmarco.wowcraft.economy.MoneyPouch;
import net.minecraft.world.entity.player.Player;

/**
 * Helper class to access and modify player data.
 * Provides a clean API for other parts of the mod.
 */
public class PlayerDataManager {

    /**
     * Get the WowCraft data for a player
     */
    public static PlayerData getData(Player player) {
        return player.getAttachedOrCreate(PlayerDataRegistry.PLAYER_DATA);
    }

    /**
     * Set the WowCraft data for a player
     */
    public static void setData(Player player, PlayerData data) {
        player.setAttached(PlayerDataRegistry.PLAYER_DATA, data);
    }

    /**
     * Get the player's selected class
     */
    public static PlayerClass getPlayerClass(Player player) {
        return getData(player).playerClass();
    }

    /**
     * Check if player has selected a class
     */
    public static boolean hasSelectedClass(Player player) {
        return getData(player).hasSelectedClass();
    }

    /**
     * Set the player's class (usually only done once during class selection)
     */
    public static void setPlayerClass(Player player, PlayerClass playerClass) {
        PlayerData current = getData(player);
        setData(player, current.withClass(playerClass));
    }

    /**
     * Get the player's current resource (mana/rage)
     */
    public static int getCurrentResource(Player player) {
        return getData(player).currentResource();
    }

    /**
     * Get the player's max resource
     */
    public static int getMaxResource(Player player) {
        return getData(player).maxResource();
    }

    /**
     * Modify the player's resource (for ability costs, regeneration, etc.)
     */
    public static void modifyResource(Player player, int amount) {
        PlayerData current = getData(player);
        int newValue = current.currentResource() + amount;
        setData(player, current.withResource(newValue, current.maxResource()));
    }

    /**
     * Set the player's resource to a specific value
     */
    public static void setResource(Player player, int value) {
        PlayerData current = getData(player);
        setData(player, current.withResource(value, current.maxResource()));
    }

    /**
     * Check if player has enough resource to use an ability
     */
    public static boolean hasResource(Player player, int cost) {
        return getCurrentResource(player) >= cost;
    }

    /**
     * Spend resource for an ability (returns false if not enough)
     */
    public static boolean spendResource(Player player, int cost) {
        if (!hasResource(player, cost)) {
            return false;
        }
        modifyResource(player, -cost);
        return true;
    }

    /**
     * Add experience to the player
     */
    public static void addExperience(Player player, int xp) {
        PlayerData current = getData(player);
        setData(player, current.withAddedExperience(xp));
    }

    /**
     * Get the player's current level
     */
    public static int getLevel(Player player) {
        return getData(player).level();
    }

    // ==================== Money Pouch Methods ====================

    /**
     * Get the player's money pouch
     */
    public static MoneyPouch getMoneyPouch(Player player) {
        return getData(player).moneyPouch();
    }

    /**
     * Set the player's money pouch
     */
    public static void setMoneyPouch(Player player, MoneyPouch moneyPouch) {
        PlayerData current = getData(player);
        setData(player, current.withMoneyPouch(moneyPouch));
    }

    /**
     * Add copper coins to the player's money pouch
     */
    public static void addCopper(Player player, int amount) {
        MoneyPouch current = getMoneyPouch(player);
        setMoneyPouch(player, current.addCopper(amount));
    }

    /**
     * Add silver coins to the player's money pouch
     */
    public static void addSilver(Player player, int amount) {
        MoneyPouch current = getMoneyPouch(player);
        setMoneyPouch(player, current.addSilver(amount));
    }

    /**
     * Add gold coins to the player's money pouch
     */
    public static void addGold(Player player, int amount) {
        MoneyPouch current = getMoneyPouch(player);
        setMoneyPouch(player, current.addGold(amount));
    }

    /**
     * Try to spend copper from the player's money pouch.
     * Returns true if successful, false if not enough funds.
     */
    public static boolean spendCopper(Player player, int amount) {
        MoneyPouch current = getMoneyPouch(player);
        MoneyPouch result = current.removeCopper(amount);
        if (result == null) {
            return false; // Not enough funds
        }
        setMoneyPouch(player, result);
        return true;
    }

    /**
     * Try to spend silver from the player's money pouch.
     * Returns true if successful, false if not enough funds.
     */
    public static boolean spendSilver(Player player, int amount) {
        return spendCopper(player, amount * MoneyPouch.COPPER_PER_SILVER);
    }

    /**
     * Try to spend gold from the player's money pouch.
     * Returns true if successful, false if not enough funds.
     */
    public static boolean spendGold(Player player, int amount) {
        return spendCopper(player, amount * MoneyPouch.COPPER_PER_GOLD);
    }

    /**
     * Check if the player has at least this much copper (total value)
     */
    public static boolean hasFunds(Player player, int copperAmount) {
        return getMoneyPouch(player).hasFunds(copperAmount);
    }
}

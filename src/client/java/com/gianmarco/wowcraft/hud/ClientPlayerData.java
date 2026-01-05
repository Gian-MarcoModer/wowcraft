package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.economy.MoneyPouch;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;

/**
 * Client-side cache of player data for HUD rendering.
 * Updated when we receive sync packets from the server.
 */
public class ClientPlayerData {
    private static PlayerData cachedData = PlayerData.DEFAULT;

    public static PlayerData getData() {
        return cachedData;
    }

    public static void setData(PlayerData data) {
        cachedData = data;
    }

    public static void update(PlayerClass playerClass, int level, int experience,
            int currentResource, int maxResource, int comboPoints, MoneyPouch moneyPouch) {
        // Preserve the action bar from the cached data (action bar is managed
        // separately on client)
        cachedData = new PlayerData(playerClass, level, experience, currentResource, maxResource, comboPoints,
                moneyPouch, cachedData.actionBar());
    }

    public static boolean hasSelectedClass() {
        return cachedData.hasSelectedClass();
    }

    public static PlayerClass getPlayerClass() {
        return cachedData.playerClass();
    }

    public static int getCurrentResource() {
        return cachedData.currentResource();
    }

    public static int getMaxResource() {
        return cachedData.maxResource();
    }

    public static int getLevel() {
        return cachedData.level();
    }

    public static MoneyPouch getMoneyPouch() {
        return cachedData.moneyPouch();
    }

    /**
     * Reset when player disconnects
     */
    public static void reset() {
        cachedData = PlayerData.DEFAULT;
    }
}

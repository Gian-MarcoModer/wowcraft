package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Server-side storage for player action bar configurations.
 * Now backed by PlayerData for persistence across sessions.
 */
public class ServerActionBar {

    /**
     * Initialize a player's action bar with defaults for their class.
     * This is called when the player first selects a class or when switching
     * classes.
     */
    public static void initFromDefaults(Player player, PlayerClass playerClass) {
        Ability[] defaults = AbilityRegistry.getDefaultActionBar(playerClass);
        List<String> bar = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            bar.add((i < defaults.length && defaults[i] != null) ? defaults[i].getId() : "");
        }

        // Save to PlayerData for persistence
        PlayerData current = PlayerDataManager.getData(player);
        PlayerDataManager.setData(player, current.withActionBar(bar));
    }

    /**
     * Set a slot to a specific ability
     */
    public static void setSlot(Player player, int slot, String abilityId) {
        if (slot < 0 || slot >= 8)
            return;

        PlayerData current = PlayerDataManager.getData(player);
        List<String> bar = new ArrayList<>(current.actionBar());

        // Ensure the list has 8 elements
        while (bar.size() < 8) {
            bar.add("");
        }

        bar.set(slot, abilityId != null ? abilityId : "");
        PlayerDataManager.setData(player, current.withActionBar(bar));
    }

    /**
     * Get the ability ID in a slot
     */
    public static String getSlotId(Player player, int slot) {
        if (slot < 0 || slot >= 8)
            return null;

        List<String> bar = PlayerDataManager.getData(player).actionBar();
        if (slot < bar.size()) {
            String id = bar.get(slot);
            return id.isEmpty() ? null : id;
        }
        return null;
    }

    /**
     * Get the Ability for a slot
     */
    public static Ability getSlot(Player player, int slot) {
        String id = getSlotId(player, slot);
        return id != null ? AbilityRegistry.getAbilityById(id) : null;
    }

    /**
     * Check if player has an action bar configured (not empty defaults)
     */
    public static boolean isInitialized(Player player) {
        List<String> bar = PlayerDataManager.getData(player).actionBar();
        // Check if action bar has any non-empty slots
        return bar.stream().anyMatch(id -> id != null && !id.isEmpty());
    }

    /**
     * Clear player action bar (resets to empty)
     * Note: This doesn't need to remove from a map anymore since data is in
     * PlayerData
     */
    public static void clear(Player player) {
        PlayerData current = PlayerDataManager.getData(player);
        PlayerDataManager.setData(player, current.withActionBar(PlayerData.EMPTY_ACTION_BAR));
    }

    /**
     * Set entire action bar at once (from sync packet)
     */
    public static void setFullActionBar(Player player, String[] abilityIds) {
        List<String> bar = new ArrayList<>(8);
        for (int i = 0; i < 8; i++) {
            if (i < abilityIds.length && abilityIds[i] != null) {
                bar.add(abilityIds[i]);
            } else {
                bar.add("");
            }
        }

        PlayerData current = PlayerDataManager.getData(player);
        PlayerDataManager.setData(player, current.withActionBar(bar));
    }
}

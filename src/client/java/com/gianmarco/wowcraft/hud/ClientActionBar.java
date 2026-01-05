package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.ability.AbilityRegistry;
import com.gianmarco.wowcraft.playerclass.PlayerClass;

import java.util.List;

/**
 * Client-side action bar storage.
 * Stores the player's customized action bar assignments.
 * This is synced from the server on login and whenever the action bar changes.
 */
public class ClientActionBar {

    private static final String[] actionBarIds = new String[8];
    private static boolean initialized = false;

    /**
     * Initialize from defaults for a class (called on login/class selection)
     */
    public static void initFromDefaults(PlayerClass playerClass) {
        Ability[] defaults = AbilityRegistry.getDefaultActionBar(playerClass);
        for (int i = 0; i < 8; i++) {
            actionBarIds[i] = (i < defaults.length && defaults[i] != null) ? defaults[i].getId() : null;
        }
        initialized = true;
    }

    /**
     * Set action bar from a list of ability IDs (synced from server)
     */
    public static void setFromList(List<String> abilityIds) {
        for (int i = 0; i < 8; i++) {
            if (i < abilityIds.size()) {
                String id = abilityIds.get(i);
                actionBarIds[i] = (id != null && !id.isEmpty()) ? id : null;
            } else {
                actionBarIds[i] = null;
            }
        }
        initialized = true;
    }

    /**
     * Set an ability in a slot
     */
    public static void setSlot(int slot, String abilityId) {
        if (slot >= 0 && slot < 8) {
            actionBarIds[slot] = abilityId;
        }
    }

    /**
     * Clear a slot
     */
    public static void clearSlot(int slot) {
        if (slot >= 0 && slot < 8) {
            actionBarIds[slot] = null;
        }
    }

    /**
     * Get the ability ID in a slot
     */
    public static String getSlotId(int slot) {
        if (slot >= 0 && slot < 8) {
            return actionBarIds[slot];
        }
        return null;
    }

    /**
     * Get the Ability object in a slot
     */
    public static Ability getSlot(int slot) {
        String id = getSlotId(slot);
        return id != null ? AbilityRegistry.getAbilityById(id) : null;
    }

    /**
     * Get all abilities as array
     */
    public static Ability[] getAllSlots() {
        Ability[] abilities = new Ability[8];
        for (int i = 0; i < 8; i++) {
            abilities[i] = getSlot(i);
        }
        return abilities;
    }

    /**
     * Check if initialized
     */
    public static boolean isInitialized() {
        return initialized;
    }

    /**
     * Reset (for logout)
     */
    public static void reset() {
        for (int i = 0; i < 8; i++) {
            actionBarIds[i] = null;
        }
        initialized = false;
    }
}

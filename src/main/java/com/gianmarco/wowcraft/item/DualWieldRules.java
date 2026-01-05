package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.world.entity.player.Player;

/**
 * Rules for dual wielding weapons in WoW Classic style.
 * Rogues can dual wield from level 1.
 * Warriors and Hunters learn dual wield at level 10.
 */
public class DualWieldRules {

    /**
     * Check if a player can dual wield based on class and level
     */
    public static boolean canDualWield(PlayerClass playerClass, int playerLevel) {
        return switch (playerClass) {
            case WARRIOR -> playerLevel >= 10;  // Warriors learn at level 10
            // Future classes:
            // case ROGUE -> true;  // Rogues can dual wield from level 1
            // case HUNTER -> playerLevel >= 10;  // Hunters learn at level 10
            default -> false;
        };
    }

    /**
     * Check if a player can equip a specific item in their off-hand
     */
    public static boolean canEquipOffHand(Player player, WowItem mainHandItem, WowItem offHandItem, PlayerClass playerClass, int playerLevel) {
        BaseItemType offHandType = offHandItem.getBaseType();

        // Shields and caster off-hands can always be equipped (if class appropriate)
        if (offHandType.isOffHandOnly()) {
            return true;
        }

        // Check if main hand is a two-handed weapon
        if (mainHandItem != null && mainHandItem.getBaseType().isTwoHanded()) {
            return false; // Cannot use off-hand with 2H weapon
        }

        // Weapons in off-hand require dual wield ability
        if (offHandType.isWeapon() && offHandType.canDualWield()) {
            return canDualWield(playerClass, playerLevel);
        }

        return false;
    }

    /**
     * Get the off-hand damage penalty
     * In WoW Classic, off-hand weapons deal 50% damage
     */
    public static float getOffHandDamagePenalty() {
        return 0.5f;  // 50% damage
    }

    /**
     * Get the dual wield miss chance penalty
     * In WoW Classic, dual wielding adds 19% miss chance without the dual wield skill
     * With the skill, it's reduced to the normal miss chance
     */
    public static float getDualWieldMissPenalty(PlayerClass playerClass, int playerLevel) {
        // If player has dual wield skill (can dual wield), no penalty
        if (canDualWield(playerClass, playerLevel)) {
            return 0.0f;  // Normal miss chance
        }

        // Without dual wield skill, 19% additional miss chance
        return 0.19f;
    }

    /**
     * Check if a two-handed weapon is equipped
     */
    public static boolean hasTwoHandedWeapon(WowItem mainHandItem) {
        return mainHandItem != null && mainHandItem.getBaseType().isTwoHanded();
    }
}

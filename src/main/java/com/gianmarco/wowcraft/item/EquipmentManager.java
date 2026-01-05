package com.gianmarco.wowcraft.item;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Manages equipment tracking - reads WoW items from player's vanilla equipment
 * slots.
 * Items in vanilla armor/weapon slots with WowItemData contribute stats.
 */
public class EquipmentManager {

    /**
     * Calculate total stats from all equipped items with WowItemData.
     * Reads from vanilla Minecraft equipment slots.
     * Note: Items that can't be worn by the player's class are prevented from being
     * equipped by InventoryArmorMixin, so we don't need to check here.
     */
    public static WowStats calculateEquippedStats(Player player) {
        WowStats total = WowStats.ZERO;

        // Check all equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // Check if this item has WoW data
            WowItemData data = stack.get(WowItemComponents.WOW_ITEM_DATA);
            if (data != null && data.isValid()) {
                total = total.add(data.getStats());
            }
        }

        return total;
    }

    /**
     * Calculate total armor from all equipped items with WowItemData.
     * Reads from vanilla Minecraft equipment slots.
     * Note: Items that can't be worn by the player's class are prevented from being
     * equipped by InventoryArmorMixin, so we don't need to check here.
     */
    public static int calculateTotalArmor(Player player) {
        int totalArmor = 0;

        // Check all equipment slots
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.isEmpty()) {
                continue;
            }

            // Check if this item has WoW data
            WowItemData data = stack.get(WowItemComponents.WOW_ITEM_DATA);
            if (data != null && data.isValid()) {
                totalArmor += data.armorValue();
            }
        }

        return totalArmor;
    }

    /**
     * Get the WoW item in a specific equipment slot
     */
    public static WowItemData getEquippedItem(Player player, EquipmentSlot slot) {
        ItemStack stack = player.getItemBySlot(slot);
        if (stack.isEmpty()) {
            return null;
        }
        return stack.get(WowItemComponents.WOW_ITEM_DATA);
    }

    /**
     * Check if a player has any WoW items equipped
     */
    public static boolean hasWowEquipment(Player player) {
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                WowItemData data = stack.get(WowItemComponents.WOW_ITEM_DATA);
                if (data != null && data.isValid()) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Get description of all equipped WoW items (for debugging)
     */
    public static String getEquipmentSummary(Player player) {
        StringBuilder sb = new StringBuilder("Equipped WoW Items:\n");
        boolean hasAny = false;

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack stack = player.getItemBySlot(slot);
            if (!stack.isEmpty()) {
                WowItemData data = stack.get(WowItemComponents.WOW_ITEM_DATA);
                if (data != null && data.isValid()) {
                    sb.append("  ").append(slot.getName()).append(": ")
                            .append(data.getDisplayName()).append("\n");
                    hasAny = true;
                }
            }
        }

        if (!hasAny) {
            sb.append("  (none)");
        }

        return sb.toString();
    }
}

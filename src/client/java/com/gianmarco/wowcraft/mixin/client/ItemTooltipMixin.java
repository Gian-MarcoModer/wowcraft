package com.gianmarco.wowcraft.mixin.client;

import com.gianmarco.wowcraft.hud.ClientPlayerData;
import com.gianmarco.wowcraft.item.BaseItemType;
import com.gianmarco.wowcraft.item.WowItemComponents;
import com.gianmarco.wowcraft.item.WowItemData;
import com.gianmarco.wowcraft.item.WowStats;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin to add WoW Classic-style tooltips to items with WowItemData.
 * 
 * WoW Classic tooltip format:
 * - Item Name (color-coded by rarity)
 * - Bind on Equip (if applicable)
 * - Slot                    Armor Type
 * - Armor Value
 * - +Stats (green)
 * - Durability
 * - Requires Level X
 * - Equip: effects (green)
 * - "Flavor text" (yellow)
 * - Sell Price
 */
@Mixin(ItemStack.class)
public abstract class ItemTooltipMixin {

    @Inject(method = "getTooltipLines", at = @At("RETURN"))
    private void wowcraft$addWowTooltip(Item.TooltipContext context, Player player, TooltipFlag flag,
            CallbackInfoReturnable<List<Component>> cir) {
        ItemStack self = (ItemStack) (Object) this;

        // Check if this item has WoW data
        WowItemData data = self.get(WowItemComponents.WOW_ITEM_DATA);
        if (data == null || !data.isValid()) {
            return;
        }

        List<Component> tooltip = cir.getReturnValue();

        // Remove vanilla attribute lines (Attack Damage, Attack Speed, Armor)
        List<Component> toRemove = new ArrayList<>();
        for (Component line : tooltip) {
            String text = line.getString().toLowerCase();
            if (text.contains("attack damage") || text.contains("attack speed") ||
                    text.contains("armor") || text.contains("when in main hand") ||
                    text.contains("when in off hand") || text.contains("when on")) {
                toRemove.add(line);
            }
        }
        tooltip.removeAll(toRemove);

        // Find insertion point (after the item name)
        int insertIndex = 1;

        // Bind on Equip (WoW-style)
        tooltip.add(insertIndex++, Component.literal("§fBind on Equip"));

        // Slot and Armor Type on same line (like WoW)
        BaseItemType.ArmorClass armorClass = data.getArmorClass();
        String slotName = data.getSlot().getDisplayName();

        // Get player class for armor restriction checks
        PlayerClass playerClass = ClientPlayerData.getPlayerClass();

        if (armorClass != null) {
            // Format: "Head                     Cloth"
            // Color armor type red if player can't wear it, otherwise white
            boolean canWear = armorClass.canBeWornBy(playerClass);
            String armorColor = canWear ? "§f" : "§c";
            String armorType = armorClass.getDisplayName();
            tooltip.add(insertIndex++, Component.literal("§f" + slotName + "                              " + armorColor + armorType));
        } else if (data.isWeapon()) {
            // For weapons: show weapon type
            BaseItemType baseType = data.getBaseType();
            String weaponType = baseType.getWeaponClass() != null ? baseType.getWeaponClass().getDisplayName() : "Weapon";
            if (data.isTwoHanded()) {
                tooltip.add(insertIndex++, Component.literal("§f" + slotName + "                         " + weaponType));
                tooltip.add(insertIndex++, Component.literal("§fTwo-Hand"));
            } else {
                tooltip.add(insertIndex++, Component.literal("§f" + slotName + "                         " + weaponType));
            }
        } else {
            tooltip.add(insertIndex++, Component.literal("§f" + slotName));
        }

        // Weapon stats (damage and speed on same line like WoW)
        if (data.isWeapon() && data.attackDamage() > 0) {
            float minDmg = data.attackDamage() * 0.85f;
            float maxDmg = data.attackDamage() * 1.15f;
            float speedInSeconds = 1.0f / data.attackSpeed();
            
            // WoW format: "23 - 35 Damage                Speed 2.60"
            tooltip.add(insertIndex++, Component.literal(
                String.format("§f%.0f - %.0f Damage                        Speed %.2f", minDmg, maxDmg, speedInSeconds)));
            
            // DPS in parentheses
            float dps = data.getDPS();
            tooltip.add(insertIndex++, Component.literal(String.format("§7(%.1f damage per second)", dps)));
        }

        // Armor value (white, prominent)
        if (data.armorValue() > 0) {
            tooltip.add(insertIndex++, Component.literal("§f" + data.armorValue() + " Armor"));
        }

        // Primary stats (green with + sign, like WoW)
        WowStats stats = data.getStats();
        if (stats.strength() > 0) {
            tooltip.add(insertIndex++, Component.literal("§a+" + stats.strength() + " Strength"));
        }
        if (stats.agility() > 0) {
            tooltip.add(insertIndex++, Component.literal("§a+" + stats.agility() + " Agility"));
        }
        if (stats.stamina() > 0) {
            tooltip.add(insertIndex++, Component.literal("§a+" + stats.stamina() + " Stamina"));
        }
        if (stats.intellect() > 0) {
            tooltip.add(insertIndex++, Component.literal("§a+" + stats.intellect() + " Intellect"));
        }
        if (stats.spirit() > 0) {
            tooltip.add(insertIndex++, Component.literal("§a+" + stats.spirit() + " Spirit"));
        }

        // Durability (WoW-style) - placeholder for now
        tooltip.add(insertIndex++, Component.literal("§fDurability 100 / 100"));

        // Requires Level (red if player is too low, white otherwise)
        int requiredLevel = Math.max(1, data.itemLevel());
        int playerLevel = ClientPlayerData.getLevel();
        String levelColor = playerLevel >= requiredLevel ? "§f" : "§c";
        tooltip.add(insertIndex++, Component.literal(levelColor + "Requires Level " + requiredLevel));

        // Note: Armor restriction is now shown by coloring the armor type text red (see above)
        // Removed the "(Cannot use: X)" line as requested

        // Suffix flavor text removed for generated items as requested
        // Only named/unique items should have flavor text

        // Item Level at bottom (like modern WoW)
        tooltip.add(insertIndex++, Component.literal("§eItem Level " + data.itemLevel()));

        // Sell Price (WoW-style with copper/silver/gold)
        int sellPrice = calculateSellPrice(data);
        tooltip.add(insertIndex++, Component.literal("§fSell Price: " + formatMoney(sellPrice)));
    }

    /**
     * Calculate sell price based on item level and rarity
     */
    private int calculateSellPrice(WowItemData data) {
        int basePrice = data.itemLevel() * 10;
        return (int) (basePrice * data.getRarity().getStatMultiplier());
    }

    /**
     * Format copper value as gold/silver/copper like WoW
     */
    private String formatMoney(int copper) {
        int gold = copper / 10000;
        int silver = (copper % 10000) / 100;
        int remainingCopper = copper % 100;

        StringBuilder sb = new StringBuilder();
        if (gold > 0) {
            sb.append("§6").append(gold).append("§eg ");
        }
        if (silver > 0 || gold > 0) {
            sb.append("§7").append(silver).append("§fs ");
        }
        sb.append("§c").append(remainingCopper).append("§fc");
        return sb.toString();
    }
}


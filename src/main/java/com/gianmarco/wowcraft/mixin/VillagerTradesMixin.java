package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.world.entity.npc.AbstractVillager;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Set;

/**
 * Mixin to disable weapon trades from villagers (and wandering traders).
 * This removes swords, axes (as weapons), bows, crossbows, arrows, and shields
 * from villager trades.
 * Later, Blacksmiths from the profession system will be the only source of
 * crafted weapons.
 */
@Mixin(AbstractVillager.class)
public abstract class VillagerTradesMixin {

    // Weapons we want to disable trading for
    private static final Set<Item> DISABLED_WEAPON_ITEMS = Set.of(
            // Swords
            Items.WOODEN_SWORD,
            Items.STONE_SWORD,
            Items.IRON_SWORD,
            Items.GOLDEN_SWORD,
            Items.DIAMOND_SWORD,
            Items.NETHERITE_SWORD,
            // Axes (used as weapons)
            Items.WOODEN_AXE,
            Items.STONE_AXE,
            Items.IRON_AXE,
            Items.GOLDEN_AXE,
            Items.DIAMOND_AXE,
            Items.NETHERITE_AXE,
            // Ranged weapons
            Items.BOW,
            Items.CROSSBOW,
            Items.TRIDENT,
            // Ammunition
            Items.ARROW,
            Items.SPECTRAL_ARROW,
            Items.TIPPED_ARROW,
            // Shields
            Items.SHIELD);

    /**
     * Called after villager updates its trades.
     * We inject after the trade list is populated and remove any weapon trades.
     */
    @Inject(method = "updateTrades", at = @At("TAIL"))
    private void wowcraft$removeWeaponTrades(CallbackInfo ci) {
        AbstractVillager self = (AbstractVillager) (Object) this;
        MerchantOffers offers = self.getOffers();
        if (offers == null || offers.isEmpty()) {
            return;
        }

        int removedCount = 0;
        // Iterate backwards to safely remove items
        for (int i = offers.size() - 1; i >= 0; i--) {
            MerchantOffer offer = offers.get(i);
            ItemStack result = offer.getResult();

            if (isWeaponItem(result)) {
                offers.remove(i);
                removedCount++;
            }
        }

        if (removedCount > 0) {
            WowCraft.LOGGER.debug("Removed {} weapon trade(s) from villager", removedCount);
        }
    }

    /**
     * Check if the item is a weapon we want to disable.
     */
    private boolean isWeaponItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        Item item = stack.getItem();

        // Check explicit list - covers all vanilla weapons
        return DISABLED_WEAPON_ITEMS.contains(item);
    }
}

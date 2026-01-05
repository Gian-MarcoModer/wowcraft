package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.item.BaseItemType;
import com.gianmarco.wowcraft.item.WowItemComponents;
import com.gianmarco.wowcraft.item.WowItemData;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent players from equipping armor they can't use.
 * Uses both setItemSlot (for hotbar swaps) and a return value check to prevent issues.
 */
@Mixin(LivingEntity.class)
public abstract class ArmorEquipMixin {

    /**
     * Intercept setItemSlot to validate armor before equipping.
     * This catches hotbar key swaps and some other equip methods.
     */
    @Inject(method = "setItemSlot", at = @At("HEAD"), cancellable = true)
    private void wowcraft$validateArmorEquip(EquipmentSlot slot, ItemStack stack, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only apply to players on server side
        if (!(self instanceof Player player) || player.level().isClientSide()) {
            return;
        }

        // Only check armor slots and offhand
        if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR && slot != EquipmentSlot.OFFHAND) {
            return;
        }

        // Allow removing items (empty stack)
        if (stack.isEmpty()) {
            return;
        }

        System.out.println("WOWCRAFT DEBUG: setItemSlot called for " + slot + " with item: " + stack.getHoverName().getString());

        // Only check WoW items
        WowItemData data = stack.get(WowItemComponents.WOW_ITEM_DATA);
        if (data == null || !data.isValid()) {
            return;
        }

        // Check if this is armor with class restrictions
        BaseItemType.ArmorClass armorClass = data.getArmorClass();
        if (armorClass == null) {
            return; // Not armor, allow it
        }

        // Get player's class and check if they can wear it
        PlayerClass playerClass = PlayerDataManager.getPlayerClass(player);
        System.out.println("WOWCRAFT DEBUG: Player class: " + playerClass + ", Armor class: " + armorClass + ", Can wear: " + armorClass.canBeWornBy(playerClass));

        if (!armorClass.canBeWornBy(playerClass)) {
            // Block the equip
            String itemType = armorClass == BaseItemType.ArmorClass.SHIELD ? "shield"
                : armorClass.getDisplayName() + " armor";
            player.displayClientMessage(
                Component.literal("§cYou cannot use a " + itemType + " as a " + playerClass.getDisplayName() + "!"),
                true
            );
            System.out.println("WOWCRAFT DEBUG: BLOCKING equip!");
            ci.cancel();
        }
    }

    /**
     * Also intercept getEquipmentSlotForItem to prevent vanilla auto-equipping.
     * This is called when shift-clicking armor or using certain equip commands.
     */
    @Inject(method = "getEquipmentSlotForItem", at = @At("HEAD"), cancellable = true)
    private static void wowcraft$preventAutoEquipWrongArmor(ItemStack stack, CallbackInfoReturnable<EquipmentSlot> cir) {
        // This method is static, so we can't access the player here
        // We'll handle this case in the setItemSlot injection above
    }
}

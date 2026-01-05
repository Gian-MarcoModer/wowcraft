package com.gianmarco.wowcraft.mixin.client;

import com.gianmarco.wowcraft.item.WowItemComponents;
import com.gianmarco.wowcraft.item.WowItemData;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Enforces WoW-style attack speed cooldowns on the client side.
 * This ensures all weapons (including non-standard items like staves/wands)
 * properly respect their attack speed attributes.
 */
@Mixin(LocalPlayer.class)
public abstract class AttackCooldownMixin {

    @Unique
    private long wowcraft$lastAttackTime = 0;

    @Unique
    private float wowcraft$currentWeaponSpeed = 4.0f; // Default Minecraft speed

    @Unique
    private ItemStack wowcraft$getMainHandItem() {
        return ((LivingEntity) (Object) this).getMainHandItem();
    }

    /**
     * Inject into the tick method to update current weapon speed
     */
    @Inject(method = "tick", at = @At("HEAD"))
    private void wowcraft$updateWeaponSpeed(CallbackInfo ci) {
        ItemStack mainHand = wowcraft$getMainHandItem();
        WowItemData wowData = mainHand.get(WowItemComponents.WOW_ITEM_DATA);

        if (wowData != null && wowData.isWeapon() && wowData.attackSpeed() > 0) {
            // Use WoW weapon speed
            wowcraft$currentWeaponSpeed = wowData.attackSpeed();
        } else {
            // Fallback to Minecraft default
            wowcraft$currentWeaponSpeed = 4.0f;
        }
    }

    /**
     * Inject into resetAttackStrengthTicker to enforce WoW attack speed cooldowns
     * This is called when the player attacks
     */
    @Inject(method = "resetAttackStrengthTicker", at = @At("HEAD"), cancellable = true)
    private void wowcraft$enforceAttackCooldown(CallbackInfo ci) {
        // Check if holding a WoW weapon
        ItemStack mainHand = wowcraft$getMainHandItem();
        WowItemData wowData = mainHand.get(WowItemComponents.WOW_ITEM_DATA);

        if (wowData != null && wowData.isWeapon() && wowData.attackSpeed() > 0) {
            long currentTime = System.currentTimeMillis();

            // Calculate cooldown in milliseconds based on attack speed
            // Attack speed is attacks per second, so cooldown = 1000ms / attackSpeed
            float cooldownMs = 1000.0f / wowcraft$currentWeaponSpeed;

            // Check if enough time has passed since last attack
            long timeSinceLastAttack = currentTime - wowcraft$lastAttackTime;

            if (timeSinceLastAttack < cooldownMs) {
                // Still on cooldown, cancel the attack reset
                ci.cancel();
                return;
            }

            // Attack is allowed, update last attack time
            wowcraft$lastAttackTime = currentTime;
        }
        // If not a WoW weapon, let vanilla behavior handle it
    }
}

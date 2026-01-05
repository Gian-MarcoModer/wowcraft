package com.gianmarco.wowcraft.mixin;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla hunger mechanics.
 * - Prevents hunger depletion from sprinting/jumping
 * - Prevents vanilla health regeneration from food
 * - Prevents starvation damage
 * 
 * Health regeneration is handled by our Spirit-based system in
 * ResourceRegeneration.java
 */
@Mixin(FoodData.class)
public class HungerLogicMixin {

    /**
     * Cancel the hunger tick to disable all vanilla hunger logic.
     * We set food level to max as a safeguard before cancelling.
     */
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void wowcraft$disableHungerTick(ServerPlayer player, CallbackInfo ci) {
        FoodData self = (FoodData) (Object) this;

        // Keep food level at max to prevent any hunger-related effects
        self.setFoodLevel(20);
        self.setSaturation(5.0f);

        // Cancel the vanilla tick - this disables:
        // - Hunger depletion
        // - Vanilla health regen from food
        // - Starvation damage
        ci.cancel();
    }
}

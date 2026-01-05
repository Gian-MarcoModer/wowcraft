package com.gianmarco.wowcraft.mixin.client;

import com.gianmarco.wowcraft.hud.ClientPlayerData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Blocks player movement and actions until a class is selected.
 * Forces players to complete class selection on first join.
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerMixin {

    @Inject(method = "aiStep", at = @At("HEAD"), cancellable = true)
    private void wowcraft$blockMovementWithoutClass(CallbackInfo ci) {
        // Block all movement if no class is selected
        if (!ClientPlayerData.hasSelectedClass()) {
            // Cancel the entire aiStep to prevent any movement
            ci.cancel();
        }
    }
}

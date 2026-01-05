package com.gianmarco.wowcraft.mixin.client;

import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Gui.class)
public class InGameHudMixin {

    // Hide vanilla hearts/health bar
    @Inject(method = "renderHearts", at = @At("HEAD"), cancellable = true)
    private void hideHearts(GuiGraphics guiGraphics, Player player, int x, int y, int height, int offsetHeartIndex,
            float maxHealth, int currentHealth, int displayHealth, int absorption, boolean renderHighlight,
            CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla food/hunger bar
    @Inject(method = "renderFood", at = @At("HEAD"), cancellable = true)
    private void hideFood(GuiGraphics guiGraphics, Player player, int x, int y, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla XP bar (repurposed as crafting resource in character sheet)
    @Inject(method = "renderExperienceBar", at = @At("HEAD"), cancellable = true)
    private void hideXpBar(GuiGraphics guiGraphics, int x, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide experience level number
    @Inject(method = "renderExperienceLevel", at = @At("HEAD"), cancellable = true)
    private void hideXpLevel(GuiGraphics guiGraphics, net.minecraft.client.DeltaTracker delta, CallbackInfo ci) {
        ci.cancel();
    }

    // Hide vanilla armor bar
    @Inject(method = "renderArmor", at = @At("HEAD"), cancellable = true)
    private static void hideArmor(GuiGraphics guiGraphics, Player player, int x, int y, int maxHealth, int rowHeight,
            CallbackInfo ci) {
        ci.cancel();
    }
}

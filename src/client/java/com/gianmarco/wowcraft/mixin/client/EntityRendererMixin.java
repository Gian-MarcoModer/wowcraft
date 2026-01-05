package com.gianmarco.wowcraft.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Disables vanilla entity name tag rendering completely.
 * We use our own custom 3D nameplate system instead (EntityNameplateRenderer).
 */
@Mixin(EntityRenderer.class)
public class EntityRendererMixin {

    /**
     * Cancel all vanilla name tag rendering.
     * We render our own custom nameplates via EntityNameplateRenderer instead.
     */
    @Inject(method = "renderNameTag(Lnet/minecraft/client/renderer/entity/state/EntityRenderState;Lnet/minecraft/network/chat/Component;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;I)V",
            at = @At("HEAD"),
            cancellable = true)
    private void onRenderNameTag(EntityRenderState renderState, Component name,
                                  PoseStack poseStack, MultiBufferSource bufferSource, int packedLight,
                                  CallbackInfo ci) {
        // Cancel all vanilla name tag rendering
        // Our custom EntityNameplateRenderer handles this instead
        ci.cancel();
    }
}

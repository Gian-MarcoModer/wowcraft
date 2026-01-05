package com.gianmarco.wowcraft.mixin.client;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.mojang.blaze3d.vertex.PoseStack;

/**
 * This mixin is no longer needed for FCT rendering.
 * FCT now uses WorldRenderEvents.AFTER_ENTITIES for world-space rendering.
 * Keeping this class for potential future use but with empty implementation.
 */
@Mixin(EntityRenderDispatcher.class)
public class EntityRenderDispatcherMixin {

    // FCT rendering moved to FloatingDamageTextRenderer.register() using
    // WorldRenderEvents
    // @Inject(method = "render", at = @At("TAIL"))
    // public <E extends Entity> void renderFCT(E entity, double x, double y, double
    // z, float rotationYaw,
    // PoseStack poseStack, MultiBufferSource buffer, int packedLight, CallbackInfo
    // ci) {
    // // No longer used
    // }
}

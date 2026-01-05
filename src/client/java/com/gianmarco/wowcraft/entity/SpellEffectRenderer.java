package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Renderer for SpellEffectEntity that draws a flat textured quad on the ground.
 * Creates WoW-style ground decal effects for spells like Frost Nova and Arcane Explosion.
 */
@Environment(EnvType.CLIENT)
public class SpellEffectRenderer extends EntityRenderer<SpellEffectEntity, SpellEffectRenderState> {

    // Texture locations for each effect type
    private static final ResourceLocation FROST_NOVA_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "textures/effects/frost_nova.png");
    private static final ResourceLocation ARCANE_EXPLOSION_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "textures/effects/arcane_explosion.png");
    private static final ResourceLocation CONSECRATION_TEXTURE = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "textures/effects/consecration.png");

    public SpellEffectRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public SpellEffectRenderState createRenderState() {
        return new SpellEffectRenderState();
    }

    @Override
    public void extractRenderState(SpellEffectEntity entity, SpellEffectRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        state.effectType = entity.getEffectType();
        state.currentRadius = entity.getCurrentRadius(partialTick);
        state.alpha = entity.getAlpha(partialTick);
        state.rotation = entity.getRotation(partialTick);
    }

    @Override
    public void render(SpellEffectRenderState state, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        if (state.alpha <= 0) return;

        poseStack.pushPose();

        // Rotate to lie flat on the ground (facing up)
        poseStack.mulPose(com.mojang.math.Axis.XP.rotationDegrees(90));

        // Apply rotation for visual interest
        poseStack.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(state.rotation));

        // Scale to current radius
        float scale = state.currentRadius;
        poseStack.scale(scale, scale, 1.0f);

        // Get texture for this effect type
        ResourceLocation texture = getTextureForType(state.effectType);

        // Use translucent render type for transparency
        RenderType renderType = RenderType.entityTranslucent(texture);
        VertexConsumer vertexConsumer = bufferSource.getBuffer(renderType);

        // Get transformation matrix
        Matrix4f matrix = poseStack.last().pose();

        // Calculate alpha (0-255)
        int alpha = (int) (state.alpha * 255);

        // Draw quad centered at origin, extending from -1 to 1
        // Vertices in counter-clockwise order when viewed from above
        float size = 1.0f;

        // Bottom-left
        vertexConsumer.addVertex(matrix, -size, -size, 0)
                .setColor(255, 255, 255, alpha)
                .setUv(0, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 0, 1);

        // Bottom-right
        vertexConsumer.addVertex(matrix, size, -size, 0)
                .setColor(255, 255, 255, alpha)
                .setUv(1, 1)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 0, 1);

        // Top-right
        vertexConsumer.addVertex(matrix, size, size, 0)
                .setColor(255, 255, 255, alpha)
                .setUv(1, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 0, 1);

        // Top-left
        vertexConsumer.addVertex(matrix, -size, size, 0)
                .setColor(255, 255, 255, alpha)
                .setUv(0, 0)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(0, 0, 1);

        poseStack.popPose();
    }

    private ResourceLocation getTextureForType(SpellEffectEntity.EffectType type) {
        return switch (type) {
            case FROST_NOVA -> FROST_NOVA_TEXTURE;
            case ARCANE_EXPLOSION -> ARCANE_EXPLOSION_TEXTURE;
            case CONSECRATION -> CONSECRATION_TEXTURE;
        };
    }
}

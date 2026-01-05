package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side entity rendering registration.
 */
@Environment(EnvType.CLIENT)
public class ModEntityRenderers {

    /**
     * Register all entity renderers.
     * Called from WowCraftClient.onInitializeClient()
     */
    public static void register() {
        // Register fireball projectile with invisible renderer (uses particles)
        EntityRendererRegistry.register(ModEntities.FIREBALL_PROJECTILE, InvisibleProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.ARCANE_MISSILE_PROJECTILE, InvisibleProjectileRenderer::new);
        EntityRendererRegistry.register(ModEntities.ICE_LANCE_PROJECTILE, InvisibleProjectileRenderer::new);

        // Spell effect ground decal renderer
        EntityRendererRegistry.register(ModEntities.SPELL_EFFECT, SpellEffectRenderer::new);

        WowCraft.LOGGER.info("Registered WowCraft entity renderers");
    }

    /**
     * Invisible renderer for projectiles that use particles instead of models
     */
    public static class InvisibleProjectileRenderer<T extends net.minecraft.world.entity.Entity> extends EntityRenderer<T, EntityRenderState> {

        public InvisibleProjectileRenderer(EntityRendererProvider.Context context) {
            super(context);
        }

        @Override
        public EntityRenderState createRenderState() {
            return new EntityRenderState();
        }

        @Override
        public void render(EntityRenderState state, PoseStack poseStack,
                MultiBufferSource buffer, int packedLight) {
            // Don't render anything - projectile uses particles only
        }
    }
}

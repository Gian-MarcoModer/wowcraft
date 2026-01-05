package com.gianmarco.wowcraft.client;

import com.gianmarco.wowcraft.WowCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Renders floating damage text in 3D world space as billboard text.
 * Uses WorldRenderEvents to render text at actual world positions.
 */
public class FloatingDamageTextRenderer {

    private static boolean registered = false;

    // Active damage texts to render
    private static final List<DamageText> activeDamageTexts = new ArrayList<>();

    /**
     * Register the FCT handler using WorldRenderEvents.
     */
    public static void register() {
        if (registered)
            return;
        registered = true;

        // Register for world render event - after entities for proper world-space
        // rendering
        WorldRenderEvents.AFTER_ENTITIES.register(FloatingDamageTextRenderer::onWorldRender);

        WowCraft.LOGGER.info("Registered FloatingDamageTextRenderer (3D world-space)");
    }

    /**
     * Add a new floating damage text at the specified world position.
     */
    public static void addDamageText(int entityId, float damage, boolean isCritical, boolean isSpell, double x,
            double y, double z) {
        WowCraft.LOGGER.info("[FCT] Adding 3D damage text: {} at ({}, {}, {}), crit={}, spell={}",
                damage, x, y, z, isCritical, isSpell);

        synchronized (activeDamageTexts) {
            activeDamageTexts.add(new DamageText(x, y, z, damage, isCritical, isSpell));
        }
    }

    /**
     * Clear all active damage texts.
     */
    public static void clear() {
        synchronized (activeDamageTexts) {
            activeDamageTexts.clear();
        }
    }

    /**
     * Called when the world is rendered.
     */
    private static void onWorldRender(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        synchronized (activeDamageTexts) {
            if (activeDamageTexts.isEmpty())
                return;

            Camera camera = context.camera();
            PoseStack poseStack = context.matrixStack();

            // Null check - matrixStack can be null in END event
            if (poseStack == null) {
                WowCraft.LOGGER.warn("[FCT] matrixStack is null, creating new one");
                poseStack = new PoseStack();
            }

            Vec3 cameraPos = camera.getPosition();

            // Get buffer source for rendering
            MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
            Font font = mc.font;

            Iterator<DamageText> iterator = activeDamageTexts.iterator();
            while (iterator.hasNext()) {
                DamageText dt = iterator.next();

                // Update lifetime (per frame at ~60 FPS)
                dt.age++;
                dt.y += 0.01; // Float upward in world space (slower rise)

                if (dt.age > dt.maxAge) {
                    iterator.remove();
                    continue;
                }

                // Calculate distance to camera
                double dx = dt.x - cameraPos.x;
                double dy = dt.y - cameraPos.y;
                double dz = dt.z - cameraPos.z;
                double distance = Math.sqrt(dx * dx + dy * dy + dz * dz);

                // Don't render if too far
                if (distance > 64)
                    continue;

                // Calculate alpha fade
                float lifeProgress = (float) dt.age / (float) dt.maxAge;
                int alpha = 255;
                if (lifeProgress > 0.7f) {
                    alpha = (int) (255 * (1.0f - (lifeProgress - 0.7f) * 3.33f));
                }
                alpha = Math.max(10, Math.min(255, alpha));

                // Build color with alpha
                int color = (dt.color & 0x00FFFFFF) | (alpha << 24);

                // Push pose for this text
                poseStack.pushPose();

                // Translate to world position (relative to camera)
                poseStack.translate(dx, dy, dz);

                // Billboard rotation - face the camera
                poseStack.mulPose(camera.rotation());

                // Flip so text is right-side up and readable
                poseStack.mulPose(new Quaternionf().rotationY((float) Math.PI));

                // Scale based on distance - compensate so text looks same size at any distance
                // Bigger base size, more conservative distance scaling
                float baseScale = 0.03f; // Increased from 0.015f for better visibility
                float distanceCompensation = (float) Math.max(1.0, distance / 6.0); // More conservative scaling (was 4.0)
                float scale = baseScale * distanceCompensation;

                // Crits are bigger
                if (dt.isCrit) {
                    scale *= 2.5f; // Bigger crits
                }

                poseStack.scale(-scale, -scale, scale);

                // Center the text
                int textWidth = font.width(dt.text);
                float textX = -textWidth / 2.0f;
                float textY = 0;

                // Render text
                Matrix4f matrix = poseStack.last().pose();

                // Draw with background for visibility
                font.drawInBatch(
                        dt.text,
                        textX,
                        textY,
                        color,
                        true, // shadow
                        matrix,
                        bufferSource,
                        Font.DisplayMode.NORMAL,
                        0, // background color (0 = no background)
                        15728880 // full brightness
                );

                poseStack.popPose();
            }

            // Flush the buffer
            bufferSource.endBatch();
        }
    }

    /**
     * Inner class to track damage text data.
     */
    private static class DamageText {
        double x, y, z;
        final String text;
        final int color;
        final boolean isCrit;
        final boolean isSpell;
        int age = 0;
        final int maxAge;

        DamageText(double x, double y, double z, float damage, boolean isCrit, boolean isSpell) {
            // Add random offset to spread out multiple hits
            double offsetX = (Math.random() - 0.5) * 0.8; // ±0.4 blocks
            double offsetZ = (Math.random() - 0.5) * 0.8;

            this.x = x + offsetX;
            this.y = y;
            this.z = z + offsetZ;
            this.isCrit = isCrit;
            this.isSpell = isSpell;

            // Longer duration for crits
            this.maxAge = isCrit ? 210 : 150; // 3.5s for crit, 2.5s for normal

            // Format text
            if (isCrit) {
                this.text = String.format("%.0f!", damage); // Crit mark, usually larger font or style
            } else {
                this.text = String.format("%.0f", damage);
            }

            // Set color based on type
            if (isSpell) {
                this.color = 0xFFFFFF00; // Yellow for spells
            } else {
                this.color = 0xFFFFFFFF; // White for melee/physical
            }
        }
    }
}

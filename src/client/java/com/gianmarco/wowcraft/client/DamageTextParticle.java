package com.gianmarco.wowcraft.client;

import com.gianmarco.wowcraft.WowCraft;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.renderer.MultiBufferSource;
import org.joml.Matrix4f;

/**
 * Custom particle for floating damage text.
 * Uses NO_RENDER type and renders via tick() method instead.
 */
public class DamageTextParticle extends Particle {

    private final String text;
    private final int textColor;

    public DamageTextParticle(ClientLevel level, double x, double y, double z,
            float damage, boolean isCritical) {
        super(level, x, y, z, 0, 0, 0);

        // Format the damage text
        this.text = isCritical
                ? String.format("%.0f!", damage)
                : String.format("%.0f", damage);

        // Color: yellow for crit, white otherwise
        this.textColor = isCritical ? 0xFFFFFF00 : 0xFFFFFFFF;

        // Particle settings
        this.friction = 0.99f;
        this.gravity = 0.75f;
        this.lifetime = 32;

        // Initial velocity
        this.xd = (Math.random() - 0.5) * 0.05;
        this.yd = 0.1;
        this.zd = (Math.random() - 0.5) * 0.05;
    }

    @Override
    public void tick() {
        super.tick();

        // Render during tick since CUSTOM render() isn't being called
        renderText();
    }

    private void renderText() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        Camera camera = mc.gameRenderer.getMainCamera();
        var cameraPos = camera.getPosition();

        // Calculate camera-relative position
        float particleX = (float) (this.x - cameraPos.x);
        float particleY = (float) (this.y - cameraPos.y);
        float particleZ = (float) (this.z - cameraPos.z);

        // Build transformation matrix
        Matrix4f matrix = new Matrix4f();
        matrix.translation(particleX, particleY, particleZ);
        matrix.rotate(camera.rotation());
        matrix.rotate((float) Math.PI, 0.0f, 1.0f, 0.0f);
        matrix.scale(-0.025f, -0.025f, -0.025f);

        // Calculate alpha fade
        float lifeProgress = (float) this.age / (float) this.lifetime;
        int alpha = 255;
        if (lifeProgress > 0.75f) {
            alpha = (int) (255 * (1.0f - (lifeProgress - 0.75f) * 4.0f));
        }
        int color = (textColor & 0x00FFFFFF) | (alpha << 24);

        // Get buffer and font
        Font font = mc.font;
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        float textX = font.width(text) / -2.0f;

        // Draw text
        font.drawInBatch(
                text, textX, 0, color, false, matrix,
                bufferSource, Font.DisplayMode.NORMAL, 0, 15728880);

        // Flush buffer
        bufferSource.endBatch();
    }

    @Override
    public void render(VertexConsumer vertexConsumer, Camera camera, float tickDelta) {
        // Empty - we render in tick() instead because CUSTOM type doesn't call this
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.NO_RENDER;
    }
}

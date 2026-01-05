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
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

/**
 * Renders custom 3D nameplates above entities in world space.
 * Shows entity name, level, and health bar above their head.
 */
public class EntityNameplateRenderer {

    private static boolean registered = false;

    // Nameplate settings
    private static final float NAMEPLATE_RANGE = 32.0f; // Max distance to render nameplates
    private static final float NAMEPLATE_HEIGHT_OFFSET = 0.5f; // Height above entity
    private static final float NAMEPLATE_SCALE = 0.02f; // Base scale for nameplate (reduced from 0.025f)

    // Colors (ARGB format) - WoW style
    private static final int COLOR_HEALTH_HOSTILE = 0xFFFF0000; // Bright red for hostile
    private static final int COLOR_HEALTH_NEUTRAL = 0xFFFFFF00; // Yellow for neutral
    private static final int COLOR_HEALTH_FRIENDLY = 0xFF00FF00; // Bright green for friendly
    private static final int COLOR_NAME_HOSTILE = 0xFFFF4040;
    private static final int COLOR_NAME_NEUTRAL = 0xFFFFFF00;
    private static final int COLOR_NAME_FRIENDLY = 0xFF40FF40;
    private static final int COLOR_LEVEL = 0xFFFFFFFF; // White level text
    private static final int COLOR_HEALTH_BG = 0xFF000000; // Black health bar background
    private static final int COLOR_HEALTH_BORDER = 0xFF000000; // Black border

    /**
     * Register the nameplate renderer
     */
    public static void register() {
        if (registered)
            return;
        registered = true;

        WorldRenderEvents.AFTER_ENTITIES.register(EntityNameplateRenderer::onWorldRender);
        WowCraft.LOGGER.info("Registered EntityNameplateRenderer (3D world-space)");
    }

    /**
     * Called when the world is rendered
     */
    private static void onWorldRender(WorldRenderContext context) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null)
            return;

        // Don't render during screens or F1
        if (mc.screen != null || mc.options.hideGui)
            return;

        Camera camera = context.camera();
        PoseStack poseStack = context.matrixStack();

        if (poseStack == null) {
            poseStack = new PoseStack();
        }

        Vec3 cameraPos = camera.getPosition();
        Player player = mc.player;

        // Get buffer source for rendering
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        Font font = mc.font;

        // Find entities in range
        AABB searchBox = new AABB(cameraPos, cameraPos).inflate(NAMEPLATE_RANGE);
        for (LivingEntity entity : mc.level.getEntitiesOfClass(LivingEntity.class, searchBox)) {
            // Don't render nameplate for the player themselves
            if (entity == player)
                continue;

            // Check if entity is alive
            if (!entity.isAlive())
                continue;

            // Check distance
            double distance = player.distanceTo(entity);
            if (distance > NAMEPLATE_RANGE)
                continue;

            // Check line of sight
            if (!player.hasLineOfSight(entity))
                continue;

            // Render the nameplate
            renderNameplate(entity, player, cameraPos, camera, poseStack, bufferSource, font, distance);
        }

        // Flush the buffer
        bufferSource.endBatch();
    }

    /**
     * Render a nameplate for a single entity
     */
    private static void renderNameplate(LivingEntity entity, Player player, Vec3 cameraPos, Camera camera,
                                        PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                        Font font, double distance) {
        // Calculate position above entity
        double entityX = entity.getX();
        double entityY = entity.getY() + entity.getBbHeight() + NAMEPLATE_HEIGHT_OFFSET;
        double entityZ = entity.getZ();

        // Relative to camera
        double dx = entityX - cameraPos.x;
        double dy = entityY - cameraPos.y;
        double dz = entityZ - cameraPos.z;

        // Determine entity type
        boolean hostile = isHostile(entity);
        boolean friendly = isFriendly(entity);
        int healthColor = hostile ? COLOR_HEALTH_HOSTILE :
                         friendly ? COLOR_HEALTH_FRIENDLY :
                         COLOR_HEALTH_NEUTRAL;
        int nameColor = hostile ? COLOR_NAME_HOSTILE :
                       friendly ? COLOR_NAME_FRIENDLY :
                       COLOR_NAME_NEUTRAL;

        // Get entity info
        String name = entity.getDisplayName().getString();

        float health = entity.getHealth();
        float maxHealth = entity.getMaxHealth();
        float healthPercent = health / maxHealth;

        // Removed debug logging

        // Push pose for this nameplate
        poseStack.pushPose();

        // Translate to world position (relative to camera)
        poseStack.translate(dx, dy, dz);

        // Billboard rotation - face the camera
        poseStack.mulPose(camera.rotation());

        // Flip so text is right-side up and readable
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.PI));

        // Scale based on distance - less aggressive scaling for far distances
        float distanceCompensation = (float) Math.max(1.0, distance / 12.0); // Changed from /8.0 to /12.0
        float scale = NAMEPLATE_SCALE * distanceCompensation;
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();

        // WoW-style nameplate layout
        // The name already includes level like "[Lv.5] Zombie"

        String nameText = name.length() > 24 ? name.substring(0, 22) + ".." : name;
        int textWidth = font.width(nameText);
        int barWidth = Math.max(textWidth - 5, 60); // Reduced width (was 80, now 60)
        int barHeight = 5; // Slightly thinner health bar

        // Center everything
        float y = -15;

        // Draw entity name (centered above health bar)
        float textX = -textWidth / 2.0f;
        font.drawInBatch(
            nameText,
            textX,
            y,
            nameColor,
            true, // shadow
            matrix,
            bufferSource,
            Font.DisplayMode.NORMAL,
            0,
            15728880 // full brightness
        );

        // Health bar position (below the name)
        float barX = -barWidth / 2.0f; // Center the bar
        float barY = y + 11;

        // Render health bar using quads
        renderHealthBar(poseStack, bufferSource, barX, barY, barWidth, barHeight, healthPercent, healthColor);

        poseStack.popPose();
    }

    /**
     * Render a health bar using vertex buffers with gradient styling
     */
    private static void renderHealthBar(PoseStack poseStack, MultiBufferSource.BufferSource bufferSource,
                                        float x, float y, int width, int height,
                                        float healthPercent, int healthColor) {
        Matrix4f matrix = poseStack.last().pose();

        // Use guiOverlay() which disables depth testing - prevents flickering
        VertexConsumer buffer = bufferSource.getBuffer(RenderType.guiOverlay());

        // Background - subtle dark with slight transparency
        int bgColor = 0xE0101010; // Semi-transparent dark gray
        drawQuad(buffer, matrix, x, y, width, height, bgColor);

        // Health bar with gradient (brighter at top, darker at bottom like player frame)
        int filledWidth = (int)(width * healthPercent);
        if (filledWidth > 0) {
            // Create gradient: lighter top, darker bottom
            int topColor = adjustBrightness(healthColor, 1.1f); // Slightly brighter
            int bottomColor = adjustBrightness(healthColor, 0.7f); // Darker
            drawGradientQuad(buffer, matrix, x, y, filledWidth, height, topColor, bottomColor);
        }

        // Border - subtle dark outline
        int borderColor = 0xC0000000; // Semi-transparent black
        drawQuadOutline(buffer, matrix, x, y, width, height, borderColor);
    }

    /**
     * Draw a filled quad
     */
    private static void drawQuad(VertexConsumer buffer, Matrix4f matrix,
                                 float x, float y, float width, float height, int color) {
        float r = ((color >> 16) & 0xFF) / 255.0f;
        float g = ((color >> 8) & 0xFF) / 255.0f;
        float b = (color & 0xFF) / 255.0f;
        float a = ((color >> 24) & 0xFF) / 255.0f;

        // Bottom-left
        buffer.addVertex(matrix, x, y + height, 0).setColor(r, g, b, a);
        // Bottom-right
        buffer.addVertex(matrix, x + width, y + height, 0).setColor(r, g, b, a);
        // Top-right
        buffer.addVertex(matrix, x + width, y, 0).setColor(r, g, b, a);
        // Top-left
        buffer.addVertex(matrix, x, y, 0).setColor(r, g, b, a);
    }

    /**
     * Draw a gradient quad (top color to bottom color)
     */
    private static void drawGradientQuad(VertexConsumer buffer, Matrix4f matrix,
                                         float x, float y, float width, float height,
                                         int topColor, int bottomColor) {
        // Top color
        float tr = ((topColor >> 16) & 0xFF) / 255.0f;
        float tg = ((topColor >> 8) & 0xFF) / 255.0f;
        float tb = (topColor & 0xFF) / 255.0f;
        float ta = ((topColor >> 24) & 0xFF) / 255.0f;

        // Bottom color
        float br = ((bottomColor >> 16) & 0xFF) / 255.0f;
        float bg = ((bottomColor >> 8) & 0xFF) / 255.0f;
        float bb = (bottomColor & 0xFF) / 255.0f;
        float ba = ((bottomColor >> 24) & 0xFF) / 255.0f;

        // Bottom-left (dark)
        buffer.addVertex(matrix, x, y + height, 0).setColor(br, bg, bb, ba);
        // Bottom-right (dark)
        buffer.addVertex(matrix, x + width, y + height, 0).setColor(br, bg, bb, ba);
        // Top-right (bright)
        buffer.addVertex(matrix, x + width, y, 0).setColor(tr, tg, tb, ta);
        // Top-left (bright)
        buffer.addVertex(matrix, x, y, 0).setColor(tr, tg, tb, ta);
    }

    /**
     * Draw a quad outline (border)
     */
    private static void drawQuadOutline(VertexConsumer buffer, Matrix4f matrix,
                                        float x, float y, float width, float height, int color) {
        float thickness = 0.5f;

        // Top border
        drawQuad(buffer, matrix, x, y, width, thickness, color);
        // Bottom border
        drawQuad(buffer, matrix, x, y + height - thickness, width, thickness, color);
        // Left border
        drawQuad(buffer, matrix, x, y, thickness, height, color);
        // Right border
        drawQuad(buffer, matrix, x + width - thickness, y, thickness, height, color);
    }

    /**
     * Adjust brightness of a color
     */
    private static int adjustBrightness(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * factor));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * factor));
        int b = Math.min(255, (int)((color & 0xFF) * factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    /**
     * Check if entity is hostile
     */
    private static boolean isHostile(LivingEntity entity) {
        String className = entity.getClass().getSimpleName().toLowerCase();
        return className.contains("zombie") ||
                className.contains("skeleton") ||
                className.contains("creeper") ||
                className.contains("spider") ||
                className.contains("enderman") ||
                className.contains("witch") ||
                className.contains("slime") ||
                className.contains("phantom") ||
                className.contains("drowned") ||
                className.contains("husk") ||
                className.contains("stray") ||
                className.contains("blaze") ||
                className.contains("ghast") ||
                className.contains("piglin") ||
                className.contains("hoglin") ||
                className.contains("warden") ||
                className.contains("wither");
    }

    /**
     * Check if entity is friendly
     */
    private static boolean isFriendly(LivingEntity entity) {
        String className = entity.getClass().getSimpleName().toLowerCase();
        return className.contains("villager") ||
                className.contains("irongolem") ||
                className.contains("cat") ||
                className.contains("wolf") ||
                className.contains("horse") ||
                className.contains("donkey") ||
                className.contains("mule") ||
                className.contains("llama") ||
                className.contains("parrot") ||
                className.contains("chicken") ||
                className.contains("cow") ||
                className.contains("sheep") ||
                className.contains("pig") ||
                className.contains("rabbit") ||
                className.contains("fox") ||
                className.contains("bee") ||
                className.contains("turtle") ||
                className.contains("panda") ||
                className.contains("strider") ||
                className.contains("axolotl") ||
                className.contains("frog") ||
                className.contains("allay");
    }

    /**
     * Parse level from entity custom name
     */
    private static int parseLevelFromEntity(LivingEntity entity) {
        if (entity.hasCustomName()) {
            String name = entity.getCustomName().getString();
            // Expected format: "[Lv.5] EntityName"
            if (name.startsWith("[Lv.") && name.contains("]")) {
                try {
                    String levelStr = name.substring(4, name.indexOf("]"));
                    return Integer.parseInt(levelStr);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 1; // Default
    }
}

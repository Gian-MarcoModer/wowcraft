package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;

/**
 * Target frame HUD - Shows the health of the entity the player is looking at.
 * Positioned in the top-right of the screen.
 */
public class TargetFrameHud implements HudRenderCallback {

    // Detection range for targeting
    private static final double TARGET_RANGE = 32.0;

    // HUD positioning
    private static final int MARGIN = 10;
    private static final int FRAME_WIDTH = 160;
    private static final int FRAME_HEIGHT = 45;

    // Colors (ARGB format)
    private static final int COLOR_FRAME_BG = 0xCC1a1a1a;
    private static final int COLOR_HEALTH_HOSTILE = 0xFFcc2222; // Red for hostile
    private static final int COLOR_HEALTH_NEUTRAL = 0xFFFFCC00; // Yellow for neutral
    private static final int COLOR_HEALTH_FRIENDLY = 0xFF22aa22; // Green for friendly
    private static final int COLOR_BORDER = 0xFF444444;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_BG_DARK = 0xFF1a1a1a;

    /**
     * Register the target frame renderer
     */
    @SuppressWarnings("deprecation")
    public static void register() {
        HudRenderCallback.EVENT.register(new TargetFrameHud());
        WowCraft.LOGGER.info("Registered WowCraft Target Frame HUD");
    }

    @Override
    public void onHudRender(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null)
            return;

        // Don't render during screens or F1
        if (client.screen != null || client.options.hideGui)
            return;

        // Get what the player is looking at with extended range
        LivingEntity target = getTargetedEntity(client);
        if (target == null)
            return;

        renderTargetFrame(graphics, client, target);
    }

    private LivingEntity getTargetedEntity(Minecraft client) {
        Player player = client.player;
        if (player == null)
            return null;

        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos = eyePos.add(lookDir.scale(TARGET_RANGE));

        // Create a search box along the look direction
        AABB searchBox = player.getBoundingBox()
                .expandTowards(lookDir.scale(TARGET_RANGE))
                .inflate(1.0);

        // Find all living entities in range
        Optional<LivingEntity> closest = player.level().getEntitiesOfClass(
                LivingEntity.class,
                searchBox,
                e -> e != player && e.isAlive()).stream()
                .filter(e -> {
                    // Check if entity is in player's line of sight cone
                    Vec3 toEntity = e.getBoundingBox().getCenter().subtract(eyePos).normalize();
                    double dot = toEntity.dot(lookDir);
                    return dot > 0.98; // Within ~11 degree cone (cos(11°) ≈ 0.98)
                })
                .filter(e -> player.hasLineOfSight(e))
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(player),
                        b.distanceToSqr(player)));

        return closest.orElse(null);
    }

    private void renderTargetFrame(GuiGraphics graphics, Minecraft client, LivingEntity target) {
        Font font = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();

        // Position in top-right
        int x = screenWidth - FRAME_WIDTH - MARGIN;
        int y = MARGIN;

        // Frame background with slight transparency
        graphics.fill(x, y, x + FRAME_WIDTH, y + FRAME_HEIGHT, COLOR_FRAME_BG);

        // Beveled frame border for 3D effect
        drawBeveledBorder(graphics, x, y, FRAME_WIDTH, FRAME_HEIGHT);

        // Determine health bar color based on entity type
        boolean hostile = isHostile(target);
        boolean friendly = isFriendly(target);
        int healthColor = hostile ? COLOR_HEALTH_HOSTILE :
                         friendly ? COLOR_HEALTH_FRIENDLY :
                         COLOR_HEALTH_NEUTRAL;

        // Target name with color coding
        String name = target.getDisplayName().getString();
        if (name.length() > 18) {
            name = name.substring(0, 16) + "..";
        }
        int nameColor = hostile ? 0xFFFF5555 : friendly ? 0xFF55FF55 : 0xFFFFFF55;
        graphics.drawString(font, name, x + 5, y + 5, nameColor, true);

        // Target level
        int level = parseLevelFromEntity(target);
        String levelText = "Lv." + level;
        int levelWidth = font.width(levelText);
        graphics.drawString(font, levelText, x + FRAME_WIDTH - levelWidth - 5, y + 5, 0xFFFFFF00, true);

        // Health bar with enhanced visuals
        int barX = x + 5;
        int barY = y + 18;
        int barWidth = FRAME_WIDTH - 10;
        int barHeight = 14;

        float healthPercent = target.getHealth() / target.getMaxHealth();
        drawEnhancedBar(graphics, barX, barY, barWidth, barHeight, healthPercent, healthColor);

        // Health text
        String healthText = String.format("%.0f / %.0f", target.getHealth(), target.getMaxHealth());
        int textWidth = font.width(healthText);
        graphics.drawString(font, healthText, barX + barWidth / 2 - textWidth / 2, barY + 3, COLOR_TEXT, true);

        // Distance indicator
        double distance = client.player.distanceTo(target);
        String distText = String.format("%.1fm", distance);
        graphics.drawString(font, distText, x + FRAME_WIDTH - font.width(distText) - 5, y + FRAME_HEIGHT - 12,
                0xFF888888, false);

        // Mob type indicator with icon
        String typeIcon = hostile ? "⚔" : friendly ? "✓" : "●";
        String typeText = hostile ? "§c" + typeIcon + " Hostile" :
                         friendly ? "§a" + typeIcon + " Friendly" :
                         "§e" + typeIcon + " Neutral";
        graphics.drawString(font, typeText, x + 5, y + FRAME_HEIGHT - 12, COLOR_TEXT, false);
    }

    private boolean isHostile(LivingEntity entity) {
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

    private boolean isFriendly(LivingEntity entity) {
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

    private int parseLevelFromEntity(LivingEntity entity) {
        if (entity.hasCustomName()) {
            String name = entity.getCustomName().getString();
            // Expected format: "[Lv.5] Zombie"
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

    /**
     * Draw enhanced bar with gradient fill and beveled border (matching PlayerFrameWidget style).
     */
    private void drawEnhancedBar(GuiGraphics graphics, int x, int y, int width, int height,
                                  float fillPercent, int fillColor) {
        // Background with darker color
        graphics.fill(x, y, x + width, y + height, COLOR_BG_DARK);

        // Calculate fill width
        int inset = 2;
        int fillWidth = (int) ((width - inset * 2) * Math.max(0, Math.min(1, fillPercent)));

        if (fillWidth > 0) {
            // Draw gradient fill (top lighter, bottom darker for depth)
            int topColor = adjustAlpha(fillColor, 1.0f);
            int bottomColor = adjustAlpha(fillColor, 0.75f);

            int halfHeight = (height - inset * 2) / 2;

            // Top half (lighter)
            graphics.fill(x + inset, y + inset,
                         x + inset + fillWidth, y + inset + halfHeight, topColor);

            // Bottom half (darker)
            graphics.fill(x + inset, y + inset + halfHeight,
                         x + inset + fillWidth, y + height - inset, bottomColor);

            // Add highlight on top edge of fill for glossy effect
            int highlightColor = adjustAlpha(0xFFFFFFFF, 0.3f);
            graphics.fill(x + inset, y + inset,
                         x + inset + fillWidth, y + inset + 1, highlightColor);
        }

        // Draw beveled border for 3D effect
        drawBeveledBorder(graphics, x, y, width, height);
    }

    /**
     * Draw a beveled border for 3D effect.
     */
    private void drawBeveledBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        int highlight = 0xFF666666;
        int shadow = 0xFF222222;

        // Top and left (highlight)
        graphics.fill(x, y, x + width, y + 1, highlight);
        graphics.fill(x, y, x + 1, y + height, highlight);

        // Bottom and right (shadow)
        graphics.fill(x, y + height - 1, x + width, y + height, shadow);
        graphics.fill(x + width - 1, y, x + width, y + height, shadow);
    }

    /**
     * Adjust alpha channel of a color.
     */
    private int adjustAlpha(int color, float alphaMultiplier) {
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int) (alpha * alphaMultiplier);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }
}

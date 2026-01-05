package com.gianmarco.wowcraft.ui.widget;

import com.gianmarco.wowcraft.hud.ClientPlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.ResourceType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Player frame widget - shows HP, resource, and XP bars with WoW-style artwork.
 * Positioned in the top-left corner.
 */
public class PlayerFrameWidget extends WowWidget {

    private static final int MARGIN = 10;
    private static final int BAR_WIDTH = 150;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_SPACING = 4;
    private static final int COMBO_POINT_SIZE = 14;
    private static final int COMBO_POINT_SPACING = 6;
    private static final int COLOR_COMBO_ACTIVE = 0xFFFFF569; // Rogue yellow
    private static final int COLOR_COMBO_INACTIVE = 0xFF3a3a3a; // Dark gray

    // Optional: Texture paths for decorative frame elements
    // These can be added later for even more visual flair
    private static final ResourceLocation FRAME_TEXTURE = ResourceLocation.parse("wowcraft:textures/gui/hud/player_frame.png");
    private static final ResourceLocation COMBO_POINT_TEXTURE = ResourceLocation.parse("wowcraft:textures/gui/hud/combo_point.png");

    public PlayerFrameWidget() {
        super(MARGIN, MARGIN, BAR_WIDTH, 60);
    }

    @Override
    public void render(GuiGraphics graphics, Minecraft client, float partialTick) {
        Player player = client.player;
        if (player == null)
            return;

        PlayerData data = ClientPlayerData.getData();
        if (!data.hasSelectedClass())
            return;

        Font font = client.font;
        int currentY = y;

        // Draw class name and level
        String classText = String.format("Lv.%d %s", data.level(), data.playerClass().getDisplayName());
        drawText(graphics, font, classText, x + 2, currentY, data.playerClass().getColorWithAlpha());
        currentY += 12;

        // Calculate health percentage
        float healthPercent = player.getHealth() / player.getMaxHealth();
        int healthColor = healthPercent < 0.25f ? COLOR_HEALTH_LOW : COLOR_HEALTH;

        // Draw health bar with improved visuals
        drawEnhancedBar(graphics, x, currentY, BAR_WIDTH, BAR_HEIGHT, healthPercent, healthColor);
        String healthText = String.format("%.0f / %.0f", player.getHealth(), player.getMaxHealth());
        drawCenteredText(graphics, font, healthText, x + BAR_WIDTH / 2, currentY + 4, COLOR_TEXT);
        currentY += BAR_HEIGHT + BAR_SPACING;

        // Calculate resource percentage
        float resourcePercent = (float) data.currentResource() / Math.max(1, data.maxResource());
        int resourceColor = data.playerClass().getResourceType() == ResourceType.RAGE
                ? COLOR_RAGE
                : COLOR_MANA;

        // Draw resource bar (Mana/Rage/Energy) with improved visuals
        drawEnhancedBar(graphics, x, currentY, BAR_WIDTH, BAR_HEIGHT, resourcePercent, resourceColor);
        String resourceText = String.format("%d / %d", data.currentResource(), data.maxResource());
        drawCenteredText(graphics, font, resourceText, x + BAR_WIDTH / 2, currentY + 4, COLOR_TEXT);
        currentY += BAR_HEIGHT + BAR_SPACING;

        // Draw XP bar (smaller) with improved visuals
        float xpPercent = data.getLevelProgress() / 100f;
        drawEnhancedBar(graphics, x, currentY, BAR_WIDTH, 8, xpPercent, COLOR_XP);
        currentY += 8 + BAR_SPACING;

        // Draw combo points (Rogue only)
        if (data.playerClass() == PlayerClass.ROGUE) {
            drawComboPoints(graphics, x, currentY, data.comboPoints());
        }
    }

    /**
     * Draw enhanced bar with gradient fill and beveled border.
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
     * Draw combo point indicators for Rogues with enhanced visuals.
     */
    private void drawComboPoints(GuiGraphics graphics, int x, int y, int activePoints) {
        int totalWidth = (COMBO_POINT_SIZE * 5) + (COMBO_POINT_SPACING * 4);
        int startX = x + (BAR_WIDTH - totalWidth) / 2; // Center the combo points

        for (int i = 0; i < 5; i++) {
            int dotX = startX + (i * (COMBO_POINT_SIZE + COMBO_POINT_SPACING));
            boolean isActive = i < activePoints;

            // Draw combo point with improved visuals
            drawComboPoint(graphics, dotX, y, isActive);
        }
    }

    /**
     * Draw a single combo point with gradient and border.
     */
    private void drawComboPoint(GuiGraphics graphics, int x, int y, boolean isActive) {
        int size = COMBO_POINT_SIZE;

        if (isActive) {
            // Active combo point - glowing yellow with gradient
            int topColor = COLOR_COMBO_ACTIVE;
            int bottomColor = adjustAlpha(COLOR_COMBO_ACTIVE, 0.8f);

            // Draw gradient fill
            int halfSize = size / 2;
            graphics.fill(x, y, x + size, y + halfSize, topColor);
            graphics.fill(x, y + halfSize, x + size, y + size, bottomColor);

            // Add glow effect (outer border)
            int glowColor = adjustAlpha(COLOR_COMBO_ACTIVE, 0.5f);
            graphics.fill(x - 1, y - 1, x + size + 1, y, glowColor); // Top
            graphics.fill(x - 1, y + size, x + size + 1, y + size + 1, glowColor); // Bottom
            graphics.fill(x - 1, y, x, y + size, glowColor); // Left
            graphics.fill(x + size, y, x + size + 1, y + size, glowColor); // Right
        } else {
            // Inactive combo point - dark gray
            graphics.fill(x, y, x + size, y + size, COLOR_COMBO_INACTIVE);
        }

        // Draw beveled border for depth
        int highlight = isActive ? 0xFF888888 : 0xFF444444;
        int shadow = isActive ? 0xFF333333 : 0xFF111111;

        // Top and left (highlight)
        graphics.fill(x, y, x + size, y + 1, highlight);
        graphics.fill(x, y, x + 1, y + size, highlight);

        // Bottom and right (shadow)
        graphics.fill(x, y + size - 1, x + size, y + size, shadow);
        graphics.fill(x + size - 1, y, x + size, y + size, shadow);
    }
}

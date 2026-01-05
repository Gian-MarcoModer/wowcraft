package com.gianmarco.wowcraft.ui.widget;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

/**
 * Base class for all WowCraft HUD widgets.
 * Provides common rendering utilities and a standard interface.
 */
public abstract class WowWidget {

    // Common color constants (ARGB format)
    protected static final int COLOR_BG_DARK = 0xFF1a1a1a;
    protected static final int COLOR_BORDER = 0xFF444444;
    protected static final int COLOR_TEXT = 0xFFFFFFFF;
    protected static final int COLOR_HEALTH = 0xFF22aa22;
    protected static final int COLOR_HEALTH_LOW = 0xFFcc2222;
    protected static final int COLOR_MANA = 0xFF3366ff;
    protected static final int COLOR_RAGE = 0xFFcc3333;
    protected static final int COLOR_XP = 0xFF9933ff;
    protected static final int COLOR_GOLD = 0xFFFFD700;

    protected int x;
    protected int y;
    protected int width;
    protected int height;
    protected boolean visible = true;

    protected WowWidget(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * Render this widget. Override in subclasses.
     */
    public abstract void render(GuiGraphics graphics, Minecraft client, float partialTick);

    /**
     * Called each tick to update widget state (optional).
     */
    public void tick() {
    }

    // ========== Visibility ==========

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    // ========== Position/Size ==========

    public void setPosition(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    // ========== Common Rendering Utilities ==========

    /**
     * Draw a resource bar with fill and border.
     */
    protected void drawBar(GuiGraphics graphics, int x, int y, int width, int height,
            float fillPercent, int fillColor) {
        // Background
        graphics.fill(x, y, x + width, y + height, COLOR_BG_DARK);

        // Fill
        int fillWidth = (int) (width * Math.max(0, Math.min(1, fillPercent)));
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, fillColor);
        }

        // Border
        drawBorder(graphics, x, y, width, height, COLOR_BORDER);
    }

    /**
     * Draw a rectangular border.
     */
    protected void drawBorder(GuiGraphics graphics, int x, int y, int width, int height, int color) {
        graphics.fill(x, y, x + width, y + 1, color); // Top
        graphics.fill(x, y + height - 1, x + width, y + height, color); // Bottom
        graphics.fill(x, y, x + 1, y + height, color); // Left
        graphics.fill(x + width - 1, y, x + width, y + height, color); // Right
    }

    /**
     * Draw centered text.
     */
    protected void drawCenteredText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        int textWidth = font.width(text);
        graphics.drawString(font, text, x - textWidth / 2, y, color, true);
    }

    /**
     * Draw text with shadow.
     */
    protected void drawText(GuiGraphics graphics, Font font, String text, int x, int y, int color) {
        graphics.drawString(font, text, x, y, color, true);
    }

    /**
     * Draw a texture at the specified position.
     */
    protected void drawTexture(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height) {
        graphics.blit(RenderType::guiTextured, texture, x, y, 0, 0, width, height, width, height);
    }

    /**
     * Draw a texture with UV coordinates (for sprite sheets).
     */
    protected void drawTextureUV(GuiGraphics graphics, ResourceLocation texture,
                                int x, int y, int width, int height,
                                float u, float v, int regionWidth, int regionHeight,
                                int textureWidth, int textureHeight) {
        graphics.blit(RenderType::guiTextured, texture, x, y, u, v, width, height, textureWidth, textureHeight);
    }

    /**
     * Draw a textured bar with background, fill, and border.
     * Uses textures for more WoW-like appearance.
     */
    protected void drawTexturedBar(GuiGraphics graphics, ResourceLocation barBg,
                                   int x, int y, int width, int height,
                                   float fillPercent, int fillColor) {
        // Draw textured background if provided
        if (barBg != null) {
            drawTexture(graphics, barBg, x, y, width, height);
        } else {
            graphics.fill(x, y, x + width, y + height, COLOR_BG_DARK);
        }

        // Draw fill with slight inset for better appearance
        int inset = 2;
        int fillWidth = (int) ((width - inset * 2) * Math.max(0, Math.min(1, fillPercent)));
        if (fillWidth > 0) {
            // Add gradient effect by drawing two rectangles with slight alpha variation
            int topColor = adjustAlpha(fillColor, 0.9f);
            int bottomColor = adjustAlpha(fillColor, 0.7f);

            int halfHeight = (height - inset * 2) / 2;
            graphics.fill(x + inset, y + inset, x + inset + fillWidth, y + inset + halfHeight, topColor);
            graphics.fill(x + inset, y + inset + halfHeight, x + inset + fillWidth, y + height - inset, bottomColor);
        }

        // Draw border with highlight/shadow for depth
        drawBeveledBorder(graphics, x, y, width, height);
    }

    /**
     * Draw a beveled border for 3D effect.
     */
    protected void drawBeveledBorder(GuiGraphics graphics, int x, int y, int width, int height) {
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
    protected int adjustAlpha(int color, float alphaMultiplier) {
        int alpha = (color >> 24) & 0xFF;
        int newAlpha = (int) (alpha * alphaMultiplier);
        return (color & 0x00FFFFFF) | (newAlpha << 24);
    }
}

package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Renders a red vignette overlay and text when the player is in combat.
 * Combat state is now synced from the server via CombatStatePacket.
 */
public class CombatIndicatorHud implements HudRenderCallback {

    // Combat timeout in ticks (6 seconds = 120 ticks)
    private static final long COMBAT_TIMEOUT_TICKS = 120;

    // Text display duration in milliseconds
    private static final long TEXT_DISPLAY_MS = 2000;

    // Track combat state (synced from server)
    private static boolean serverInCombat = false; // Directly from server
    private static boolean wasInCombat = false;
    private static long combatStateChangeTime = 0;
    private static String combatText = "";
    private static float currentOpacity = 0f;

    /**
     * Register the HUD renderer
     */
    @SuppressWarnings("deprecation")
    public static void register() {
        HudRenderCallback.EVENT.register(new CombatIndicatorHud());
        WowCraft.LOGGER.info("Registered combat indicator HUD");
    }

    /**
     * Update combat state from server sync packet
     */
    public static void updateCombatState(boolean inCombat, long serverLastCombatTick) {
        serverInCombat = inCombat;
        WowCraft.LOGGER.info("[CombatIndicator] Updated combat state - InCombat: {}", inCombat);
    }

    @Override
    public void onHudRender(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            return;
        }

        // Don't render during screens or when HUD is hidden
        if (client.screen != null || client.options.hideGui) {
            return;
        }

        // Use combat state synced from server directly
        boolean inCombat = serverInCombat;

        // Detect combat state changes
        long now = System.currentTimeMillis();
        if (inCombat && !wasInCombat) {
            // Just entered combat
            combatText = "§c⚔ Entering Combat ⚔";
            combatStateChangeTime = now;
            wasInCombat = true;
            WowCraft.LOGGER.info("[CombatIndicator] ENTERING COMBAT");
        } else if (!inCombat && wasInCombat) {
            // Just left combat
            combatText = "§a✓ Leaving Combat ✓";
            combatStateChangeTime = now;
            wasInCombat = false;
            WowCraft.LOGGER.info("[CombatIndicator] LEAVING COMBAT");
        }

        // Update opacity for vignette (smooth fade)
        float targetOpacity = inCombat ? 1.0f : 0.0f;
        float fadeSpeed = 0.05f;

        if (currentOpacity < targetOpacity) {
            currentOpacity = Math.min(targetOpacity, currentOpacity + fadeSpeed);
        } else if (currentOpacity > targetOpacity) {
            currentOpacity = Math.max(targetOpacity, currentOpacity - fadeSpeed);
        }

        // Render vignette if in combat
        if (currentOpacity > 0.01f) {
            renderVignette(graphics, client, currentOpacity);
        }

        // Render combat text notification
        if (now - combatStateChangeTime < TEXT_DISPLAY_MS && !combatText.isEmpty()) {
            renderCombatText(graphics, client);
        }
    }

    /**
     * Render combat text notification in center of screen
     */
    private void renderCombatText(GuiGraphics graphics, Minecraft client) {
        Font font = client.font;
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Calculate text position (center of screen, slightly above middle)
        int textWidth = font.width(combatText);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight / 3;

        // Draw text with shadow
        graphics.drawString(font, combatText, x, y, 0xFFFFFF, true);
    }

    /**
     * Render the combat vignette overlay
     */
    private void renderVignette(GuiGraphics graphics, Minecraft client, float opacity) {
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // Calculate color with current opacity (ARGB format)
        // Use higher opacity for visibility: max 40% (0x66)
        int alpha = (int) (opacity * 0x66);
        if (alpha < 10)
            return;

        int color = (alpha << 24) | 0xAA0000; // Dark red

        // Draw thin red border around entire screen (more visible)
        int borderSize = 4;

        // Top border
        graphics.fill(0, 0, screenWidth, borderSize, color);
        // Bottom border
        graphics.fill(0, screenHeight - borderSize, screenWidth, screenHeight, color);
        // Left border
        graphics.fill(0, 0, borderSize, screenHeight, color);
        // Right border
        graphics.fill(screenWidth - borderSize, 0, screenWidth, screenHeight, color);

        // Also draw subtle corner overlays
        int cornerSize = 30;
        int cornerAlpha = alpha / 2;
        int cornerColor = (cornerAlpha << 24) | 0xAA0000;

        // Top-left corner
        graphics.fill(0, 0, cornerSize, cornerSize, cornerColor);
        // Top-right corner
        graphics.fill(screenWidth - cornerSize, 0, screenWidth, cornerSize, cornerColor);
        // Bottom-left corner
        graphics.fill(0, screenHeight - cornerSize, cornerSize, screenHeight, cornerColor);
        // Bottom-right corner
        graphics.fill(screenWidth - cornerSize, screenHeight - cornerSize, screenWidth, screenHeight, cornerColor);
    }
}

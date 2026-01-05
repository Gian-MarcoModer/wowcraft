package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Renders WoW-style zone entry announcements.
 * Shows large zone name with subtitle, fades in and out.
 */
public class ZoneAnnouncementHud {

    /** Current zone being displayed */
    @Nullable
    private static String currentZoneName = null;
    @Nullable
    private static String currentSubtitle = null;
    private static int currentLevelMin = 0;
    private static int currentLevelMax = 0;

    /** Display timing */
    private static long displayStartTime = 0;
    private static final int FADE_IN_MS = 500;
    private static final int DISPLAY_MS = 3000;
    private static final int FADE_OUT_MS = 1000;
    private static final int TOTAL_DURATION = FADE_IN_MS + DISPLAY_MS + FADE_OUT_MS;

    /**
     * Registers the zone announcement HUD renderer.
     */
    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            render(graphics);
        });
        WowCraft.LOGGER.info("ZoneAnnouncementHud registered");
    }

    /**
     * Shows a zone entry announcement.
     */
    public static void showZoneEntry(String zoneName, @Nullable String subtitle, int levelMin, int levelMax) {
        currentZoneName = zoneName;
        currentSubtitle = subtitle;
        currentLevelMin = levelMin;
        currentLevelMax = levelMax;
        displayStartTime = System.currentTimeMillis();

        WowCraft.LOGGER.info("Showing zone announcement: {} ({})", zoneName, subtitle);
    }

    private static void render(GuiGraphics graphics) {
        if (currentZoneName == null)
            return;

        long elapsed = System.currentTimeMillis() - displayStartTime;
        if (elapsed > TOTAL_DURATION) {
            currentZoneName = null;
            currentSubtitle = null;
            return;
        }

        // Calculate alpha based on timing
        float alpha;
        if (elapsed < FADE_IN_MS) {
            alpha = (float) elapsed / FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + DISPLAY_MS) {
            alpha = 1.0f;
        } else {
            alpha = 1.0f - ((float) (elapsed - FADE_IN_MS - DISPLAY_MS) / FADE_OUT_MS);
        }

        Minecraft mc = Minecraft.getInstance();
        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();
        int screenHeight = mc.getWindow().getGuiScaledHeight();

        // Position: Upper center of screen
        int centerX = screenWidth / 2;
        int y = screenHeight / 5;

        int alphaInt = (int) (alpha * 255);
        if (alphaInt < 4)
            return; // Too transparent to render

        // Render zone name (large, golden text)
        String zoneName = currentZoneName;
        int zoneNameWidth = font.width(zoneName);
        int zoneColor = (alphaInt << 24) | 0xFFD700; // Gold with alpha

        // Draw zone name with shadow
        graphics.drawString(font, Component.literal(zoneName),
                centerX - zoneNameWidth / 2, y, zoneColor, true);

        // Render subtitle if present
        if (currentSubtitle != null && !currentSubtitle.isEmpty()) {
            String subtitle = currentSubtitle;
            int subtitleWidth = font.width(subtitle);
            int subtitleColor = (alphaInt << 24) | 0xCCCCCC; // Light gray with alpha

            graphics.drawString(font, Component.literal(subtitle),
                    centerX - subtitleWidth / 2, y + 14, subtitleColor, true);
        }

        // Render level range
        if (currentLevelMin > 0 && currentLevelMax > 0) {
            String levelText = "Lv " + currentLevelMin + "-" + currentLevelMax;
            int levelWidth = font.width(levelText);

            // Color based on player level comparison (green/yellow/red)
            int levelColor = getLevelColor(alphaInt);

            graphics.drawString(font, Component.literal(levelText),
                    centerX - levelWidth / 2, y + (currentSubtitle != null ? 28 : 14),
                    levelColor, true);
        }
    }

    /**
     * Gets color for level range based on player level.
     */
    private static int getLevelColor(int alpha) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return (alpha << 24) | 0xFFFFFF;
        }

        // Get player level from client data
        int playerLevel = ClientPlayerData.getLevel();

        if (playerLevel < currentLevelMin - 5) {
            // Much higher level zone - red
            return (alpha << 24) | 0xFF4444;
        } else if (playerLevel < currentLevelMin) {
            // Slightly higher - orange
            return (alpha << 24) | 0xFF8844;
        } else if (playerLevel <= currentLevelMax) {
            // In range - yellow/green
            return (alpha << 24) | 0x88FF88;
        } else if (playerLevel <= currentLevelMax + 5) {
            // Slightly below - gray/green
            return (alpha << 24) | 0x88CC88;
        } else {
            // Much lower - gray (trivial)
            return (alpha << 24) | 0x888888;
        }
    }
}

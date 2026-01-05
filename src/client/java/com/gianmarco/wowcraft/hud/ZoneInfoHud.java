package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Persistent zone info display that sits below the minimap area (top-right
 * corner).
 * Shows current zone name and level range at all times.
 */
public class ZoneInfoHud {

    /** Current zone data */
    @Nullable
    private static String currentZoneName = null;
    @Nullable
    private static String currentSubtitle = null;
    private static int currentLevelMin = 0;
    private static int currentLevelMax = 0;

    /** Offset from top-right corner (adjustable to fit below Xaero's minimap) */
    private static final int RIGHT_MARGIN = 5;
    private static final int TOP_MARGIN = 140; // Below typical minimap position

    /**
     * Registers the zone info HUD renderer.
     */
    public static void register() {
        HudRenderCallback.EVENT.register((graphics, tickCounter) -> {
            render(graphics);
        });
        WowCraft.LOGGER.info("ZoneInfoHud registered");
    }

    /**
     * Updates the current zone info (called when player enters a new zone).
     */
    public static void updateZone(String zoneName, @Nullable String subtitle, int levelMin, int levelMax) {
        currentZoneName = zoneName;
        currentSubtitle = subtitle;
        currentLevelMin = levelMin;
        currentLevelMax = levelMax;
    }

    /**
     * Clears zone info (on disconnect).
     */
    public static void clear() {
        currentZoneName = null;
        currentSubtitle = null;
        currentLevelMin = 0;
        currentLevelMax = 0;
    }

    private static void render(GuiGraphics graphics) {
        if (currentZoneName == null)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui)
            return;

        Font font = mc.font;
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // Position: Below minimap in top-right
        int rightEdge = screenWidth - RIGHT_MARGIN;
        int y = TOP_MARGIN;

        // Zone name (golden/yellow)
        String zoneName = currentZoneName;
        int zoneNameWidth = font.width(zoneName);
        int zoneColor = 0xFFD700; // Gold

        // Draw zone name (right-aligned)
        graphics.drawString(font, Component.literal(zoneName),
                rightEdge - zoneNameWidth, y, zoneColor, true);

        // Level range below zone name
        if (currentLevelMin > 0 && currentLevelMax > 0) {
            y += 12;
            String levelText = "Level " + currentLevelMin + "-" + currentLevelMax;
            int levelWidth = font.width(levelText);

            // Color based on player level
            int levelColor = getLevelColor();

            graphics.drawString(font, Component.literal(levelText),
                    rightEdge - levelWidth, y, levelColor, true);
        }
    }

    /**
     * Gets color for level range based on player level.
     */
    private static int getLevelColor() {
        int playerLevel = ClientPlayerData.getLevel();

        if (playerLevel < currentLevelMin - 5) {
            return 0xFF4444; // Red - much higher level zone
        } else if (playerLevel < currentLevelMin) {
            return 0xFF8844; // Orange - slightly higher
        } else if (playerLevel <= currentLevelMax) {
            return 0x88FF88; // Green - in range
        } else if (playerLevel <= currentLevelMax + 5) {
            return 0x88CC88; // Light green - slightly below
        } else {
            return 0x888888; // Gray - trivial
        }
    }
}

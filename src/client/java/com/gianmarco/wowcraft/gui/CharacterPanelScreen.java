package com.gianmarco.wowcraft.gui;

import com.gianmarco.wowcraft.hud.ClientPlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsCalculator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Character Panel screen - shows player stats like WoW's "C" panel.
 */
public class CharacterPanelScreen extends Screen {

    private static final int PANEL_WIDTH = 220;
    private static final int PANEL_HEIGHT = 420;

    // Colors
    private static final int COLOR_BG = 0xCC1a1a1a;
    private static final int COLOR_BORDER = 0xFF444444;
    private static final int COLOR_HEADER = 0xFF2a2a2a;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_STAT_LABEL = 0xFFAAAAAA;
    private static final int COLOR_STAT_VALUE = 0xFF00FF00;

    // Stat colors
    private static final int COLOR_STRENGTH = 0xFFFF5555;
    private static final int COLOR_AGILITY = 0xFF55FF55;
    private static final int COLOR_STAMINA = 0xFFFFAA00;
    private static final int COLOR_INTELLECT = 0xFF5555FF;
    private static final int COLOR_SPIRIT = 0xFFFF55FF;

    public CharacterPanelScreen() {
        super(Component.literal("Character"));
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Dark overlay background
        renderBackground(graphics, mouseX, mouseY, delta);

        // Calculate panel position (centered)
        int x = (this.width - PANEL_WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        // Get player data and calculate full stats including equipment
        PlayerData data = ClientPlayerData.getData();
        net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
        CharacterStats stats = StatsCalculator.calculate(client.player, data.playerClass(), data.level());

        // Draw panel background
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_BG);

        // Draw border
        drawBorder(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        // Draw header
        graphics.fill(x, y, x + PANEL_WIDTH, y + 25, COLOR_HEADER);
        drawBorder(graphics, x, y, PANEL_WIDTH, 25);

        // Title
        String title = "Character: " + data.playerClass().getDisplayName();
        int titleWidth = this.font.width(title);
        graphics.drawString(this.font, title, x + (PANEL_WIDTH - titleWidth) / 2, y + 8, COLOR_TEXT);

        // Level and Class
        int lineY = y + 35;
        graphics.drawString(this.font, "§7Level §f" + data.level() + " " + data.playerClass().getDisplayName(), x + 10,
                lineY, COLOR_TEXT);
        lineY += 20;

        // Separator
        graphics.fill(x + 10, lineY, x + PANEL_WIDTH - 10, lineY + 1, COLOR_BORDER);
        lineY += 10;

        // Primary Stats header
        graphics.drawString(this.font, "§nPrimary Stats", x + 10, lineY, COLOR_TEXT);
        lineY += 15;

        // Stats
        drawStat(graphics, x + 10, lineY, "Strength", stats.getStrength(), COLOR_STRENGTH);
        lineY += 12;
        drawStat(graphics, x + 10, lineY, "Agility", stats.getAgility(), COLOR_AGILITY);
        lineY += 12;
        drawStat(graphics, x + 10, lineY, "Stamina", stats.getStamina(), COLOR_STAMINA);
        lineY += 12;
        drawStat(graphics, x + 10, lineY, "Intellect", stats.getIntellect(), COLOR_INTELLECT);
        lineY += 12;
        drawStat(graphics, x + 10, lineY, "Spirit", stats.getSpirit(), COLOR_SPIRIT);
        lineY += 20;

        // Separator
        graphics.fill(x + 10, lineY, x + PANEL_WIDTH - 10, lineY + 1, COLOR_BORDER);
        lineY += 10;

        // Derived Stats header
        graphics.drawString(this.font, "§nDerived Stats", x + 10, lineY, COLOR_TEXT);
        lineY += 15;

        // Derived stats
        drawStatLine(graphics, x + 10, lineY, "Attack Power", String.valueOf(stats.getAttackPower()));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Bonus Damage", String.format("+%.1f", stats.getBonusMeleeDamage()));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Crit Chance", String.format("%.1f%%", stats.getCritChance() * 100));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Max Health", String.valueOf(stats.getMaxHealth()));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Max Mana", String.valueOf(stats.getMaxMana()));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Mana Regen", stats.getManaRegenPer5() + " /5s");
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Spell Power", String.format("%.0f", stats.getSpellPower()));
        lineY += 20;

        // Separator
        graphics.fill(x + 10, lineY, x + PANEL_WIDTH - 10, lineY + 1, COLOR_BORDER);
        lineY += 10;

        // Money section
        graphics.drawString(this.font, "§nMoney", x + 10, lineY, COLOR_TEXT);
        lineY += 15;

        // Get money from client cache
        var money = ClientPlayerData.getMoneyPouch();

        // Draw money with WoW-style colors (gold, silver, copper)
        // Gold = yellow/gold, Silver = gray/white, Copper = orange/brown
        String moneyStr = String.format("§6%dg §7%ds §#c87533%dc",
                money.gold(), money.silver(), money.copper());
        graphics.drawString(this.font, moneyStr, x + 10, lineY, COLOR_TEXT);
        lineY += 20;

        // Separator
        graphics.fill(x + 10, lineY, x + PANEL_WIDTH - 10, lineY + 1, COLOR_BORDER);
        lineY += 10;

        // Crafting Resources section (XP repurposed)
        graphics.drawString(this.font, "§nCrafting Resources", x + 10, lineY, COLOR_TEXT);
        lineY += 15;

        // Get vanilla XP from player
        int currentXp = client.player.totalExperience;
        int xpLevel = client.player.experienceLevel;
        float xpProgress = client.player.experienceProgress;
        int xpForNextLevel = client.player.getXpNeededForNextLevel();

        // Draw crafting XP with a progress bar
        String xpLabel = "Crafting XP:";
        graphics.drawString(this.font, xpLabel, x + 10, lineY, COLOR_STAT_LABEL);

        // Progress bar background
        int barX = x + 85;
        int barWidth = 110;
        int barHeight = 8;
        graphics.fill(barX, lineY + 1, barX + barWidth, lineY + 1 + barHeight, 0xFF333333);

        // Progress bar fill (green gradient effect)
        int fillWidth = (int) (barWidth * xpProgress);
        if (fillWidth > 0) {
            graphics.fill(barX, lineY + 1, barX + fillWidth, lineY + 1 + barHeight, 0xFF00AA00);
        }

        // Progress bar border
        graphics.fill(barX, lineY + 1, barX + barWidth, lineY + 2, 0xFF555555);
        graphics.fill(barX, lineY + barHeight, barX + barWidth, lineY + 1 + barHeight, 0xFF555555);
        graphics.fill(barX, lineY + 1, barX + 1, lineY + 1 + barHeight, 0xFF555555);
        graphics.fill(barX + barWidth - 1, lineY + 1, barX + barWidth, lineY + 1 + barHeight, 0xFF555555);

        lineY += 15;

        // XP details
        drawStatLine(graphics, x + 10, lineY, "Level", String.valueOf(xpLevel));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "Total XP", String.valueOf(currentXp));
        lineY += 12;
        drawStatLine(graphics, x + 10, lineY, "To Next", String.valueOf(xpForNextLevel));

        // Footer hint
        graphics.drawString(this.font, "§7Press C or ESC to close", x + 10, y + PANEL_HEIGHT - 15, COLOR_STAT_LABEL);

        super.render(graphics, mouseX, mouseY, delta);
    }

    private void drawStat(GuiGraphics graphics, int x, int y, String name, int value, int color) {
        graphics.drawString(this.font, name + ":", x, y, COLOR_STAT_LABEL);
        String valueStr = String.valueOf(value);
        graphics.drawString(this.font, valueStr, x + 80, y, color);
    }

    private void drawStatLine(GuiGraphics graphics, int x, int y, String name, String value) {
        graphics.drawString(this.font, name + ":", x, y, COLOR_STAT_LABEL);
        graphics.drawString(this.font, value, x + 100, y, COLOR_STAT_VALUE);
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int w, int h) {
        graphics.fill(x, y, x + w, y + 1, COLOR_BORDER);
        graphics.fill(x, y + h - 1, x + w, y + h, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + h, COLOR_BORDER);
        graphics.fill(x + w - 1, y, x + w, y + h, COLOR_BORDER);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        // Close on C key press (same key that opens it)
        if (keyCode == org.lwjgl.glfw.GLFW.GLFW_KEY_C) {
            this.onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

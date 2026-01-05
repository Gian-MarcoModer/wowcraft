package com.gianmarco.wowcraft.hud;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.WowCraftClient;
import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.ability.AbilityManager;
import com.gianmarco.wowcraft.ability.AbilityRegistry;
import com.gianmarco.wowcraft.client.FloatingDamageTextRenderer;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.world.entity.player.Player;

/**
 * WoW-style HUD overlay showing:
 * - Health bar (green)
 * - Resource bar (mana=blue, rage=red)
 * - Class name and level
 * - 8-slot ability bar with cooldowns
 */
public class WowCraftHud implements HudRenderCallback {

    // HUD positioning
    private static final int MARGIN = 10;
    private static final int BAR_WIDTH = 150;
    private static final int BAR_HEIGHT = 16;
    private static final int BAR_SPACING = 4;

    // Colors (ARGB format)
    private static final int COLOR_HEALTH_BG = 0xFF1a1a1a;
    private static final int COLOR_HEALTH_FILL = 0xFF22aa22;
    private static final int COLOR_HEALTH_LOW = 0xFFcc2222;
    private static final int COLOR_MANA_FILL = 0xFF3366ff;
    private static final int COLOR_RAGE_FILL = 0xFFcc3333;
    private static final int COLOR_BORDER = 0xFF444444;
    private static final int COLOR_TEXT = 0xFFFFFFFF;

    /**
     * Register the HUD renderer
     */
    @SuppressWarnings("deprecation") // HudRenderCallback is deprecated but still works
    public static void register() {
        HudRenderCallback.EVENT.register(new WowCraftHud());
        WowCraft.LOGGER.info("Registered WowCraft HUD renderer");
    }

    @Override
    public void onHudRender(GuiGraphics graphics, DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null)
            return;

        // Use client-side cached data
        PlayerData data = ClientPlayerData.getData();
        if (!data.hasSelectedClass())
            return;

        // Don't render during screens (inventory, etc)
        if (client.screen != null)
            return;

        // Don't render during F1 (hide HUD)
        if (client.options.hideGui)
            return;

        renderPlayerFrame(graphics, client, data);
        renderAbilityBar(graphics, client, data);
    }

    private void renderPlayerFrame(GuiGraphics graphics, Minecraft client, PlayerData data) {
        Font font = client.font;
        Player player = client.player;

        int x = MARGIN;
        int y = MARGIN;

        // Calculate health percentage
        float healthPercent = player.getHealth() / player.getMaxHealth();
        int healthColor = healthPercent < 0.25f ? COLOR_HEALTH_LOW : COLOR_HEALTH_FILL;

        // Calculate resource percentage
        float resourcePercent = (float) data.currentResource() / Math.max(1, data.maxResource());
        int resourceColor = data.playerClass().getResourceType() == com.gianmarco.wowcraft.playerclass.ResourceType.RAGE
                ? COLOR_RAGE_FILL
                : COLOR_MANA_FILL;

        // Draw class name and level
        String classText = String.format("Lv.%d %s", data.level(), data.playerClass().getDisplayName());
        graphics.drawString(font, classText, x + 2, y, data.playerClass().getColorWithAlpha(), true);
        y += 12;

        // Draw health bar
        drawResourceBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, healthPercent, healthColor, "HP");
        String healthText = String.format("%.0f / %.0f", player.getHealth(), player.getMaxHealth());
        graphics.drawString(font, healthText, x + BAR_WIDTH / 2 - font.width(healthText) / 2, y + 4, COLOR_TEXT, true);
        y += BAR_HEIGHT + BAR_SPACING;

        // Draw resource bar (Mana/Rage)
        String resourceName = data.playerClass().getResourceType().getDisplayName();
        drawResourceBar(graphics, x, y, BAR_WIDTH, BAR_HEIGHT, resourcePercent, resourceColor, resourceName);
        String resourceText = String.format("%d / %d", data.currentResource(), data.maxResource());
        graphics.drawString(font, resourceText, x + BAR_WIDTH / 2 - font.width(resourceText) / 2, y + 4, COLOR_TEXT,
                true);
        y += BAR_HEIGHT + BAR_SPACING;

        // Draw XP bar (smaller)
        float xpPercent = data.getLevelProgress() / 100f;
        drawResourceBar(graphics, x, y, BAR_WIDTH, 8, xpPercent, 0xFF9933ff, "XP");
    }

    private void drawResourceBar(GuiGraphics graphics, int x, int y, int width, int height,
            float fillPercent, int fillColor, String label) {
        // Background
        graphics.fill(x, y, x + width, y + height, COLOR_HEALTH_BG);

        // Fill
        int fillWidth = (int) (width * Math.max(0, Math.min(1, fillPercent)));
        if (fillWidth > 0) {
            graphics.fill(x, y, x + fillWidth, y + height, fillColor);
        }

        // Border
        graphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
    }

    private void renderAbilityBar(GuiGraphics graphics, Minecraft client, PlayerData data) {
        Font font = client.font;
        Player player = client.player;

        // Get screen dimensions
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        // 8-slot ability bar settings
        int slotSize = 24;
        int slotSpacing = 2;
        int totalSlots = WowCraftClient.ACTION_BAR_SIZE;
        int totalWidth = totalSlots * slotSize + (totalSlots - 1) * slotSpacing;
        int startX = screenWidth / 2 - totalWidth / 2;
        int y = screenHeight - 48; // Just above the hotbar

        // Initialize action bar if needed
        if (!ClientActionBar.isInitialized()) {
            ClientActionBar.initFromDefaults(data.playerClass());
        }

        // Get abilities from client action bar (customizable)
        Ability[] abilities = ClientActionBar.getAllSlots();
        if (abilities == null)
            return;

        for (int i = 0; i < totalSlots; i++) {
            int slotX = startX + i * (slotSize + slotSpacing);
            Ability ability = abilities[i];

            // Draw slot background
            graphics.fill(slotX, y, slotX + slotSize, y + slotSize, 0xAA000000);

            // Draw keybind label
            String keybind = WowCraftClient.KEY_LABELS[i];
            graphics.drawString(font, keybind, slotX + 2, y + 2, 0xFFFFFF00, false);

            if (ability == null) {
                // Empty slot - draw border only
                drawSlotBorder(graphics, slotX, y, slotSize, 0xFF333333);
                continue;
            }

            // Check cooldown and resource
            float cooldown = AbilityManager.getRemainingCooldown(player, ability);
            boolean onCooldown = cooldown > 0;
            boolean hasResource = data.currentResource() >= ability.getResourceCost();

            // Draw ability icon
            net.minecraft.resources.ResourceLocation icon = ability.getIconTexture();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color
            graphics.pose().pushPose();
            graphics.pose().translate(slotX + 2, y + 2, 0);
            float scale = (slotSize - 4) / 1024f; // Scale 1024x1024 texture to fit slot
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.blit(RenderType::guiTextured, icon, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
            graphics.pose().popPose();

            // Draw cooldown/resource overlay
            if (onCooldown) {
                graphics.fill(slotX + 2, y + 2, slotX + slotSize - 2, y + slotSize - 2, 0xCC000000); // Darker overlay
                                                                                                     // for CD
            } else if (!hasResource) {
                graphics.fill(slotX + 2, y + 2, slotX + slotSize - 2, y + slotSize - 2, 0x66553333); // Reddish for no
                                                                                                     // resource
            }

            // Draw cooldown overlay
            if (onCooldown) {
                int cooldownHeight = (int) ((cooldown / ability.getCooldownSeconds()) * (slotSize - 4));
                // Draw a wipe effect or just keep the text

                // Draw cooldown text
                String cdText = String.format("%.1f", cooldown);
                graphics.drawString(font, cdText,
                        slotX + slotSize / 2 - font.width(cdText) / 2,
                        y + slotSize / 2 - 4, 0xFFFFFFFF, true);
            }

            // Draw border
            int borderColor = onCooldown ? 0xFF666666 : (hasResource ? 0xFFFFD700 : 0xFF993333);
            drawSlotBorder(graphics, slotX, y, slotSize, borderColor);
        }
    }

    private void drawSlotBorder(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y + size - 1, x + size, y + size, color);
        graphics.fill(x, y, x + 1, y + size, color);
        graphics.fill(x + size - 1, y, x + size, y + size, color);
    }
}

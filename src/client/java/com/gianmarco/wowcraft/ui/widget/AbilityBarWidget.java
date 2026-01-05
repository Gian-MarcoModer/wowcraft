package com.gianmarco.wowcraft.ui.widget;

import com.gianmarco.wowcraft.WowCraftClient;
import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.ability.AbilityManager;
import com.gianmarco.wowcraft.hud.ClientActionBar;
import com.gianmarco.wowcraft.hud.ClientPlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;

/**
 * Ability bar widget - shows 8 ability slots with cooldowns.
 * Positioned at the bottom center of the screen.
 */
public class AbilityBarWidget extends WowWidget {

    private static final int SLOT_SIZE = 24;
    private static final int SLOT_SPACING = 2;

    public AbilityBarWidget() {
        // Initial position will be recalculated based on screen size
        super(0, 0, 0, SLOT_SIZE);
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

        // Get screen dimensions and calculate position
        int screenWidth = client.getWindow().getGuiScaledWidth();
        int screenHeight = client.getWindow().getGuiScaledHeight();

        int totalSlots = WowCraftClient.ACTION_BAR_SIZE;
        int totalWidth = totalSlots * SLOT_SIZE + (totalSlots - 1) * SLOT_SPACING;
        int startX = screenWidth / 2 - totalWidth / 2;
        int slotY = screenHeight - 48; // Just above the hotbar

        // Initialize action bar if needed
        if (!ClientActionBar.isInitialized()) {
            ClientActionBar.initFromDefaults(data.playerClass());
        }

        // Get abilities from client action bar (customizable)
        Ability[] abilities = ClientActionBar.getAllSlots();
        if (abilities == null)
            return;

        for (int i = 0; i < totalSlots; i++) {
            int slotX = startX + i * (SLOT_SIZE + SLOT_SPACING);
            Ability ability = abilities[i];

            // Draw slot background
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, 0xAA000000);

            // Draw keybind label
            String keybind = WowCraftClient.KEY_LABELS[i];
            graphics.drawString(font, keybind, slotX + 2, slotY + 2, 0xFFFFFF00, false);

            if (ability == null) {
                // Empty slot - draw border only
                drawBorder(graphics, slotX, slotY, SLOT_SIZE, SLOT_SIZE, 0xFF333333);
                continue;
            }

            // Check cooldown and resource
            float cooldown = AbilityManager.getRemainingCooldown(player, ability);
            boolean onCooldown = cooldown > 0;
            boolean hasResource = data.currentResource() >= ability.getResourceCost();

            // Draw ability icon
            ResourceLocation icon = ability.getIconTexture();
            RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
            graphics.pose().pushPose();
            graphics.pose().translate(slotX + 2, slotY + 2, 0);
            float scale = (SLOT_SIZE - 4) / 1024f;
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.blit(RenderType::guiTextured, icon, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
            graphics.pose().popPose();

            // Draw cooldown/resource overlay
            if (onCooldown) {
                graphics.fill(slotX + 2, slotY + 2, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, 0xCC000000);
            } else if (!hasResource) {
                graphics.fill(slotX + 2, slotY + 2, slotX + SLOT_SIZE - 2, slotY + SLOT_SIZE - 2, 0x66553333);
            }

            // Draw cooldown text
            if (onCooldown) {
                String cdText = String.format("%.1f", cooldown);
                int textX = slotX + SLOT_SIZE / 2 - font.width(cdText) / 2;
                graphics.drawString(font, cdText, textX, slotY + SLOT_SIZE / 2 - 4, 0xFFFFFFFF, true);
            }

            // Draw border
            int borderColor = onCooldown ? 0xFF666666 : (hasResource ? COLOR_GOLD : 0xFF993333);
            drawBorder(graphics, slotX, slotY, SLOT_SIZE, SLOT_SIZE, borderColor);
        }
    }
}

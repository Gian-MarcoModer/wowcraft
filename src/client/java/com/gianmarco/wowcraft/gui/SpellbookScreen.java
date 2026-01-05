package com.gianmarco.wowcraft.gui;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.ability.AbilityRegistry;
import com.gianmarco.wowcraft.hud.ClientActionBar;
import com.gianmarco.wowcraft.hud.ClientPlayerData;
import com.gianmarco.wowcraft.network.ActionBarSyncPacket;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Spellbook - Shows all learned abilities and allows assigning to action bar.
 * 
 * Layout:
 * - TOP: All learned spells (click to select)
 * - BOTTOM: Action bar slots (click to assign selected spell)
 */
public class SpellbookScreen extends Screen {

    private static final int PANEL_WIDTH = 380;
    private static final int PANEL_HEIGHT = 320;
    private static final int SLOT_SIZE = 36;

    // Colors - brighter for better visibility
    private static final int COLOR_BG = 0xFF2a2a2a;
    private static final int COLOR_BORDER = 0xFF666666;
    private static final int COLOR_HEADER = 0xFF3a3a3a;
    private static final int COLOR_TEXT = 0xFFFFFFFF;
    private static final int COLOR_SLOT_BG = 0xFF444444;
    private static final int COLOR_SLOT_HOVER = 0xFF5a5a5a;
    private static final int COLOR_SELECTED = 0xFFFFD700;

    // State
    private Ability selectedSpell = null;
    private int hoveredActionSlot = -1;
    private int hoveredSpellIndex = -1;

    public SpellbookScreen() {
        super(Component.literal("Spellbook"));
    }

    @Override
    protected void init() {
        super.init();

        int x = (this.width - PANEL_WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        // Close button
        this.addRenderableWidget(Button.builder(Component.literal("×"), b -> this.onClose())
                .bounds(x + PANEL_WIDTH - 22, y + 4, 18, 18)
                .build());

        // Initialize action bar if not already
        PlayerData data = ClientPlayerData.getData();
        if (data.hasSelectedClass() && !ClientActionBar.isInitialized()) {
            ClientActionBar.initFromDefaults(data.playerClass());
        }
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // Override to disable Minecraft's native blur effect
        // Just draw a semi-transparent dark overlay
        graphics.fill(0, 0, this.width, this.height, 0x88000000);
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // renderBackground handles the overlay

        int x = (this.width - PANEL_WIDTH) / 2;
        int y = (this.height - PANEL_HEIGHT) / 2;

        PlayerData data = ClientPlayerData.getData();
        if (!data.hasSelectedClass()) {
            graphics.drawCenteredString(this.font, "Select a class first (press C)!", this.width / 2, this.height / 2,
                    0xFFFF5555);
            super.render(graphics, mouseX, mouseY, delta);
            return;
        }

        PlayerClass playerClass = data.playerClass();
        List<Ability> learnedAbilities = getAllLearnedAbilities(playerClass);

        // Main panel
        graphics.fill(x, y, x + PANEL_WIDTH, y + PANEL_HEIGHT, COLOR_BG);
        drawBorder(graphics, x, y, PANEL_WIDTH, PANEL_HEIGHT);

        // Header
        graphics.fill(x, y, x + PANEL_WIDTH, y + 28, COLOR_HEADER);
        drawBorder(graphics, x, y, PANEL_WIDTH, 28);
        graphics.drawCenteredString(this.font, "§l" + playerClass.getDisplayName() + " Spellbook",
                x + PANEL_WIDTH / 2, y + 10, playerClass.getColorWithAlpha());

        // === LEARNED SPELLS SECTION ===
        int spellsY = y + 40;
        graphics.drawString(this.font, "§nLearned Spells §7(Click to select)", x + 15, spellsY, COLOR_TEXT);
        spellsY += 18;

        hoveredSpellIndex = -1;
        int col = 0;
        int row = 0;
        for (int i = 0; i < learnedAbilities.size(); i++) {
            Ability ability = learnedAbilities.get(i);
            int slotX = x + 15 + col * (SLOT_SIZE + 8);
            int slotY = spellsY + row * (SLOT_SIZE + 8);

            boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                    mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            boolean isSelected = ability == selectedSpell;

            if (isHovered)
                hoveredSpellIndex = i;

            // Slot background
            int bgColor = isSelected ? COLOR_SELECTED : (isHovered ? COLOR_SLOT_HOVER : COLOR_SLOT_BG);
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);

            // Ability icon
            net.minecraft.resources.ResourceLocation icon = ability.getIconTexture();
            com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color
            graphics.pose().pushPose();
            graphics.pose().translate(slotX + 4, slotY + 4, 0);
            float scale = (SLOT_SIZE - 8) / 1024f;
            graphics.pose().scale(scale, scale, 1.0f);
            graphics.blit(RenderType::guiTextured, icon, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
            graphics.pose().popPose();

            // Border
            int borderColor = isSelected ? COLOR_SELECTED : COLOR_BORDER;
            drawSlotBorder(graphics, slotX, slotY, SLOT_SIZE, borderColor);

            col++;
            if (col >= 8) {
                col = 0;
                row++;
            }
        }

        // === ACTION BAR SECTION ===
        int actionY = y + 180;
        graphics.fill(x + 10, actionY - 5, x + PANEL_WIDTH - 10, actionY - 4, COLOR_BORDER);
        graphics.drawString(this.font, "§nAction Bar §7(Click to assign, Right-click to clear)", x + 15, actionY,
                COLOR_TEXT);
        actionY += 20;

        String[] keyLabels = { "1", "2", "3", "4", "5", "R", "F", "G" };
        hoveredActionSlot = -1;

        for (int i = 0; i < 8; i++) {
            int slotX = x + 15 + i * (SLOT_SIZE + 8);
            int slotY = actionY;

            boolean isHovered = mouseX >= slotX && mouseX < slotX + SLOT_SIZE &&
                    mouseY >= slotY && mouseY < slotY + SLOT_SIZE;
            if (isHovered)
                hoveredActionSlot = i;

            // Slot background
            int bgColor = isHovered ? COLOR_SLOT_HOVER : COLOR_SLOT_BG;
            graphics.fill(slotX, slotY, slotX + SLOT_SIZE, slotY + SLOT_SIZE, bgColor);

            // Ability in slot - READ FROM ClientActionBar
            Ability ability = ClientActionBar.getSlot(i);
            if (ability != null) {
                // Draw ability icon
                net.minecraft.resources.ResourceLocation icon = ability.getIconTexture();
                com.mojang.blaze3d.systems.RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F); // Reset color
                graphics.pose().pushPose();
                graphics.pose().translate(slotX + 4, slotY + 4, 0);
                float scale = (SLOT_SIZE - 8) / 1024f;
                graphics.pose().scale(scale, scale, 1.0f);
                graphics.blit(RenderType::guiTextured, icon, 0, 0, 0, 0, 1024, 1024, 1024, 1024);
                graphics.pose().popPose();
            }

            // Keybind label
            graphics.drawString(this.font, keyLabels[i], slotX + 3, slotY + 3, 0xFFFFFF00, false);

            // Border
            int borderColor = isHovered && selectedSpell != null ? COLOR_SELECTED : COLOR_BORDER;
            drawSlotBorder(graphics, slotX, slotY, SLOT_SIZE, borderColor);
        }

        // === INFO PANEL ===
        int infoY = actionY + SLOT_SIZE + 15;
        graphics.fill(x + 10, infoY - 5, x + PANEL_WIDTH - 10, infoY - 4, COLOR_BORDER);

        // Show hovered spell info
        Ability infoAbility = null;
        if (hoveredSpellIndex >= 0 && hoveredSpellIndex < learnedAbilities.size()) {
            infoAbility = learnedAbilities.get(hoveredSpellIndex);
        } else if (hoveredActionSlot >= 0) {
            infoAbility = ClientActionBar.getSlot(hoveredActionSlot);
        } else if (selectedSpell != null) {
            infoAbility = selectedSpell;
        }

        if (infoAbility != null) {
            graphics.drawString(this.font, "§l" + infoAbility.getDisplayName(), x + 15, infoY, COLOR_TEXT);
            String info = String.format("§7Cost: §f%d %s §7| Cooldown: §f%ds",
                    infoAbility.getResourceCost(),
                    playerClass.getResourceType().getDisplayName(),
                    infoAbility.getCooldownSeconds());
            graphics.drawString(this.font, info, x + 15, infoY + 12, COLOR_TEXT);
            graphics.drawString(this.font, "§7" + truncate(infoAbility.getDescription(), 55), x + 15, infoY + 24,
                    0xFFAAAAAA);
        } else {
            graphics.drawString(this.font, "§8Click a spell to select, then click an action bar slot to assign.",
                    x + 15, infoY + 5, 0xFF888888);
        }

        // Selected spell indicator
        if (selectedSpell != null) {
            graphics.drawString(this.font,
                    "§aSelected: §f" + selectedSpell.getDisplayName() + " §7- Click action bar slot to assign",
                    x + 15, y + PANEL_HEIGHT - 18, COLOR_TEXT);
        } else {
            graphics.drawString(this.font, "§7Press §fP §7or §fEsc §7to close", x + 15, y + PANEL_HEIGHT - 18,
                    0xFF888888);
        }

        super.render(graphics, mouseX, mouseY, delta);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0) { // Left click
            PlayerData data = ClientPlayerData.getData();
            if (!data.hasSelectedClass())
                return super.mouseClicked(mouseX, mouseY, button);

            List<Ability> learnedAbilities = getAllLearnedAbilities(data.playerClass());

            // Click on spell to select
            if (hoveredSpellIndex >= 0 && hoveredSpellIndex < learnedAbilities.size()) {
                selectedSpell = learnedAbilities.get(hoveredSpellIndex);
                return true;
            }

            // Click on action bar slot to assign
            if (hoveredActionSlot >= 0 && selectedSpell != null) {
                ClientActionBar.setSlot(hoveredActionSlot, selectedSpell.getId());
                // Sync to server
                ClientPlayNetworking.send(new ActionBarSyncPacket(hoveredActionSlot, selectedSpell.getId()));
                selectedSpell = null; // Deselect after assigning
                return true;
            }

        } else if (button == 1 && hoveredActionSlot >= 0) {
            // Right-click to clear slot
            ClientActionBar.clearSlot(hoveredActionSlot);
            // Sync to server
            ClientPlayNetworking.send(new ActionBarSyncPacket(hoveredActionSlot, null));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private List<Ability> getAllLearnedAbilities(PlayerClass playerClass) {
        // For now, return all abilities for the class
        // Later this will be filtered by what player has actually learned
        List<Ability> abilities = new ArrayList<>();
        for (Ability ability : AbilityRegistry.getDefaultActionBar(playerClass)) {
            if (ability != null)
                abilities.add(ability);
        }
        return abilities;
    }

    // Removed getAbilityColor as it uses solid colors

    private String truncate(String s, int maxLen) {
        return s.length() > maxLen ? s.substring(0, maxLen - 3) + "..." : s;
    }

    private void drawBorder(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.fill(x, y, x + width, y + 1, COLOR_BORDER);
        graphics.fill(x, y + height - 1, x + width, y + height, COLOR_BORDER);
        graphics.fill(x, y, x + 1, y + height, COLOR_BORDER);
        graphics.fill(x + width - 1, y, x + width, y + height, COLOR_BORDER);
    }

    private void drawSlotBorder(GuiGraphics graphics, int x, int y, int size, int color) {
        graphics.fill(x, y, x + size, y + 1, color);
        graphics.fill(x, y + size - 1, x + size, y + size, color);
        graphics.fill(x, y, x + 1, y + size, color);
        graphics.fill(x + size - 1, y, x + size, y + size, color);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

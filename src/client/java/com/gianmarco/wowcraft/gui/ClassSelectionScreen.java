package com.gianmarco.wowcraft.gui;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.network.ClassSelectionPacket;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * The class selection screen shown when a player first joins.
 * Displays available classes with their descriptions and lets the player
 * choose.
 */
public class ClassSelectionScreen extends Screen {

    private static final int BUTTON_WIDTH = 200;
    private static final int BUTTON_HEIGHT = 40;
    private static final int BUTTON_SPACING = 20;

    // Currently hovered class for showing description
    private PlayerClass hoveredClass = null;

    public ClassSelectionScreen() {
        super(Component.literal("Choose Your Class"));
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 2 - 80; // Adjusted for 3 classes

        // Get all selectable classes (exclude NONE)
        PlayerClass[] selectableClasses = new PlayerClass[] {
            PlayerClass.WARRIOR,
            PlayerClass.MAGE,
            PlayerClass.ROGUE
        };

        // Create button for each class
        int yOffset = 0;
        for (PlayerClass playerClass : selectableClasses) {
            String icon = getClassIcon(playerClass);
            Button classButton = Button.builder(
                    Component.literal(icon + " " + playerClass.getDisplayName().toUpperCase() + " " + icon)
                            .withColor(playerClass.getColorWithAlpha()),
                    button -> selectClass(playerClass))
                    .bounds(centerX - BUTTON_WIDTH / 2, startY + yOffset, BUTTON_WIDTH, BUTTON_HEIGHT)
                    .build();

            this.addRenderableWidget(classButton);
            yOffset += BUTTON_HEIGHT + BUTTON_SPACING;
        }
    }

    private String getClassIcon(PlayerClass playerClass) {
        return switch (playerClass) {
            case WARRIOR -> "⚔";
            case MAGE -> "✦";
            case ROGUE -> "✣";
            default -> "●";
        };
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Render dark background
        this.renderBackground(graphics, mouseX, mouseY, partialTick);

        int centerX = this.width / 2;
        int titleY = this.height / 2 - 120;

        // Draw title with golden WoW-style color
        graphics.drawCenteredString(this.font, "§6═══ CHOOSE YOUR CLASS ═══", centerX, titleY, 0xFFD100);
        graphics.drawCenteredString(this.font, "§7Your destiny awaits...", centerX, titleY + 15, 0xAAAAAA);

        // Check which button is hovered
        int startY = this.height / 2 - 80;
        int buttonLeft = centerX - BUTTON_WIDTH / 2;
        int buttonRight = centerX + BUTTON_WIDTH / 2;

        PlayerClass[] selectableClasses = new PlayerClass[] {
            PlayerClass.WARRIOR,
            PlayerClass.MAGE,
            PlayerClass.ROGUE
        };

        hoveredClass = null;
        if (mouseX >= buttonLeft && mouseX <= buttonRight) {
            int yOffset = 0;
            for (PlayerClass playerClass : selectableClasses) {
                int buttonTop = startY + yOffset;
                int buttonBottom = buttonTop + BUTTON_HEIGHT;

                if (mouseY >= buttonTop && mouseY <= buttonBottom) {
                    hoveredClass = playerClass;
                    break;
                }
                yOffset += BUTTON_HEIGHT + BUTTON_SPACING;
            }
        }

        // Render widgets (buttons)
        super.render(graphics, mouseX, mouseY, partialTick);

        // Draw class description if hovering
        if (hoveredClass != null) {
            int descY = this.height / 2 + 60;
            graphics.drawCenteredString(this.font, hoveredClass.getDescription(), centerX, descY, 0xFFFFFF);

            // Show resource type
            String resourceInfo = "Resource: §" + getResourceColorCode(hoveredClass) +
                    hoveredClass.getResourceType().getDisplayName();
            graphics.drawCenteredString(this.font, resourceInfo, centerX, descY + 15, 0xAAAAAA);
        }
    }

    private String getResourceColorCode(PlayerClass playerClass) {
        return switch (playerClass) {
            case WARRIOR -> "c"; // Red for rage
            case MAGE -> "9"; // Blue for mana
            case ROGUE -> "e"; // Yellow for energy
            default -> "7"; // Gray
        };
    }

    private void selectClass(PlayerClass playerClass) {
        WowCraft.LOGGER.info("Player selected class: {}", playerClass.getDisplayName());

        // Send selection to server
        ClientPlayNetworking.send(new ClassSelectionPacket(playerClass));

        // Close the screen
        this.onClose();
    }

    @Override
    public boolean shouldCloseOnEsc() {
        // Don't allow closing without selecting a class
        return false;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}

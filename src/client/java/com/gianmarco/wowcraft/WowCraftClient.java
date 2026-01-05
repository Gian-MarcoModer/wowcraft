package com.gianmarco.wowcraft;

import com.gianmarco.wowcraft.gui.CharacterPanelScreen;
import com.gianmarco.wowcraft.gui.ClassSelectionScreen;
import com.gianmarco.wowcraft.gui.SpellbookScreen;
import com.gianmarco.wowcraft.hud.ClientPlayerData;
import com.gianmarco.wowcraft.hud.TargetFrameHud;
import com.gianmarco.wowcraft.network.AbilityUsePacket;
import com.gianmarco.wowcraft.network.ClientNetworkHandler;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import org.lwjgl.glfw.GLFW;

/**
 * WowCraft - Client-side initialization
 * Registers keybinds, HUD rendering, and client-only features
 */
public class WowCraftClient implements ClientModInitializer {

    // 8-slot action bar keybinds: 1, 2, 3, 4, 5, R, F, G
    public static final int ACTION_BAR_SIZE = 8;
    public static KeyMapping[] ACTION_BAR_KEYS = new KeyMapping[ACTION_BAR_SIZE];

    // Key labels for display
    public static final String[] KEY_LABELS = { "1", "2", "3", "4", "5", "R", "F", "G" };
    public static final int[] KEY_CODES = {
            GLFW.GLFW_KEY_1, GLFW.GLFW_KEY_2, GLFW.GLFW_KEY_3, GLFW.GLFW_KEY_4, GLFW.GLFW_KEY_5,
            GLFW.GLFW_KEY_R, GLFW.GLFW_KEY_F, GLFW.GLFW_KEY_G
    };

    // Other keybinds
    public static KeyMapping KEY_CHARACTER_PANEL;
    public static KeyMapping KEY_SPELLBOOK;

    @Override
    public void onInitializeClient() {
        WowCraft.LOGGER.info("WowCraft client initializing...");

        // Register network handlers
        ClientNetworkHandler.register();

        // Register keybinds
        registerKeybinds();

        // Register HUD overlays
        // Note: WowCraftHud replaced by v2 widget system
        // WowCraftHud.register(); // DISABLED - replaced by PlayerFrameWidget +
        // AbilityBarWidget
        // TargetFrameHud.register(); // DISABLED - using 3D nameplates instead

        // Register combat indicator (red vignette when in combat)
        com.gianmarco.wowcraft.hud.CombatIndicatorHud.register();

        // Register v2 widget system
        com.gianmarco.wowcraft.ui.widget.WowWidgetManager.register();
        com.gianmarco.wowcraft.ui.widget.WowWidgetManager.addWidget(
                new com.gianmarco.wowcraft.ui.widget.PlayerFrameWidget());
        com.gianmarco.wowcraft.ui.widget.WowWidgetManager.addWidget(
                new com.gianmarco.wowcraft.ui.widget.AbilityBarWidget());

        // Register entity renderers
        com.gianmarco.wowcraft.entity.ModEntityRenderers.register();

        // Register tick event to check for class selection
        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);

        // Register FCT Renderer (world-space rendering via WorldRenderEvents)
        com.gianmarco.wowcraft.client.FloatingDamageTextRenderer.register();

        // Register 3D nameplate renderer (shows health bars above entities)
        com.gianmarco.wowcraft.client.EntityNameplateRenderer.register();

        // Register zone announcement HUD (shows zone name when entering new zones)
        com.gianmarco.wowcraft.hud.ZoneAnnouncementHud.register();

        // Register persistent zone info HUD (shows current zone below minimap area)
        com.gianmarco.wowcraft.hud.ZoneInfoHud.register();

        WowCraft.LOGGER.info("WowCraft client initialized!");
    }

    private void registerKeybinds() {
        // Register all 8 action bar keys
        for (int i = 0; i < ACTION_BAR_SIZE; i++) {
            ACTION_BAR_KEYS[i] = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                    "key.wowcraft.ability" + (i + 1),
                    InputConstants.Type.KEYSYM,
                    KEY_CODES[i],
                    "category.wowcraft.abilities"));
        }

        KEY_CHARACTER_PANEL = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wowcraft.character",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_C,
                "category.wowcraft.general"));

        KEY_SPELLBOOK = KeyBindingHelper.registerKeyBinding(new KeyMapping(
                "key.wowcraft.spellbook",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_P,
                "category.wowcraft.general"));

        WowCraft.LOGGER.info("Registered WowCraft keybinds: 1-5,R,F,G for abilities, C for character, P for spellbook");
    }

    private void onClientTick(Minecraft client) {
        if (client.player == null || client.level == null) {
            return;
        }

        // C key: Open character panel if class selected, otherwise class selection
        while (KEY_CHARACTER_PANEL.consumeClick()) {
            if (ClientPlayerData.getData().hasSelectedClass()) {
                client.setScreen(new CharacterPanelScreen());
            } else {
                client.setScreen(new ClassSelectionScreen());
            }
        }

        // P key: Open spellbook (requires class selected)
        while (KEY_SPELLBOOK.consumeClick()) {
            if (ClientPlayerData.getData().hasSelectedClass()) {
                client.setScreen(new SpellbookScreen());
            }
        }

        // Handle all 8 action bar keys
        for (int i = 0; i < ACTION_BAR_SIZE; i++) {
            while (ACTION_BAR_KEYS[i].consumeClick()) {
                ClientPlayNetworking.send(new AbilityUsePacket(i));
            }
        }
    }
}

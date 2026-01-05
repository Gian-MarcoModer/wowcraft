package com.gianmarco.wowcraft.ui.widget;

import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;

/**
 * Central manager for all WowCraft HUD widgets.
 * Handles registration, ordered rendering, and global visibility.
 */
public final class WowWidgetManager {

    private static final List<WowWidget> widgets = new ArrayList<>();
    private static boolean initialized = false;

    private WowWidgetManager() {
    }

    /**
     * Register the widget manager with the HUD render callback.
     * Call this once during client initialization.
     */
    @SuppressWarnings("deprecation")
    public static void register() {
        if (initialized)
            return;

        HudRenderCallback.EVENT.register(WowWidgetManager::onHudRender);
        initialized = true;
        WowCraft.LOGGER.info("Registered WowWidgetManager");
    }

    /**
     * Add a widget to the manager.
     * Widgets are rendered in the order they are added.
     */
    public static void addWidget(WowWidget widget) {
        widgets.add(widget);
        WowCraft.LOGGER.debug("Added widget: {}", widget.getClass().getSimpleName());
    }

    /**
     * Remove a widget from the manager.
     */
    public static void removeWidget(WowWidget widget) {
        widgets.remove(widget);
    }

    /**
     * Clear all registered widgets.
     */
    public static void clear() {
        widgets.clear();
    }

    /**
     * Set visibility for all widgets.
     */
    public static void setAllVisible(boolean visible) {
        for (WowWidget widget : widgets) {
            widget.setVisible(visible);
        }
    }

    /**
     * Get a widget by class type.
     */
    @SuppressWarnings("unchecked")
    public static <T extends WowWidget> T getWidget(Class<T> type) {
        for (WowWidget widget : widgets) {
            if (type.isInstance(widget)) {
                return (T) widget;
            }
        }
        return null;
    }

    /**
     * Main HUD render callback.
     */
    private static void onHudRender(GuiGraphics graphics, net.minecraft.client.DeltaTracker deltaTracker) {
        Minecraft client = Minecraft.getInstance();

        // Don't render if no player/world
        if (client.player == null || client.level == null)
            return;

        // Don't render during screens (inventory, etc)
        if (client.screen != null)
            return;

        // Don't render if HUD is hidden (F1)
        if (client.options.hideGui)
            return;

        float partialTick = deltaTracker.getGameTimeDeltaPartialTick(true);

        // Render all visible widgets
        for (WowWidget widget : widgets) {
            if (widget.isVisible()) {
                widget.render(graphics, client, partialTick);
            }
        }
    }

    /**
     * Tick all widgets (called from client tick event).
     */
    public static void tick() {
        for (WowWidget widget : widgets) {
            widget.tick();
        }
    }
}

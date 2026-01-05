package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.client.FloatingDamageTextRenderer;
import com.gianmarco.wowcraft.hud.ClientActionBar;
import com.gianmarco.wowcraft.hud.ClientPlayerData;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * Client-side network registration
 * Handles receiving packets from the server.
 */
public class ClientNetworkHandler {

    // Client-side casting state
    public static String currentCastSpell = "";
    public static float currentCastDuration = 0;
    public static float currentCastProgress = 0;
    public static boolean isCasting = false;

    /**
     * Register client-side packet handlers (receivers for S2C packets)
     */
    public static void register() {
        WowCraft.LOGGER.info("Registering WowCraft client network handlers...");

        // Note: S2C packet types are registered by the server-side NetworkHandler.
        // We only register the receiver here.

        // Handle player data sync from server
        ClientPlayNetworking.registerGlobalReceiver(PlayerDataSyncPacket.TYPE, (payload, context) -> {
            // Update client-side cache
            ClientPlayerData.update(
                    payload.playerClass(),
                    payload.level(),
                    payload.experience(),
                    payload.currentResource(),
                    payload.maxResource(),
                    payload.comboPoints(),
                    payload.getMoneyPouch());

            // Always sync action bar from server (server is authoritative)
            ClientActionBar.setFromList(payload.actionBar());

            // Auto-open class selection if player has no class
            if (payload.playerClass() == com.gianmarco.wowcraft.playerclass.PlayerClass.NONE) {
                com.gianmarco.wowcraft.WowCraft.LOGGER.info("Player has no class - opening class selection screen");
                context.client().execute(() -> {
                    // Only open if not already on the class selection screen
                    if (!(context.client().screen instanceof com.gianmarco.wowcraft.gui.ClassSelectionScreen)) {
                        context.client().setScreen(new com.gianmarco.wowcraft.gui.ClassSelectionScreen());
                    }
                });
            }
        });

        // Handle casting update from server
        ClientPlayNetworking.registerGlobalReceiver(CastingUpdatePacket.TYPE, (payload, context) -> {
            if (payload.interrupted() || payload.spellName().isEmpty()) {
                // Cast ended or interrupted
                isCasting = false;
                currentCastSpell = "";
                currentCastProgress = 0;
                currentCastDuration = 0;
            } else {
                // Casting in progress
                isCasting = true;
                currentCastSpell = payload.spellName();
                currentCastDuration = payload.castDuration();
                currentCastProgress = payload.progress();
            }
        });

        // Handle damage display from server
        ClientPlayNetworking.registerGlobalReceiver(DamageDisplayPacket.TYPE, (payload, context) -> {
            WowCraft.LOGGER.info("[FCT] Client received damage packet - Damage: {}, IsCrit: {}, Pos: ({}, {}, {})",
                    payload.damage(), payload.isCritical(), payload.x(), payload.y(), payload.z());

            // Add the damage text to the renderer
            // Add the damage text to the renderer
            FloatingDamageTextRenderer.addDamageText(
                    payload.entityId(),
                    payload.damage(),
                    payload.isCritical(),
                    payload.isSpell(),
                    payload.x(),
                    payload.y(),
                    payload.z());
        });

        // Handle combat state sync from server
        ClientPlayNetworking.registerGlobalReceiver(CombatStatePacket.TYPE, (payload, context) -> {
            WowCraft.LOGGER.info("[CombatSync] Client received combat state - InCombat: {}, Tick: {}",
                    payload.inCombat(), payload.lastCombatTick());

            // Update the client-side combat indicator
            com.gianmarco.wowcraft.hud.CombatIndicatorHud.updateCombatState(
                    payload.inCombat(),
                    payload.lastCombatTick());
        });

        // Handle zone entry from server
        ClientPlayNetworking.registerGlobalReceiver(ZoneEntryPacket.TYPE, (payload, context) -> {
            WowCraft.LOGGER.info(
                    "[Zone] Client received zone entry - Zone: {}, Subtitle: {}, Level: {}-{}, Pos: ({}, {}, {})",
                    payload.zoneName(), payload.subtitle(), payload.levelMin(), payload.levelMax(),
                    payload.posX(), payload.posY(), payload.posZ());

            // Show zone announcement
            com.gianmarco.wowcraft.hud.ZoneAnnouncementHud.showZoneEntry(
                    payload.zoneName(),
                    payload.subtitle(),
                    payload.levelMin(),
                    payload.levelMax());

            // Create Xaero's Minimap waypoint (if available)
            com.gianmarco.wowcraft.integration.XaeroIntegration.createZoneWaypointFromPacket(
                    payload.zoneName(),
                    payload.subtitle(),
                    payload.levelMin(),
                    payload.levelMax(),
                    payload.posX(),
                    payload.posY(),
                    payload.posZ());

            // Update persistent zone info display (below minimap)
            com.gianmarco.wowcraft.hud.ZoneInfoHud.updateZone(
                    payload.zoneName(),
                    payload.subtitle(),
                    payload.levelMin(),
                    payload.levelMax());
        });

        // Reset client data on disconnect
        net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT
                .register((handler, client) -> {
                    ClientPlayerData.reset();
                    ClientActionBar.reset(); // Reset action bar to prevent showing wrong class abilities
                    com.gianmarco.wowcraft.hud.ZoneInfoHud.clear(); // Clear zone info
                    com.gianmarco.wowcraft.integration.XaeroIntegration.clearTrackedWaypoints();
                    WowCraft.LOGGER.info("Reset client player data and action bar cache");
                });

        WowCraft.LOGGER.info("WowCraft client network ready.");
    }
}

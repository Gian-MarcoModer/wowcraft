package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.ability.AbilityManager;
import com.gianmarco.wowcraft.ability.ServerActionBar;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Handles registration and processing of all network packets.
 */
public class NetworkHandler {

    /**
     * Register all server-side packet receivers
     */
    public static void registerServerReceivers() {
        WowCraft.LOGGER.info("Registering WowCraft network handlers...");

        // Register packet types (C2S = client to server)
        PayloadTypeRegistry.playC2S().register(ClassSelectionPacket.TYPE, ClassSelectionPacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(AbilityUsePacket.TYPE, AbilityUsePacket.STREAM_CODEC);
        PayloadTypeRegistry.playC2S().register(ActionBarSyncPacket.TYPE, ActionBarSyncPacket.STREAM_CODEC);

        // Register packet types (S2C = server to client)
        PayloadTypeRegistry.playS2C().register(PlayerDataSyncPacket.TYPE, PlayerDataSyncPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CastingUpdatePacket.TYPE, CastingUpdatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(DamageDisplayPacket.TYPE, DamageDisplayPacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(CombatStatePacket.TYPE, CombatStatePacket.STREAM_CODEC);
        PayloadTypeRegistry.playS2C().register(ZoneEntryPacket.TYPE, ZoneEntryPacket.STREAM_CODEC);

        // Handle class selection from client
        ServerPlayNetworking.registerGlobalReceiver(ClassSelectionPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            PlayerClass selectedClass = payload.selectedClass();

            // Validate the selection
            if (selectedClass == PlayerClass.NONE) {
                WowCraft.LOGGER.warn("Player {} tried to select NONE class", player.getName().getString());
                return;
            }

            // Check if player already has a class
            if (PlayerDataManager.hasSelectedClass(player)) {
                WowCraft.LOGGER.warn("Player {} already has a class selected", player.getName().getString());
                player.sendSystemMessage(Component.literal("§cYou have already chosen a class!"));
                return;
            }

            // Set the player's class
            PlayerDataManager.setPlayerClass(player, selectedClass);

            // Reset server-side action bar to defaults for the new class
            ServerActionBar.initFromDefaults(player, selectedClass);

            WowCraft.LOGGER.info("Player {} selected class: {}",
                    player.getName().getString(), selectedClass.getDisplayName());

            // Send confirmation message
            player.sendSystemMessage(Component.literal(
                    "§a✓ You are now a §" + getClassColorCode(selectedClass) +
                            selectedClass.getDisplayName() + "§a!"));

            // Recalculate stats for the new class (updates Max HP)
            var stats = com.gianmarco.wowcraft.stats.StatsManager.recalculateStats(player);

            // Heal to full HP (since max HP likely increased significantly)
            player.setHealth(stats.getMaxHealth());

            // Sync data to client for HUD
            syncPlayerData(player);
        });

        // Handle ability use from client
        ServerPlayNetworking.registerGlobalReceiver(AbilityUsePacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            int slot = payload.abilitySlot();

            // Validate slot (0-7 for 8 slots)
            if (slot < 0 || slot > 7) {
                WowCraft.LOGGER.warn("Player {} tried to use invalid ability slot: {}",
                        player.getName().getString(), slot);
                return;
            }

            // Get player's class
            PlayerClass playerClass = PlayerDataManager.getPlayerClass(player);
            if (playerClass == PlayerClass.NONE) {
                player.sendSystemMessage(Component.literal("§cYou must select a class first! Press C."));
                return;
            }

            // Initialize server action bar if needed
            if (!ServerActionBar.isInitialized(player)) {
                ServerActionBar.initFromDefaults(player, playerClass);
            }

            // Get the ability from player's customized action bar
            Ability ability = ServerActionBar.getSlot(player, slot);
            if (ability == null) {
                // Empty slot - no ability assigned
                return;
            }

            // Try to use the ability
            boolean success = AbilityManager.tryUseAbility(player, ability);

            if (!success) {
                // Determine why it failed
                if (AbilityManager.isOnCooldown(player, ability)) {
                    float remaining = AbilityManager.getRemainingCooldown(player, ability);
                    player.sendSystemMessage(Component.literal(
                            String.format("§c%s is on cooldown (%.1fs)", ability.getDisplayName(), remaining)));
                } else if (playerClass != ability.getRequiredClass()) {
                    player.sendSystemMessage(Component.literal(
                            String.format("§cYou must be a %s to use %s",
                                    ability.getRequiredClass().getDisplayName(),
                                    ability.getDisplayName())));
                } else if (!PlayerDataManager.hasResource(player, ability.getResourceCost())) {
                    player.sendSystemMessage(Component.literal(
                            String.format("§cNot enough %s for %s",
                                    playerClass.getResourceType().getDisplayName(),
                                    ability.getDisplayName())));
                } else if (!ability.canUse(player)) {
                    player.sendSystemMessage(Component.literal(
                            String.format("§cCannot use %s right now", ability.getDisplayName())));
                }
            }

            // Always sync after ability attempt (resource changed)
            syncPlayerData(player);
        });

        // Handle action bar sync from client (when player customizes action bar)
        ServerPlayNetworking.registerGlobalReceiver(ActionBarSyncPacket.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            int slot = payload.slot();
            String abilityId = payload.abilityId();

            // Validate slot
            if (slot < 0 || slot > 7) {
                return;
            }

            // Update server-side action bar
            ServerActionBar.setSlot(player, slot, abilityId);
            WowCraft.LOGGER.debug("Player {} updated action bar slot {} to {}",
                    player.getName().getString(), slot, abilityId);
        });
    }

    private static String getClassColorCode(PlayerClass playerClass) {
        return switch (playerClass) {
            case WARRIOR -> "6"; // Orange/brown
            case MAGE -> "b"; // Light blue
            default -> "7"; // Gray
        };
    }

    /**
     * Send player data sync packet to client for HUD
     */
    public static void syncPlayerData(ServerPlayer player) {
        PlayerData data = PlayerDataManager.getData(player);
        var money = data.moneyPouch();
        ServerPlayNetworking.send(player, new PlayerDataSyncPacket(
                data.playerClass(),
                data.level(),
                data.experience(),
                data.currentResource(),
                data.maxResource(),
                data.comboPoints(),
                money.copper(),
                money.silver(),
                money.gold(),
                data.actionBar()));
    }

    /**
     * Send damage display packet to nearby players
     */
    public static void sendDamageDisplay(ServerPlayer player, int entityId, float damage, boolean isCritical,
            boolean isSpell, double x, double y, double z) {
        DamageDisplayPacket packet = new DamageDisplayPacket(entityId, damage, isCritical, isSpell, x, y, z);

        // Send to the player who dealt the damage
        ServerPlayNetworking.send(player, packet);

        // Also send to nearby players (within 64 blocks)
        for (ServerPlayer nearbyPlayer : player.serverLevel().players()) {
            if (nearbyPlayer != player && nearbyPlayer.distanceTo(player) < 64.0) {
                ServerPlayNetworking.send(nearbyPlayer, packet);
            }
        }
    }

    /**
     * Send combat state sync packet to client
     */
    public static void syncCombatState(ServerPlayer player, boolean inCombat, long lastCombatTick) {
        ServerPlayNetworking.send(player, new CombatStatePacket(inCombat, lastCombatTick));
    }

    /**
     * Send zone entry packet to client when player enters a new zone
     */
    public static void sendZoneEntry(ServerPlayer player, com.gianmarco.wowcraft.zone.ZoneRegion zone) {
        ServerPlayNetworking.send(player, new ZoneEntryPacket(
                zone.assignedName() != null ? zone.assignedName() : "Unknown",
                zone.subtitle(),
                zone.suggestedLevelMin(),
                zone.suggestedLevelMax(),
                zone.center().getX(),
                zone.center().getY(),
                zone.center().getZ()));
    }
}

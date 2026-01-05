package com.gianmarco.wowcraft;

import com.gianmarco.wowcraft.ability.CastingManager;
import com.gianmarco.wowcraft.entity.ModEntities;
import com.gianmarco.wowcraft.item.WowItemComponents;
import com.gianmarco.wowcraft.network.NetworkHandler;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import com.gianmarco.wowcraft.playerclass.ResourceRegeneration;
import com.gianmarco.wowcraft.stats.AttackHandler;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.bernie.geckolib.GeckoLib;

/**
 * WowCraft - A Minecraft mod inspired by World of Warcraft Classic
 * Main server-side initialization
 */
public class WowCraft implements ModInitializer {
    public static final String MOD_ID = "wowcraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        // This code runs as soon as Minecraft is in a mod-load-ready state.
        LOGGER.info("=================================");
        LOGGER.info("  WowCraft is loading!");
        LOGGER.info("  For the Horde! For the Alliance!");
        LOGGER.info("=================================");

        // Initialize GeckoLib
        // GeckoLib.initialize(); // Using self-init or not required in this version

        // Register custom item data components (MUST be first!)
        WowItemComponents.register();

        // Register player data attachments
        PlayerDataRegistry.register();

        // Register custom entities (projectiles, etc.)
        ModEntities.register();

        // Register network packets
        NetworkHandler.registerServerReceivers();

        // Register resource regeneration (MP5 mana regen, rage decay)
        ResourceRegeneration.register();

        // Register combat state tick handler (includes mob aggro detection)
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_SERVER_TICK.register(server -> {
            com.gianmarco.wowcraft.combat.CombatStateManager.tick();
            // Check for mobs targeting players (aggro detection)
            for (net.minecraft.server.level.ServerPlayer player : server.getPlayerList().getPlayers()) {
                com.gianmarco.wowcraft.combat.CombatStateManager.checkForAggroingMobs(player);
            }
        });

        // Register attack handler (uses v2 DamagePipeline)
        AttackHandler.register();

        // Register casting manager (handles cast times)
        CastingManager.register();

        // Note: Old DamageEventHandler removed - v2 FloatingTextHandler handles FCT via
        // PostDamageEvent

        // Register charge ability tick handler
        com.gianmarco.wowcraft.ability.warrior.Charge.register();

        // Register commands
        com.gianmarco.wowcraft.command.WowCommands.register();

        // ========== V2 SYSTEMS ==========

        // Register v2 floating text handler (uses PostDamageEvent)
        com.gianmarco.wowcraft.combat.handlers.FloatingTextHandler.register();

        // Register v2 data registries (JSON class/ability loading)
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.gianmarco.wowcraft.class_.ClassRegistry.createReloadListener());
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.gianmarco.wowcraft.ability.AbilityDataRegistry.createReloadListener());

        // Register zone name registry (loads zone names from JSON)
        net.fabricmc.fabric.api.resource.ResourceManagerHelper.get(net.minecraft.server.packs.PackType.SERVER_DATA)
                .registerReloadListener(com.gianmarco.wowcraft.zone.ZoneRegistry.createReloadListener());

        // Register zone manager (tracks player zone transitions using lazy detection)
        com.gianmarco.wowcraft.zone.ZoneManager.register();

        // ========== MOB PACK SYSTEM ==========

        // Initialize pack templates
        com.gianmarco.wowcraft.mobpack.MobPackTemplateLoader.init();

        // Register chunk load event for pack spawning
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerChunkEvents.CHUNK_LOAD.register((world, chunk) -> {
            if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                com.gianmarco.wowcraft.mobpack.MobPackManager.onChunkLoad(serverLevel, chunk.getPos());
            }
        });

        // Register server tick for pack respawns
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents.END_WORLD_TICK.register(level -> {
            if (level instanceof net.minecraft.server.level.ServerLevel serverLevel
                    && serverLevel.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                com.gianmarco.wowcraft.mobpack.MobPackManager.onServerTick(serverLevel);
            }
        });

        // Save and clear zones and packs on world unload
        net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents.UNLOAD.register((server, world) -> {
            if (world.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
                // Save mob packs before clearing
                if (world instanceof net.minecraft.server.level.ServerLevel serverLevel) {
                    com.gianmarco.wowcraft.zone.ZoneSaveData saveData =
                        com.gianmarco.wowcraft.zone.ZoneSaveData.get(serverLevel);
                    saveData.save(true); // Include mob packs on shutdown
                    LOGGER.info("Saved zone data and mob packs on world unload");
                }

                com.gianmarco.wowcraft.zone.ZoneRegistry.clearZones();
                com.gianmarco.wowcraft.zone.ZoneManager.clearAll();
                com.gianmarco.wowcraft.zone.ZoneSaveData.clearCache();
                com.gianmarco.wowcraft.mobpack.MobPackManager.clear();
            }
        });

        // Sync player data when they join (ALWAYS - even without class, for class
        // selection screen)
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            var player = handler.getPlayer();
            LOGGER.info("Syncing player data for {} on join (class: {})",
                    player.getName().getString(),
                    PlayerDataManager.getPlayerClass(player));

            if (PlayerDataManager.hasSelectedClass(player)) {
                // Force stat recalc to apply HP attributes
                com.gianmarco.wowcraft.stats.StatsManager.recalculateStats(player);
            }
            // Always sync - so client knows if class selection is needed
            NetworkHandler.syncPlayerData(player);
        });

        // Sync player data after respawn (class persists through death)
        net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN
                .register((oldPlayer, newPlayer, alive) -> {
                    if (PlayerDataManager.hasSelectedClass(newPlayer)) {
                        LOGGER.info("Syncing player data for {} after respawn", newPlayer.getName().getString());
                        // Force stat recalc to apply HP attributes
                        com.gianmarco.wowcraft.stats.StatsManager.recalculateStats(newPlayer);
                        NetworkHandler.syncPlayerData(newPlayer);
                    }
                });

        LOGGER.info("WowCraft initialized successfully!");
    }
}

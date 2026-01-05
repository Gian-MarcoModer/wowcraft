package com.gianmarco.wowcraft.zone;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.network.NetworkHandler;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages player zone tracking and triggers zone entry events.
 * Uses lazy zone detection based on player's current biome.
 * First encounter of each biome type = first (lowest level) zone name.
 * Zones are persisted to world data so they survive server restarts.
 */
public class ZoneManager {

    /** Tracks which zone each player is currently in (by biome group) */
    private static final Map<UUID, BiomeGroup> PLAYER_CURRENT_BIOME = new ConcurrentHashMap<>();

    /** Tracks the last zone announcement shown to each player */
    private static final Map<UUID, String> PLAYER_LAST_ZONE_NAME = new ConcurrentHashMap<>();

    /** Tick counter for throttling zone checks */
    private static int tickCounter = 0;

    /** Check zone every N ticks (20 ticks = 1 second) */
    private static final int CHECK_INTERVAL = 20;

    /**
     * Registers the zone manager tick handler.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            if (tickCounter >= CHECK_INTERVAL) {
                tickCounter = 0;

                for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                    checkPlayerZone(player);
                }
            }
        });

        WowCraft.LOGGER.info("ZoneManager registered");
    }

    /**
     * Checks if a player has entered a new zone based on their current biome.
     */
    private static void checkPlayerZone(ServerPlayer player) {
        // Only check in overworld
        if (player.level().dimension() != Level.OVERWORLD) {
            return;
        }

        BlockPos pos = player.blockPosition();

        // Get biome at player's position
        Holder<Biome> biomeHolder = player.level().getBiome(pos);
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
        BiomeGroup currentGroup = BiomeGroup.fromBiome(biomeKey);

        if (currentGroup == null || !currentGroup.isNameable()) {
            return; // In river, beach, or ocean - don't announce
        }

        BiomeGroup previousGroup = PLAYER_CURRENT_BIOME.get(player.getUUID());

        // Check if biome group changed
        if (currentGroup != previousGroup) {
            PLAYER_CURRENT_BIOME.put(player.getUUID(), currentGroup);

            // Get zone for this biome group (from persistent storage or create new)
            ZoneRegion zone = getOrCreateZone(player.serverLevel(), currentGroup, pos);

            // Check if we should announce (different zone name than last announced)
            String lastZoneName = PLAYER_LAST_ZONE_NAME.get(player.getUUID());
            if (zone != null && (lastZoneName == null || !lastZoneName.equals(zone.assignedName()))) {
                PLAYER_LAST_ZONE_NAME.put(player.getUUID(), zone.assignedName());
                onZoneEnter(player, zone);
            }
        }
    }

    /**
     * Gets or creates a zone for the given biome group.
     * Uses persistent world data to store/retrieve zones.
     */
    private static ZoneRegion getOrCreateZone(ServerLevel level, BiomeGroup group, BlockPos pos) {
        BlockPos spawn = level.getSharedSpawnPos();
        int distance = (int) Math.sqrt(pos.distSqr(spawn));

        // Check if beyond named zone radius
        if (distance > ZoneRegion.MAX_NAMED_DISTANCE) {
            return new ZoneRegion(
                    UUID.nameUUIDFromBytes("unexplored_wilds".getBytes()),
                    null,
                    pos,
                    0,
                    distance,
                    ZoneRegistry.UNEXPLORED_WILDS.name(),
                    ZoneRegistry.UNEXPLORED_WILDS.subtitle(),
                    ZoneRegistry.UNEXPLORED_WILDS.levelMin(),
                    ZoneRegistry.UNEXPLORED_WILDS.levelMax());
        }

        // Get persistent storage
        ZoneSaveData saveData = ZoneSaveData.get(level);

        // Check if zone was already discovered (persisted)
        ZoneRegion existingZone = saveData.getZone(group);
        if (existingZone != null) {
            WowCraft.LOGGER.debug("Found persisted {} zone: {} (L{}-{})",
                    group,
                    existingZone.assignedName(),
                    existingZone.suggestedLevelMin(),
                    existingZone.suggestedLevelMax());
            return existingZone;
        }

        // First time discovering this biome group - create and persist new zone
        ZoneRegion region = new ZoneRegion(
                UUID.randomUUID(),
                group,
                pos,
                50, // Assumed size
                distance,
                null,
                null,
                1, // Placeholder - will be set by assignName
                5);

        // Use the persisted discovery count for name assignment
        int discoveryIndex = saveData.getDiscoveryCount(group);
        saveData.incrementDiscoveryCount(group);

        // Get zone definition at this index
        ZoneDefinition zoneDef = ZoneRegistry.getZoneAtIndex(group, discoveryIndex);

        ZoneRegion namedRegion = region
                .withName(zoneDef.name(), zoneDef.subtitle())
                .withLevelRange(zoneDef.levelMin(), zoneDef.levelMax());

        // Persist to world data
        saveData.setZone(group, namedRegion);

        WowCraft.LOGGER.info("Discovered new {} zone: {} (L{}-{}) - saved to world data",
                group,
                namedRegion.assignedName(),
                namedRegion.suggestedLevelMin(),
                namedRegion.suggestedLevelMax());

        return namedRegion;
    }

    /**
     * Called when a player enters a new zone.
     */
    private static void onZoneEnter(ServerPlayer player, ZoneRegion zone) {
        WowCraft.LOGGER.info("Announcing zone entry to {}: {} (L{}-{})",
                player.getName().getString(),
                zone.assignedName(),
                zone.suggestedLevelMin(),
                zone.suggestedLevelMax());

        // Send zone entry packet to client
        NetworkHandler.sendZoneEntry(player, zone);
    }

    /**
     * Clears zone data for a player (on disconnect).
     */
    public static void clearPlayer(UUID playerId) {
        PLAYER_CURRENT_BIOME.remove(playerId);
        PLAYER_LAST_ZONE_NAME.remove(playerId);
    }

    /**
     * Clears all player tracking data (on world unload).
     * Note: Zone data is persisted, only player tracking is cleared.
     */
    public static void clearAll() {
        PLAYER_CURRENT_BIOME.clear();
        PLAYER_LAST_ZONE_NAME.clear();
    }
}

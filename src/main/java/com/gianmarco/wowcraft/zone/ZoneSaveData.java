package com.gianmarco.wowcraft.zone;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.mobpack.MobPackManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

/**
 * Persistent storage for discovered zones.
 * Saved to a JSON file in the world folder so zones survive server restarts.
 * All players on the server share the same zone discoveries.
 */
public class ZoneSaveData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wowcraft_zones.json";

    /** Discovered zones per biome group */
    private final Map<BiomeGroup, ZoneRegion> discoveredZones = new EnumMap<>(BiomeGroup.class);

    /** Discovery count per biome group (for assigning next zone name) */
    private final Map<BiomeGroup, Integer> discoveryCounts = new EnumMap<>(BiomeGroup.class);

    /** Path to the save file */
    private Path savePath;

    /** Singleton instance per level */
    private static final Map<String, ZoneSaveData> INSTANCES = new java.util.concurrent.ConcurrentHashMap<>();

    private ZoneSaveData() {
    }

    /**
     * Gets the zone for a biome group, or null if not yet discovered.
     */
    public ZoneRegion getZone(BiomeGroup group) {
        return discoveredZones.get(group);
    }

    /**
     * Registers a discovered zone.
     */
    public void setZone(BiomeGroup group, ZoneRegion zone) {
        discoveredZones.put(group, zone);
        save(); // Auto-save
    }

    /**
     * Gets the current discovery count for a biome group.
     */
    public int getDiscoveryCount(BiomeGroup group) {
        return discoveryCounts.getOrDefault(group, 0);
    }

    /**
     * Increments and returns the discovery count for a biome group.
     */
    public int incrementDiscoveryCount(BiomeGroup group) {
        int count = discoveryCounts.getOrDefault(group, 0);
        discoveryCounts.put(group, count + 1);
        save(); // Auto-save
        return count;
    }

    /**
     * Saves zone data to JSON file.
     * @param includeMobPacks if true, includes mob pack data (only on shutdown)
     */
    public void save(boolean includeMobPacks) {
        if (savePath == null)
            return;

        try {
            JsonObject root = new JsonObject();

            // Save discovered zones
            JsonArray zonesArray = new JsonArray();
            for (Map.Entry<BiomeGroup, ZoneRegion> entry : discoveredZones.entrySet()) {
                JsonObject zoneObj = new JsonObject();
                ZoneRegion zone = entry.getValue();

                zoneObj.addProperty("biomeGroup", entry.getKey().name());
                zoneObj.addProperty("id", zone.id().toString());
                zoneObj.addProperty("name", zone.assignedName() != null ? zone.assignedName() : "");
                zoneObj.addProperty("subtitle", zone.subtitle() != null ? zone.subtitle() : "");
                zoneObj.addProperty("levelMin", zone.suggestedLevelMin());
                zoneObj.addProperty("levelMax", zone.suggestedLevelMax());
                zoneObj.addProperty("centerX", zone.center().getX());
                zoneObj.addProperty("centerY", zone.center().getY());
                zoneObj.addProperty("centerZ", zone.center().getZ());
                zoneObj.addProperty("chunkCount", zone.chunkCount());
                zoneObj.addProperty("distance", zone.distanceFromSpawn());

                zonesArray.add(zoneObj);
            }
            root.add("zones", zonesArray);

            // Save discovery counts
            JsonObject countsObj = new JsonObject();
            for (Map.Entry<BiomeGroup, Integer> entry : discoveryCounts.entrySet()) {
                countsObj.addProperty(entry.getKey().name(), entry.getValue());
            }
            root.add("discoveryCounts", countsObj);

            // Save active mob packs (only on shutdown to avoid performance issues)
            if (includeMobPacks) {
                root.add("mobPacks", MobPackManager.toJson());
            }

            // Write to file
            Files.createDirectories(savePath.getParent());
            try (Writer writer = new BufferedWriter(new OutputStreamWriter(
                    Files.newOutputStream(savePath), StandardCharsets.UTF_8))) {
                GSON.toJson(root, writer);
            }

            WowCraft.LOGGER.debug("Saved {} zones to {}", discoveredZones.size(), savePath);
        } catch (Exception e) {
            WowCraft.LOGGER.error("Failed to save zone data: {}", e.getMessage());
        }
    }

    /**
     * Saves zone data to JSON file (without mob packs for performance).
     */
    public void save() {
        save(false);
    }

    /**
     * Loads zone data from JSON file.
     */
    private void load() {
        if (savePath == null || !Files.exists(savePath))
            return;

        try (Reader reader = new BufferedReader(new InputStreamReader(
                Files.newInputStream(savePath), StandardCharsets.UTF_8))) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null)
                return;

            // Load discovered zones
            if (root.has("zones")) {
                JsonArray zonesArray = root.getAsJsonArray("zones");
                for (JsonElement element : zonesArray) {
                    try {
                        JsonObject zoneObj = element.getAsJsonObject();

                        String groupName = zoneObj.get("biomeGroup").getAsString();
                        BiomeGroup group = BiomeGroup.valueOf(groupName);

                        String idStr = zoneObj.get("id").getAsString();
                        UUID id = UUID.fromString(idStr);

                        String name = zoneObj.get("name").getAsString();
                        String subtitle = zoneObj.get("subtitle").getAsString();
                        int levelMin = zoneObj.get("levelMin").getAsInt();
                        int levelMax = zoneObj.get("levelMax").getAsInt();
                        int centerX = zoneObj.get("centerX").getAsInt();
                        int centerY = zoneObj.get("centerY").getAsInt();
                        int centerZ = zoneObj.get("centerZ").getAsInt();
                        int chunkCount = zoneObj.get("chunkCount").getAsInt();
                        int distance = zoneObj.get("distance").getAsInt();

                        ZoneRegion zone = new ZoneRegion(
                                id, group,
                                new BlockPos(centerX, centerY, centerZ),
                                chunkCount, distance,
                                name.isEmpty() ? null : name,
                                subtitle.isEmpty() ? null : subtitle,
                                levelMin, levelMax);

                        discoveredZones.put(group, zone);
                    } catch (Exception e) {
                        WowCraft.LOGGER.warn("Failed to load zone: {}", e.getMessage());
                    }
                }
            }

            // Load discovery counts
            if (root.has("discoveryCounts")) {
                JsonObject countsObj = root.getAsJsonObject("discoveryCounts");
                for (BiomeGroup group : BiomeGroup.values()) {
                    if (countsObj.has(group.name())) {
                        discoveryCounts.put(group, countsObj.get(group.name()).getAsInt());
                    }
                }
            }

            // Load mob packs
            if (root.has("mobPacks")) {
                JsonArray packsArray = root.getAsJsonArray("mobPacks");
                MobPackManager.fromJson(packsArray);
            }

            WowCraft.LOGGER.info("Loaded {} zones from {}", discoveredZones.size(), savePath);
        } catch (Exception e) {
            WowCraft.LOGGER.error("Failed to load zone data: {}", e.getMessage());
        }
    }

    /**
     * Gets or creates the zone save data for a server level.
     */
    public static ZoneSaveData get(ServerLevel level) {
        // Use world folder path as key
        Path worldFolder = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT);
        String key = worldFolder.toString();

        return INSTANCES.computeIfAbsent(key, k -> {
            ZoneSaveData data = new ZoneSaveData();
            data.savePath = worldFolder.resolve("data").resolve(FILE_NAME);
            data.load();
            return data;
        });
    }

    /**
     * Clears the instance cache (on world unload).
     */
    public static void clearCache() {
        INSTANCES.clear();
    }
}

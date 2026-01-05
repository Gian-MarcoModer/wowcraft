package com.gianmarco.wowcraft.poi;

import com.gianmarco.wowcraft.WowCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Persists POI data to world save files.
 * Saves/loads POIs to ensure they persist across server restarts.
 */
public class POISaveData {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wowcraft_pois.json";

    private final POIManager poiManager;
    private Path savePath;

    /** Singleton instance per level */
    private static final Map<String, POISaveData> INSTANCES = new ConcurrentHashMap<>();

    private POISaveData(long worldSeed) {
        this.poiManager = new POIManager(worldSeed);
    }

    /**
     * Get or create POI save data for a level.
     */
    public static POISaveData get(ServerLevel level) {
        String levelKey = level.dimension().location().toString();
        return INSTANCES.computeIfAbsent(levelKey, k -> {
            POISaveData data = new POISaveData(level.getSeed());
            data.savePath = level.getServer().getWorldPath(net.minecraft.world.level.storage.LevelResource.ROOT)
                    .resolve(FILE_NAME);
            data.load();
            return data;
        });
    }

    /**
     * Get the POI manager.
     */
    public POIManager getManager() {
        return poiManager;
    }

    /**
     * Save POI data to JSON file.
     */
    public void save() {
        if (savePath == null) {
            WowCraft.LOGGER.warn("Save path not set, cannot save POI data");
            return;
        }

        try {
            JsonObject root = new JsonObject();
            JsonArray poisArray = new JsonArray();

            // Serialize all POIs
            for (PointOfInterest poi : poiManager.getAllPOIs()) {
                poisArray.add(poi.toJson());
            }

            root.add("pois", poisArray);
            root.addProperty("count", poiManager.getPOICount());

            // Write to file
            try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            WowCraft.LOGGER.info("Saved {} POIs to {}", poiManager.getPOICount(), FILE_NAME);

        } catch (IOException e) {
            WowCraft.LOGGER.error("Failed to save POI data: {}", e.getMessage());
        }
    }

    /**
     * Load POI data from JSON file.
     */
    private void load() {
        if (savePath == null || !Files.exists(savePath)) {
            WowCraft.LOGGER.info("No existing POI data found, starting fresh");
            return;
        }

        try (Reader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);

            if (root != null && root.has("pois")) {
                JsonArray poisArray = root.getAsJsonArray("pois");

                for (int i = 0; i < poisArray.size(); i++) {
                    JsonObject poiJson = poisArray.get(i).getAsJsonObject();

                    try {
                        PointOfInterest poi = PointOfInterest.fromJson(poiJson);
                        poiManager.addPOI(poi);

                        // Mark the region as generated
                        poiManager.markRegionGenerated(poi.getPosition());

                    } catch (Exception e) {
                        WowCraft.LOGGER.error("Failed to load POI: {}", e.getMessage());
                    }
                }

                WowCraft.LOGGER.info("Loaded {} POIs from {}", poiManager.getPOICount(), FILE_NAME);
            }

        } catch (IOException e) {
            WowCraft.LOGGER.error("Failed to load POI data: {}", e.getMessage());
        }
    }

    /**
     * Clear all POIs and regenerate (for debugging).
     */
    public void clearAndRegenerate() {
        poiManager.clearAll();
        save();
        WowCraft.LOGGER.info("Cleared all POI data");
    }
}

package com.gianmarco.wowcraft.roads;

import com.gianmarco.wowcraft.WowCraft;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.LevelResource;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class RoadSaveData {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String FILE_NAME = "wowcraft_roads.json";

    private static final Map<String, RoadSaveData> INSTANCES = new ConcurrentHashMap<>();

    private Path savePath;

    private RoadSaveData(ServerLevel level) {
        this.savePath = level.getServer().getWorldPath(LevelResource.ROOT).resolve(FILE_NAME);
    }

    public static RoadSaveData get(ServerLevel level) {
        String levelKey = level.dimension().location().toString();
        return INSTANCES.computeIfAbsent(levelKey, k -> new RoadSaveData(level));
    }

    public void loadInto(RoadRegistry registry) {
        if (savePath == null || !Files.exists(savePath)) {
            WowCraft.LOGGER.info("No existing road data found, starting fresh");
            return;
        }

        try (Reader reader = Files.newBufferedReader(savePath, StandardCharsets.UTF_8)) {
            JsonObject root = GSON.fromJson(reader, JsonObject.class);
            if (root == null) {
                return;
            }

            registry.clear();

            if (root.has("nodes")) {
                JsonArray nodes = root.getAsJsonArray("nodes");
                for (int i = 0; i < nodes.size(); i++) {
                    RoadNode node = RoadNode.fromJson(nodes.get(i).getAsJsonObject());
                    registry.addNode(node);
                }
            }

            if (root.has("pairs")) {
                JsonArray pairs = root.getAsJsonArray("pairs");
                for (int i = 0; i < pairs.size(); i++) {
                    registry.addBuiltPairKey(pairs.get(i).getAsString());
                }
            }

            WowCraft.LOGGER.info("Loaded {} road nodes from {}", registry.getNodeCount(), FILE_NAME);
        } catch (IOException e) {
            WowCraft.LOGGER.error("Failed to load road data: {}", e.getMessage());
        }
    }

    public void save(RoadRegistry registry) {
        if (savePath == null) {
            WowCraft.LOGGER.warn("Save path not set, cannot save road data");
            return;
        }

        try {
            JsonObject root = new JsonObject();
            JsonArray nodes = new JsonArray();
            for (RoadNode node : registry.getNodes()) {
                nodes.add(node.toJson());
            }
            root.add("nodes", nodes);

            JsonArray pairs = new JsonArray();
            for (String pair : registry.getBuiltPairs()) {
                pairs.add(pair);
            }
            root.add("pairs", pairs);

            try (Writer writer = Files.newBufferedWriter(savePath, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }

            WowCraft.LOGGER.info("Saved {} road nodes to {}", registry.getNodeCount(), FILE_NAME);
        } catch (IOException e) {
            WowCraft.LOGGER.error("Failed to save road data: {}", e.getMessage());
        }
    }
}

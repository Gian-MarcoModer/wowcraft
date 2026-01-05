package com.gianmarco.wowcraft.class_;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a player class loaded from JSON.
 * Immutable record containing all class data.
 */
public record PlayerClass(
        ResourceLocation id,
        String displayName,
        String description,
        ResourceType primaryResource,
        ClassStats baseStats,
        ClassStats statsPerLevel,
        List<ResourceLocation> startingAbilities,
        Map<Integer, List<ResourceLocation>> learnableAbilities,
        ResourceLocation icon) {

    /**
     * Parse a PlayerClass from JSON.
     */
    public static PlayerClass fromJson(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(json.get("id").getAsString());
        String displayName = json.get("display_name").getAsString();
        String description = json.has("description") ? json.get("description").getAsString() : "";

        // Parse resource type
        String resourceStr = json.get("primary_resource").getAsString().toUpperCase();
        ResourceType resource = switch (resourceStr) {
            case "MANA" -> ResourceType.MANA;
            case "RAGE" -> ResourceType.RAGE;
            case "ENERGY" -> ResourceType.ENERGY;
            default -> ResourceType.NONE;
        };

        // Parse stats
        ClassStats baseStats = ClassStats.fromJson(json.getAsJsonObject("base_stats"));
        ClassStats perLevel = json.has("stats_per_level")
                ? ClassStats.fromJson(json.getAsJsonObject("stats_per_level"))
                : ClassStats.ZERO;

        // Parse starting abilities
        List<ResourceLocation> starting = new ArrayList<>();
        if (json.has("starting_abilities")) {
            for (JsonElement elem : json.getAsJsonArray("starting_abilities")) {
                starting.add(ResourceLocation.parse(elem.getAsString()));
            }
        }

        // Parse learnable abilities (level -> ability list)
        Map<Integer, List<ResourceLocation>> learnable = new HashMap<>();
        if (json.has("learnable_abilities")) {
            JsonObject learnObj = json.getAsJsonObject("learnable_abilities");
            for (String levelKey : learnObj.keySet()) {
                int level = Integer.parseInt(levelKey);
                List<ResourceLocation> abilities = new ArrayList<>();
                for (JsonElement elem : learnObj.getAsJsonArray(levelKey)) {
                    abilities.add(ResourceLocation.parse(elem.getAsString()));
                }
                learnable.put(level, abilities);
            }
        }

        // Parse icon
        ResourceLocation icon = json.has("icon")
                ? ResourceLocation.parse(json.get("icon").getAsString())
                : ResourceLocation.fromNamespaceAndPath("wowcraft", "textures/gui/class/default.png");

        return new PlayerClass(
                id, displayName, description, resource,
                baseStats, perLevel, starting, learnable, icon);
    }

    /**
     * Get stats for a specific level.
     */
    public ClassStats getStatsAtLevel(int level) {
        return baseStats.add(statsPerLevel.multiply(level - 1));
    }

    /**
     * Get all abilities available at a given level.
     */
    public List<ResourceLocation> getAvailableAbilities(int level) {
        List<ResourceLocation> available = new ArrayList<>(startingAbilities);

        for (Map.Entry<Integer, List<ResourceLocation>> entry : learnableAbilities.entrySet()) {
            if (entry.getKey() <= level) {
                available.addAll(entry.getValue());
            }
        }

        return available;
    }

    /**
     * Check if this class uses mana.
     */
    public boolean usesMana() {
        return primaryResource == ResourceType.MANA;
    }

    /**
     * Check if this class uses rage.
     */
    public boolean usesRage() {
        return primaryResource == ResourceType.RAGE;
    }
}

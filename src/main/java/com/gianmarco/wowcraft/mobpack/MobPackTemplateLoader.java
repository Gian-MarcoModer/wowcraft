package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import java.io.InputStreamReader;
import java.util.*;

/**
 * Loads and provides access to mob pack templates.
 * Templates are defined in code for now (can be moved to JSON data files
 * later).
 */
public class MobPackTemplateLoader {

    private static final Map<String, MobPackTemplate> TEMPLATES = new HashMap<>();
    private static boolean initialized = false;

    /**
     * Initialize templates. Called on mod initialization.
     */
    public static void init() {
        if (initialized)
            return;

        registerDefaultTemplates();
        initialized = true;

        WowCraft.LOGGER.info("Loaded {} mob pack templates", TEMPLATES.size());
    }

    /**
     * Register the 3 basic pack templates for testing.
     */
    private static void registerDefaultTemplates() {
        // Zombie Pack - spawns in forests and plains
        TEMPLATES.put("zombie_pack", new MobPackTemplate(
                "zombie_pack",
                Set.of(BiomeGroup.FOREST, BiomeGroup.PLAINS),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 1, 3, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:husk"), 0, 1, 30)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // Skeleton Pack - spawns in forests and snowy areas
        TEMPLATES.put("skeleton_pack", new MobPackTemplate(
                "skeleton_pack",
                Set.of(BiomeGroup.FOREST, BiomeGroup.TAIGA, BiomeGroup.SNOWY_PLAINS),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:skeleton"), 2, 3, 80),
                        new MobEntry(ResourceLocation.parse("minecraft:stray"), 0, 1, 20)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // Spider Nest - spawns in forests and swamps
        TEMPLATES.put("spider_nest", new MobPackTemplate(
                "spider_nest",
                Set.of(BiomeGroup.FOREST, BiomeGroup.SWAMP),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:spider"), 2, 4, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:cave_spider"), 0, 2, 30)),
                3, 5,
                6.0f, // Smaller social aggro radius for spiders
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));
    }

    /**
     * Get a template by ID.
     */
    public static MobPackTemplate getTemplate(String id) {
        return TEMPLATES.get(id);
    }

    /**
     * Get all templates that can spawn in a given zone.
     */
    public static List<MobPackTemplate> getTemplatesForZone(BiomeGroup zone) {
        List<MobPackTemplate> result = new ArrayList<>();
        for (MobPackTemplate template : TEMPLATES.values()) {
            if (template.canSpawnInZone(zone)) {
                result.add(template);
            }
        }
        return result;
    }

    /**
     * Get a random template for a zone.
     */
    public static MobPackTemplate getRandomTemplateForZone(BiomeGroup zone, Random random) {
        List<MobPackTemplate> valid = getTemplatesForZone(zone);
        if (valid.isEmpty()) {
            return null;
        }
        return valid.get(random.nextInt(valid.size()));
    }

    /**
     * Check if templates are loaded.
     */
    public static boolean isInitialized() {
        return initialized;
    }
}

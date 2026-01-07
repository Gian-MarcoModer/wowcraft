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
     * Register mob pack templates for all biome types.
     */
    private static void registerDefaultTemplates() {
        // === PLAINS PACKS ===
        TEMPLATES.put("bandit_camp", new MobPackTemplate(
                "bandit_camp",
                Set.of(BiomeGroup.PLAINS),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:pillager"), 1, 2, 60),
                        new MobEntry(ResourceLocation.parse("minecraft:vindicator"), 0, 1, 40)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // Note: Wolves use vanilla spawning now - they are not hostile by default

        TEMPLATES.put("zombie_pack", new MobPackTemplate(
                "zombie_pack",
                Set.of(BiomeGroup.PLAINS, BiomeGroup.FOREST),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 1, 3, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:husk"), 0, 1, 30)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === SNOWY PLAINS PACKS ===
        TEMPLATES.put("frost_skeleton_pack", new MobPackTemplate(
                "frost_skeleton_pack",
                Set.of(BiomeGroup.SNOWY_PLAINS, BiomeGroup.MOUNTAIN),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:stray"), 2, 3, 100)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // Note: Polar bears use vanilla spawning now

        // === FOREST PACKS ===
        TEMPLATES.put("skeleton_pack", new MobPackTemplate(
                "skeleton_pack",
                Set.of(BiomeGroup.FOREST, BiomeGroup.TAIGA, BiomeGroup.DARK_FOREST),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:skeleton"), 2, 3, 80),
                        new MobEntry(ResourceLocation.parse("minecraft:stray"), 0, 1, 20)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("spider_nest", new MobPackTemplate(
                "spider_nest",
                Set.of(BiomeGroup.FOREST, BiomeGroup.SWAMP, BiomeGroup.DARK_FOREST),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:spider"), 2, 4, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:cave_spider"), 0, 2, 30)),
                3, 5,
                6.0f,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("forest_creeper_pack", new MobPackTemplate(
                "forest_creeper_pack",
                Set.of(BiomeGroup.FOREST, BiomeGroup.CHERRY_GROVE),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:creeper"), 1, 3, 100)),
                2, 3,
                5.0f, // Creepers don't coordinate as much
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === DARK FOREST PACKS ===
        TEMPLATES.put("dark_zombie_horde", new MobPackTemplate(
                "dark_zombie_horde",
                Set.of(BiomeGroup.DARK_FOREST),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 2, 4, 60),
                        new MobEntry(ResourceLocation.parse("minecraft:zombie_villager"), 0, 1, 40)),
                3, 5,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("witch_coven", new MobPackTemplate(
                "witch_coven",
                Set.of(BiomeGroup.DARK_FOREST, BiomeGroup.SWAMP),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:witch"), 1, 2, 100)),
                1, 3,
                12.0f, // Witches support each other at range
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === TAIGA PACKS ===
        // Note: Wolves use vanilla spawning now

        TEMPLATES.put("frozen_undead", new MobPackTemplate(
                "frozen_undead",
                Set.of(BiomeGroup.TAIGA),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:stray"), 1, 2, 60),
                        new MobEntry(ResourceLocation.parse("minecraft:skeleton"), 0, 2, 40)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === JUNGLE PACKS ===
        TEMPLATES.put("jungle_spider_nest", new MobPackTemplate(
                "jungle_spider_nest",
                Set.of(BiomeGroup.JUNGLE),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:spider"), 2, 3, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:cave_spider"), 1, 2, 30)),
                3, 5,
                6.0f,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("jungle_zombie_pack", new MobPackTemplate(
                "jungle_zombie_pack",
                Set.of(BiomeGroup.JUNGLE),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 2, 4, 100)),
                2, 5,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === DESERT PACKS ===
        TEMPLATES.put("desert_husk_pack", new MobPackTemplate(
                "desert_husk_pack",
                Set.of(BiomeGroup.DESERT),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:husk"), 2, 4, 100)),
                3, 5,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("desert_bandit_camp", new MobPackTemplate(
                "desert_bandit_camp",
                Set.of(BiomeGroup.DESERT),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:pillager"), 1, 2, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:vindicator"), 0, 1, 30)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === SAVANNA PACKS ===
        TEMPLATES.put("savanna_creeper_pack", new MobPackTemplate(
                "savanna_creeper_pack",
                Set.of(BiomeGroup.SAVANNA),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:creeper"), 1, 3, 100)),
                2, 4,
                5.0f,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("savanna_skeleton_pack", new MobPackTemplate(
                "savanna_skeleton_pack",
                Set.of(BiomeGroup.SAVANNA),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:skeleton"), 2, 3, 100)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === BADLANDS PACKS ===
        TEMPLATES.put("badlands_husk_pack", new MobPackTemplate(
                "badlands_husk_pack",
                Set.of(BiomeGroup.BADLANDS),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:husk"), 2, 4, 80),
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 0, 1, 20)),
                3, 5,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("canyon_spider_pack", new MobPackTemplate(
                "canyon_spider_pack",
                Set.of(BiomeGroup.BADLANDS),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:spider"), 2, 3, 100)),
                2, 4,
                6.0f,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === SWAMP PACKS ===
        TEMPLATES.put("swamp_zombie_pack", new MobPackTemplate(
                "swamp_zombie_pack",
                Set.of(BiomeGroup.SWAMP),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:drowned"), 1, 2, 60),
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 1, 2, 40)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("slime_pool", new MobPackTemplate(
                "slime_pool",
                Set.of(BiomeGroup.SWAMP),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:slime"), 2, 4, 100)),
                3, 6,
                8.0f,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === MOUNTAIN & WINDSWEPT PACKS ===
        // Note: Goats use vanilla spawning now - they are passive

        TEMPLATES.put("windswept_skeleton_pack", new MobPackTemplate(
                "windswept_skeleton_pack",
                Set.of(BiomeGroup.WINDSWEPT),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:skeleton"), 2, 3, 70),
                        new MobEntry(ResourceLocation.parse("minecraft:creeper"), 0, 1, 30)),
                2, 4,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        // === CHERRY GROVE PACKS ===
        // Note: Bees use vanilla spawning now - they are not hostile by default

        // === MUSHROOM ISLAND PACKS ===
        // Note: Mooshrooms use vanilla spawning now - they are passive

        // === CAVE PACKS ===
        TEMPLATES.put("cave_zombie_pack", new MobPackTemplate(
                "cave_zombie_pack",
                Set.of(BiomeGroup.CAVE),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:zombie"), 2, 4, 100)),
                2, 5,
                MobPackTemplate.DEFAULT_SOCIAL_AGGRO_RADIUS,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("cave_spider_nest", new MobPackTemplate(
                "cave_spider_nest",
                Set.of(BiomeGroup.CAVE),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:cave_spider"), 3, 5, 100)),
                4, 7,
                6.0f,
                MobPackTemplate.DEFAULT_RESPAWN_DELAY));

        TEMPLATES.put("deep_dark_warden", new MobPackTemplate(
                "deep_dark_warden",
                Set.of(BiomeGroup.CAVE),
                List.of(
                        new MobEntry(ResourceLocation.parse("minecraft:warden"), 1, 1, 100)),
                1, 1,
                20.0f, // Warden has massive aggro range
                600)); // 10 minute respawn for warden
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

package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * Generates creative, context-aware names for pack mobs based on their type and biome.
 * Names are more interesting than "Plains Zombie" - they evoke the character of the area.
 */
public class MobNameGenerator {

    // Name templates for each mob type and biome combination
    private static final Map<BiomeGroup, Map<String, List<String>>> NAMES = new HashMap<>();

    static {
        initializeNames();
    }

    /**
     * Get a creative name for a mob based on its type and biome.
     */
    public static String getName(ResourceLocation mobType, BiomeGroup biome) {
        String mobPath = mobType.getPath();

        // Try biome-specific names first
        Map<String, List<String>> biomeNames = NAMES.get(biome);
        if (biomeNames != null) {
            List<String> nameList = biomeNames.get(mobPath);
            if (nameList != null && !nameList.isEmpty()) {
                // Pick randomly from the list
                return nameList.get(new Random().nextInt(nameList.size()));
            }
        }

        // Try default fallback names (stored under null key)
        Map<String, List<String>> defaultNames = NAMES.get(null);
        if (defaultNames != null) {
            List<String> nameList = defaultNames.get(mobPath);
            if (nameList != null && !nameList.isEmpty()) {
                return nameList.get(new Random().nextInt(nameList.size()));
            }
        }

        // Last resort: capitalize and clean up mob name
        return capitalize(mobPath.replace("_", " "));
    }

    private static String capitalize(String str) {
        String[] words = str.split(" ");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0)))
                      .append(word.substring(1))
                      .append(" ");
            }
        }
        return result.toString().trim();
    }

    /**
     * Initialize all mob names for each biome.
     */
    private static void initializeNames() {
        // === PLAINS ===
        Map<String, List<String>> plainsNames = new HashMap<>();
        plainsNames.put("zombie", List.of("Grassland Shambler", "Field Wanderer", "Prairie Corpse", "Meadow Lurker"));
        plainsNames.put("skeleton", List.of("Grassland Archer", "Field Bones", "Prairie Sentinel", "Meadow Marksman"));
        plainsNames.put("pillager", List.of("Plains Marauder", "Grassland Raider", "Field Bandit", "Prairie Outlaw"));
        plainsNames.put("vindicator", List.of("Plains Enforcer", "Grassland Brute", "Meadow Thug", "Field Axeman"));
        plainsNames.put("husk", List.of("Sunbaked Wanderer", "Withered Shambler", "Dried Corpse"));
        NAMES.put(BiomeGroup.PLAINS, plainsNames);

        // === FOREST ===
        Map<String, List<String>> forestNames = new HashMap<>();
        forestNames.put("zombie", List.of("Woodland Shambler", "Forest Lurker", "Timber Corpse", "Grove Walker"));
        forestNames.put("husk", List.of("Withered Walker", "Dried Shambler", "Forest Husk"));
        forestNames.put("skeleton", List.of("Woodland Archer", "Forest Bones", "Timber Sentinel", "Grove Marksman"));
        forestNames.put("spider", List.of("Forest Spinner", "Woodland Stalker", "Canopy Hunter", "Grove Crawler"));
        forestNames.put("cave_spider", List.of("Forest Hatchling", "Small Spinner", "Venomous Stalker"));
        forestNames.put("stray", List.of("Woodland Phantom", "Forest Shade", "Timber Wraith"));
        forestNames.put("creeper", List.of("Forest Creeper", "Woodland Skulker", "Moss Creeper", "Grove Stalker"));
        NAMES.put(BiomeGroup.FOREST, forestNames);

        // === DARK FOREST ===
        Map<String, List<String>> darkForestNames = new HashMap<>();
        darkForestNames.put("zombie", List.of("Shadow Shambler", "Dark Lurker", "Twilight Corpse", "Gloom Walker"));
        darkForestNames.put("zombie_villager", List.of("Lost Villager", "Corrupted Wanderer", "Forsaken Soul"));
        darkForestNames.put("skeleton", List.of("Shadow Archer", "Dark Bones", "Twilight Sentinel", "Gloom Marksman"));
        darkForestNames.put("spider", List.of("Shadow Spinner", "Twilight Stalker", "Dark Crawler", "Gloom Hunter"));
        darkForestNames.put("cave_spider", List.of("Dark Hatchling", "Shadow Creeper", "Gloom Stalker"));
        darkForestNames.put("witch", List.of("Coven Witch", "Shadow Hexer", "Dark Enchantress", "Gloom Sorceress"));
        NAMES.put(BiomeGroup.DARK_FOREST, darkForestNames);

        // === TAIGA ===
        Map<String, List<String>> taigaNames = new HashMap<>();
        taigaNames.put("skeleton", List.of("Tundra Archer", "Frostwood Bones", "Northern Sentinel", "Boreal Marksman"));
        taigaNames.put("stray", List.of("Frostbitten Archer", "Frozen Hunter", "Ice Phantom", "Glacial Wraith"));
        taigaNames.put("zombie", List.of("Frozen Shambler", "Tundra Corpse", "Boreal Walker"));
        taigaNames.put("creeper", List.of("Snow Creeper", "Tundra Stalker", "Frozen Skulker"));
        NAMES.put(BiomeGroup.TAIGA, taigaNames);

        // === SNOWY PLAINS ===
        Map<String, List<String>> snowyPlainsNames = new HashMap<>();
        snowyPlainsNames.put("stray", List.of("Frost Archer", "Tundra Phantom", "Snowfield Wraith", "Frozen Sentinel"));
        snowyPlainsNames.put("skeleton", List.of("Frost Bones", "Snowfield Archer", "Ice Sentinel"));
        snowyPlainsNames.put("zombie", List.of("Frost Shambler", "Snowfield Corpse", "Frozen Walker"));
        NAMES.put(BiomeGroup.SNOWY_PLAINS, snowyPlainsNames);

        // === MOUNTAIN ===
        Map<String, List<String>> mountainNames = new HashMap<>();
        mountainNames.put("skeleton", List.of("Highland Archer", "Peak Bones", "Summit Sentinel", "Alpine Marksman"));
        mountainNames.put("stray", List.of("Highland Phantom", "Peak Wraith", "Alpine Shade", "Summit Hunter"));
        mountainNames.put("creeper", List.of("Cliff Skulker", "Highland Creeper", "Summit Stalker"));
        NAMES.put(BiomeGroup.MOUNTAIN, mountainNames);

        // === WINDSWEPT ===
        Map<String, List<String>> windsweptNames = new HashMap<>();
        windsweptNames.put("skeleton", List.of("Windswept Archer", "Storm Bones", "Gale Sentinel", "Wind Runner"));
        windsweptNames.put("creeper", List.of("Storm Creeper", "Wind Stalker", "Gale Skulker"));
        NAMES.put(BiomeGroup.WINDSWEPT, windsweptNames);

        // === JUNGLE ===
        Map<String, List<String>> jungleNames = new HashMap<>();
        jungleNames.put("zombie", List.of("Jungle Shambler", "Vine Walker", "Tropical Corpse", "Rainforest Lurker"));
        jungleNames.put("spider", List.of("Canopy Weaver", "Jungle Stalker", "Vine Crawler", "Tropical Hunter"));
        jungleNames.put("cave_spider", List.of("Jungle Hatchling", "Venomous Creeper", "Small Stalker"));
        NAMES.put(BiomeGroup.JUNGLE, jungleNames);

        // === DESERT ===
        Map<String, List<String>> desertNames = new HashMap<>();
        desertNames.put("husk", List.of("Dune Stroller", "Sand Walker", "Desert Corpse", "Arid Shambler", "Wasteland Wanderer"));
        desertNames.put("pillager", List.of("Dune Raider", "Sand Bandit", "Desert Marauder", "Wasteland Outlaw"));
        desertNames.put("vindicator", List.of("Dune Enforcer", "Sand Brute", "Desert Axeman", "Wasteland Thug"));
        NAMES.put(BiomeGroup.DESERT, desertNames);

        // === SAVANNA ===
        Map<String, List<String>> savannaNames = new HashMap<>();
        savannaNames.put("skeleton", List.of("Savanna Archer", "Grassland Bones", "Dry Plains Sentinel", "Acacia Marksman"));
        savannaNames.put("creeper", List.of("Savanna Stalker", "Dry Plains Creeper", "Grassland Skulker"));
        NAMES.put(BiomeGroup.SAVANNA, savannaNames);

        // === BADLANDS ===
        Map<String, List<String>> badlandsNames = new HashMap<>();
        badlandsNames.put("husk", List.of("Canyon Walker", "Mesa Corpse", "Rust Shambler", "Terracotta Wanderer"));
        badlandsNames.put("zombie", List.of("Badlands Shambler", "Canyon Corpse", "Red Rock Lurker"));
        badlandsNames.put("spider", List.of("Canyon Crawler", "Mesa Stalker", "Cliff Spinner", "Red Rock Hunter"));
        NAMES.put(BiomeGroup.BADLANDS, badlandsNames);

        // === SWAMP ===
        Map<String, List<String>> swampNames = new HashMap<>();
        swampNames.put("zombie", List.of("Bog Shambler", "Swamp Walker", "Marsh Corpse", "Mire Lurker"));
        swampNames.put("drowned", List.of("Swamp Drowned", "Bog Drifter", "Marsh Swimmer", "Mire Corpse"));
        swampNames.put("spider", List.of("Bog Spinner", "Swamp Crawler", "Marsh Stalker", "Mire Hunter"));
        swampNames.put("witch", List.of("Swamp Witch", "Bog Hexer", "Marsh Enchantress", "Mire Sorceress"));
        swampNames.put("slime", List.of("Bog Ooze", "Swamp Slime", "Marsh Blob", "Mire Glob"));
        NAMES.put(BiomeGroup.SWAMP, swampNames);

        // === CHERRY GROVE ===
        Map<String, List<String>> cherryNames = new HashMap<>();
        cherryNames.put("creeper", List.of("Blossom Creeper", "Cherry Stalker", "Petal Skulker", "Grove Creeper"));
        NAMES.put(BiomeGroup.CHERRY_GROVE, cherryNames);

        // === CAVE (Deep, dark, underground) ===
        Map<String, List<String>> caveNames = new HashMap<>();
        caveNames.put("zombie", List.of("Cave Shambler", "Tunnel Lurker", "Underground Corpse", "Depths Walker"));
        caveNames.put("skeleton", List.of("Cave Archer", "Tunnel Bones", "Underground Sentinel", "Depths Marksman"));
        caveNames.put("spider", List.of("Cave Weaver", "Tunnel Stalker", "Underground Crawler", "Depths Hunter"));
        caveNames.put("cave_spider", List.of("Small Cave Stalker", "Tunnel Hatchling", "Venomous Crawler"));
        caveNames.put("creeper", List.of("Cave Creeper", "Tunnel Stalker", "Underground Skulker"));
        caveNames.put("warden", List.of("Deep Guardian", "Depths Warden", "Ancient Sentinel"));
        NAMES.put(BiomeGroup.CAVE, caveNames);

        // === DEFAULT/FALLBACK NAMES (for any biome) ===
        // These are used as a last resort for mob/biome combinations not explicitly defined
        Map<String, List<String>> defaultNames = new HashMap<>();
        defaultNames.put("zombie", List.of("Wandering Corpse", "Lost Shambler", "Undead Walker"));
        defaultNames.put("zombie_villager", List.of("Corrupted Villager", "Lost Soul", "Forsaken One"));
        defaultNames.put("husk", List.of("Dried Corpse", "Withered Walker", "Desert Husk"));
        defaultNames.put("drowned", List.of("Waterlogged Corpse", "Drowned Wanderer", "Sodden Shambler"));
        defaultNames.put("skeleton", List.of("Bone Archer", "Skeletal Warrior", "Undead Marksman"));
        defaultNames.put("stray", List.of("Frozen Archer", "Icy Phantom", "Frost Bones"));
        defaultNames.put("spider", List.of("Web Spinner", "Eight-Legged Stalker", "Arachnid Hunter"));
        defaultNames.put("cave_spider", List.of("Venomous Stalker", "Small Spider", "Toxic Creeper"));
        defaultNames.put("creeper", List.of("Explosive Stalker", "Silent Hunter", "Green Menace"));
        defaultNames.put("witch", List.of("Dark Witch", "Potion Brewer", "Hexer"));
        defaultNames.put("pillager", List.of("Armed Raider", "Crossbow Bandit", "Outlaw"));
        defaultNames.put("vindicator", List.of("Axe Warrior", "Iron Brute", "Heavy Fighter"));
        defaultNames.put("slime", List.of("Gelatinous Blob", "Bouncing Ooze", "Slime Creature"));
        // Store as special entry that getName() can fallback to
        NAMES.put(null, defaultNames);
    }
}

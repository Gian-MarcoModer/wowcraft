package com.gianmarco.wowcraft.zone;

import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Groups Minecraft biomes into logical zone categories for naming purposes.
 * Only Overworld biomes are included - Nether and End dimensions are excluded.
 */
public enum BiomeGroup {
    // === FLATLAND BIOMES ===
    PLAINS("Plains"),
    SNOWY_PLAINS("Snowy Plains"),

    // === WOODLAND BIOMES ===
    FOREST("Forest"),
    DARK_FOREST("Dark Forest"),
    TAIGA("Taiga"),
    JUNGLE("Jungle"),
    CHERRY_GROVE("Cherry Grove"),

    // === HIGHLAND BIOMES ===
    MOUNTAIN("Mountain"),
    WINDSWEPT("Windswept Hills"),

    // === ARID BIOMES ===
    DESERT("Desert"),
    SAVANNA("Savanna"),
    BADLANDS("Badlands"),

    // === WETLAND BIOMES ===
    SWAMP("Swamp"),

    // === SPECIAL BIOMES ===
    MUSHROOM("Mushroom Island"),
    CAVE("Cave"),

    // === EXCLUDED FROM NAMING (used for lookup but won't get zone names) ===
    BEACH("Beach"),
    RIVER("River"),
    OCEAN("Ocean");

    private final String displayName;

    BiomeGroup(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns true if this biome group should receive named zones.
     * Rivers, beaches, and oceans are excluded as they act as boundaries or are too
     * large.
     */
    public boolean isNameable() {
        return this != BEACH && this != RIVER && this != OCEAN;
    }

    /**
     * Maps a Minecraft biome to its corresponding BiomeGroup.
     * Returns null for Nether and End biomes (not supported).
     */
    public static BiomeGroup fromBiome(ResourceKey<Biome> biome) {
        if (biome == null)
            return null;

        String biomeName = biome.location().getPath();

        // === FLATLAND ===
        if (biomeName.equals("plains") || biomeName.equals("sunflower_plains") || biomeName.equals("meadow")) {
            return PLAINS;
        }
        if (biomeName.equals("snowy_plains") || biomeName.equals("ice_spikes")) {
            return SNOWY_PLAINS;
        }

        // === WOODLAND ===
        if (biomeName.equals("forest") || biomeName.equals("flower_forest") ||
                biomeName.equals("birch_forest") || biomeName.equals("old_growth_birch_forest")) {
            return FOREST;
        }
        if (biomeName.equals("dark_forest") || biomeName.equals("pale_garden")) {
            return DARK_FOREST;
        }
        if (biomeName.equals("taiga") || biomeName.equals("old_growth_pine_taiga") ||
                biomeName.equals("old_growth_spruce_taiga") || biomeName.equals("snowy_taiga")) {
            return TAIGA;
        }
        if (biomeName.equals("jungle") || biomeName.equals("sparse_jungle") || biomeName.equals("bamboo_jungle")) {
            return JUNGLE;
        }
        if (biomeName.equals("cherry_grove")) {
            return CHERRY_GROVE;
        }

        // === HIGHLAND ===
        if (biomeName.equals("jagged_peaks") || biomeName.equals("frozen_peaks") ||
                biomeName.equals("stony_peaks") || biomeName.equals("snowy_slopes") || biomeName.equals("grove")) {
            return MOUNTAIN;
        }
        if (biomeName.equals("windswept_hills") || biomeName.equals("windswept_gravelly_hills") ||
                biomeName.equals("windswept_forest") || biomeName.equals("windswept_savanna")) {
            return WINDSWEPT;
        }

        // === ARID ===
        if (biomeName.equals("desert")) {
            return DESERT;
        }
        if (biomeName.equals("savanna") || biomeName.equals("savanna_plateau")) {
            return SAVANNA;
        }
        if (biomeName.equals("badlands") || biomeName.equals("wooded_badlands")
                || biomeName.equals("eroded_badlands")) {
            return BADLANDS;
        }

        // === WETLAND ===
        if (biomeName.equals("swamp") || biomeName.equals("mangrove_swamp")) {
            return SWAMP;
        }

        // === SPECIAL ===
        if (biomeName.equals("mushroom_fields")) {
            return MUSHROOM;
        }
        if (biomeName.equals("dripstone_caves") || biomeName.equals("lush_caves") || biomeName.equals("deep_dark")) {
            return CAVE;
        }

        // === EXCLUDED (boundaries/transitions) ===
        if (biomeName.equals("beach") || biomeName.equals("snowy_beach") || biomeName.equals("stony_shore")) {
            return BEACH;
        }
        if (biomeName.equals("river") || biomeName.equals("frozen_river")) {
            return RIVER;
        }
        if (biomeName.contains("ocean")) {
            return OCEAN;
        }

        // Nether and End biomes return null (not supported)
        if (biomeName.contains("nether") || biomeName.contains("soul_sand") ||
                biomeName.contains("crimson") || biomeName.contains("warped") ||
                biomeName.contains("basalt_deltas") || biomeName.contains("end")) {
            return null;
        }

        // Unknown biome - default to null
        return null;
    }
}

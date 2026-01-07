package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.zone.BiomeGroup;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Provides mob options based on biome and hostility type.
 * Uses custom pack entities for hostile mobs.
 */
public class MobOptionProvider {

    private static ResourceLocation rl(String path) {
        return ResourceLocation.parse("minecraft:" + path);
    }

    /**
     * Get mob options for a given biome and hostility type.
     */
    public static List<MobOption> getMobOptions(BiomeGroup biome, SpawnHostility hostility) {
        return switch (hostility) {
            case ALWAYS_HOSTILE -> getHostileMobs(biome);
            case NEUTRAL_DEFENSIVE -> getNeutralMobs(biome);
            case NEUTRAL_TERRITORIAL -> getTerritorialMobs(biome);
            case PASSIVE -> getPassiveMobs(biome);
        };
    }

    /**
     * Hostile mobs - attack on sight.
     * These use our custom pack entities.
     */
    public static List<MobOption> getHostileMobs(BiomeGroup biome) {
        return switch (biome) {
            case PLAINS -> List.of(
                new MobOption(rl("zombie"), 40, 1, 2),
                new MobOption(rl("skeleton"), 35, 1, 2),
                new MobOption(rl("creeper"), 15, 1, 1),
                new MobOption(rl("pillager"), 10, 1, 2)
            );

            case FOREST -> List.of(
                new MobOption(rl("skeleton"), 40, 1, 2),
                new MobOption(rl("spider"), 35, 1, 3),
                new MobOption(rl("zombie"), 25, 1, 2)
            );

            case DARK_FOREST -> List.of(
                new MobOption(rl("zombie"), 35, 2, 3),
                new MobOption(rl("skeleton"), 30, 1, 2),
                new MobOption(rl("witch"), 20, 1, 2),
                new MobOption(rl("spider"), 15, 2, 3)
            );

            case TAIGA -> List.of(
                new MobOption(rl("stray"), 50, 1, 2),
                new MobOption(rl("skeleton"), 30, 1, 2),
                new MobOption(rl("spider"), 20, 1, 2)
            );

            case JUNGLE -> List.of(
                new MobOption(rl("zombie"), 40, 2, 4),
                new MobOption(rl("spider"), 35, 2, 3),
                new MobOption(rl("cave_spider"), 25, 1, 2)
            );

            case DESERT -> List.of(
                new MobOption(rl("husk"), 60, 2, 4),
                new MobOption(rl("skeleton"), 25, 1, 2),
                new MobOption(rl("pillager"), 15, 1, 2)
            );

            case SAVANNA -> List.of(
                new MobOption(rl("creeper"), 40, 1, 2),
                new MobOption(rl("skeleton"), 35, 2, 3),
                new MobOption(rl("zombie"), 25, 1, 2)
            );

            case BADLANDS -> List.of(
                new MobOption(rl("husk"), 50, 2, 4),
                new MobOption(rl("spider"), 30, 2, 3),
                new MobOption(rl("zombie"), 20, 1, 2)
            );

            case SWAMP -> List.of(
                new MobOption(rl("drowned"), 40, 1, 2),
                new MobOption(rl("zombie"), 30, 1, 2),
                new MobOption(rl("witch"), 20, 1, 2),
                new MobOption(rl("slime"), 10, 2, 4)
            );

            case SNOWY_PLAINS, MOUNTAIN, WINDSWEPT -> List.of(
                new MobOption(rl("stray"), 50, 2, 3),
                new MobOption(rl("skeleton"), 30, 1, 2),
                new MobOption(rl("creeper"), 20, 1, 1)
            );

            case CAVE -> List.of(
                new MobOption(rl("zombie"), 40, 2, 4),
                new MobOption(rl("skeleton"), 30, 2, 3),
                new MobOption(rl("spider"), 20, 2, 3),
                new MobOption(rl("cave_spider"), 10, 3, 5)
            );

            default -> List.of(
                new MobOption(rl("zombie"), 50, 1, 2),
                new MobOption(rl("skeleton"), 30, 1, 2),
                new MobOption(rl("creeper"), 20, 1, 1)
            );
        };
    }

    /**
     * Neutral mobs - only attack if provoked.
     * These use vanilla entities (wolves, iron golems, etc.)
     */
    public static List<MobOption> getNeutralMobs(BiomeGroup biome) {
        return switch (biome) {
            case PLAINS -> List.of(
                new MobOption(rl("wolf"), 50, 2, 4),
                new MobOption(rl("iron_golem"), 30, 1, 1),
                new MobOption(rl("llama"), 20, 2, 3)
            );

            case FOREST, TAIGA, DARK_FOREST -> List.of(
                new MobOption(rl("wolf"), 70, 2, 5),
                new MobOption(rl("iron_golem"), 30, 1, 1)
            );

            case JUNGLE -> List.of(
                new MobOption(rl("panda"), 50, 1, 2),
                new MobOption(rl("ocelot"), 30, 1, 2),
                new MobOption(rl("iron_golem"), 20, 1, 1)
            );

            case DESERT, BADLANDS -> List.of(
                new MobOption(rl("llama"), 40, 2, 3),
                new MobOption(rl("iron_golem"), 30, 1, 1),
                new MobOption(rl("wolf"), 30, 2, 3)
            );

            case SAVANNA -> List.of(
                new MobOption(rl("llama"), 50, 2, 4),
                new MobOption(rl("wolf"), 30, 2, 3),
                new MobOption(rl("iron_golem"), 20, 1, 1)
            );

            case SNOWY_PLAINS, MOUNTAIN -> List.of(
                new MobOption(rl("wolf"), 60, 2, 4),
                new MobOption(rl("fox"), 40, 2, 3)
            );

            case SWAMP -> List.of(
                new MobOption(rl("iron_golem"), 60, 1, 1),
                new MobOption(rl("frog"), 40, 2, 4)
            );

            default -> List.of(
                new MobOption(rl("wolf"), 60, 2, 4),
                new MobOption(rl("iron_golem"), 40, 1, 1)
            );
        };
    }

    /**
     * Territorial mobs - attack if player gets too close.
     * Currently using endermen and some neutral mobs.
     */
    public static List<MobOption> getTerritorialMobs(BiomeGroup biome) {
        return switch (biome) {
            case DESERT, BADLANDS, SAVANNA -> List.of(
                new MobOption(rl("enderman"), 60, 1, 2),
                new MobOption(rl("iron_golem"), 40, 1, 1)
            );

            case SWAMP -> List.of(
                new MobOption(rl("enderman"), 70, 1, 2),
                new MobOption(rl("slime"), 30, 2, 3)
            );

            default -> List.of(
                new MobOption(rl("enderman"), 80, 1, 2),
                new MobOption(rl("iron_golem"), 20, 1, 1)
            );
        };
    }

    /**
     * Passive mobs - never attack.
     * These use vanilla spawning mostly, but can supplement with our system.
     */
    public static List<MobOption> getPassiveMobs(BiomeGroup biome) {
        return switch (biome) {
            case PLAINS -> List.of(
                new MobOption(rl("cow"), 40, 2, 4),
                new MobOption(rl("sheep"), 30, 2, 4),
                new MobOption(rl("chicken"), 20, 3, 6),
                new MobOption(rl("pig"), 10, 2, 3)
            );

            case FOREST -> List.of(
                new MobOption(rl("rabbit"), 40, 2, 3),
                new MobOption(rl("chicken"), 30, 2, 4),
                new MobOption(rl("cow"), 20, 2, 3),
                new MobOption(rl("sheep"), 10, 2, 3)
            );

            case DARK_FOREST -> List.of(
                new MobOption(rl("rabbit"), 50, 2, 4),
                new MobOption(rl("chicken"), 30, 2, 3),
                new MobOption(rl("mushroom"), 20, 1, 2)
            );

            case TAIGA -> List.of(
                new MobOption(rl("rabbit"), 50, 2, 4),
                new MobOption(rl("fox"), 30, 1, 2),
                new MobOption(rl("chicken"), 20, 2, 3)
            );

            case JUNGLE -> List.of(
                new MobOption(rl("parrot"), 40, 2, 4),
                new MobOption(rl("chicken"), 30, 2, 4),
                new MobOption(rl("ocelot"), 30, 1, 2)
            );

            case DESERT, BADLANDS -> List.of(
                new MobOption(rl("rabbit"), 60, 2, 3),
                new MobOption(rl("chicken"), 40, 2, 3)
            );

            case SAVANNA -> List.of(
                new MobOption(rl("cow"), 40, 2, 4),
                new MobOption(rl("chicken"), 30, 2, 4),
                new MobOption(rl("sheep"), 30, 2, 3)
            );

            case SNOWY_PLAINS, MOUNTAIN -> List.of(
                new MobOption(rl("rabbit"), 50, 2, 4),
                new MobOption(rl("fox"), 30, 1, 2),
                new MobOption(rl("polar_bear"), 20, 1, 2)
            );

            case SWAMP -> List.of(
                new MobOption(rl("frog"), 60, 2, 4),
                new MobOption(rl("chicken"), 40, 2, 3)
            );

            default -> List.of(
                new MobOption(rl("chicken"), 40, 2, 4),
                new MobOption(rl("cow"), 30, 2, 3),
                new MobOption(rl("sheep"), 30, 2, 3)
            );
        };
    }

    /**
     * Get elite mob options (for POI camps and lairs).
     * Higher health/damage, fewer but stronger mobs.
     */
    public static List<MobOption> getEliteMobs(BiomeGroup biome) {
        return switch (biome) {
            case PLAINS -> List.of(
                new MobOption(rl("vindicator"), 50, 2, 3),
                new MobOption(rl("pillager"), 30, 2, 4),
                new MobOption(rl("zombie"), 20, 3, 5)
            );

            case FOREST, DARK_FOREST -> List.of(
                new MobOption(rl("witch"), 40, 1, 2),
                new MobOption(rl("vindicator"), 30, 2, 3),
                new MobOption(rl("spider"), 30, 3, 5)
            );

            case DESERT, BADLANDS -> List.of(
                new MobOption(rl("husk"), 50, 3, 5),
                new MobOption(rl("vindicator"), 30, 2, 3),
                new MobOption(rl("pillager"), 20, 2, 4)
            );

            case SWAMP -> List.of(
                new MobOption(rl("witch"), 50, 1, 3),
                new MobOption(rl("drowned"), 30, 2, 4),
                new MobOption(rl("slime"), 20, 3, 6)
            );

            case SNOWY_PLAINS, TAIGA, MOUNTAIN -> List.of(
                new MobOption(rl("stray"), 50, 3, 4),
                new MobOption(rl("vindicator"), 30, 2, 3),
                new MobOption(rl("pillager"), 20, 2, 3)
            );

            default -> List.of(
                new MobOption(rl("zombie"), 40, 3, 5),
                new MobOption(rl("skeleton"), 30, 3, 4),
                new MobOption(rl("vindicator"), 30, 2, 3)
            );
        };
    }
}

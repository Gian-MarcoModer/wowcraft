package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import java.util.Set;

/**
 * Calculates mob levels based on biome zones.
 * 
 * Tiered zone progression (slower at higher levels):
 * - Zone 0-2 (Lv 1-9): Every 2 biomes = 1 zone tier
 * - Zone 3-5 (Lv 10-18): Every 3 biomes = 1 zone tier
 * - Zone 6+ (Lv 19-30): Every 4 biomes = 1 zone tier
 * 
 * Special biomes: Fixed or minimum levels for dangerous areas.
 */
public class BiomeZoneCalculator {

    // Zone configuration
    private static final int LEVELS_PER_ZONE = 3;
    private static final int SAMPLE_INTERVAL = 16; // Check biome every 16 blocks along raycast
    private static final int MAX_LEVEL = 30;

    // Tiered progression: biomes needed per zone at different tiers
    private static final int BIOMES_PER_ZONE_EARLY = 2;   // Zone 0-2 (levels 1-9)
    private static final int BIOMES_PER_ZONE_MID = 3;     // Zone 3-5 (levels 10-18)
    private static final int BIOMES_PER_ZONE_LATE = 4;    // Zone 6+ (levels 19-30)

    // Special biome level overrides
    private static final int DEEP_DARK_BASE_LEVEL = 25;
    private static final int END_BASE_LEVEL = 28;
    private static final int NETHER_MIN_LEVEL = 15;

    // End biomes (fixed high level)
    private static final Set<String> END_BIOMES = Set.of(
            "minecraft:the_end",
            "minecraft:end_highlands",
            "minecraft:end_midlands",
            "minecraft:end_barrens",
            "minecraft:small_end_islands"
    );

    // Nether biomes (minimum level 15)
    private static final Set<String> NETHER_BIOMES = Set.of(
            "minecraft:nether_wastes",
            "minecraft:soul_sand_valley",
            "minecraft:crimson_forest",
            "minecraft:warped_forest",
            "minecraft:basalt_deltas"
    );

    /**
     * Calculate mob level based on position and biome.
     */
    public static int calculateLevel(Level world, BlockPos mobPos, RandomSource random) {
        Holder<Biome> biomeHolder = world.getBiome(mobPos);
        String biomeId = getBiomeId(biomeHolder);

        // Check special biomes first (these override normal calculation)
        if (isDeepDark(biomeId)) {
            int level = DEEP_DARK_BASE_LEVEL + random.nextInt(4); // 25-28
            WowCraft.LOGGER.debug("Deep Dark biome detected, level: {}", level);
            return Math.min(level, MAX_LEVEL);
        }

        if (isEndBiome(biomeId)) {
            int level = END_BASE_LEVEL + random.nextInt(3); // 28-30
            WowCraft.LOGGER.debug("End biome detected, level: {}", level);
            return Math.min(level, MAX_LEVEL);
        }

        // Normal zone calculation with tiered progression
        BlockPos spawn = getWorldSpawn(world);
        int biomeTransitions = countBiomeTransitions(world, spawn, mobPos);
        int zoneTier = calculateZoneTier(biomeTransitions);
        int baseLevel = (zoneTier * LEVELS_PER_ZONE) + 1;
        int level = baseLevel + random.nextInt(3); // +0 to +2 variation

        WowCraft.LOGGER.debug("Zone calculation: {} transitions, tier {}, base level {}", 
                biomeTransitions, zoneTier, baseLevel);

        // Apply Nether minimum
        if (isNetherBiome(biomeId)) {
            level = Math.max(NETHER_MIN_LEVEL, level);
            WowCraft.LOGGER.debug("Nether biome detected, enforcing min level 15, final: {}", level);
        }

        return Math.min(level, MAX_LEVEL);
    }

    /**
     * Count how many biome borders exist between spawn and target position.
     * Uses raycast sampling to detect biome changes.
     */
    public static int countBiomeTransitions(Level world, BlockPos spawn, BlockPos target) {
        int transitions = 0;

        // Calculate distance and direction
        double dx = target.getX() - spawn.getX();
        double dz = target.getZ() - spawn.getZ();
        double distance = Math.sqrt(dx * dx + dz * dz);

        if (distance < SAMPLE_INTERVAL) {
            return 0; // Too close, same zone
        }

        // Normalize direction
        double dirX = dx / distance;
        double dirZ = dz / distance;

        // Sample along the line
        int samples = (int) (distance / SAMPLE_INTERVAL);
        String lastBiomeId = getBiomeId(world.getBiome(spawn));

        for (int i = 1; i <= samples; i++) {
            int x = spawn.getX() + (int) (dirX * i * SAMPLE_INTERVAL);
            int z = spawn.getZ() + (int) (dirZ * i * SAMPLE_INTERVAL);
            BlockPos samplePos = new BlockPos(x, target.getY(), z);

            String currentBiomeId = getBiomeId(world.getBiome(samplePos));

            if (!currentBiomeId.equals(lastBiomeId)) {
                transitions++;
                lastBiomeId = currentBiomeId;
            }
        }

        return transitions;
    }

    /**
     * Calculate zone tier from biome transitions using tiered progression.
     * 
     * - Zone 0-2: 2 biomes each (0-5 transitions = zones 0-2)
     * - Zone 3-5: 3 biomes each (6-14 transitions = zones 3-5)
     * - Zone 6+: 4 biomes each (15+ transitions = zones 6+)
     */
    public static int calculateZoneTier(int biomeTransitions) {
        // Early zones (0-2): 2 biomes each = 6 biomes total for zones 0-2
        if (biomeTransitions < 6) {
            return biomeTransitions / BIOMES_PER_ZONE_EARLY; // 0-5 -> 0-2
        }
        
        // Mid zones (3-5): 3 biomes each = 9 biomes for zones 3-5
        int remaining = biomeTransitions - 6;
        if (remaining < 9) {
            return 3 + (remaining / BIOMES_PER_ZONE_MID); // 6-14 -> 3-5
        }
        
        // Late zones (6+): 4 biomes each
        remaining = remaining - 9;
        return 6 + (remaining / BIOMES_PER_ZONE_LATE); // 15+ -> 6+
    }

    /**
     * Get zone tier directly from level (for coloring).
     */
    public static int getZoneTierFromLevel(int level) {
        // Reverse calculation: level -> zone tier
        // level = (zoneTier * 3) + 1 + variation
        // So zoneTier ≈ (level - 1) / 3
        return Math.max(0, (level - 1) / LEVELS_PER_ZONE);
    }

    /**
     * Get color code for nameplate based on zone tier.
     */
    public static String getZoneColor(int zoneTier) {
        if (zoneTier <= 0) {
            return "§a"; // Green - Safe
        } else if (zoneTier <= 2) {
            return "§e"; // Yellow - Easy
        } else if (zoneTier <= 4) {
            return "§6"; // Gold - Medium
        } else if (zoneTier <= 6) {
            return "§c"; // Red - Hard
        } else {
            return "§4"; // Dark Red - Deadly
        }
    }

    /**
     * Get world spawn point.
     */
    private static BlockPos getWorldSpawn(Level world) {
        return world.getSharedSpawnPos();
    }

    /**
     * Extract biome ID string from holder.
     */
    private static String getBiomeId(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrapKey()
                .map(ResourceKey::location)
                .map(ResourceLocation::toString)
                .orElse("minecraft:plains");
    }

    private static boolean isDeepDark(String biomeId) {
        return "minecraft:deep_dark".equals(biomeId);
    }

    private static boolean isEndBiome(String biomeId) {
        return END_BIOMES.contains(biomeId);
    }

    private static boolean isNetherBiome(String biomeId) {
        return NETHER_BIOMES.contains(biomeId);
    }
}

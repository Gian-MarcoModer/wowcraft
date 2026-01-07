package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import com.gianmarco.wowcraft.zone.ZoneRegion;
import com.gianmarco.wowcraft.zone.ZoneSaveData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

/**
 * Manages mob leveling, stat scaling, and initialization.
 * 
 * Mob levels are determined by the zone system:
 * - Mobs spawn at levels matching the zone's level range
 * - "Goldwind Meadows" (L1-5) = mobs spawn L1-5
 * - "Shepherd's Reach" (L12-18) = mobs spawn L12-18
 * 
 * Special overrides for dangerous dimensions:
 * - Deep Dark: Fixed 25-28
 * - End: Fixed 28-30
 * - Nether: Minimum 15
 */
public class MobLevelManager {

    // Special biome level overrides
    private static final int DEEP_DARK_BASE_LEVEL = 25;
    private static final int END_BASE_LEVEL = 28;
    private static final int NETHER_MIN_LEVEL = 15;
    private static final int MAX_LEVEL = 60;
    private static final int DEFAULT_LEVEL = 1;

    /**
     * Initialize a mob with a level and scaled stats.
     * Called when a mob spawns.
     */
    public static void initializeMob(LivingEntity mob) {
        // Level enemies (monsters), wolves, and bees
        boolean isEnemy = mob instanceof Enemy;
        boolean isWolf = mob.getType() == net.minecraft.world.entity.EntityType.WOLF;
        boolean isBee = mob.getType() == net.minecraft.world.entity.EntityType.BEE;

        boolean isLevelableMob = isEnemy || isWolf || isBee;

        if (!isLevelableMob) {
            return;
        }

        // Calculate level based on zone system
        int level = calculateSpawnLevel(mob);
        int zoneTier = getZoneTierFromLevel(level);

        // Set level and zone data
        setMobData(mob, level, zoneTier);

        // Scale stats
        applyLevelStats(mob, level);

        // Update name with zone-colored level
        updateNameplate(mob, level, zoneTier);

        // Heal to full (since MaxHP changed)
        mob.setHealth(mob.getMaxHealth());

        WowCraft.LOGGER.debug("Spawned Lvl {} (Zone {}) {} at {}",
                level, zoneTier, mob.getName().getString(), mob.blockPosition());
    }

    /**
     * Calculate level based on the zone system.
     * Uses discovered zones with their fixed level ranges.
     */
    private static int calculateSpawnLevel(LivingEntity mob) {
        Level world = mob.level();
        BlockPos pos = mob.blockPosition();

        // Check for special dimension/biome overrides first
        Holder<Biome> biomeHolder = world.getBiome(pos);
        String biomeId = getBiomeId(biomeHolder);

        // Deep Dark - fixed high level
        if ("minecraft:deep_dark".equals(biomeId)) {
            int level = DEEP_DARK_BASE_LEVEL + mob.getRandom().nextInt(4); // 25-28
            WowCraft.LOGGER.debug("Deep Dark biome, mob level: {}", level);
            return Math.min(level, MAX_LEVEL);
        }

        // End dimension - fixed high level
        if (world.dimension() == Level.END) {
            int level = END_BASE_LEVEL + mob.getRandom().nextInt(3); // 28-30
            WowCraft.LOGGER.debug("End dimension, mob level: {}", level);
            return Math.min(level, MAX_LEVEL);
        }

        // Nether dimension - minimum level 15
        if (world.dimension() == Level.NETHER) {
            int baseLevel = calculateZoneLevel(world, pos, mob);
            int level = Math.max(NETHER_MIN_LEVEL, baseLevel);
            WowCraft.LOGGER.debug("Nether dimension, mob level: {} (min {})", level, NETHER_MIN_LEVEL);
            return Math.min(level, MAX_LEVEL);
        }

        // Normal Overworld - use zone system
        return calculateZoneLevel(world, pos, mob);
    }

    /**
     * Calculate level based on discovered zones.
     */
    private static int calculateZoneLevel(Level world, BlockPos pos, LivingEntity mob) {
        // Only works on server
        if (!(world instanceof ServerLevel serverLevel)) {
            return DEFAULT_LEVEL;
        }

        // Get biome group at mob position
        Holder<Biome> biomeHolder = world.getBiome(pos);
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
        BiomeGroup group = BiomeGroup.fromBiome(biomeKey);

        if (group == null || !group.isNameable()) {
            // River, beach, ocean, etc. - use fallback
            return DEFAULT_LEVEL + mob.getRandom().nextInt(3);
        }

        // Look up discovered zone for this biome group
        ZoneSaveData saveData = ZoneSaveData.get(serverLevel);
        ZoneRegion zone = saveData.getZone(group);

        if (zone != null) {
            // Zone exists - spawn mob within its level range
            int minLevel = zone.suggestedLevelMin();
            int maxLevel = zone.suggestedLevelMax();
            int range = maxLevel - minLevel + 1;
            int level = minLevel + mob.getRandom().nextInt(range);

            WowCraft.LOGGER.debug("Zone {} (L{}-{}) mob spawned at level {}",
                    zone.assignedName(), minLevel, maxLevel, level);
            return Math.min(level, MAX_LEVEL);
        }

        // Zone not discovered yet - use low level fallback
        // This handles mobs that spawn before any player enters the area
        WowCraft.LOGGER.debug("No zone discovered for {} biome, using default level", group);
        return DEFAULT_LEVEL + mob.getRandom().nextInt(5);
    }

    public static int getMobLevel(LivingEntity mob) {
        if (!mob.hasAttached(PlayerDataRegistry.MOB_DATA)) {
            return 1;
        }
        return mob.getAttached(PlayerDataRegistry.MOB_DATA).level();
    }

    public static int getMobZoneTier(LivingEntity mob) {
        if (!mob.hasAttached(PlayerDataRegistry.MOB_DATA)) {
            return 0;
        }
        return mob.getAttached(PlayerDataRegistry.MOB_DATA).zoneTier();
    }

    private static void setMobData(LivingEntity mob, int level, int zoneTier) {
        mob.setAttached(PlayerDataRegistry.MOB_DATA, new MobData(level, zoneTier));
    }

    /**
     * Apply stat scaling based on level.
     */
    private static void applyLevelStats(LivingEntity mob, int level) {
        // Health Scaling: Base * 4 * (1.08 ^ (Level - 1))
        // At Level 1: 4x multiplier (zombie = 80 HP)
        // At Level 10: ~8x multiplier
        // At Level 30: ~40x multiplier
        double hpMultiplier = 4.0 * Math.pow(1.08, level - 1);

        var hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) {
            double baseHp = hpAttr.getBaseValue();
            hpAttr.setBaseValue(baseHp * hpMultiplier);
        }

        // Damage Scaling: Base * 1.5 * (1.05 ^ (Level - 1))
        // At Level 1: 1.5x multiplier
        // At Level 10: ~2.3x multiplier
        // At Level 30: ~6.3x multiplier
        double dmgMultiplier = 1.5 * Math.pow(1.05, level - 1);

        var dmgAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (dmgAttr != null) {
            double baseDmg = dmgAttr.getBaseValue();
            dmgAttr.setBaseValue(baseDmg * dmgMultiplier);
        }
    }

    /**
     * Get zone tier from level for nameplate coloring.
     */
    private static int getZoneTierFromLevel(int level) {
        // Zone tiers for color coding
        // 1-10: Tier 0-1 (green/yellow)
        // 11-25: Tier 2-4 (gold/orange)
        // 26-40: Tier 5-6 (red)
        // 41+: Tier 7+ (dark red)
        if (level <= 5)
            return 0;
        if (level <= 15)
            return 1;
        if (level <= 25)
            return 2;
        if (level <= 35)
            return 3;
        if (level <= 45)
            return 4;
        if (level <= 55)
            return 5;
        return 6;
    }

    /**
     * Update nameplate to show level with hostility-based color.
     * Follows WoW-style system:
     * - Hostile mobs (Enemy) -> Red
     * - Neutral mobs (Wolves) -> Yellow
     * - Passive mobs (Bees) -> Green
     */
    private static void updateNameplate(LivingEntity mob, int level, int zoneTier) {
        // Get creative name based on biome
        String name = getCreativeMobName(mob);
        String color = getMobHostilityColor(mob);
        mob.setCustomName(Component.literal(color + "[Lv." + level + "] " + name));
        mob.setCustomNameVisible(true);
    }

    /**
     * Get creative name for mob based on its biome.
     */
    private static String getCreativeMobName(LivingEntity mob) {
        // Get biome at mob position
        BlockPos pos = mob.blockPosition();
        Holder<Biome> biomeHolder = mob.level().getBiome(pos);

        // Convert to BiomeGroup
        ResourceKey<Biome> biomeKey = biomeHolder.unwrapKey().orElse(null);
        com.gianmarco.wowcraft.zone.BiomeGroup biomeGroup =
            com.gianmarco.wowcraft.zone.BiomeGroup.fromBiome(biomeKey);

        // Get mob type
        net.minecraft.resources.ResourceLocation mobType =
            net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType());

        // Get creative name
        return com.gianmarco.wowcraft.mobpack.MobNameGenerator.getName(mobType, biomeGroup);
    }

    /**
     * Get color based on mob hostility type.
     */
    private static String getMobHostilityColor(LivingEntity mob) {
        // Hostile mobs -> Red
        if (mob instanceof Enemy) {
            return "§c";
        }
        // Wolves -> Yellow (neutral, can be hostile if provoked)
        if (mob.getType() == net.minecraft.world.entity.EntityType.WOLF) {
            return "§e";
        }
        // Bees -> Green (passive unless provoked)
        if (mob.getType() == net.minecraft.world.entity.EntityType.BEE) {
            return "§a";
        }
        // Default to white
        return "§f";
    }

    /**
     * Extract biome ID string from holder.
     */
    private static String getBiomeId(Holder<Biome> biomeHolder) {
        return biomeHolder.unwrapKey()
                .map(key -> key.location().toString())
                .orElse("minecraft:plains");
    }
}

package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.playerclass.PlayerClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Generates random WoW-style items based on level.
 * Items have: Prefix (level-based) + Base Type + Suffix (stat-based)
 * Example: "Sturdy Iron Sword of the Bear"
 */
public class ItemGenerator {
    private static final Random random = new Random();

    /**
     * Generate a random item appropriate for the given mob level.
     */
    public static WowItem generateItem(int mobLevel, PlayerClass playerClass) {
        // Determine rarity
        ItemRarity rarity = rollRarity();

        // Get base type appropriate for level
        BaseItemType baseType = getRandomBaseType(mobLevel);

        // Get prefix based on item level
        ItemPrefix prefix = ItemPrefix.getRandomForLevel(mobLevel);

        // Determine suffix (only for uncommon+)
        ItemSuffix suffix = null;
        if (rarity.ordinal() >= ItemRarity.UNCOMMON.ordinal()) {
            suffix = ItemSuffix.getRandomSuffix(playerClass);
        }

        // Item level is mob level +/- 2
        int itemLevel = Math.max(1, mobLevel + random.nextInt(5) - 2);

        return new WowItem(prefix, baseType, rarity, suffix, itemLevel);
    }

    /**
     * Generate a specific item (for testing or named items)
     */
    public static WowItem generateItem(ItemPrefix prefix, BaseItemType type, ItemRarity rarity, ItemSuffix suffix, int level) {
        return new WowItem(prefix, type, rarity, suffix, level);
    }

    /**
     * Roll for rarity based on weights
     */
    private static ItemRarity rollRarity() {
        float roll = random.nextFloat() * 100;
        float cumulative = 0;

        for (ItemRarity rarity : ItemRarity.values()) {
            cumulative += rarity.getDropWeight();
            if (roll < cumulative) {
                return rarity;
            }
        }

        return ItemRarity.COMMON;
    }

    /**
     * Get a random base type available at the given level
     */
    private static BaseItemType getRandomBaseType(int level) {
        List<BaseItemType> available = new ArrayList<>();

        for (BaseItemType type : BaseItemType.values()) {
            if (type.isAvailableAtLevel(level)) {
                available.add(type);
            }
        }

        if (available.isEmpty()) {
            // Fallback to shortsword if nothing available
            return BaseItemType.SHORTSWORD;
        }

        return available.get(random.nextInt(available.size()));
    }

    /**
     * Should this mob drop loot?
     * 
     * @param mobLevel The mob's level
     * @return true if loot should drop
     */
    public static boolean shouldDropLoot(int mobLevel) {
        // Base 40% drop chance, increases with level
        float dropChance = 0.40f + (mobLevel * 0.02f);
        return random.nextFloat() < Math.min(dropChance, 0.80f);
    }

    /**
     * Estimate mob level from max health (legacy fallback)
     */
    public static int estimateMobLevel(float maxHealth) {
        // Basic formula: level = health / 2, capped at 1-30
        int level = Math.max(1, Math.min(30, (int) (maxHealth / 2)));
        return level;
    }
}

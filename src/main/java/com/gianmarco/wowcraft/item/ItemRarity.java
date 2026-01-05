package com.gianmarco.wowcraft.item;

/**
 * Item rarity tiers matching WoW Classic.
 * Each rarity has a color code and stat multiplier.
 */
public enum ItemRarity {
    POOR("Poor", "§7", 0x777777, 0.0f, 60), // Gray - vendor trash
    COMMON("Common", "§f", 0xFFFFFF, 1.0f, 25), // White - basic items
    UNCOMMON("Uncommon", "§a", 0x1EFF00, 1.5f, 10), // Green - has suffix
    RARE("Rare", "§9", 0x0070DD, 2.0f, 4), // Blue - named items
    EPIC("Epic", "§5", 0xA335EE, 2.5f, 0.9f), // Purple - boss drops
    LEGENDARY("Legendary", "§6", 0xFF8000, 3.0f, 0.1f); // Orange - ultra rare

    private final String displayName;
    private final String colorCode;
    private final int color;
    private final float statMultiplier;
    private final float dropWeight;

    ItemRarity(String displayName, String colorCode, int color, float statMultiplier, float dropWeight) {
        this.displayName = displayName;
        this.colorCode = colorCode;
        this.color = color;
        this.statMultiplier = statMultiplier;
        this.dropWeight = dropWeight;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getColorCode() {
        return colorCode;
    }

    public int getColor() {
        return color;
    }

    public int getColorWithAlpha() {
        return 0xFF000000 | color;
    }

    public float getStatMultiplier() {
        return statMultiplier;
    }

    public float getDropWeight() {
        return dropWeight;
    }

    /**
     * Format an item name with this rarity's color
     */
    public String formatName(String itemName) {
        return colorCode + itemName;
    }
}

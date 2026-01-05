package com.gianmarco.wowcraft.item;

/**
 * Level-based item name prefixes for random generated items.
 * These make items feel more immersive and indicate quality at a glance.
 * 
 * Example: "Sturdy Iron Sword of the Bear" (level 8)
 */
public enum ItemPrefix {
    // Level 1-5: Starter gear, worn and basic
    WORN("Worn", 1, 5),
    FRAYED("Frayed", 1, 5),
    CRUDE("Crude", 1, 5),
    BATTERED("Battered", 1, 5),

    // Level 6-10: Getting better
    STURDY("Sturdy", 6, 10),
    SIMPLE("Simple", 6, 10),
    PLAIN("Plain", 6, 10),
    ROUGH("Rough", 6, 10),

    // Level 11-15: Decent quality
    HEAVY("Heavy", 11, 15),
    THICK("Thick", 11, 15),
    REINFORCED("Reinforced", 11, 15),
    HARDENED("Hardened", 11, 15),

    // Level 16-20: Good quality
    SOLID("Solid", 16, 20),
    TEMPERED("Tempered", 16, 20),
    FORTIFIED("Fortified", 16, 20),
    RUGGED("Rugged", 16, 20),

    // Level 21-25: High quality
    SUPERIOR("Superior", 21, 25),
    FINE("Fine", 21, 25),
    POLISHED("Polished", 21, 25),
    GLEAMING("Gleaming", 21, 25),

    // Level 26-30: Endgame quality
    MASTERWORK("Masterwork", 26, 30),
    EXQUISITE("Exquisite", 26, 30),
    FLAWLESS("Flawless", 26, 30),
    EXCEPTIONAL("Exceptional", 26, 30);

    private final String displayName;
    private final int minLevel;
    private final int maxLevel;

    ItemPrefix(String displayName, int minLevel, int maxLevel) {
        this.displayName = displayName;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    /**
     * Get a random prefix appropriate for the given item level.
     */
    public static ItemPrefix getRandomForLevel(int itemLevel) {
        java.util.List<ItemPrefix> available = new java.util.ArrayList<>();

        for (ItemPrefix prefix : values()) {
            if (itemLevel >= prefix.minLevel && itemLevel <= prefix.maxLevel) {
                available.add(prefix);
            }
        }

        if (available.isEmpty()) {
            // Fallback: use closest tier
            if (itemLevel < 1) {
                return WORN;
            } else {
                return MASTERWORK; // Max level
            }
        }

        return available.get((int) (Math.random() * available.size()));
    }

    /**
     * Get the tier number (0-5) for this prefix level range.
     */
    public int getTier() {
        return (minLevel - 1) / 5;
    }
}

package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.playerclass.PlayerClass;

/**
 * Random item suffixes from WoW Classic.
 * Each suffix grants two stats.
 */
public enum ItemSuffix {
    // Strength combinations
    BEAR("of the Bear", Stat.STRENGTH, Stat.STAMINA, PlayerClass.WARRIOR),
    TIGER("of the Tiger", Stat.AGILITY, Stat.STRENGTH, PlayerClass.WARRIOR),
    BOAR("of the Boar", Stat.SPIRIT, Stat.STRENGTH, null),
    GORILLA("of the Gorilla", Stat.INTELLECT, Stat.STRENGTH, null),

    // Agility combinations
    MONKEY("of the Monkey", Stat.AGILITY, Stat.STAMINA, null),
    FALCON("of the Falcon", Stat.AGILITY, Stat.INTELLECT, null),
    WOLF("of the Wolf", Stat.AGILITY, Stat.SPIRIT, null),

    // Intellect combinations
    EAGLE("of the Eagle", Stat.INTELLECT, Stat.STAMINA, PlayerClass.MAGE),
    OWL("of the Owl", Stat.INTELLECT, Stat.SPIRIT, PlayerClass.MAGE),

    // Spirit combinations
    WHALE("of the Whale", Stat.SPIRIT, Stat.STAMINA, null);

    private final String displayName;
    private final Stat primaryStat;
    private final Stat secondaryStat;
    private final PlayerClass preferredClass; // null = any class

    ItemSuffix(String displayName, Stat primaryStat, Stat secondaryStat, PlayerClass preferredClass) {
        this.displayName = displayName;
        this.primaryStat = primaryStat;
        this.secondaryStat = secondaryStat;
        this.preferredClass = preferredClass;
    }

    public String getDisplayName() {
        return displayName;
    }

    public Stat getPrimaryStat() {
        return primaryStat;
    }

    public Stat getSecondaryStat() {
        return secondaryStat;
    }

    public PlayerClass getPreferredClass() {
        return preferredClass;
    }

    /**
     * Get a random suffix, optionally weighted toward a class
     */
    public static ItemSuffix getRandomSuffix(PlayerClass playerClass) {
        ItemSuffix[] values = values();

        // 30% chance to get a class-appropriate suffix if class is specified
        if (playerClass != null && playerClass != PlayerClass.NONE && Math.random() < 0.3) {
            for (ItemSuffix suffix : values) {
                if (suffix.preferredClass == playerClass) {
                    return suffix;
                }
            }
        }

        // Otherwise random
        return values[(int) (Math.random() * values.length)];
    }

    /**
     * Gets totally random suffix
     */
    public static ItemSuffix getRandomSuffix() {
        ItemSuffix[] values = values();
        return values[(int) (Math.random() * values.length)];
    }

    /**
     * The 5 primary stats
     */
    public enum Stat {
        STRENGTH("Strength", "§c", 0xFF5555), // Red
        AGILITY("Agility", "§a", 0x55FF55), // Green
        STAMINA("Stamina", "§6", 0xFFAA00), // Orange
        INTELLECT("Intellect", "§9", 0x5555FF), // Blue
        SPIRIT("Spirit", "§d", 0xFF55FF); // Pink

        private final String displayName;
        private final String colorCode;
        private final int color;

        Stat(String displayName, String colorCode, int color) {
            this.displayName = displayName;
            this.colorCode = colorCode;
            this.color = color;
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
    }
}

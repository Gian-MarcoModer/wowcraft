package com.gianmarco.wowcraft.class_;

/**
 * Resource types for player classes.
 * Determines which resource pool the class uses.
 */
public enum ResourceType {
    NONE,
    MANA,
    RAGE,
    ENERGY,
    COMBO_POINTS;

    /**
     * Check if this resource regenerates over time.
     */
    public boolean doesRegenerate() {
        return this == MANA || this == ENERGY;
    }

    /**
     * Check if this resource decays over time.
     */
    public boolean doesDecay() {
        return this == RAGE;
    }

    /**
     * Get the starting value for this resource.
     */
    public int getStartingValue(int maxValue) {
        return switch (this) {
            case MANA, ENERGY -> maxValue; // Start full
            case RAGE, COMBO_POINTS -> 0; // Start empty
            case NONE -> 0;
        };
    }

    /**
     * Get the display name for this resource type.
     */
    public String getDisplayName() {
        return switch (this) {
            case MANA -> "Mana";
            case RAGE -> "Rage";
            case ENERGY -> "Energy";
            case COMBO_POINTS -> "Combo Points";
            case NONE -> "None";
        };
    }
}

package com.gianmarco.wowcraft.zone;

/**
 * Defines a zone name with its associated level range.
 * Loaded from JSON data files. Zones are ordered by level (first = lowest).
 */
public record ZoneDefinition(
        String name,
        String subtitle,
        int levelMin,
        int levelMax) {
    /**
     * Creates a ZoneDefinition with default level range.
     */
    public static ZoneDefinition of(String name, String subtitle) {
        return new ZoneDefinition(name, subtitle, 1, 5);
    }

    /**
     * Creates a ZoneDefinition with specified level range.
     */
    public static ZoneDefinition of(String name, String subtitle, int levelMin, int levelMax) {
        return new ZoneDefinition(name, subtitle, levelMin, levelMax);
    }
}

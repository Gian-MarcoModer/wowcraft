package com.gianmarco.wowcraft.poi;

/**
 * Types of Points of Interest for mob spawning.
 * Each type has different spawning behavior and pack density.
 */
public enum POIType {
    /**
     * Fixed camp location with 2-4 mob packs.
     * Examples: Bandit camps, murloc villages, kobold mines.
     * High density, mobs defend the area.
     */
    CAMP,

    /**
     * Large roaming wildlife area with 1-2 wandering packs.
     * Examples: Harvest golems in fields, bears in forest.
     * Lower density, more spread out.
     */
    WILDLIFE,

    /**
     * Mobile pack that patrols between 2-3 waypoints.
     * Examples: Guard patrols, traveling bandits.
     * Creates dynamic encounters.
     */
    PATROL_ROUTE,

    /**
     * Thematic spawns near environmental features.
     * Examples: Murlocs near water, bandits near farms.
     * Spawns 1-3 packs based on nearby terrain.
     */
    RESOURCE_AREA,

    /**
     * Elite/boss encounter with 1 elite pack + guards.
     * Examples: Gnoll Chieftain's Den, Spider Queen's Nest.
     * Rare, higher respawn timer, better loot potential.
     */
    LAIR;

    /**
     * Check if this POI type spawns multiple packs.
     */
    public boolean isMultiPackType() {
        return this == CAMP || this == RESOURCE_AREA;
    }

    /**
     * Check if this POI type has elite mobs.
     */
    public boolean hasEliteMobs() {
        return this == LAIR;
    }

    /**
     * Check if this POI type involves movement.
     */
    public boolean isDynamic() {
        return this == PATROL_ROUTE || this == WILDLIFE;
    }
}

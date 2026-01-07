package com.gianmarco.wowcraft.spawn;

/**
 * Defines hostility behavior for spawn points.
 * Based on WoW Classic mob behavior patterns.
 */
public enum SpawnHostility {
    /**
     * ALWAYS_HOSTILE: Attacks player on sight
     * Examples: Zombies, Skeletons, Spiders, Creepers
     * Used for: POI camps, hostile scatter spawns
     */
    ALWAYS_HOSTILE,

    /**
     * NEUTRAL_DEFENSIVE: Only attacks if player attacks first
     * Examples: Wolves, Piglins, Iron Golems
     * Used for: Neutral scatter spawns, wildlife POIs
     */
    NEUTRAL_DEFENSIVE,

    /**
     * NEUTRAL_TERRITORIAL: Attacks if player gets too close or lingers
     * Examples: Endermen (stare = aggro), guards
     * Used for: Resource guarding, patrol boundaries
     */
    NEUTRAL_TERRITORIAL,

    /**
     * PASSIVE: Never attacks
     * Examples: Vanilla animals (cows, pigs, sheep)
     * Used for: Wildlife areas, farming zones
     */
    PASSIVE
}

package com.gianmarco.wowcraft.spawn;

/**
 * State of a spawn point in the lazy spawning system.
 */
public enum SpawnPointState {
    /**
     * DORMANT: Too far from any player (>192 blocks)
     * - No tick processing
     * - No respawn timers
     * - No entities
     * - Minimal memory (just position + config)
     */
    DORMANT,

    /**
     * VIRTUAL_SPAWNED: Active point, no nearby player (128-192 blocks)
     * - Respawn timers tick
     * - Tracks "should have mobs" but no actual entities
     * - Stores mob count, types, levels
     * - When player approaches, spawn entities from virtual state
     */
    VIRTUAL_SPAWNED,

    /**
     * VIRTUAL_DEAD: Active point, mobs dead, no nearby player
     * - Respawn timer ticking
     * - No entities
     * - When timer expires, transition to VIRTUAL_SPAWNED
     * - When player approaches, check if respawn ready
     */
    VIRTUAL_DEAD,

    /**
     * ENTITY_SPAWNED: Player nearby (<128 blocks)
     * - Actual mob entities exist in world
     * - Normal gameplay
     * - If player leaves, save entities and transition to VIRTUAL
     */
    ENTITY_SPAWNED,

    /**
     * INACTIVE: Point is in the spawn pool but not active this rotation
     * - Don't spawn or respawn
     * - If has entities, let them live but don't replace when killed
     * - Minimal processing
     */
    INACTIVE
}

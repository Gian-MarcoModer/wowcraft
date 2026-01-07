package com.gianmarco.wowcraft.spawn;

/**
 * Types of spawn points in the system.
 */
public enum SpawnPointType {
    /**
     * POI_CAMP: Single camp spawn point (always active, 3-5 mobs)
     */
    POI_CAMP,

    /**
     * POI_COMPOUND: Multi-camp compound with clustered spawn points (always active, 3-5 mobs per camp)
     */
    POI_COMPOUND,

    /**
     * POI_LAIR: Boss lair with special spawn (always active, elite mob)
     */
    POI_LAIR,

    /**
     * POI_PATROL: Patrol route with waypoints (always active, moving)
     */
    POI_PATROL,

    /**
     * POI_WILDLIFE: Wildlife area with neutral animals (always active)
     */
    POI_WILDLIFE,

    /**
     * SCATTER: Normal ambient spawn (can rotate, 1-3 mobs)
     */
    SCATTER,

    /**
     * NAMED_MOB: Named/quest mob at fixed location (always active, protected)
     */
    NAMED_MOB
}

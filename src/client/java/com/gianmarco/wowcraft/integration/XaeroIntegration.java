package com.gianmarco.wowcraft.integration;

import com.gianmarco.wowcraft.WowCraft;

import java.util.HashSet;
import java.util.Set;

/**
 * Optional integration with Xaero's Minimap.
 * Creates waypoints when discovering new zones.
 * 
 * NOTE: Currently disabled - Xaero's API has changed and the waypoints classes
 * are not stable. This is a stub that logs but doesn't create waypoints.
 * TODO: Update when Xaero's provides stable API documentation.
 */
public class XaeroIntegration {

    private static boolean xaeroAvailable = false;
    private static boolean checked = false;

    /** Track zones we've already logged for */
    private static final Set<String> loggedZones = new HashSet<>();

    /**
     * Checks if Xaero's Minimap is available at runtime.
     */
    public static boolean isAvailable() {
        if (!checked) {
            checked = true;
            try {
                Class.forName("xaero.common.XaeroMinimapSession");
                xaeroAvailable = true;
                WowCraft.LOGGER.info("Xaero's Minimap detected (waypoint integration pending API update)");
            } catch (ClassNotFoundException e) {
                xaeroAvailable = false;
                WowCraft.LOGGER.debug("Xaero's Minimap not found");
            }
        }
        return xaeroAvailable;
    }

    /**
     * Creates a waypoint for a discovered zone from packet data.
     * Currently just logs - waypoint creation disabled until API stabilizes.
     */
    public static void createZoneWaypointFromPacket(String zoneName, String subtitle,
            int levelMin, int levelMax, int posX, int posY, int posZ) {
        // Only log once per zone
        if (loggedZones.contains(zoneName))
            return;
        loggedZones.add(zoneName);

        if (isAvailable()) {
            WowCraft.LOGGER.info("[Xaero] Would create waypoint: {} [L{}-{}] at ({}, {}, {})",
                    zoneName, levelMin, levelMax, posX, posY, posZ);
            // TODO: Actually create waypoint when Xaero's API is stable
            // XaeroWaypointHelper.createWaypointFromPacket(...);
        }
    }

    /**
     * Clears tracked zones (on world change).
     */
    public static void clearTrackedWaypoints() {
        loggedZones.clear();
    }
}

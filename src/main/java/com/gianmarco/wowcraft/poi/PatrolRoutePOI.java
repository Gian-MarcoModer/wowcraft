package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Patrol route POI with waypoints for mobile packs.
 * Spawns 1 pack that patrols between waypoints.
 */
public class PatrolRoutePOI extends PointOfInterest {

    private final List<BlockPos> waypoints;
    private final int waitTimeSeconds;

    public PatrolRoutePOI(UUID poiId, BlockPos position, List<BlockPos> waypoints, int waitTimeSeconds) {
        super(poiId, POIType.PATROL_ROUTE, position, calculateRadius(waypoints));
        this.waypoints = new ArrayList<>(waypoints);
        this.waitTimeSeconds = waitTimeSeconds;
    }

    private static int calculateRadius(List<BlockPos> waypoints) {
        if (waypoints.isEmpty()) {
            return 50;
        }
        // Radius is the furthest waypoint from first waypoint
        BlockPos first = waypoints.get(0);
        int maxDist = 0;
        for (BlockPos wp : waypoints) {
            int dist = (int) Math.sqrt(first.distSqr(wp));
            if (dist > maxDist) {
                maxDist = dist;
            }
        }
        return maxDist;
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        // Patrol pack spawns at first waypoint
        List<BlockPos> positions = new ArrayList<>();
        if (!waypoints.isEmpty()) {
            positions.add(waypoints.get(0));
        }
        return positions;
    }

    @Override
    public int getMaxPackCount() {
        return 1; // Only one patrol pack
    }

    public List<BlockPos> getWaypoints() {
        return new ArrayList<>(waypoints);
    }

    public int getWaitTimeSeconds() {
        return waitTimeSeconds;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("waitTime", waitTimeSeconds);

        JsonArray waypointsArray = new JsonArray();
        for (BlockPos wp : waypoints) {
            JsonObject wpJson = new JsonObject();
            wpJson.addProperty("x", wp.getX());
            wpJson.addProperty("y", wp.getY());
            wpJson.addProperty("z", wp.getZ());
            waypointsArray.add(wpJson);
        }
        json.add("waypoints", waypointsArray);

        return json;
    }

    public static PatrolRoutePOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        int waitTime = json.get("waitTime").getAsInt();

        List<BlockPos> waypoints = new ArrayList<>();
        JsonArray waypointsArray = json.getAsJsonArray("waypoints");
        for (int i = 0; i < waypointsArray.size(); i++) {
            JsonObject wpJson = waypointsArray.get(i).getAsJsonObject();
            waypoints.add(new BlockPos(
                    wpJson.get("x").getAsInt(),
                    wpJson.get("y").getAsInt(),
                    wpJson.get("z").getAsInt()));
        }

        return new PatrolRoutePOI(id, pos, waypoints, waitTime);
    }
}

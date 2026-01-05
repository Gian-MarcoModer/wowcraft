package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Base class for all Points of Interest.
 * POIs define spawn locations and behavior for mob packs.
 */
public abstract class PointOfInterest {
    protected final UUID poiId;
    protected final POIType type;
    protected final BlockPos position;
    protected final int radius;
    protected final List<UUID> assignedPackIds;

    protected PointOfInterest(UUID poiId, POIType type, BlockPos position, int radius) {
        this.poiId = poiId;
        this.type = type;
        this.position = position;
        this.radius = radius;
        this.assignedPackIds = new ArrayList<>();
    }

    // === Getters ===

    public UUID getPoiId() {
        return poiId;
    }

    public POIType getType() {
        return type;
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getRadius() {
        return radius;
    }

    public List<UUID> getAssignedPackIds() {
        return assignedPackIds;
    }

    // === Pack Management ===

    public void assignPack(UUID packId) {
        if (!assignedPackIds.contains(packId)) {
            assignedPackIds.add(packId);
        }
    }

    public void removePack(UUID packId) {
        assignedPackIds.remove(packId);
    }

    // === Spawn Point Generation ===

    /**
     * Get spawn positions for mob packs within this POI.
     * Subclasses override to implement specific spawn patterns.
     */
    public abstract List<BlockPos> getSpawnPositions();

    /**
     * Get the maximum number of packs this POI should spawn.
     */
    public abstract int getMaxPackCount();

    // === Serialization ===

    /**
     * Serialize to JSON for persistence.
     */
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", poiId.toString());
        json.addProperty("type", type.name());
        json.addProperty("x", position.getX());
        json.addProperty("y", position.getY());
        json.addProperty("z", position.getZ());
        json.addProperty("radius", radius);
        return json;
    }

    /**
     * Deserialize from JSON.
     * Subclasses must implement their own deserialization.
     */
    public static PointOfInterest fromJson(JsonObject json) {
        String typeStr = json.get("type").getAsString();
        POIType type = POIType.valueOf(typeStr);

        // Delegate to specific POI type deserializers
        return switch (type) {
            case CAMP -> CampPOI.fromJson(json);
            case WILDLIFE -> WildlifePOI.fromJson(json);
            case PATROL_ROUTE -> PatrolRoutePOI.fromJson(json);
            case RESOURCE_AREA -> ResourceAreaPOI.fromJson(json);
            case LAIR -> LairPOI.fromJson(json);
        };
    }

    @Override
    public String toString() {
        return String.format("%s POI at %s (radius: %d, packs: %d)",
                type, position, radius, assignedPackIds.size());
    }
}

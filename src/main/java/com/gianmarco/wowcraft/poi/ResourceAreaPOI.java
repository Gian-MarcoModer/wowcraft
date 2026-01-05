package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Resource area POI near environmental features.
 * Examples: Murlocs near water, bandits near farms.
 */
public class ResourceAreaPOI extends PointOfInterest {

    private final int minPacks;
    private final int maxPacks;
    private final String resourceType; // e.g., "water", "crops", "cave"

    public ResourceAreaPOI(UUID poiId, BlockPos position, int radius, int minPacks, int maxPacks,
            String resourceType) {
        super(poiId, POIType.RESOURCE_AREA, position, radius);
        this.minPacks = minPacks;
        this.maxPacks = maxPacks;
        this.resourceType = resourceType;
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        List<BlockPos> positions = new ArrayList<>();
        Random random = new Random(poiId.getMostSignificantBits());

        int packCount = minPacks + random.nextInt(maxPacks - minPacks + 1);

        for (int i = 0; i < packCount; i++) {
            // Spawn near the resource feature (clustered)
            int offsetX = random.nextInt(radius) - radius / 2;
            int offsetZ = random.nextInt(radius) - radius / 2;

            positions.add(position.offset(offsetX, 0, offsetZ));
        }

        return positions;
    }

    @Override
    public int getMaxPackCount() {
        return maxPacks;
    }

    public int getMinPacks() {
        return minPacks;
    }

    public String getResourceType() {
        return resourceType;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("minPacks", minPacks);
        json.addProperty("maxPacks", maxPacks);
        json.addProperty("resourceType", resourceType);
        return json;
    }

    public static ResourceAreaPOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        int radius = json.get("radius").getAsInt();
        int minPacks = json.get("minPacks").getAsInt();
        int maxPacks = json.get("maxPacks").getAsInt();
        String resourceType = json.get("resourceType").getAsString();

        return new ResourceAreaPOI(id, pos, radius, minPacks, maxPacks, resourceType);
    }
}

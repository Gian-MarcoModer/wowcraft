package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Wildlife roaming area that spawns 1-2 packs that wander.
 * Examples: Harvest golems in fields, bears in forest.
 */
public class WildlifePOI extends PointOfInterest {

    private final int minPacks;
    private final int maxPacks;

    public WildlifePOI(UUID poiId, BlockPos position, int radius, int minPacks, int maxPacks) {
        super(poiId, POIType.WILDLIFE, position, radius);
        this.minPacks = minPacks;
        this.maxPacks = maxPacks;
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        List<BlockPos> positions = new ArrayList<>();
        Random random = new Random(poiId.getMostSignificantBits());

        int packCount = minPacks + random.nextInt(maxPacks - minPacks + 1);

        for (int i = 0; i < packCount; i++) {
            // Scatter packs randomly across the wildlife area
            int offsetX = random.nextInt(radius * 2) - radius;
            int offsetZ = random.nextInt(radius * 2) - radius;

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

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("minPacks", minPacks);
        json.addProperty("maxPacks", maxPacks);
        return json;
    }

    public static WildlifePOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        int radius = json.get("radius").getAsInt();
        int minPacks = json.get("minPacks").getAsInt();
        int maxPacks = json.get("maxPacks").getAsInt();

        return new WildlifePOI(id, pos, radius, minPacks, maxPacks);
    }
}

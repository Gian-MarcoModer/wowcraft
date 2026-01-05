package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Fixed camp POI that spawns 2-4 mob packs in a cluster.
 * Examples: Bandit camps, murloc villages, kobold mines.
 */
public class CampPOI extends PointOfInterest {

    private final int minPacks;
    private final int maxPacks;
    private final int packSpacing;

    public CampPOI(UUID poiId, BlockPos position, int radius, int minPacks, int maxPacks, int packSpacing) {
        super(poiId, POIType.CAMP, position, radius);
        this.minPacks = minPacks;
        this.maxPacks = maxPacks;
        this.packSpacing = packSpacing;
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        List<BlockPos> positions = new ArrayList<>();
        Random random = new Random(poiId.getMostSignificantBits());

        int packCount = minPacks + random.nextInt(maxPacks - minPacks + 1);

        for (int i = 0; i < packCount; i++) {
            // Distribute packs in a circle around the center
            double angle = (2 * Math.PI * i) / packCount;
            double distance = packSpacing + random.nextInt(radius - packSpacing);

            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);

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

    public int getPackSpacing() {
        return packSpacing;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("minPacks", minPacks);
        json.addProperty("maxPacks", maxPacks);
        json.addProperty("packSpacing", packSpacing);
        return json;
    }

    public static CampPOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        int radius = json.get("radius").getAsInt();
        int minPacks = json.get("minPacks").getAsInt();
        int maxPacks = json.get("maxPacks").getAsInt();
        int packSpacing = json.get("packSpacing").getAsInt();

        return new CampPOI(id, pos, radius, minPacks, maxPacks, packSpacing);
    }
}

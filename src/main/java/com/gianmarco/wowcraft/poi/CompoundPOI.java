package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

/**
 * Compound POI - multiple camps clustered together in an area.
 * Examples: Large bandit compound, murloc village, kobold mine complex.
 * Contains 2-4 individual camp spawn points spread around a central location.
 */
public class CompoundPOI extends PointOfInterest {

    private final int minCamps;
    private final int maxCamps;
    private final int campSpacing;

    public CompoundPOI(UUID poiId, BlockPos position, int radius, int minCamps, int maxCamps, int campSpacing) {
        super(poiId, POIType.COMPOUND, position, radius);
        this.minCamps = minCamps;
        this.maxCamps = maxCamps;
        this.campSpacing = campSpacing;
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        List<BlockPos> positions = new ArrayList<>();
        Random random = new Random(poiId.getMostSignificantBits());

        int campCount = minCamps + random.nextInt(maxCamps - minCamps + 1);

        for (int i = 0; i < campCount; i++) {
            // Distribute camps in a circle around the center
            double angle = (2 * Math.PI * i) / campCount;
            double distance = campSpacing + random.nextInt(radius - campSpacing);

            int offsetX = (int) (Math.cos(angle) * distance);
            int offsetZ = (int) (Math.sin(angle) * distance);

            positions.add(position.offset(offsetX, 0, offsetZ));
        }

        return positions;
    }

    @Override
    public int getMaxPackCount() {
        return maxCamps;
    }

    public int getMinCamps() {
        return minCamps;
    }

    public int getCampSpacing() {
        return campSpacing;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("minCamps", minCamps);
        json.addProperty("maxCamps", maxCamps);
        json.addProperty("campSpacing", campSpacing);
        return json;
    }

    public static CompoundPOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        int radius = json.get("radius").getAsInt();
        int minCamps = json.get("minCamps").getAsInt();
        int maxCamps = json.get("maxCamps").getAsInt();
        int campSpacing = json.get("campSpacing").getAsInt();

        return new CompoundPOI(id, pos, radius, minCamps, maxCamps, campSpacing);
    }
}

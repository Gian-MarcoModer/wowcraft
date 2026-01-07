package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Single camp POI - one spawn point with 3-5 mobs.
 * Examples: Small bandit camp, murloc group, kobold patrol.
 * This is the basic hostile mob encounter unit.
 */
public class CampPOI extends PointOfInterest {

    public CampPOI(UUID poiId, BlockPos position) {
        super(poiId, POIType.CAMP, position, 10); // Small 10-block radius for single camp
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        // Single camp = single spawn position at the center
        List<BlockPos> positions = new ArrayList<>();
        positions.add(position);
        return positions;
    }

    @Override
    public int getMaxPackCount() {
        return 1; // Single spawn point
    }

    @Override
    public JsonObject toJson() {
        return super.toJson(); // Just use base implementation
    }

    public static CampPOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());

        return new CampPOI(id, pos);
    }
}

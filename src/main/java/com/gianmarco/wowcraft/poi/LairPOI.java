package com.gianmarco.wowcraft.poi;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Elite lair POI that spawns 1 elite pack.
 * Examples: Boss dens, named mob lairs.
 * Higher respawn timer, better loot potential.
 */
public class LairPOI extends PointOfInterest {

    private final int levelBonus; // Bonus levels for elite mob
    private final int respawnDelaySeconds; // Extended respawn time

    public LairPOI(UUID poiId, BlockPos position, int radius, int levelBonus, int respawnDelaySeconds) {
        super(poiId, POIType.LAIR, position, radius);
        this.levelBonus = levelBonus;
        this.respawnDelaySeconds = respawnDelaySeconds;
    }

    @Override
    public List<BlockPos> getSpawnPositions() {
        // Elite spawns at center of lair
        List<BlockPos> positions = new ArrayList<>();
        positions.add(position);
        return positions;
    }

    @Override
    public int getMaxPackCount() {
        return 1; // One elite pack
    }

    public int getLevelBonus() {
        return levelBonus;
    }

    public int getRespawnDelaySeconds() {
        return respawnDelaySeconds;
    }

    @Override
    public JsonObject toJson() {
        JsonObject json = super.toJson();
        json.addProperty("levelBonus", levelBonus);
        json.addProperty("respawnDelay", respawnDelaySeconds);
        return json;
    }

    public static LairPOI fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        BlockPos pos = new BlockPos(
                json.get("x").getAsInt(),
                json.get("y").getAsInt(),
                json.get("z").getAsInt());
        int radius = json.get("radius").getAsInt();
        int levelBonus = json.get("levelBonus").getAsInt();
        int respawnDelay = json.get("respawnDelay").getAsInt();

        return new LairPOI(id, pos, radius, levelBonus, respawnDelay);
    }
}

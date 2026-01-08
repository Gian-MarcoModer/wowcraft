package com.gianmarco.wowcraft.roads;

import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

public final class RoadNode {
    private final UUID id;
    private final BlockPos position;
    private final RoadNodeType type;

    public RoadNode(UUID id, BlockPos position, RoadNodeType type) {
        this.id = id;
        this.position = position;
        this.type = type;
    }

    public UUID getId() {
        return id;
    }

    public BlockPos getPosition() {
        return position;
    }

    public RoadNodeType getType() {
        return type;
    }

    public static UUID makeDeterministicId(RoadNodeType type, String key, BlockPos pos) {
        String seed = type.name() + "|" + key + "|" + pos.getX() + "," + pos.getY() + "," + pos.getZ();
        return UUID.nameUUIDFromBytes(seed.getBytes(StandardCharsets.UTF_8));
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("id", id.toString());
        json.addProperty("type", type.name());
        json.addProperty("x", position.getX());
        json.addProperty("y", position.getY());
        json.addProperty("z", position.getZ());
        return json;
    }

    public static RoadNode fromJson(JsonObject json) {
        UUID id = UUID.fromString(json.get("id").getAsString());
        RoadNodeType type = RoadNodeType.valueOf(json.get("type").getAsString());
        int x = json.get("x").getAsInt();
        int y = json.get("y").getAsInt();
        int z = json.get("z").getAsInt();
        return new RoadNode(id, new BlockPos(x, y, z), type);
    }
}

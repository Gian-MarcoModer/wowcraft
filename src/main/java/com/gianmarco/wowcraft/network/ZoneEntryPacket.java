package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

/**
 * Packet sent from server to client when player enters a new zone.
 * Triggers the WoW-style zone announcement on the client.
 * Also includes position for Xaero's Minimap waypoint integration.
 */
public record ZoneEntryPacket(
        String zoneName,
        @Nullable String subtitle,
        int levelMin,
        int levelMax,
        int posX,
        int posY,
        int posZ) implements CustomPacketPayload {

    public static final Type<ZoneEntryPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "zone_entry"));

    public static final StreamCodec<FriendlyByteBuf, ZoneEntryPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.zoneName);
                buf.writeBoolean(packet.subtitle != null);
                if (packet.subtitle != null) {
                    buf.writeUtf(packet.subtitle);
                }
                buf.writeVarInt(packet.levelMin);
                buf.writeVarInt(packet.levelMax);
                buf.writeVarInt(packet.posX);
                buf.writeVarInt(packet.posY);
                buf.writeVarInt(packet.posZ);
            },
            buf -> {
                String zoneName = buf.readUtf();
                String subtitle = buf.readBoolean() ? buf.readUtf() : null;
                int levelMin = buf.readVarInt();
                int levelMax = buf.readVarInt();
                int posX = buf.readVarInt();
                int posY = buf.readVarInt();
                int posZ = buf.readVarInt();
                return new ZoneEntryPacket(zoneName, subtitle, levelMin, levelMax, posX, posY, posZ);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

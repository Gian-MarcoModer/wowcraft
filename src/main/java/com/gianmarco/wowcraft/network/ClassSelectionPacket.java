package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server when player selects a class.
 */
public record ClassSelectionPacket(PlayerClass selectedClass) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ClassSelectionPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "class_selection"));

    public static final StreamCodec<FriendlyByteBuf, ClassSelectionPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeEnum(packet.selectedClass),
            buf -> new ClassSelectionPacket(buf.readEnum(PlayerClass.class)));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

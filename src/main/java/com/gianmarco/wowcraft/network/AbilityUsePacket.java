package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to use an ability.
 */
public record AbilityUsePacket(int abilitySlot) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<AbilityUsePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "ability_use"));

    public static final StreamCodec<FriendlyByteBuf, AbilityUsePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> buf.writeVarInt(packet.abilitySlot),
            buf -> new AbilityUsePacket(buf.readVarInt()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

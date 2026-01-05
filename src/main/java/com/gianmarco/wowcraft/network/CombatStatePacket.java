package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from server to client to sync combat state.
 * Sent when player enters or leaves combat.
 */
public record CombatStatePacket(
        boolean inCombat,
        long lastCombatTick) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<CombatStatePacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "combat_state_sync"));

    public static final StreamCodec<FriendlyByteBuf, CombatStatePacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeBoolean(packet.inCombat);
                buf.writeVarLong(packet.lastCombatTick);
            },
            buf -> new CombatStatePacket(
                    buf.readBoolean(),
                    buf.readVarLong()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

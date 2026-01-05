package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from client to server to sync action bar changes.
 */
public record ActionBarSyncPacket(int slot, String abilityId) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<ActionBarSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "action_bar_sync"));

    public static final StreamCodec<FriendlyByteBuf, ActionBarSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeVarInt(packet.slot);
                buf.writeBoolean(packet.abilityId != null);
                if (packet.abilityId != null) {
                    buf.writeUtf(packet.abilityId);
                }
            },
            buf -> {
                int slot = buf.readVarInt();
                String abilityId = buf.readBoolean() ? buf.readUtf() : null;
                return new ActionBarSyncPacket(slot, abilityId);
            });

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

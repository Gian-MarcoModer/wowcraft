package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Server-to-client packet for casting bar updates.
 */
public record CastingUpdatePacket(
        String spellName,
        float castDuration,
        float progress,
        boolean interrupted) implements CustomPacketPayload {

    public static final Type<CastingUpdatePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "casting_update"));

    public static final StreamCodec<RegistryFriendlyByteBuf, CastingUpdatePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.STRING_UTF8, CastingUpdatePacket::spellName,
            ByteBufCodecs.FLOAT, CastingUpdatePacket::castDuration,
            ByteBufCodecs.FLOAT, CastingUpdatePacket::progress,
            ByteBufCodecs.BOOL, CastingUpdatePacket::interrupted,
            CastingUpdatePacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

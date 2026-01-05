package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Packet sent from server to client to display floating damage text.
 * Shows damage numbers that float upward and fade out, WoW-style.
 */
public record DamageDisplayPacket(
                int entityId, // Entity that took damage
                float damage, // Amount of damage
                boolean isCritical, // Whether it was a critical hit
                boolean isSpell, // Whether this is spell damage (vs melee)
                double x, // World X position
                double y, // World Y position
                double z // World Z position
) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<DamageDisplayPacket> TYPE = new CustomPacketPayload.Type<>(
                        ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "damage_display"));

        public static final StreamCodec<RegistryFriendlyByteBuf, DamageDisplayPacket> STREAM_CODEC = StreamCodec
                        .composite(
                                        ByteBufCodecs.VAR_INT,
                                        DamageDisplayPacket::entityId,
                                        ByteBufCodecs.FLOAT,
                                        DamageDisplayPacket::damage,
                                        ByteBufCodecs.BOOL,
                                        DamageDisplayPacket::isCritical,
                                        ByteBufCodecs.BOOL,
                                        DamageDisplayPacket::isSpell,
                                        ByteBufCodecs.DOUBLE,
                                        DamageDisplayPacket::x,
                                        ByteBufCodecs.DOUBLE,
                                        DamageDisplayPacket::y,
                                        ByteBufCodecs.DOUBLE,
                                        DamageDisplayPacket::z,
                                        DamageDisplayPacket::new);

        @Override
        public Type<? extends CustomPacketPayload> type() {
                return TYPE;
        }
}

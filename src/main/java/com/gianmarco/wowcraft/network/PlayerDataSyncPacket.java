package com.gianmarco.wowcraft.network;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.economy.MoneyPouch;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Packet sent from server to client to sync player data.
 * Sent when class is selected, resource changes, or action bar updates.
 */
public record PlayerDataSyncPacket(
        PlayerClass playerClass,
        int level,
        int experience,
        int currentResource,
        int maxResource,
        int comboPoints,
        int copper,
        int silver,
        int gold,
        List<String> actionBar) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<PlayerDataSyncPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "player_data_sync"));

    public static final StreamCodec<FriendlyByteBuf, PlayerDataSyncPacket> STREAM_CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeEnum(packet.playerClass);
                buf.writeVarInt(packet.level);
                buf.writeVarInt(packet.experience);
                buf.writeVarInt(packet.currentResource);
                buf.writeVarInt(packet.maxResource);
                buf.writeVarInt(packet.comboPoints);
                buf.writeVarInt(packet.copper);
                buf.writeVarInt(packet.silver);
                buf.writeVarInt(packet.gold);
                // Write action bar (8 slots)
                buf.writeVarInt(packet.actionBar.size());
                for (String abilityId : packet.actionBar) {
                    buf.writeUtf(abilityId != null ? abilityId : "");
                }
            },
            buf -> {
                PlayerClass playerClass = buf.readEnum(PlayerClass.class);
                int level = buf.readVarInt();
                int experience = buf.readVarInt();
                int currentResource = buf.readVarInt();
                int maxResource = buf.readVarInt();
                int comboPoints = buf.readVarInt();
                int copper = buf.readVarInt();
                int silver = buf.readVarInt();
                int gold = buf.readVarInt();
                // Read action bar
                int size = buf.readVarInt();
                List<String> actionBar = new ArrayList<>(8);
                for (int i = 0; i < size; i++) {
                    actionBar.add(buf.readUtf());
                }
                // Ensure we have 8 slots
                while (actionBar.size() < 8) {
                    actionBar.add("");
                }
                return new PlayerDataSyncPacket(playerClass, level, experience, currentResource, maxResource,
                        comboPoints, copper, silver, gold, actionBar);
            });

    /**
     * Helper to get a MoneyPouch from the packet data
     */
    public MoneyPouch getMoneyPouch() {
        return new MoneyPouch(copper, silver, gold);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

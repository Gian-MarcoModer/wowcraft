package com.gianmarco.wowcraft.playerclass;

import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.resources.ResourceLocation;

/**
 * Registry for player data attachments.
 * Uses Fabric's Attachment API to persist data with players.
 */
public class PlayerDataRegistry {

    // The attachment type that holds our player data
    @SuppressWarnings("deprecation")
    public static final AttachmentType<PlayerData> PLAYER_DATA = AttachmentRegistry.<PlayerData>builder()
            .persistent(PlayerData.CODEC)
            .copyOnDeath() // Keep class data when player dies!
            .initializer(() -> PlayerData.DEFAULT)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "player_data"));

    // Attachment for Mob Data
    @SuppressWarnings("deprecation")
    public static final AttachmentType<com.gianmarco.wowcraft.entity.MobData> MOB_DATA = AttachmentRegistry.<com.gianmarco.wowcraft.entity.MobData>builder()
            .persistent(com.gianmarco.wowcraft.entity.MobData.CODEC)
            .initializer(() -> com.gianmarco.wowcraft.entity.MobData.DEFAULT)
            .buildAndRegister(ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "mob_data"));

    /**
     * Called during mod initialization to ensure the registry is loaded
     */
    public static void register() {
        WowCraft.LOGGER.info("Registering WowCraft data attachments...");
    }
}

package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.UUID;

/**
 * Handles social aggro when a player attacks a pack mob.
 * Nearby pack members within the social aggro radius will also target the
 * player.
 */
public class SocialAggroHandler {

    /**
     * Called when a player damages a mob.
     * Checks if the mob is part of a pack and triggers social aggro.
     */
    public static void onPlayerDamageMob(Player player, LivingEntity target, ServerLevel level) {
        if (!(target instanceof Mob mob)) {
            return;
        }

        // Check if target is a pack mob
        MobData data = mob.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data == null || data.packId() == null) {
            return; // Not a pack mob
        }

        // Get the pack
        SpawnedMobPack pack = MobPackManager.getPack(data.packId());
        if (pack == null) {
            return;
        }

        // Get all alive pack mobs within social aggro radius of the attacked mob
        List<UUID> nearbyMobIds = pack.getAliveMobsWithinRadius(
                level,
                mob.blockPosition(),
                pack.getSocialAggroRadius());

        // Set aggro for nearby mobs
        int aggroCount = 0;
        for (UUID mobId : nearbyMobIds) {
            if (mobId.equals(mob.getUUID())) {
                continue; // Skip the mob that was directly attacked
            }

            // Find the entity in the world
            var entity = level.getEntity(mobId);
            if (entity instanceof Mob nearbyMob && nearbyMob.isAlive()) {
                // Set the player as target
                nearbyMob.setTarget(player);
                aggroCount++;
            }
        }

        if (aggroCount > 0) {
            WowCraft.LOGGER.debug("Social aggro triggered: {} additional mobs targeting {}",
                    aggroCount, player.getName().getString());
        }
    }
}

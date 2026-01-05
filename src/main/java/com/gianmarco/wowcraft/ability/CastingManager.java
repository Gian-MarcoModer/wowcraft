package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.network.CastingUpdatePacket;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Manages casting spells with cast times.
 * Tracks active casts, handles interrupts on movement, and executes abilities
 * on completion.
 */
public class CastingManager {

    private static final Map<UUID, ActiveCast> activeCasts = new HashMap<>();

    /**
     * Data for an active cast
     */
    public record ActiveCast(
            Ability ability,
            int startTick,
            int castTimeTicks,
            Vec3 startPosition) {
        public float getProgress(int currentTick) {
            return (float) (currentTick - startTick) / castTimeTicks;
        }

        public boolean isComplete(int currentTick) {
            return currentTick - startTick >= castTimeTicks;
        }
    }

    /**
     * Register the tick handler for cast progression
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int currentTick = server.getTickCount();

            Iterator<Map.Entry<UUID, ActiveCast>> iter = activeCasts.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<UUID, ActiveCast> entry = iter.next();
                UUID playerId = entry.getKey();
                ActiveCast cast = entry.getValue();

                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null) {
                    iter.remove();
                    continue;
                }

                // Check for movement interrupt
                Vec3 currentPos = player.position();
                double movedDistance = currentPos.distanceTo(cast.startPosition);
                if (movedDistance > 0.1) {
                    // Player moved - interrupt cast
                    iter.remove();
                    player.sendSystemMessage(Component.literal("§cCast interrupted - you moved!"));
                    sendCastUpdate(player, null, 0, 0, true);
                    WowCraft.LOGGER.info("Cast interrupted for {} - moved", player.getName().getString());
                    continue;
                }

                // Check if cast is complete
                if (cast.isComplete(currentTick)) {
                    iter.remove();
                    // Execute the ability
                    cast.ability.execute(player);
                    sendCastUpdate(player, null, 0, 0, false);
                    WowCraft.LOGGER.info("Cast complete for {} - {}",
                            player.getName().getString(), cast.ability.getDisplayName());
                } else {
                    // Update client with progress
                    float progress = cast.getProgress(currentTick);
                    sendCastUpdate(player, cast.ability.getDisplayName(),
                            (float) cast.castTimeTicks / 20, progress, false);

                    // Spawn casting particles every 5 ticks
                    if ((currentTick - cast.startTick) % 5 == 0) {
                        spawnCastingParticles(player, cast.ability.getId());
                    }
                }
            }
        });

        WowCraft.LOGGER.info("Registered CastingManager");
    }

    /**
     * Start casting an ability with cast time
     */
    public static boolean startCast(Player player, Ability ability, float castTimeSeconds) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }

        // Check if already casting
        if (activeCasts.containsKey(player.getUUID())) {
            serverPlayer.sendSystemMessage(Component.literal("§cAlready casting!"));
            return false;
        }

        int castTimeTicks = (int) (castTimeSeconds * 20);
        int currentTick = serverPlayer.server.getTickCount();

        ActiveCast cast = new ActiveCast(
                ability,
                currentTick,
                castTimeTicks,
                player.position());

        activeCasts.put(player.getUUID(), cast);

        // Notify player
        serverPlayer.sendSystemMessage(Component.literal(
                String.format("§6Casting %s... (%.1fs)", ability.getDisplayName(), castTimeSeconds)));

        // Send initial cast update to client
        sendCastUpdate(serverPlayer, ability.getDisplayName(), castTimeSeconds, 0, false);

        WowCraft.LOGGER.info("Started casting {} for {} ({}s)",
                ability.getDisplayName(), player.getName().getString(), castTimeSeconds);

        return true;
    }

    /**
     * Cancel any active cast for a player
     */
    public static void cancelCast(Player player) {
        if (activeCasts.remove(player.getUUID()) != null) {
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.sendSystemMessage(Component.literal("§cCast cancelled"));
                sendCastUpdate(serverPlayer, null, 0, 0, true);
            }
        }
    }

    /**
     * Check if player is currently casting
     */
    public static boolean isCasting(Player player) {
        return activeCasts.containsKey(player.getUUID());
    }

    /**
     * Send cast update packet to client for UI
     */
    private static void sendCastUpdate(ServerPlayer player, String spellName,
            float castDuration, float progress, boolean interrupted) {
        try {
            ServerPlayNetworking.send(player, new CastingUpdatePacket(
                    spellName != null ? spellName : "",
                    castDuration,
                    progress,
                    interrupted));
        } catch (Exception e) {
            // Packet type might not be registered yet, ignore
        }
    }

    /**
     * Spawn particles during casting for visual feedback
     */
    private static void spawnCastingParticles(ServerPlayer player, String abilityId) {
        if (!(player.level() instanceof net.minecraft.server.level.ServerLevel serverLevel)) {
            return;
        }

        // Choose particles based on ability
        net.minecraft.core.particles.ParticleOptions particle = switch (abilityId) {
            case "fireball" -> net.minecraft.core.particles.ParticleTypes.FLAME;
            case "frostnova" -> net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
            default -> net.minecraft.core.particles.ParticleTypes.ENCHANT;
        };

        // Spawn particles around hands/body
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Spiral effect around the player
        double angle = (System.currentTimeMillis() / 100.0) % (Math.PI * 2);
        double offsetX = Math.cos(angle) * 0.5;
        double offsetZ = Math.sin(angle) * 0.5;

        serverLevel.sendParticles(particle,
                x + offsetX, y, z + offsetZ,
                3, 0.1, 0.2, 0.1, 0.02);
    }
}

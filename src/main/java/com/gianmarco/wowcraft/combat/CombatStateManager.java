package com.gianmarco.wowcraft.combat;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Manages combat state for players.
 * Players enter combat when they attack, take damage, use offensive abilities,
 * or when a mob is targeting them.
 * Players leave combat after 6 seconds of no combat activity.
 * 
 * WoW Classic style:
 * - Health regeneration only happens out of combat
 * - Rage decays only out of combat
 * - Mana regeneration follows the "5 second rule" (full regen after 5s of no casting)
 */
public class CombatStateManager {

    // Time in ticks before dropping combat (6 seconds = 120 ticks)
    private static final long COMBAT_TIMEOUT_TICKS = 120;
    
    // How often to check for mob aggro (every 10 ticks = 0.5 seconds)
    private static final int AGGRO_CHECK_INTERVAL = 10;
    
    // Range to check for mobs targeting the player (32 blocks)
    private static final double AGGRO_CHECK_RANGE = 32.0;

    // Track last combat activity time for each player
    private static final Map<UUID, Long> lastCombatTime = new HashMap<>();

    // Track last spell cast time for mana regen (5 second rule)
    private static final Map<UUID, Long> lastSpellCastTime = new HashMap<>();

    // Track previous combat state to detect transitions
    private static final Map<UUID, Boolean> wasInCombat = new HashMap<>();

    // Current server tick (updated by tick handler)
    private static long currentTick = 0;

    /**
     * Update the current server tick and check for mob aggro
     */
    public static void tick() {
        currentTick++;
    }

    /**
     * Check if any mobs are targeting a player and put them in combat.
     * Should be called periodically (e.g., every 10 ticks) for performance.
     * Also checks for combat state transitions to sync with client.
     */
    public static void checkForAggroingMobs(ServerPlayer player) {
        // Check every AGGRO_CHECK_INTERVAL ticks
        if (currentTick % AGGRO_CHECK_INTERVAL != 0) {
            return;
        }

        // Get all mobs in range
        AABB searchBox = player.getBoundingBox().inflate(AGGRO_CHECK_RANGE);
        List<Mob> nearbyMobs = player.level().getEntitiesOfClass(Mob.class, searchBox);

        // Check if any mob is targeting this player
        for (Mob mob : nearbyMobs) {
            if (mob.getTarget() == player && mob.isAlive()) {
                // A mob is targeting us - enter combat!
                enterCombat(player);
                return; // Only need one mob to trigger combat
            }
        }

        // Check for combat state transitions (entering/leaving combat)
        checkCombatStateTransition(player);
    }

    /**
     * Check if player's combat state has changed and sync to client if needed
     */
    private static void checkCombatStateTransition(ServerPlayer player) {
        UUID uuid = player.getUUID();
        boolean currentlyInCombat = isInCombat(player);
        Boolean previousState = wasInCombat.get(uuid);

        // First time checking this player, just record state
        if (previousState == null) {
            wasInCombat.put(uuid, currentlyInCombat);
            return;
        }

        // Detect transition OUT of combat (most important for client sync)
        if (previousState && !currentlyInCombat) {
            // Just left combat - sync to client
            Long lastTime = lastCombatTime.get(uuid);
            com.gianmarco.wowcraft.network.NetworkHandler.syncCombatState(
                    player, false, lastTime != null ? lastTime : 0);
            wasInCombat.put(uuid, false);
        } else if (!previousState && currentlyInCombat) {
            // Just entered combat - update state (already synced in enterCombat)
            wasInCombat.put(uuid, true);
        }
    }

    /**
     * Called when a player performs a combat action (attack, ability use, etc.)
     */
    public static void enterCombat(Player player) {
        lastCombatTime.put(player.getUUID(), currentTick);

        // Sync to client if this is a server player
        if (player instanceof ServerPlayer serverPlayer) {
            com.gianmarco.wowcraft.network.NetworkHandler.syncCombatState(
                    serverPlayer, true, currentTick);
        }
    }

    /**
     * Called when a player takes damage
     */
    public static void onDamageTaken(Player player) {
        lastCombatTime.put(player.getUUID(), currentTick);

        // Sync to client if this is a server player
        if (player instanceof ServerPlayer serverPlayer) {
            com.gianmarco.wowcraft.network.NetworkHandler.syncCombatState(
                    serverPlayer, true, currentTick);
        }
    }

    /**
     * Called when a player casts a spell (for 5 second mana rule)
     */
    public static void onSpellCast(Player player) {
        lastCombatTime.put(player.getUUID(), currentTick);
        lastSpellCastTime.put(player.getUUID(), currentTick);

        // Sync to client if this is a server player
        if (player instanceof ServerPlayer serverPlayer) {
            com.gianmarco.wowcraft.network.NetworkHandler.syncCombatState(
                    serverPlayer, true, currentTick);
        }
    }

    /**
     * Check if a player is currently in combat
     */
    public static boolean isInCombat(Player player) {
        Long lastTime = lastCombatTime.get(player.getUUID());
        if (lastTime == null) {
            return false;
        }
        return (currentTick - lastTime) < COMBAT_TIMEOUT_TICKS;
    }

    /**
     * Check if a player is out of combat (for health regen, rage decay)
     */
    public static boolean isOutOfCombat(Player player) {
        return !isInCombat(player);
    }

    /**
     * Get time out of combat in ticks (for regen calculations)
     */
    public static long getTicksOutOfCombat(Player player) {
        Long lastTime = lastCombatTime.get(player.getUUID());
        if (lastTime == null) {
            return Long.MAX_VALUE; // Never been in combat
        }
        long ticksSince = currentTick - lastTime;
        if (ticksSince < COMBAT_TIMEOUT_TICKS) {
            return 0; // Still in combat
        }
        return ticksSince - COMBAT_TIMEOUT_TICKS;
    }

    /**
     * Check if 5 seconds have passed since last spell cast (for full mana regen)
     * WoW's "5 second rule" - mana regens at full rate if not casting
     */
    public static boolean canFullManaRegen(Player player) {
        Long lastCast = lastSpellCastTime.get(player.getUUID());
        if (lastCast == null) {
            return true; // Never cast a spell
        }
        // 5 seconds = 100 ticks
        return (currentTick - lastCast) >= 100;
    }

    /**
     * Clear combat state for a player (on logout, death, etc.)
     */
    public static void clearState(Player player) {
        UUID uuid = player.getUUID();
        lastCombatTime.remove(uuid);
        lastSpellCastTime.remove(uuid);
        wasInCombat.remove(uuid);
    }

    /**
     * Get the current tick
     */
    public static long getCurrentTick() {
        return currentTick;
    }
}


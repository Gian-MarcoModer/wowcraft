package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.entity.MobLevelManager;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;

/**
 * Dynamic aggro range goal that adjusts detection range based on level difference.
 *
 * Formula:
 * - Base range: 16 blocks
 * - Add 1 block per level mob is ABOVE player (max +8 to reach 24)
 * - Subtract 1 block per level player is ABOVE mob (min 4 blocks)
 *
 * Examples:
 * - Player L10 vs Mob L10 → 16 blocks
 * - Player L10 vs Mob L15 → 21 blocks (higher level = more alert)
 * - Player L15 vs Mob L10 → 11 blocks (player outlevels = less threatening)
 * - Player L20 vs Mob L10 → 6 blocks (very safe in low zone)
 * - Player L5 vs Mob L20 → 24 blocks (max range, very dangerous)
 */
public class DynamicAggroRangeGoal extends NearestAttackableTargetGoal<Player> {

    private static final int BASE_AGGRO_RANGE = 16;
    private static final int MIN_AGGRO_RANGE = 4;
    private static final int MAX_AGGRO_RANGE = 24;

    private final Mob mob;
    private int mobLevel;
    private int ticksSinceLastUpdate = 0;
    private static final int UPDATE_INTERVAL = 20; // Update every 1 second

    public DynamicAggroRangeGoal(Mob mob) {
        super(mob, Player.class, true);
        this.mob = mob;
        this.mobLevel = MobLevelManager.getMobLevel(mob);
        this.setFlags(EnumSet.of(Goal.Flag.TARGET));

        // Set initial follow range to max (will be dynamically adjusted)
        updateFollowRange(MAX_AGGRO_RANGE);
    }

    @Override
    public boolean canUse() {
        // Update aggro range periodically
        ticksSinceLastUpdate++;
        if (ticksSinceLastUpdate >= UPDATE_INTERVAL) {
            updateDynamicAggroRange();
            ticksSinceLastUpdate = 0;
        }

        return super.canUse();
    }

    /**
     * Dynamically update follow range based on nearest player's level.
     */
    private void updateDynamicAggroRange() {
        // Find nearest player to calculate appropriate aggro range
        Player nearestPlayer = mob.level().getNearestPlayer(mob, MAX_AGGRO_RANGE);

        if (nearestPlayer == null) {
            // No players nearby, use max range for detection
            updateFollowRange(MAX_AGGRO_RANGE);
            return;
        }

        // Get player level
        int playerLevel = 1;
        if (nearestPlayer.hasAttached(PlayerDataRegistry.PLAYER_DATA)) {
            PlayerData data = nearestPlayer.getAttached(PlayerDataRegistry.PLAYER_DATA);
            playerLevel = data.level();
        }

        // Calculate dynamic range
        int levelDiff = mobLevel - playerLevel;
        int aggroRange = BASE_AGGRO_RANGE + levelDiff;

        // Clamp to min/max
        aggroRange = Math.max(MIN_AGGRO_RANGE, Math.min(MAX_AGGRO_RANGE, aggroRange));

        // Update the mob's follow range attribute
        updateFollowRange(aggroRange);
    }

    /**
     * Update the mob's FOLLOW_RANGE attribute.
     */
    private void updateFollowRange(double range) {
        var followRangeAttr = mob.getAttribute(Attributes.FOLLOW_RANGE);
        if (followRangeAttr != null && followRangeAttr.getBaseValue() != range) {
            followRangeAttr.setBaseValue(range);
        }
    }

    @Override
    protected double getFollowDistance() {
        // Use the dynamically updated follow range
        var attr = mob.getAttribute(Attributes.FOLLOW_RANGE);
        return attr != null ? attr.getValue() : BASE_AGGRO_RANGE;
    }
}

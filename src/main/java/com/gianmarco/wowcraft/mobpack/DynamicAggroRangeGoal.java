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
 * Based on WoW Classic's ~20 yard base aggro (≈18 blocks).
 *
 * Formula:
 * - Base range: 10 blocks (~11 yards)
 * - Add 0.5 blocks per level mob is ABOVE player (max 18 blocks)
 * - Subtract 0.5 blocks per level player is ABOVE mob (min 2 blocks)
 *
 * Examples:
 * - Player L10 vs Mob L10 → 10 blocks (~11 yards)
 * - Player L10 vs Mob L15 → 12.5 blocks (higher level = more alert)
 * - Player L15 vs Mob L10 → 7.5 blocks (player outlevels = safer)
 * - Player L20 vs Mob L10 → 5 blocks (very safe in low zone)
 * - Player L25 vs Mob L10 → 2.5 blocks (practically ignore you)
 * - Player L5 vs Mob L15 → 15 blocks (dangerous, high aggro)
 * - Player L1 vs Mob L17+ → 18 blocks (max range, WoW Classic cap ~20 yards)
 */
public class DynamicAggroRangeGoal extends NearestAttackableTargetGoal<Player> {

    private static final int BASE_AGGRO_RANGE = 10;
    private static final int MIN_AGGRO_RANGE = 2;
    private static final int MAX_AGGRO_RANGE = 18;

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

        // Calculate dynamic range with gentler scaling (0.5 blocks per level)
        int levelDiff = mobLevel - playerLevel;
        double aggroRange = BASE_AGGRO_RANGE + (levelDiff * 0.5);

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

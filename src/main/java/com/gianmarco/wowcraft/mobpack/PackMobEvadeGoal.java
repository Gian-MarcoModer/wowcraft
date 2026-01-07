package com.gianmarco.wowcraft.mobpack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

import java.util.EnumSet;

/**
 * High-priority goal that forces pack mobs to return home when evading.
 * Overrides all other goals to ensure the mob returns to spawn.
 */
public class PackMobEvadeGoal extends Goal {

    private final Mob mob;
    private BlockPos homePos;
    private boolean isEvading = false;

    public PackMobEvadeGoal(Mob mob) {
        this.mob = mob;
        // HIGHEST PRIORITY - blocks movement and targeting
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK, Goal.Flag.TARGET));
    }

    /**
     * Start evading - called externally when leash timer expires.
     */
    public void startEvade(BlockPos home) {
        this.homePos = home;
        this.isEvading = true;
    }

    /**
     * Stop evading - called when mob reaches home.
     */
    public void stopEvade() {
        this.isEvading = false;
        this.homePos = null;
    }

    /**
     * Check if currently evading.
     */
    public boolean isEvading() {
        return isEvading;
    }

    @Override
    public boolean canUse() {
        return isEvading && homePos != null;
    }

    @Override
    public boolean canContinueToUse() {
        return isEvading && homePos != null;
    }

    @Override
    public void start() {
        // Clear target when evade starts
        mob.setTarget(null);
        mob.setLastHurtByMob(null);
        com.gianmarco.wowcraft.WowCraft.LOGGER.info("EVADE GOAL STARTED for {}", mob.getName().getString());
    }

    @Override
    public void tick() {
        if (homePos == null) {
            return;
        }

        // Force clear target every tick to prevent re-aggro
        if (mob.getTarget() != null) {
            mob.setTarget(null);
            com.gianmarco.wowcraft.WowCraft.LOGGER.info("EVADE: Cleared target for {}", mob.getName().getString());
        }

        // Check if reached home
        double distanceFromHome = mob.position().distanceTo(homePos.getCenter());
        if (distanceFromHome <= 2.0) {
            // Stop evading - will be handled by PackMobGoalMixin
            com.gianmarco.wowcraft.WowCraft.LOGGER.info("EVADE: {} reached home, stopping evade", mob.getName().getString());
            return;
        }

        // Force pathfind to home every tick for reliable return
        mob.getNavigation().moveTo(homePos.getX() + 0.5, homePos.getY(), homePos.getZ() + 0.5, 1.5);
    }

    @Override
    public void stop() {
        com.gianmarco.wowcraft.WowCraft.LOGGER.info("EVADE GOAL STOPPED for {}", mob.getName().getString());
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true; // Update every tick for responsive pathfinding
    }
}

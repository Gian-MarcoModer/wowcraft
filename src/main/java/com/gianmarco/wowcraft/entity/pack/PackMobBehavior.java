package com.gianmarco.wowcraft.entity.pack;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Reusable behavior component for pack mobs.
 * Handles leash enforcement, evade mechanics, health regen, and speed boosts.
 */
public class PackMobBehavior {

    private static final double LEASH_RANGE = 24.0;
    private static final int LEASH_RESET_TICKS = 100; // 5 seconds
    private static final int HEALTH_REGEN_TICKS = 100; // 5 seconds

    private final Mob mob;
    private BlockPos homePosition;
    private boolean isEvading = false;

    private int ticksOutOfLeashRange = 0;
    private int regenTicksRemaining = 0;
    private float healthPerTick = 0;
    private boolean wasAggroed = false;

    public PackMobBehavior(Mob mob) {
        this.mob = mob;
    }

    public BlockPos getHomePosition() {
        return homePosition;
    }

    public void setHomePosition(BlockPos pos) {
        this.homePosition = pos;
    }

    public boolean isEvading() {
        return isEvading;
    }

    public void startEvade() {
        this.isEvading = true;
        mob.setTarget(null);
        mob.setLastHurtByMob(null);

        // Start health regeneration
        float healthToRegen = mob.getMaxHealth() - mob.getHealth();
        this.healthPerTick = healthToRegen / HEALTH_REGEN_TICKS;
        this.regenTicksRemaining = HEALTH_REGEN_TICKS;

        // Increase movement speed
        var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && !speedAttr.hasModifier(getSpeedModifier().id())) {
            speedAttr.addPermanentModifier(getSpeedModifier());
        }

        WowCraft.LOGGER.info("{} entering EVADE mode!", mob.getName().getString());
    }

    public void stopEvade() {
        this.isEvading = false;
        this.wasAggroed = false;
        this.ticksOutOfLeashRange = 0;

        // Remove speed boost
        var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr != null && speedAttr.hasModifier(getSpeedModifier().id())) {
            speedAttr.removeModifier(getSpeedModifier().id());
        }

        WowCraft.LOGGER.info("{} stopped evading", mob.getName().getString());
    }

    public double getLeashRange() {
        return LEASH_RANGE;
    }

    public int getLeashResetTicks() {
        return LEASH_RESET_TICKS;
    }

    /**
     * Main tick method - call this every tick from customServerAiStep.
     * Optimized to skip unnecessary calculations when mob is idle.
     */
    public void tick() {
        if (homePosition == null) {
            return;
        }

        boolean hasTarget = mob.getTarget() != null;
        boolean needsHealthRegen = regenTicksRemaining > 0;

        // Performance optimization: Skip expensive calculations if mob is idle at home
        if (!isEvading && !hasTarget && !wasAggroed && !needsHealthRegen) {
            return; // Mob is completely idle, nothing to do
        }

        // Only calculate distance when needed (evading, has target, or returning home)
        double distanceFromHome = mob.position().distanceTo(homePosition.getCenter());

        // Track damage only when in combat
        boolean tookDamageRecently = false;
        if (hasTarget) {
            int ticksSinceLastHurt = mob.tickCount - mob.getLastHurtByMobTimestamp();
            tookDamageRecently = ticksSinceLastHurt < 2;
        }

        // Handle evade state
        if (isEvading) {
            handleEvadeMode(distanceFromHome);
        } else if (hasTarget) {
            handleLeashLogic(distanceFromHome, tookDamageRecently);
        } else {
            // No target - reset timer
            ticksOutOfLeashRange = 0;
            handleReturnSpeed(distanceFromHome);
        }

        // Regenerate health over time
        if (needsHealthRegen) {
            mob.setHealth(Math.min(mob.getMaxHealth(), mob.getHealth() + healthPerTick));
            regenTicksRemaining--;
        }
    }

    private void handleEvadeMode(double distanceFromHome) {
        // Force clear target every tick
        if (mob.getTarget() != null) {
            mob.setTarget(null);
        }

        // Force pathfind to home every 5 ticks
        if (mob.tickCount % 5 == 0) {
            mob.getNavigation().moveTo(
                    homePosition.getX() + 0.5,
                    homePosition.getY(),
                    homePosition.getZ() + 0.5,
                    1.5
            );
        }

        // Check if reached home
        if (distanceFromHome <= 2.0) {
            stopEvade();
        }
    }

    private void handleLeashLogic(double distanceFromHome, boolean tookDamageRecently) {
        boolean isBeyondLeash = distanceFromHome > LEASH_RANGE;

        if (isBeyondLeash) {
            // Damage resets the timer
            if (tookDamageRecently) {
                ticksOutOfLeashRange = 0;
            } else {
                ticksOutOfLeashRange++;
            }

            // Debug logging
            if (ticksOutOfLeashRange % 20 == 0) {
                WowCraft.LOGGER.info("{} at distance {} from home, timer: {}/{}",
                        mob.getName().getString(), distanceFromHome, ticksOutOfLeashRange, LEASH_RESET_TICKS);
            }

            // Check if timer expired
            if (ticksOutOfLeashRange >= LEASH_RESET_TICKS) {
                startEvade();
                wasAggroed = true;
            }
        } else {
            // Within leash range
            ticksOutOfLeashRange = 0;

            // Reset aggro flag and remove speed boost when mob gets aggroed
            wasAggroed = false;
            var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && speedAttr.hasModifier(getSpeedModifier().id())) {
                speedAttr.removeModifier(getSpeedModifier().id());
            }
        }
    }

    private void handleReturnSpeed(double distanceFromHome) {
        // If mob just lost aggro, boost speed to return home
        if (wasAggroed && distanceFromHome > 2.0) {
            var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && !speedAttr.hasModifier(getSpeedModifier().id())) {
                speedAttr.addPermanentModifier(getSpeedModifier());
            }
        } else if (distanceFromHome <= 2.0) {
            // Reached home, remove speed boost
            wasAggroed = false;
            var speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
            if (speedAttr != null && speedAttr.hasModifier(getSpeedModifier().id())) {
                speedAttr.removeModifier(getSpeedModifier().id());
            }
        }
    }

    private static AttributeModifier getSpeedModifier() {
        return new AttributeModifier(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wowcraft", "pack_return_speed"),
                0.5, // 50% speed increase
                AttributeModifier.Operation.ADD_MULTIPLIED_BASE
        );
    }
}

package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.Goal;

/**
 * AI Goal that prevents pack mobs from wandering when not aggroed.
 * This goal has high priority and cancels all movement when mob has no target.
 */
public class PackMobStationaryGoal extends Goal {
    private final Mob mob;

    public PackMobStationaryGoal(Mob mob) {
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
        // Check if this is a pack mob
        MobData data = mob.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data == null || data.packId() == null) {
            return false; // Not a pack mob
        }

        // Active when mob has no target (not aggroed)
        return mob.getTarget() == null;
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void start() {
        // Stop all movement
        mob.getNavigation().stop();
    }

    @Override
    public void tick() {
        // Keep stopping movement while goal is active
        mob.getNavigation().stop();
        mob.setDeltaMovement(0, mob.getDeltaMovement().y, 0); // Keep Y for gravity
    }
}

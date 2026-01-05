package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.mobpack.DynamicAggroRangeGoal;
import com.gianmarco.wowcraft.mobpack.PackMobStationaryGoal;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to add stationary AI goal and leash enforcement to pack mobs.
 */
@Mixin(Mob.class)
public abstract class PackMobGoalMixin {

    @Shadow
    protected net.minecraft.world.entity.ai.goal.GoalSelector goalSelector;

    @Shadow
    protected net.minecraft.world.entity.ai.goal.GoalSelector targetSelector;

    @Unique
    private static final double LEASH_RANGE = 24.0;
    @Unique
    private static final int LEASH_RESET_TICKS = 100; // 5 seconds out of leash range
    @Unique
    private static final int HEALTH_REGEN_TICKS = 100; // 5 seconds to regen health
    @Unique
    private int wowcraft$ticksOutOfLeashRange = 0; // Track time spent beyond leash
    @Unique
    private int wowcraft$regenTicksRemaining = 0;
    @Unique
    private float wowcraft$healthPerTick = 0;
    @Unique
    private boolean wowcraft$wasAggroed = false;

    /**
     * Add stationary goal to pack mobs after they're initialized.
     */
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void wowcraft$addPackGoals(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason, SpawnGroupData spawnData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        Mob self = (Mob) (Object) this;

        // Check if this is a pack mob
        MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data != null && data.packId() != null) {
            // Add dynamic aggro range goal with priority (1) - targets players based on level
            targetSelector.addGoal(1, new DynamicAggroRangeGoal(self));

            // Add stationary goal with priority (0) - runs when not aggroed
            goalSelector.addGoal(0, new PackMobStationaryGoal(self));
        }
    }

    /**
     * Enforce leash range on every tick for pack mobs.
     */
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void wowcraft$enforceLeashRange(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        // Check if this is a pack mob
        MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data == null || data.packId() == null) {
            return;
        }

        // Check if mob is too far from home position
        BlockPos home = self.getRestrictCenter();
        if (home != null) {
            double distanceFromHome = self.position().distanceTo(home.getCenter());
            boolean isBeyondLeash = distanceFromHome > LEASH_RANGE;
            boolean hasTarget = self.getTarget() != null;

            // Track if mob took damage this tick
            int ticksSinceLastHurt = self.tickCount - self.getLastHurtByMobTimestamp();
            boolean tookDamageRecently = ticksSinceLastHurt < 2; // Within last 2 ticks

            if (hasTarget && isBeyondLeash) {
                // Beyond leash range with a target

                if (tookDamageRecently) {
                    // Damage resets the timer back to 5 seconds
                    wowcraft$ticksOutOfLeashRange = 0;
                } else {
                    // Increment timer
                    wowcraft$ticksOutOfLeashRange++;
                }

                // Check if timer expired (5 seconds out of leash range)
                if (wowcraft$ticksOutOfLeashRange >= LEASH_RESET_TICKS) {
                    // Reset aggro
                    self.setTarget(null);
                    self.setLastHurtByMob(null);
                    wowcraft$wasAggroed = true;
                    wowcraft$ticksOutOfLeashRange = 0;

                    // Start health regeneration
                    float healthToRegen = self.getMaxHealth() - self.getHealth();
                    wowcraft$healthPerTick = healthToRegen / HEALTH_REGEN_TICKS;
                    wowcraft$regenTicksRemaining = HEALTH_REGEN_TICKS;

                    // Increase movement speed temporarily
                    var speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speedAttr != null && !speedAttr.hasModifier(wowcraft$getSpeedModifier().id())) {
                        speedAttr.addPermanentModifier(wowcraft$getSpeedModifier());
                    }
                }
            } else {
                // Not beyond leash or no target - reset timer
                wowcraft$ticksOutOfLeashRange = 0;
            }

            // If mob just lost aggro, boost speed
            if (wowcraft$wasAggroed && !hasTarget && distanceFromHome > 2.0) {
                var speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && !speedAttr.hasModifier(wowcraft$getSpeedModifier().id())) {
                    speedAttr.addPermanentModifier(wowcraft$getSpeedModifier());
                }
            } else if (hasTarget) {
                // Reset flag when mob gets aggroed again
                wowcraft$wasAggroed = false;
                // Remove speed boost when mob gets aggroed
                var speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.hasModifier(wowcraft$getSpeedModifier().id())) {
                    speedAttr.removeModifier(wowcraft$getSpeedModifier().id());
                }
            } else if (distanceFromHome <= 2.0) {
                // Reached home, remove speed boost
                wowcraft$wasAggroed = false;
                var speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
                if (speedAttr != null && speedAttr.hasModifier(wowcraft$getSpeedModifier().id())) {
                    speedAttr.removeModifier(wowcraft$getSpeedModifier().id());
                }
            }
        }

        // Regenerate health over time
        if (wowcraft$regenTicksRemaining > 0) {
            self.setHealth(Math.min(self.getMaxHealth(), self.getHealth() + wowcraft$healthPerTick));
            wowcraft$regenTicksRemaining--;
        }
    }

    @Unique
    private static net.minecraft.world.entity.ai.attributes.AttributeModifier wowcraft$getSpeedModifier() {
        return new net.minecraft.world.entity.ai.attributes.AttributeModifier(
                net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wowcraft", "pack_return_speed"),
                0.5, // 50% speed increase
                net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_MULTIPLIED_BASE);
    }
}

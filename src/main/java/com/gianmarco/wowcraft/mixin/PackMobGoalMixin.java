package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.mobpack.DynamicAggroRangeGoal;
import com.gianmarco.wowcraft.mobpack.PackMobEvadeGoal;
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

    @Shadow
    public abstract net.minecraft.world.entity.ai.navigation.PathNavigation getNavigation();

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
    @Unique
    private PackMobEvadeGoal wowcraft$evadeGoal = null; // Evade goal for forced return

    /**
     * Add dynamic aggro and stationary goals to vanilla pack mobs.
     * Custom pack entities (PackZombie, PackSkeleton, PackSpider) don't use this - they have built-in behavior.
     */
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void wowcraft$addPackGoals(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason, SpawnGroupData spawnData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        Mob self = (Mob) (Object) this;

        // Check if this is a pack mob (and not a custom entity with its own behavior)
        MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
        boolean isCustomPackEntity = self instanceof com.gianmarco.wowcraft.entity.pack.IPackMob;

        if (data != null && data.packId() != null && !isCustomPackEntity) {
            // Add dynamic aggro range goal with priority (1) - targets players based on level
            targetSelector.addGoal(1, new DynamicAggroRangeGoal(self));

            // Add stationary goal with priority (0) - runs when not aggroed
            goalSelector.addGoal(0, new PackMobStationaryGoal(self));

            WowCraft.LOGGER.debug("Added vanilla pack goals to {}", self.getName().getString());
        }
    }

    /**
     * Enforce leash range on every tick for vanilla pack mobs.
     * Custom pack entities handle their own behavior via PackMobBehavior.
     */
    @Inject(method = "customServerAiStep", at = @At("TAIL"))
    private void wowcraft$enforceLeashRange(CallbackInfo ci) {
        Mob self = (Mob) (Object) this;

        // Check if this is a vanilla pack mob (not a custom entity)
        MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
        boolean isCustomPackEntity = self instanceof com.gianmarco.wowcraft.entity.pack.IPackMob;

        if (data == null || data.packId() == null || isCustomPackEntity) {
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

            // Handle evade state (WoW-style forced return)
            if (wowcraft$evadeGoal != null && wowcraft$evadeGoal.isEvading()) {
                // Check if reached home
                if (distanceFromHome <= 2.0) {
                    // Reached home, exit evade
                    wowcraft$evadeGoal.stopEvade();
                    wowcraft$wasAggroed = false;
                    wowcraft$ticksOutOfLeashRange = 0;

                    // Remove speed boost
                    var speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speedAttr != null && speedAttr.hasModifier(wowcraft$getSpeedModifier().id())) {
                        speedAttr.removeModifier(wowcraft$getSpeedModifier().id());
                    }
                }
            } else if (hasTarget && isBeyondLeash) {
                // Beyond leash range with a target (not evading yet)

                if (tookDamageRecently) {
                    // Damage resets the timer back to 5 seconds
                    wowcraft$ticksOutOfLeashRange = 0;
                } else {
                    // Increment timer
                    wowcraft$ticksOutOfLeashRange++;
                }

                // Debug logging
                if (wowcraft$ticksOutOfLeashRange % 20 == 0) {
                    com.gianmarco.wowcraft.WowCraft.LOGGER.info("Wolf {} at distance {} from home, timer: {}/{}",
                        self.getName().getString(), distanceFromHome, wowcraft$ticksOutOfLeashRange, LEASH_RESET_TICKS);
                }

                // Check if timer expired (5 seconds out of leash range)
                if (wowcraft$ticksOutOfLeashRange >= LEASH_RESET_TICKS) {
                    com.gianmarco.wowcraft.WowCraft.LOGGER.info("Wolf {} entering EVADE mode!", self.getName().getString());
                    // Enter evade mode
                    if (wowcraft$evadeGoal != null) {
                        wowcraft$evadeGoal.startEvade(home);
                    }
                    wowcraft$wasAggroed = true;
                    wowcraft$ticksOutOfLeashRange = 0;

                    // Clear target
                    self.setTarget(null);
                    self.setLastHurtByMob(null);

                    // Start health regeneration
                    float healthToRegen = self.getMaxHealth() - self.getHealth();
                    wowcraft$healthPerTick = healthToRegen / HEALTH_REGEN_TICKS;
                    wowcraft$regenTicksRemaining = HEALTH_REGEN_TICKS;

                    // Increase movement speed
                    var speedAttr = self.getAttribute(Attributes.MOVEMENT_SPEED);
                    if (speedAttr != null && !speedAttr.hasModifier(wowcraft$getSpeedModifier().id())) {
                        speedAttr.addPermanentModifier(wowcraft$getSpeedModifier());
                    }
                }
            } else if (hasTarget) {
                // Within leash range but has target - log for debugging
                if (self.tickCount % 60 == 0) {
                    com.gianmarco.wowcraft.WowCraft.LOGGER.info("Wolf {} has target but within leash (distance: {})",
                        self.getName().getString(), distanceFromHome);
                }
                wowcraft$ticksOutOfLeashRange = 0;
            } else {
                // No target - reset timer
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

    /**
     * Block target acquisition during evade mode for vanilla pack mobs.
     * Custom pack entities handle this in their own setTarget override.
     */
    @Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
    private void wowcraft$blockTargetDuringEvade(net.minecraft.world.entity.LivingEntity target, CallbackInfo ci) {
        Mob self = (Mob) (Object) this;
        boolean isCustomPackEntity = self instanceof com.gianmarco.wowcraft.entity.pack.IPackMob;

        // Only apply to vanilla pack mobs
        if (!isCustomPackEntity && wowcraft$evadeGoal != null && wowcraft$evadeGoal.isEvading() && target != null) {
            // Cancel target setting while evading
            ci.cancel();
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

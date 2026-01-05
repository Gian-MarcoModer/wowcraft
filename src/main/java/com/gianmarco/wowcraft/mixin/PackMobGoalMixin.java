package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.mobpack.PackMobStationaryGoal;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to add stationary AI goal to pack mobs.
 */
@Mixin(Mob.class)
public abstract class PackMobGoalMixin {

    @Shadow
    protected net.minecraft.world.entity.ai.goal.GoalSelector goalSelector;

    /**
     * Add stationary goal to pack mobs after they're initialized.
     */
    @Inject(method = "finalizeSpawn", at = @At("RETURN"))
    private void wowcraft$addStationaryGoal(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason, SpawnGroupData spawnData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        Mob self = (Mob) (Object) this;

        // Check if this is a pack mob
        MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data != null && data.packId() != null) {
            // Add stationary goal with highest priority (0)
            goalSelector.addGoal(0, new PackMobStationaryGoal(self));
        }
    }
}

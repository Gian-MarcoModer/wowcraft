package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.entity.MobLevelManager;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Mob.class)
public class MobMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void wowcraft$onFinalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason reason, SpawnGroupData spawnData,
            CallbackInfoReturnable<SpawnGroupData> cir) {

        Mob self = (Mob) (Object) this;
        // Apply WowCraft levels and stats
        MobLevelManager.initializeMob(self);
    }
}

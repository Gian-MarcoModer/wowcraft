package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.world.entity.Mob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to prevent pack mobs from burning in sunlight.
 * This affects zombies, skeletons, and other undead that normally burn in daylight.
 */
@Mixin(Mob.class)
public abstract class PackMobSunlightMixin {

    /**
     * Intercept the isSunBurnTick() method to prevent pack mobs from burning.
     */
    @Inject(method = "isSunBurnTick", at = @At("HEAD"), cancellable = true)
    private void wowcraft$preventPackMobSunBurn(CallbackInfoReturnable<Boolean> cir) {
        Mob self = (Mob) (Object) this;

        // Check if this is a pack mob
        MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
        if (data != null && data.packId() != null) {
            // Pack mobs don't burn in sunlight
            cir.setReturnValue(false);
        }
    }
}

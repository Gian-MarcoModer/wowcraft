package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.roads.RoadGenerator;
import net.minecraft.server.level.ServerLevel;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.BooleanSupplier;

/**
 * Mixin to hook road generation into server world ticks.
 */
@Mixin(ServerLevel.class)
public class RoadTickMixin {

    private int expansionTickCounter = 0;
    private static final int EXPANSION_CHECK_FREQUENCY = 200; // Check every 10 seconds

    @Inject(method = "tick", at = @At("TAIL"))
    private void onTick(BooleanSupplier shouldKeepTicking, CallbackInfo ci) {
        ServerLevel level = (ServerLevel) (Object) this;

        // Only process in the overworld
        if (level.dimension() == ServerLevel.OVERWORLD) {
            RoadGenerator generator = RoadGenerator.getInstance();

            // Build roads
            generator.onServerTick(level);

            // Periodically check for expansion as players explore
            expansionTickCounter++;
            if (expansionTickCounter >= EXPANSION_CHECK_FREQUENCY) {
                expansionTickCounter = 0;
                generator.checkExpansion(level);
            }
        }
    }
}

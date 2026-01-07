package com.gianmarco.wowcraft.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixin to suppress vanilla hostile mob spawning on the surface.
 * Pack mobs become the primary surface threat.
 * Caves and underground retain vanilla spawns.
 * Passive mobs (animals) use vanilla spawning.
 */
@Mixin(NaturalSpawner.class)
public class MobSpawnMixin {

    /**
     * Cancels hostile mob spawns above Y=0 in the Overworld.
     * Affects: MONSTER (zombies, skeletons, creepers, etc.)
     * Allows: CREATURE (pigs, cows, chickens, etc.), underground cave spawns
     */
    @Inject(method = "isValidSpawnPostitionForType", at = @At("HEAD"), cancellable = true)
    private static void wowcraft$cancelSurfaceHostiles(
            ServerLevel level,
            MobCategory category,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            MobSpawnSettings.SpawnerData spawnerData,
            BlockPos.MutableBlockPos pos,
            double distance,
            CallbackInfoReturnable<Boolean> cir) {
        // Only cancel hostile spawns (monsters)
        if (category != MobCategory.MONSTER) {
            return;
        }

        // Only in Overworld
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }

        // Cancel spawns above Y=0 (surface)
        // Underground spawns (caves) are allowed
        if (pos.getY() > 0) {
            cir.setReturnValue(false);
        }
    }
}

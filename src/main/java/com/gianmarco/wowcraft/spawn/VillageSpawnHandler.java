package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

/**
 * Sets the world spawn point to the nearest village when the world first loads.
 * Inspired by the Village Spawn Point mod by Serilum.
 */
public class VillageSpawnHandler {

    private static final TagKey<Structure> VILLAGE_TAG = TagKey.create(Registries.STRUCTURE,
            ResourceLocation.withDefaultNamespace("village"));

    // Maximum search radius in chunks (100 chunks = 1600 blocks)
    private static final int SEARCH_RADIUS_CHUNKS = 100;

    /**
     * Attempts to set the world spawn to the nearest village.
     * Should be called when the server/world starts.
     *
     * @param serverLevel The overworld server level
     * @return true if spawn was successfully set to a village
     */
    public static boolean setSpawnToVillage(ServerLevel serverLevel) {
        // Only run in the overworld
        if (serverLevel.dimension() != net.minecraft.world.level.Level.OVERWORLD) {
            WowCraft.LOGGER.info("[VillageSpawn] Skipping - not overworld");
            return false;
        }

        // Check if structures are enabled
        if (!serverLevel.getServer().getWorldData().worldGenOptions().generateStructures()) {
            WowCraft.LOGGER.warn("[VillageSpawn] Structures are disabled, cannot find village");
            return false;
        }

        WowCraft.LOGGER.info("[VillageSpawn] Searching for nearest village to set spawn point...");

        try {
            WowCraft.LOGGER.info("[VillageSpawn] Using village tag: {}", VILLAGE_TAG.location());

            // Search for the nearest village from world origin using the tag directly
            BlockPos origin = BlockPos.ZERO;
            var result = serverLevel.findNearestMapStructure(
                    VILLAGE_TAG,
                    origin,
                    SEARCH_RADIUS_CHUNKS,
                    false  // don't skip existing chunks
            );

            if (result == null) {
                WowCraft.LOGGER.warn("[VillageSpawn] No village found within {} chunks of origin", SEARCH_RADIUS_CHUNKS);
                return false;
            }

            BlockPos villagePos = result;
            WowCraft.LOGGER.info("[VillageSpawn] Found village at {} {} {}",
                    villagePos.getX(), villagePos.getY(), villagePos.getZ());

            // Find a safe surface position at the village
            BlockPos spawnPos = findSafeSpawnPosition(serverLevel, villagePos);

            // Set the world spawn
            serverLevel.setDefaultSpawnPos(spawnPos, 0.0f);

            WowCraft.LOGGER.info("[VillageSpawn] SUCCESS! World spawn set to village at {} {} {}",
                    spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());

            return true;

        } catch (Exception e) {
            WowCraft.LOGGER.error("[VillageSpawn] Error finding village: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Finds a safe spawn position at ground level near the given position.
     * Forces chunk generation if needed to get accurate height.
     */
    private static BlockPos findSafeSpawnPosition(ServerLevel level, BlockPos pos) {
        // Force the chunk to be generated/loaded so heightmap is accurate
        int chunkX = pos.getX() >> 4;
        int chunkZ = pos.getZ() >> 4;
        level.getChunk(chunkX, chunkZ);

        // Get the highest block at this position
        int y = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                pos.getX(), pos.getZ());

        // Sanity check - if y is still at bedrock level, use sea level as fallback
        if (y <= level.getMinY()) {
            WowCraft.LOGGER.warn("[VillageSpawn] Heightmap returned minimum height, using sea level (63) as fallback");
            y = 63;
        }

        BlockPos safePos = new BlockPos(pos.getX(), y, pos.getZ());
        WowCraft.LOGGER.info("[VillageSpawn] Safe spawn position calculated at {} {} {}",
                safePos.getX(), safePos.getY(), safePos.getZ());

        return safePos;
    }
}

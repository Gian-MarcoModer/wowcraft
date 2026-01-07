package com.gianmarco.wowcraft.roads;

import com.gianmarco.wowcraft.WowCraft;
import com.mojang.datafixers.util.Pair;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.levelgen.structure.Structure;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Finds vanilla Minecraft structures for road connections.
 */
public class StructureFinder {

    // Tag for all village types
    private static final TagKey<Structure> VILLAGE_TAG = TagKey.create(
            Registries.STRUCTURE,
            ResourceLocation.withDefaultNamespace("village")
    );

    // Additional structure types to connect
    private static final List<String> EXTRA_STRUCTURES = List.of(
            "minecraft:pillager_outpost",
            "minecraft:ruined_portal",        // Overworld ruined portals
            "minecraft:swamp_hut"             // Witch huts
    );

    /**
     * Find structures near a position asynchronously.
     */
    public static CompletableFuture<List<BlockPos>> findStructuresAsync(
            ServerLevel level, BlockPos center, int searchRadius, int maxStructures) {

        return CompletableFuture.supplyAsync(() -> findStructuresSync(level, center, searchRadius, maxStructures));
    }

    /**
     * Find structures synchronously.
     */
    public static List<BlockPos> findStructuresSync(
            ServerLevel level, BlockPos center, int searchRadius, int maxStructures) {

        List<BlockPos> found = new ArrayList<>();

        try {
            // Get structure registry via RegistryAccess
            RegistryAccess registryAccess = level.registryAccess();
            var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);

            // Search for villages using tag
            Optional<HolderSet.Named<Structure>> villageSet = structureRegistry.get(VILLAGE_TAG);

            if (villageSet.isPresent()) {
                findStructuresOfType(level, center, searchRadius, villageSet.get(), found, maxStructures);
            } else {
                WowCraft.LOGGER.warn("Village tag not found in registry");
            }

            // Search for additional structure types
            for (String structureId : EXTRA_STRUCTURES) {
                if (found.size() >= maxStructures) {
                    break;
                }

                ResourceLocation loc = ResourceLocation.parse(structureId);
                Optional<Holder.Reference<Structure>> holder = structureRegistry.get(
                        ResourceKey.create(Registries.STRUCTURE, loc));

                if (holder.isPresent()) {
                    findSingleStructure(level, center, searchRadius, holder.get(), found, maxStructures);
                }
            }

        } catch (Exception e) {
            WowCraft.LOGGER.error("Error finding structures: {}", e.getMessage(), e);
        }

        return found;
    }

    /**
     * Find a single structure type and add to results.
     */
    private static void findSingleStructure(
            ServerLevel level, BlockPos center, int searchRadius,
            Holder<Structure> structure, List<BlockPos> results, int maxResults) {

        if (results.size() >= maxResults) {
            return;
        }

        try {
            HolderSet<Structure> singleSet = HolderSet.direct(structure);

            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource()
                    .getGenerator()
                    .findNearestMapStructure(
                            level,
                            singleSet,
                            center,
                            searchRadius / 16,
                            false
                    );

            if (result != null) {
                BlockPos structurePos = result.getFirst();

                // Check for duplicates
                boolean isDuplicate = false;
                for (BlockPos existing : results) {
                    if (existing.distSqr(structurePos) < 100 * 100) {
                        isDuplicate = true;
                        break;
                    }
                }

                if (!isDuplicate) {
                    results.add(structurePos);
                    WowCraft.LOGGER.info("Found structure at {}", structurePos);
                }
            }
        } catch (Exception e) {
            WowCraft.LOGGER.debug("Structure search failed: {}", e.getMessage());
        }
    }

    /**
     * Find structures matching a holder set.
     */
    private static void findStructuresOfType(
            ServerLevel level, BlockPos center, int searchRadius,
            HolderSet<Structure> structures, List<BlockPos> results, int maxResults) {

        int chunkRadius = searchRadius / 16;

        // Search in expanding radius
        for (int radius = 1; radius <= chunkRadius && results.size() < maxResults; radius += 2) {
            try {
                Pair<BlockPos, Holder<Structure>> result = level.getChunkSource()
                        .getGenerator()
                        .findNearestMapStructure(
                                level,
                                structures,
                                center,
                                radius,
                                false
                        );

                if (result != null) {
                    BlockPos structurePos = result.getFirst();

                    // Check if we already have this one (within 100 blocks)
                    boolean isDuplicate = false;
                    for (BlockPos existing : results) {
                        if (existing.distSqr(structurePos) < 100 * 100) {
                            isDuplicate = true;
                            break;
                        }
                    }

                    if (!isDuplicate) {
                        results.add(structurePos);
                        WowCraft.LOGGER.info("Found structure at {}", structurePos);
                    }
                }
            } catch (Exception e) {
                // Structure search can fail, continue
                WowCraft.LOGGER.debug("Structure search failed at radius {}: {}", radius, e.getMessage());
            }
        }
    }

    /**
     * Find a single specific structure type by resource key.
     */
    public static BlockPos findStructure(ServerLevel level, BlockPos center, int searchRadius,
                                         ResourceKey<Structure> structureKey) {
        try {
            RegistryAccess registryAccess = level.registryAccess();
            var structureRegistry = registryAccess.lookupOrThrow(Registries.STRUCTURE);

            Optional<Holder.Reference<Structure>> holder = structureRegistry.get(structureKey);
            if (holder.isEmpty()) {
                return null;
            }

            HolderSet<Structure> singleSet = HolderSet.direct(holder.get());

            Pair<BlockPos, Holder<Structure>> result = level.getChunkSource()
                    .getGenerator()
                    .findNearestMapStructure(
                            level,
                            singleSet,
                            center,
                            searchRadius / 16,
                            false
                    );

            return result != null ? result.getFirst() : null;

        } catch (Exception e) {
            WowCraft.LOGGER.error("Error finding structure {}: {}", structureKey, e.getMessage());
            return null;
        }
    }
}

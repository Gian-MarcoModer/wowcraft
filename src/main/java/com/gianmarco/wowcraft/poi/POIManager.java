package com.gianmarco.wowcraft.poi;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages all Points of Interest in the world.
 * Handles POI generation, storage, and spatial lookups.
 */
public class POIManager {

    // World seed for deterministic generation
    private final long worldSeed;

    // All POIs in the world (UUID -> POI)
    private final Map<UUID, PointOfInterest> allPOIs;

    // Spatial index: ChunkPos -> List of POI IDs in/near that chunk
    private final Map<ChunkPos, List<UUID>> chunkToPOIs;

    // Region grid size (500x500 blocks)
    private static final int REGION_SIZE = 500;

    // Minimum spacing between POIs (blocks)
    private static final int MIN_POI_SPACING = 150;

    // Track which regions have been generated
    private final Set<RegionPos> generatedRegions;

    public POIManager(long worldSeed) {
        this.worldSeed = worldSeed;
        this.allPOIs = new ConcurrentHashMap<>();
        this.chunkToPOIs = new ConcurrentHashMap<>();
        this.generatedRegions = ConcurrentHashMap.newKeySet();
    }

    /**
     * Check if POIs have been generated for a region.
     */
    public boolean isRegionGenerated(BlockPos pos) {
        RegionPos region = RegionPos.fromBlockPos(pos, REGION_SIZE);
        return generatedRegions.contains(region);
    }

    /**
     * Mark a region as generated.
     */
    public void markRegionGenerated(BlockPos pos) {
        RegionPos region = RegionPos.fromBlockPos(pos, REGION_SIZE);
        generatedRegions.add(region);
        WowCraft.LOGGER.debug("Marked region {} as generated", region);
    }

    /**
     * Add a POI to the manager.
     */
    public void addPOI(PointOfInterest poi) {
        allPOIs.put(poi.getPoiId(), poi);
        indexPOI(poi);
        WowCraft.LOGGER.debug("Added {} at {}", poi.getType(), poi.getPosition());
    }

    /**
     * Index a POI in the spatial chunk index.
     */
    private void indexPOI(PointOfInterest poi) {
        BlockPos pos = poi.getPosition();
        int radius = poi.getRadius();

        // Calculate chunk range covered by POI
        int minChunkX = (pos.getX() - radius) >> 4;
        int maxChunkX = (pos.getX() + radius) >> 4;
        int minChunkZ = (pos.getZ() - radius) >> 4;
        int maxChunkZ = (pos.getZ() + radius) >> 4;

        // Add POI to all affected chunks
        for (int cx = minChunkX; cx <= maxChunkX; cx++) {
            for (int cz = minChunkZ; cz <= maxChunkZ; cz++) {
                ChunkPos chunkPos = new ChunkPos(cx, cz);
                chunkToPOIs.computeIfAbsent(chunkPos, k -> new ArrayList<>()).add(poi.getPoiId());
            }
        }
    }

    /**
     * Get POI by ID.
     */
    public PointOfInterest getPOI(UUID poiId) {
        return allPOIs.get(poiId);
    }

    /**
     * Get all POIs near a chunk.
     */
    public List<PointOfInterest> getPOIsNearChunk(ChunkPos chunk) {
        List<PointOfInterest> pois = new ArrayList<>();
        List<UUID> poiIds = chunkToPOIs.get(chunk);

        if (poiIds != null) {
            for (UUID id : poiIds) {
                PointOfInterest poi = allPOIs.get(id);
                if (poi != null) {
                    pois.add(poi);
                }
            }
        }

        return pois;
    }

    /**
     * Get all POIs within a radius of a position.
     */
    public List<PointOfInterest> getPOIsNear(BlockPos center, int radius) {
        List<PointOfInterest> nearbyPOIs = new ArrayList<>();

        for (PointOfInterest poi : allPOIs.values()) {
            if (poi.getPosition().distSqr(center) <= radius * radius) {
                nearbyPOIs.add(poi);
            }
        }

        return nearbyPOIs;
    }

    /**
     * Check if a position is too close to existing POIs.
     */
    public boolean isTooCloseToExistingPOIs(BlockPos pos, int minDistance) {
        for (PointOfInterest poi : allPOIs.values()) {
            if (poi.getPosition().distSqr(pos) < minDistance * minDistance) {
                return true;
            }
        }
        return false;
    }

    /**
     * Get all POIs.
     */
    public Collection<PointOfInterest> getAllPOIs() {
        return allPOIs.values();
    }

    /**
     * Get total POI count.
     */
    public int getPOICount() {
        return allPOIs.size();
    }

    /**
     * Clear all POIs (for regeneration/testing).
     */
    public void clearAll() {
        allPOIs.clear();
        chunkToPOIs.clear();
        generatedRegions.clear();
        WowCraft.LOGGER.info("Cleared all POIs");
    }

    /**
     * Region position for POI generation grid.
     */
    public static class RegionPos {
        public final int x;
        public final int z;

        public RegionPos(int x, int z) {
            this.x = x;
            this.z = z;
        }

        public static RegionPos fromBlockPos(BlockPos pos, int regionSize) {
            return new RegionPos(
                    Math.floorDiv(pos.getX(), regionSize),
                    Math.floorDiv(pos.getZ(), regionSize));
        }

        public BlockPos getCenterBlockPos(int regionSize) {
            return new BlockPos(
                    x * regionSize + regionSize / 2,
                    64, // Default Y level
                    z * regionSize + regionSize / 2);
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof RegionPos other))
                return false;
            return x == other.x && z == other.z;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, z);
        }

        @Override
        public String toString() {
            return String.format("Region[%d, %d]", x, z);
        }
    }
}

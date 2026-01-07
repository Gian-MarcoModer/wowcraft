package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.poi.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.EntityType;

import java.util.*;

/**
 * Debug visualizer for POIs and spawn points.
 * Shows particles and text displays to help debug spawn system.
 */
public class POIDebugVisualizer {

    private static final Map<UUID, UUID> poiMarkers = new HashMap<>();
    private static boolean debugEnabled = false;

    /**
     * Enable or disable debug visualization.
     */
    public static void setDebugEnabled(boolean enabled) {
        debugEnabled = enabled;
    }

    public static boolean isDebugEnabled() {
        return debugEnabled;
    }

    /**
     * Visualize all POIs in a region.
     */
    public static void visualizePOIs(ServerLevel level, List<PointOfInterest> pois) {
        if (!debugEnabled) return;

        for (PointOfInterest poi : pois) {
            visualizePOI(level, poi);
        }
    }

    /**
     * Visualize a single POI with particles and text display.
     */
    public static void visualizePOI(ServerLevel level, PointOfInterest poi) {
        if (!debugEnabled) return;

        BlockPos pos = poi.getPosition();

        // Spawn text display marker
        Display.TextDisplay textDisplay = new Display.TextDisplay(EntityType.TEXT_DISPLAY, level);
        textDisplay.setPos(pos.getX() + 0.5, pos.getY() + 3, pos.getZ() + 0.5);

        // Set text based on POI type
        String poiText = getPoiDebugText(poi);
        textDisplay.setText(Component.literal(poiText));

        // Make it visible from far away
        textDisplay.setBillboardConstraints(Display.BillboardConstraints.CENTER);
        textDisplay.setViewRange(5.0f);

        level.addFreshEntity(textDisplay);
        poiMarkers.put(poi.getPoiId(), textDisplay.getUUID());

        // Spawn particles around POI
        spawnPoiParticles(level, poi);
    }

    /**
     * Get debug text for a POI.
     */
    private static String getPoiDebugText(PointOfInterest poi) {
        return switch (poi.getType()) {
            case CAMP -> {
                CampPOI camp = (CampPOI) poi;
                yield String.format("⚔ CAMP\n3-5 mobs");
            }
            case COMPOUND -> {
                CompoundPOI compound = (CompoundPOI) poi;
                yield String.format("🏰 COMPOUND\nRadius: %d\nCamps: %d",
                    compound.getRadius(), compound.getSpawnPositions().size());
            }
            case LAIR -> {
                LairPOI lair = (LairPOI) poi;
                yield String.format("☠ LAIR (BOSS)\n+%d Levels\n%ds Respawn",
                    lair.getLevelBonus(), lair.getRespawnDelaySeconds());
            }
            case WILDLIFE -> {
                WildlifePOI wildlife = (WildlifePOI) poi;
                yield String.format("🦌 WILDLIFE\nRadius: %d\nSpawns: %d",
                    wildlife.getRadius(), wildlife.getSpawnPositions().size());
            }
            case RESOURCE_AREA -> {
                ResourceAreaPOI resource = (ResourceAreaPOI) poi;
                yield String.format("⛏ RESOURCE\nRadius: %d\nSpawns: %d",
                    resource.getRadius(), resource.getSpawnPositions().size());
            }
            case PATROL_ROUTE -> {
                PatrolRoutePOI patrol = (PatrolRoutePOI) poi;
                yield String.format("👁 PATROL\nWaypoints: %d",
                    patrol.getWaypoints().size());
            }
        };
    }

    /**
     * Spawn particles to mark POI boundaries.
     */
    private static void spawnPoiParticles(ServerLevel level, PointOfInterest poi) {
        BlockPos pos = poi.getPosition();
        int radius = poi.getRadius();

        // Choose particle color based on POI type
        var particleType = switch (poi.getType()) {
            case CAMP -> ParticleTypes.FLAME;              // Orange/red for single camps
            case COMPOUND -> ParticleTypes.LAVA;           // Dark red for compounds (multi-camp)
            case LAIR -> ParticleTypes.SOUL_FIRE_FLAME;    // Blue for boss lairs
            case WILDLIFE -> ParticleTypes.HAPPY_VILLAGER; // Green for wildlife
            case RESOURCE_AREA -> ParticleTypes.WAX_ON;    // Yellow for resources
            case PATROL_ROUTE -> ParticleTypes.ENCHANT;    // Purple for patrols
        };

        // Spawn particles in a circle around POI
        int points = Math.min(32, radius * 2); // More points for larger radius
        for (int i = 0; i < points; i++) {
            double angle = (2 * Math.PI * i) / points;
            double x = pos.getX() + 0.5 + Math.cos(angle) * radius;
            double z = pos.getZ() + 0.5 + Math.sin(angle) * radius;
            double y = pos.getY() + 0.5;

            level.sendParticles(particleType, x, y, z, 1, 0, 0, 0, 0.01);
        }

        // Spawn vertical pillar at center
        for (int y = 0; y < 5; y++) {
            level.sendParticles(particleType,
                pos.getX() + 0.5,
                pos.getY() + y,
                pos.getZ() + 0.5,
                1, 0, 0, 0, 0.01);
        }
    }

    /**
     * Visualize active spawn points (for debugging spawn pool).
     */
    public static void visualizeSpawnPoint(ServerLevel level, SpawnPoint point) {
        if (!debugEnabled) return;

        BlockPos pos = point.getPosition();

        // Choose particle based on spawn point type
        var particleType = switch (point.getType()) {
            case POI_CAMP -> ParticleTypes.FLAME;
            case POI_COMPOUND -> ParticleTypes.LAVA;
            case POI_LAIR -> ParticleTypes.DRAGON_BREATH;
            case POI_WILDLIFE -> ParticleTypes.COMPOSTER;
            case POI_PATROL -> ParticleTypes.PORTAL;
            case SCATTER -> ParticleTypes.ENCHANTED_HIT;
            default -> ParticleTypes.END_ROD;
        };

        // Small particle marker
        level.sendParticles(particleType,
            pos.getX() + 0.5,
            pos.getY() + 0.5,
            pos.getZ() + 0.5,
            3, 0.2, 0.5, 0.2, 0.01);
    }

    /**
     * Clear all POI markers.
     */
    public static void clearMarkers(ServerLevel level) {
        for (UUID markerId : poiMarkers.values()) {
            var entity = level.getEntity(markerId);
            if (entity != null) {
                entity.remove(net.minecraft.world.entity.Entity.RemovalReason.DISCARDED);
            }
        }
        poiMarkers.clear();
    }

    /**
     * Refresh POI markers (call every few seconds if debug enabled).
     */
    public static void refreshMarkers(ServerLevel level) {
        if (!debugEnabled) return;

        // Respawn particles for visibility
        // POI particles would be respawned here
        // For now, they fade naturally
    }
}

package com.gianmarco.wowcraft.zone;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.gianmarco.wowcraft.WowCraft;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Central registry for zone names and assignments.
 * Loads zone name pools from JSON data files and assigns names based on
 * discovery order.
 * First discovery of each biome type = first (lowest level) zone name.
 */
public class ZoneRegistry {
    private static final Gson GSON = new Gson();

    /** Zone name pools per biome group (loaded from JSON, ordered by level) */
    private static final Map<BiomeGroup, List<ZoneDefinition>> NAME_POOLS = new EnumMap<>(BiomeGroup.class);

    /**
     * Tracks discovery count per biome group (determines which zone name to assign
     * next)
     */
    private static final Map<BiomeGroup, Integer> DISCOVERY_COUNT = new EnumMap<>(BiomeGroup.class);

    /** Currently assigned zones (region ID -> region data) */
    private static final Map<UUID, ZoneRegion> ASSIGNED_ZONES = new ConcurrentHashMap<>();

    /** The "Unexplored Wilds" fallback zone definition */
    public static final ZoneDefinition UNEXPLORED_WILDS = ZoneDefinition.of(
            "Unexplored Wilds",
            "Beyond the Known Lands",
            50,
            60);

    /**
     * Creates a resource reload listener for loading zone name data.
     */
    public static SimpleSynchronousResourceReloadListener createReloadListener() {
        return new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "zone_names");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                loadZoneNames(manager);
            }
        };
    }

    /**
     * Loads all zone name definitions from data/wowcraft/zones/*.json
     */
    private static void loadZoneNames(ResourceManager manager) {
        NAME_POOLS.clear();

        // Initialize empty lists for all biome groups
        for (BiomeGroup group : BiomeGroup.values()) {
            NAME_POOLS.put(group, new ArrayList<>());
        }

        // Load zone files
        Map<ResourceLocation, Resource> resources = manager.listResources(
                "zones",
                id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            ResourceLocation id = entry.getKey();
            try (InputStream stream = entry.getValue().open()) {
                JsonObject json = GSON.fromJson(
                        new InputStreamReader(stream, StandardCharsets.UTF_8),
                        JsonObject.class);

                String groupName = json.get("biome_group").getAsString();
                BiomeGroup group;
                try {
                    group = BiomeGroup.valueOf(groupName);
                } catch (IllegalArgumentException e) {
                    WowCraft.LOGGER.warn("Unknown biome group '{}' in zone file {}", groupName, id);
                    continue;
                }

                JsonArray zonesArray = json.getAsJsonArray("zones");
                List<ZoneDefinition> definitions = new ArrayList<>();

                for (JsonElement element : zonesArray) {
                    JsonObject zoneObj = element.getAsJsonObject();
                    String name = zoneObj.get("name").getAsString();
                    String subtitle = zoneObj.has("subtitle") ? zoneObj.get("subtitle").getAsString() : null;
                    int levelMin = zoneObj.has("level_min") ? zoneObj.get("level_min").getAsInt() : 1;
                    int levelMax = zoneObj.has("level_max") ? zoneObj.get("level_max").getAsInt() : levelMin + 5;
                    definitions.add(new ZoneDefinition(name, subtitle, levelMin, levelMax));
                }

                NAME_POOLS.put(group, definitions);
                WowCraft.LOGGER.info("Loaded {} zone names for {} (levels {}-{})",
                        definitions.size(), group,
                        definitions.isEmpty() ? 0 : definitions.get(0).levelMin(),
                        definitions.isEmpty() ? 0 : definitions.get(definitions.size() - 1).levelMax());

            } catch (Exception e) {
                WowCraft.LOGGER.error("Failed to load zone file {}: {}", id, e.getMessage());
            }
        }
    }

    /**
     * Gets the next zone name for a biome group based on discovery order.
     * First discovery = first (lowest level) zone, second discovery = second zone,
     * etc.
     * Returns the zone definition with its fixed level range.
     */
    public static ZoneDefinition getNextZoneForGroup(BiomeGroup group) {
        if (group == null || !group.isNameable()) {
            return UNEXPLORED_WILDS;
        }

        List<ZoneDefinition> pool = NAME_POOLS.getOrDefault(group, Collections.emptyList());
        if (pool.isEmpty()) {
            return UNEXPLORED_WILDS;
        }

        // Get current discovery count for this biome group
        int count = DISCOVERY_COUNT.getOrDefault(group, 0);

        // Increment for next time
        DISCOVERY_COUNT.put(group, count + 1);

        // Get zone at this index (or last one if we've exhausted the pool)
        int index = Math.min(count, pool.size() - 1);
        ZoneDefinition zone = pool.get(index);

        WowCraft.LOGGER.info("Assigned {} zone #{}: {} (L{}-{})",
                group, count + 1, zone.name(), zone.levelMin(), zone.levelMax());

        return zone;
    }

    /**
     * Assigns a name to a zone region based on its biome group.
     * Uses discovery order - first encounter = lowest level zone name.
     */
    public static ZoneRegion assignName(ZoneRegion region) {
        if (region.biomeGroup() == null || !region.biomeGroup().isNameable()) {
            return region.withName(UNEXPLORED_WILDS.name(), UNEXPLORED_WILDS.subtitle())
                    .withLevelRange(UNEXPLORED_WILDS.levelMin(), UNEXPLORED_WILDS.levelMax());
        }

        ZoneDefinition zoneDef = getNextZoneForGroup(region.biomeGroup());

        ZoneRegion namedRegion = region
                .withName(zoneDef.name(), zoneDef.subtitle())
                .withLevelRange(zoneDef.levelMin(), zoneDef.levelMax());

        ASSIGNED_ZONES.put(region.id(), namedRegion);
        return namedRegion;
    }

    /**
     * Gets the zone at a specific block position.
     */
    public static ZoneRegion getZoneAt(BlockPos pos) {
        ZoneRegion closest = null;
        double closestDist = Double.MAX_VALUE;

        for (ZoneRegion zone : ASSIGNED_ZONES.values()) {
            double dist = zone.center().distSqr(pos);
            if (dist < closestDist) {
                closestDist = dist;
                closest = zone;
            }
        }

        return closest;
    }

    /**
     * Registers a zone that has been assigned.
     */
    public static void registerZone(ZoneRegion zone) {
        ASSIGNED_ZONES.put(zone.id(), zone);
    }

    /**
     * Gets all registered zones.
     */
    public static Collection<ZoneRegion> getAllZones() {
        return Collections.unmodifiableCollection(ASSIGNED_ZONES.values());
    }

    /**
     * Clears all zone assignments and discovery counts (called on world unload).
     */
    public static void clearZones() {
        ASSIGNED_ZONES.clear();
        DISCOVERY_COUNT.clear();
    }

    /**
     * Gets the number of zones discovered for a biome group.
     */
    public static int getDiscoveryCount(BiomeGroup group) {
        return DISCOVERY_COUNT.getOrDefault(group, 0);
    }

    /**
     * Gets the zone definition at a specific discovery index.
     * Used by ZoneManager when loading from persistent storage.
     */
    public static ZoneDefinition getZoneAtIndex(BiomeGroup group, int index) {
        if (group == null || !group.isNameable()) {
            return UNEXPLORED_WILDS;
        }

        List<ZoneDefinition> pool = NAME_POOLS.getOrDefault(group, Collections.emptyList());
        if (pool.isEmpty()) {
            return UNEXPLORED_WILDS;
        }

        // Get zone at this index (or last one if we've exhausted the pool)
        int safeIndex = Math.min(index, pool.size() - 1);
        return pool.get(safeIndex);
    }
}

package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.WowCraft;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;

import org.jetbrains.annotations.Nullable;
import java.io.InputStreamReader;
import java.util.*;

/**
 * Registry for data-driven abilities.
 * Abilities are loaded from JSON files in data/wowcraft/abilities/
 */
public final class AbilityDataRegistry {

    private AbilityDataRegistry() {
    }

    private static final Map<ResourceLocation, AbilityData> abilities = new LinkedHashMap<>();

    /**
     * Get an ability by ID.
     */
    @Nullable
    public static AbilityData get(ResourceLocation id) {
        return abilities.get(id);
    }

    /**
     * Get an ability by ID, throwing if not found.
     */
    public static AbilityData getOrThrow(ResourceLocation id) {
        AbilityData ability = abilities.get(id);
        if (ability == null) {
            throw new IllegalArgumentException("Unknown ability: " + id);
        }
        return ability;
    }

    /**
     * Get all registered abilities.
     */
    public static Collection<AbilityData> getAll() {
        return Collections.unmodifiableCollection(abilities.values());
    }

    /**
     * Get all ability IDs.
     */
    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(abilities.keySet());
    }

    /**
     * Check if an ability exists.
     */
    public static boolean exists(ResourceLocation id) {
        return abilities.containsKey(id);
    }

    /**
     * Get abilities for a specific class.
     */
    public static List<AbilityData> getForClass(ResourceLocation classId) {
        List<AbilityData> result = new ArrayList<>();
        for (AbilityData ability : abilities.values()) {
            if (ability.requirements().requiredClass() == null ||
                    ability.requirements().requiredClass().equals(classId)) {
                result.add(ability);
            }
        }
        return result;
    }

    // ========== Loading ==========

    public static void loadAll(ResourceManager resourceManager) {
        abilities.clear();

        String path = "abilities";
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                path,
                id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try {
                loadAbility(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                WowCraft.LOGGER.error("Failed to load ability from {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }

        WowCraft.LOGGER.info("Loaded {} abilities", abilities.size());
    }

    private static void loadAbility(ResourceLocation fileId, Resource resource) throws Exception {
        try (var reader = new InputStreamReader(resource.open())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            AbilityData ability = AbilityData.fromJson(json);

            abilities.put(ability.id(), ability);
            WowCraft.LOGGER.debug("  Loaded ability: {} ({})",
                    ability.displayName(), ability.id());
        }
    }

    /**
     * Register default abilities if no JSON files found.
     */
    public static void registerDefaults() {
        if (!abilities.isEmpty())
            return;

        WowCraft.LOGGER.info("No ability JSON files found, registering defaults");

        // Will be populated by the JSON files we create
    }

    /**
     * Create a resource reload listener.
     */
    public static SimpleSynchronousResourceReloadListener createReloadListener() {
        return new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "ability_registry");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                loadAll(manager);
                if (abilities.isEmpty()) {
                    registerDefaults();
                }
            }
        };
    }
}

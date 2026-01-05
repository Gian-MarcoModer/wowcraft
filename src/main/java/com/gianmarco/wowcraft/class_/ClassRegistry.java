package com.gianmarco.wowcraft.class_;

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
 * Registry for player classes.
 * Classes are loaded from JSON files in data/wowcraft/classes/
 */
public final class ClassRegistry {

    private ClassRegistry() {
    } // Prevent instantiation

    private static final Map<ResourceLocation, PlayerClass> classes = new LinkedHashMap<>();

    /**
     * Get a class by ID.
     * 
     * @param id The class ID
     * @return The class, or null if not found
     */
    @Nullable
    public static PlayerClass get(ResourceLocation id) {
        return classes.get(id);
    }

    /**
     * Get a class by ID, throwing if not found.
     */
    public static PlayerClass getOrThrow(ResourceLocation id) {
        PlayerClass pc = classes.get(id);
        if (pc == null) {
            throw new IllegalArgumentException("Unknown player class: " + id);
        }
        return pc;
    }

    /**
     * Get all registered classes.
     */
    public static Collection<PlayerClass> getAll() {
        return Collections.unmodifiableCollection(classes.values());
    }

    /**
     * Get all class IDs.
     */
    public static Set<ResourceLocation> getAllIds() {
        return Collections.unmodifiableSet(classes.keySet());
    }

    /**
     * Check if a class is registered.
     */
    public static boolean exists(ResourceLocation id) {
        return classes.containsKey(id);
    }

    /**
     * Get the number of registered classes.
     */
    public static int size() {
        return classes.size();
    }

    // ========== Loading ==========

    /**
     * Load all classes from JSON files.
     * Called during resource reload.
     */
    public static void loadAll(ResourceManager resourceManager) {
        classes.clear();

        String path = "classes"; // data/wowcraft/classes/
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                path,
                id -> id.getPath().endsWith(".json"));

        for (Map.Entry<ResourceLocation, Resource> entry : resources.entrySet()) {
            try {
                loadClass(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                WowCraft.LOGGER.error("Failed to load class from {}: {}",
                        entry.getKey(), e.getMessage());
            }
        }

        WowCraft.LOGGER.info("Loaded {} player classes", classes.size());
    }

    private static void loadClass(ResourceLocation fileId, Resource resource) throws Exception {
        try (var reader = new InputStreamReader(resource.open())) {
            JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
            PlayerClass playerClass = PlayerClass.fromJson(json);

            classes.put(playerClass.id(), playerClass);
            WowCraft.LOGGER.info("  Loaded class: {} ({})",
                    playerClass.displayName(), playerClass.id());
        }
    }

    /**
     * Register default classes if no JSON files are found.
     * Used for backwards compatibility.
     */
    public static void registerDefaults() {
        if (!classes.isEmpty()) {
            return; // Already loaded
        }

        WowCraft.LOGGER.info("No class JSON files found, registering defaults");

        // Default Mage
        classes.put(
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "mage"),
                createDefaultMage());

        // Default Warrior
        classes.put(
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "warrior"),
                createDefaultWarrior());
    }

    private static PlayerClass createDefaultMage() {
        return new PlayerClass(
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "mage"),
                "Mage",
                "Masters of arcane magic",
                ResourceType.MANA,
                new ClassStats(5, 5, 15, 8, 10), // Base
                new ClassStats(0.5f, 0.5f, 2.0f, 1.0f, 1.5f), // Per level
                List.of(
                        ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "fireball"),
                        ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "frost_nova")),
                Map.of(
                        4, List.of(ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "blink"))),
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "textures/gui/class/mage.png"));
    }

    private static PlayerClass createDefaultWarrior() {
        return new PlayerClass(
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "warrior"),
                "Warrior",
                "Masters of martial combat",
                ResourceType.RAGE,
                new ClassStats(15, 8, 3, 12, 5), // Base
                new ClassStats(2.0f, 1.0f, 0.3f, 1.5f, 0.5f), // Per level
                List.of(
                        ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "heroic_strike"),
                        ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "charge")),
                Map.of(
                        4, List.of(ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "battle_shout"))),
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "textures/gui/class/warrior.png"));
    }

    /**
     * Create a resource reload listener for the registry.
     */
    public static SimpleSynchronousResourceReloadListener createReloadListener() {
        return new SimpleSynchronousResourceReloadListener() {
            @Override
            public ResourceLocation getFabricId() {
                return ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "class_registry");
            }

            @Override
            public void onResourceManagerReload(ResourceManager manager) {
                loadAll(manager);
                if (classes.isEmpty()) {
                    registerDefaults();
                }
            }
        };
    }
}

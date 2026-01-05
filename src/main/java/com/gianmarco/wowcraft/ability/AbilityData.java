package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.class_.ResourceType;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;

import org.jetbrains.annotations.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * Data-driven ability definition loaded from JSON.
 * This is the new v2 system - pure data with no behavior.
 * Execution is handled separately by AbilityExecutor.
 */
public record AbilityData(
        ResourceLocation id,
        String displayName,
        String description,
        ResourceLocation icon,
        AbilityType type,
        ResourceCost resourceCost,
        float castTime,
        float cooldown,
        boolean triggersGcd,
        Range range,
        List<AbilityEffect> effects,
        Requirements requirements) {

    /**
     * Types of abilities.
     */
    public enum AbilityType {
        INSTANT, // Activates immediately
        CAST, // Has a cast time
        CHANNELED, // Channeled over duration
        TOGGLED // On/off buff
    }

    /**
     * Resource cost structure.
     */
    public record ResourceCost(
            ResourceType type,
            int amount,
            @Nullable String scalingFormula) {
        public static final ResourceCost NONE = new ResourceCost(ResourceType.NONE, 0, null);

        public static ResourceCost fromJson(JsonObject json) {
            if (json == null || json.isJsonNull())
                return NONE;

            String typeStr = json.has("type") ? json.get("type").getAsString().toUpperCase() : "NONE";
            ResourceType type = switch (typeStr) {
                case "MANA" -> ResourceType.MANA;
                case "RAGE" -> ResourceType.RAGE;
                case "ENERGY" -> ResourceType.ENERGY;
                default -> ResourceType.NONE;
            };

            int amount = json.has("amount") ? json.get("amount").getAsInt() : 0;
            String scaling = json.has("scaling") ? json.get("scaling").getAsString() : null;

            return new ResourceCost(type, amount, scaling);
        }

        public int getCost(int level) {
            return amount;
        }
    }

    /**
     * Range restrictions.
     */
    public record Range(float min, float max) {
        public static final Range SELF = new Range(0, 0);
        public static final Range MELEE = new Range(0, 5);

        public static Range fromJson(JsonElement json) {
            if (json == null || json.isJsonNull())
                return MELEE;

            if (json.isJsonPrimitive()) {
                float max = json.getAsFloat();
                return new Range(0, max);
            }

            JsonObject obj = json.getAsJsonObject();
            float min = obj.has("min") ? obj.get("min").getAsFloat() : 0;
            float max = obj.has("max") ? obj.get("max").getAsFloat() : 5;
            return new Range(min, max);
        }

        public boolean inRange(float distance) {
            return distance >= min && distance <= max;
        }
    }

    /**
     * Requirements to use the ability.
     */
    public record Requirements(
            @Nullable ResourceLocation requiredClass,
            int minLevel,
            boolean requiresCombat,
            boolean requiresTarget,
            boolean requiresBehind) {
        public static final Requirements NONE = new Requirements(null, 1, false, false, false);

        public static Requirements fromJson(JsonObject json) {
            if (json == null || json.isJsonNull())
                return NONE;

            ResourceLocation cls = json.has("class")
                    ? ResourceLocation.parse(json.get("class").getAsString())
                    : null;
            int minLevel = json.has("min_level") ? json.get("min_level").getAsInt() : 1;
            boolean combat = json.has("requires_combat") && json.get("requires_combat").getAsBoolean();
            boolean target = json.has("requires_target") && json.get("requires_target").getAsBoolean();
            boolean behind = json.has("requires_behind") && json.get("requires_behind").getAsBoolean();

            return new Requirements(cls, minLevel, combat, target, behind);
        }
    }

    // ========== Parsing ==========

    public static AbilityData fromJson(JsonObject json) {
        ResourceLocation id = ResourceLocation.parse(json.get("id").getAsString());
        String displayName = json.get("display_name").getAsString();
        String description = json.has("description") ? json.get("description").getAsString() : "";

        ResourceLocation icon = json.has("icon")
                ? ResourceLocation.parse(json.get("icon").getAsString())
                : ResourceLocation.fromNamespaceAndPath("wowcraft", "textures/gui/abilities/default.png");

        String typeStr = json.has("type") ? json.get("type").getAsString().toUpperCase() : "INSTANT";
        AbilityType type = switch (typeStr) {
            case "CAST" -> AbilityType.CAST;
            case "CHANNELED" -> AbilityType.CHANNELED;
            case "TOGGLED" -> AbilityType.TOGGLED;
            default -> AbilityType.INSTANT;
        };

        ResourceCost cost = json.has("resource_cost")
                ? ResourceCost.fromJson(json.getAsJsonObject("resource_cost"))
                : ResourceCost.NONE;

        float castTime = json.has("cast_time") ? json.get("cast_time").getAsFloat() : 0;
        float cooldown = json.has("cooldown") ? json.get("cooldown").getAsFloat() : 0;
        boolean gcd = !json.has("gcd") || json.get("gcd").getAsBoolean();

        Range range = json.has("range") ? Range.fromJson(json.get("range")) : Range.SELF;

        List<AbilityEffect> effects = new ArrayList<>();
        if (json.has("effects")) {
            for (JsonElement elem : json.getAsJsonArray("effects")) {
                effects.add(AbilityEffect.fromJson(elem.getAsJsonObject()));
            }
        }

        Requirements reqs = json.has("requirements")
                ? Requirements.fromJson(json.getAsJsonObject("requirements"))
                : Requirements.NONE;

        return new AbilityData(id, displayName, description, icon, type, cost, castTime, cooldown, gcd, range, effects,
                reqs);
    }

    // ========== Utility ==========

    public boolean isInstant() {
        return type == AbilityType.INSTANT || castTime <= 0;
    }

    public boolean hasCastTime() {
        return type == AbilityType.CAST && castTime > 0;
    }

    public float getCastTimeSeconds() {
        return castTime;
    }

    public int getCastTimeTicks() {
        return (int) (castTime * 20);
    }

    public int getCooldownTicks() {
        return (int) (cooldown * 20);
    }
}

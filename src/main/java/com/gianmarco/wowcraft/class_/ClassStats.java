package com.gianmarco.wowcraft.class_;

import com.google.gson.JsonObject;

/**
 * Represents stat values (base or per-level) for a class.
 */
public record ClassStats(
        float strength,
        float agility,
        float intellect,
        float stamina,
        float spirit) {

    public static final ClassStats ZERO = new ClassStats(0, 0, 0, 0, 0);

    /**
     * Parse from JSON.
     */
    public static ClassStats fromJson(JsonObject json) {
        float str = json.has("strength") ? json.get("strength").getAsFloat() : 0;
        float agi = json.has("agility") ? json.get("agility").getAsFloat() : 0;
        float intel = json.has("intellect") ? json.get("intellect").getAsFloat() : 0;
        float sta = json.has("stamina") ? json.get("stamina").getAsFloat() : 0;
        float spi = json.has("spirit") ? json.get("spirit").getAsFloat() : 0;

        return new ClassStats(str, agi, intel, sta, spi);
    }

    /**
     * Add two stat blocks.
     */
    public ClassStats add(ClassStats other) {
        return new ClassStats(
                strength + other.strength,
                agility + other.agility,
                intellect + other.intellect,
                stamina + other.stamina,
                spirit + other.spirit);
    }

    /**
     * Multiply all stats by a factor.
     */
    public ClassStats multiply(float factor) {
        return new ClassStats(
                strength * factor,
                agility * factor,
                intellect * factor,
                stamina * factor,
                spirit * factor);
    }

    /**
     * Round to integers (for applying to StatsComponent).
     */
    public int strengthInt() {
        return Math.round(strength);
    }

    public int agilityInt() {
        return Math.round(agility);
    }

    public int intellectInt() {
        return Math.round(intellect);
    }

    public int staminaInt() {
        return Math.round(stamina);
    }

    public int spiritInt() {
        return Math.round(spirit);
    }
}

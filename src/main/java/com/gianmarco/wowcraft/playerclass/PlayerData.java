package com.gianmarco.wowcraft.playerclass;

import com.gianmarco.wowcraft.economy.MoneyPouch;
import com.gianmarco.wowcraft.stats.ExperienceManager;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import java.util.List;

/**
 * Stores all WowCraft-related player data.
 * This is persisted with the player and synced between client/server.
 */
public record PlayerData(
        PlayerClass playerClass,
        int level,
        int experience,
        int currentResource,
        int maxResource,
        int comboPoints,
        MoneyPouch moneyPouch,
        List<String> actionBar) {

    // Empty action bar (8 empty slots represented as empty strings)
    public static final List<String> EMPTY_ACTION_BAR = List.of("", "", "", "", "", "", "", "");

    // Default values for a new player
    public static final PlayerData DEFAULT = new PlayerData(
            PlayerClass.NONE,
            1, // Starting level
            0, // Starting XP
            100, // Current resource (mana/rage/energy)
            100, // Max resource
            0, // Combo points (for Rogue)
            MoneyPouch.EMPTY, // Starting with no money
            EMPTY_ACTION_BAR // Empty action bar
    );

    // Codec for serialization/deserialization
    public static final Codec<PlayerData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.xmap(
                    s -> PlayerClass.valueOf(s.toUpperCase()),
                    pc -> pc.name().toLowerCase()).fieldOf("playerClass").forGetter(PlayerData::playerClass),
            Codec.INT.fieldOf("level").forGetter(PlayerData::level),
            Codec.INT.fieldOf("experience").forGetter(PlayerData::experience),
            Codec.INT.fieldOf("currentResource").forGetter(PlayerData::currentResource),
            Codec.INT.fieldOf("maxResource").forGetter(PlayerData::maxResource),
            Codec.INT.optionalFieldOf("comboPoints", 0).forGetter(PlayerData::comboPoints),
            MoneyPouch.CODEC.optionalFieldOf("moneyPouch", MoneyPouch.EMPTY).forGetter(PlayerData::moneyPouch),
            Codec.STRING.listOf().optionalFieldOf("actionBar", EMPTY_ACTION_BAR).forGetter(PlayerData::actionBar))
            .apply(instance, PlayerData::new));

    /**
     * Check if the player has selected a class
     */
    public boolean hasSelectedClass() {
        return playerClass != PlayerClass.NONE;
    }

    /**
     * Create a copy with a new class selection
     * Note: For mana users, maxResource will be updated by stats system after class
     * selection
     */
    public PlayerData withClass(PlayerClass newClass) {
        int maxRes;
        if (newClass.getResourceType().hasDynamicMax()) {
            // Mana is calculated dynamically - use a reasonable starting value
            // This will be updated immediately by the stats system
            maxRes = 100; // Placeholder, will be recalculated
        } else {
            maxRes = newClass.getResourceType().getMaxValue();
        }
        int startRes = newClass.getResourceType().doesRegenerate() ? maxRes : 0;
        // Reset action bar when changing class (will be initialized with new class
        // defaults)
        return new PlayerData(newClass, this.level, this.experience, startRes, maxRes, 0, this.moneyPouch,
                EMPTY_ACTION_BAR);
    }

    /**
     * Create a copy with updated max resource (for dynamic mana scaling)
     */
    public PlayerData withMaxResource(int newMax) {
        int newCurrent = Math.min(currentResource, newMax);
        return new PlayerData(this.playerClass, this.level, this.experience, newCurrent, newMax, this.comboPoints,
                this.moneyPouch, this.actionBar);
    }

    /**
     * Create a copy with updated resource values
     */
    public PlayerData withResource(int current, int max) {
        return new PlayerData(this.playerClass, this.level, this.experience,
                Math.max(0, Math.min(current, max)), max, this.comboPoints, this.moneyPouch, this.actionBar);
    }

    /**
     * Create a copy with added experience and potentially leveled up
     */
    public PlayerData withAddedExperience(int xpGain) {
        int newXp = this.experience + xpGain;
        int newLevel = this.level;

        // Custom leveling curve: 100 + (level - 1) * 25 XP per level
        int xpNeeded = ExperienceManager.getXpRequiredForLevel(newLevel);
        while (newXp >= xpNeeded && newLevel < ExperienceManager.MAX_LEVEL) {
            newXp -= xpNeeded;
            newLevel++;
            xpNeeded = ExperienceManager.getXpRequiredForLevel(newLevel);
        }

        // Cap XP at max level
        if (newLevel >= ExperienceManager.MAX_LEVEL) {
            newLevel = ExperienceManager.MAX_LEVEL;
            newXp = 0;
        }

        return new PlayerData(this.playerClass, newLevel, newXp, this.currentResource, this.maxResource,
                this.comboPoints, this.moneyPouch, this.actionBar);
    }

    /**
     * Create a copy with updated money pouch
     */
    public PlayerData withMoneyPouch(MoneyPouch newMoneyPouch) {
        return new PlayerData(this.playerClass, this.level, this.experience,
                this.currentResource, this.maxResource, this.comboPoints, newMoneyPouch, this.actionBar);
    }

    /**
     * Create a copy with updated combo points (for Rogue)
     */
    public PlayerData withComboPoints(int newComboPoints) {
        return new PlayerData(this.playerClass, this.level, this.experience,
                this.currentResource, this.maxResource, Math.max(0, Math.min(newComboPoints, 5)), this.moneyPouch,
                this.actionBar);
    }

    /**
     * Create a copy with updated action bar
     */
    public PlayerData withActionBar(List<String> newActionBar) {
        return new PlayerData(this.playerClass, this.level, this.experience,
                this.currentResource, this.maxResource, this.comboPoints, this.moneyPouch, newActionBar);
    }

    /**
     * Get XP needed for next level
     */
    public int getExperienceForNextLevel() {
        return ExperienceManager.getXpRequiredForLevel(level);
    }

    /**
     * Get progress to next level as percentage (0-100)
     */
    public float getLevelProgress() {
        return (float) experience / getExperienceForNextLevel() * 100f;
    }
}

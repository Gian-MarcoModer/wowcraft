package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.item.ItemSuffix.Stat;

/**
 * Player stats container.
 * Holds the 5 primary stats from WoW.
 */
public record WowStats(
        int strength,
        int agility,
        int stamina,
        int intellect,
        int spirit) {
    public static final WowStats ZERO = new WowStats(0, 0, 0, 0, 0);

    /**
     * Create stats from a suffix and item level
     */
    public static WowStats fromSuffix(ItemSuffix suffix, int itemLevel, ItemRarity rarity) {
        // Base stat value scales with item level
        int baseValue = Math.max(1, itemLevel / 2);
        float multiplier = rarity.getStatMultiplier();

        int primaryValue = Math.round(baseValue * multiplier);
        int secondaryValue = Math.round(baseValue * multiplier * 0.75f);

        int str = 0, agi = 0, sta = 0, intel = 0, spi = 0;

        // Apply primary stat
        switch (suffix.getPrimaryStat()) {
            case STRENGTH -> str += primaryValue;
            case AGILITY -> agi += primaryValue;
            case STAMINA -> sta += primaryValue;
            case INTELLECT -> intel += primaryValue;
            case SPIRIT -> spi += primaryValue;
        }

        // Apply secondary stat
        switch (suffix.getSecondaryStat()) {
            case STRENGTH -> str += secondaryValue;
            case AGILITY -> agi += secondaryValue;
            case STAMINA -> sta += secondaryValue;
            case INTELLECT -> intel += secondaryValue;
            case SPIRIT -> spi += secondaryValue;
        }

        return new WowStats(str, agi, sta, intel, spi);
    }

    /**
     * Add two stat blocks together
     */
    public WowStats add(WowStats other) {
        return new WowStats(
                this.strength + other.strength,
                this.agility + other.agility,
                this.stamina + other.stamina,
                this.intellect + other.intellect,
                this.spirit + other.spirit);
    }

    /**
     * Get bonus HP from stamina (1 Sta = 10 HP)
     */
    public int getBonusHealth() {
        return stamina * 10;
    }

    /**
     * Get bonus mana from intellect (1 Int = 15 Mana)
     */
    public int getBonusMana() {
        return intellect * 15;
    }

    /**
     * Get bonus damage from strength
     */
    public float getBonusDamage() {
        return strength * 0.5f;
    }

    /**
     * Get crit chance bonus from agility (1 Agi = 0.5% crit)
     */
    public float getCritChance() {
        return agility * 0.005f;
    }

    /**
     * Check if any stats are non-zero
     */
    public boolean hasStats() {
        return strength > 0 || agility > 0 || stamina > 0 || intellect > 0 || spirit > 0;
    }

    /**
     * Get stat value by type
     */
    public int getStat(Stat stat) {
        return switch (stat) {
            case STRENGTH -> strength;
            case AGILITY -> agility;
            case STAMINA -> stamina;
            case INTELLECT -> intellect;
            case SPIRIT -> spirit;
        };
    }
}

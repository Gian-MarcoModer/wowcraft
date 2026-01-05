package com.gianmarco.wowcraft.stats;

/**
 * Stores all character stats - base, bonus from gear, and derived.
 */
public class CharacterStats {
    // Primary stats (base + gear)
    private int strength;
    private int agility;
    private int stamina;
    private int intellect;
    private int spirit;
    private int level; // Player level for mana scaling
    private int armor; // Total armor from equipped gear

    // Derived stats (calculated from primary)
    private int maxHealth;
    private int maxMana;
    private int attackPower;
    private float critChance;
    private float spellPower;
    private int manaRegenPer5;

    public CharacterStats() {
        this.strength = 0;
        this.agility = 0;
        this.stamina = 0;
        this.intellect = 0;
        this.spirit = 0;
        this.level = 1;
        this.armor = 0;
    }

    public CharacterStats(int strength, int agility, int stamina, int intellect, int spirit, int level, int armor) {
        this.strength = strength;
        this.agility = agility;
        this.stamina = stamina;
        this.intellect = intellect;
        this.spirit = spirit;
        this.level = level;
        this.armor = armor;
        calculateDerived();
    }

    /**
     * Calculate derived stats from primary stats
     */
    public void calculateDerived() {
        // Attack Power = Strength * 2
        this.attackPower = strength * 2;

        // Max Health = base 100 + (Stamina * 10)
        this.maxHealth = 100 + (stamina * 10);

        // Max Mana = base 60 + (level * 15) + (Intellect * 15)
        // WoW Classic style: scales with both level and intellect
        // At Level 1 with 22 INT: 60 + 15 + 330 = 405 mana
        this.maxMana = 60 + (level * 15) + (intellect * 15);

        // Crit Chance = 5% base + (Agility * 0.05%)
        this.critChance = 0.05f + (agility * 0.0005f);

        // Spell Power = Intellect * 0.5
        this.spellPower = intellect * 0.5f;

        // Mana Regen = base 5 + (Spirit * 0.5) per 5 seconds
        this.manaRegenPer5 = 5 + (int)(spirit * 0.5);
    }

    /**
     * Add another stat block to this one (for adding gear bonuses)
     */
    public CharacterStats add(int str, int agi, int sta, int intel, int spi) {
        return new CharacterStats(
                this.strength + str,
                this.agility + agi,
                this.stamina + sta,
                this.intellect + intel,
                this.spirit + spi,
                this.level,
                this.armor); // Armor stays the same (calculated separately from equipment)
    }

    /**
     * Set armor value (calculated from equipped gear)
     */
    public CharacterStats withArmor(int armor) {
        return new CharacterStats(
                this.strength,
                this.agility,
                this.stamina,
                this.intellect,
                this.spirit,
                this.level,
                armor);
    }

    // Primary stat getters
    public int getStrength() {
        return strength;
    }

    public int getAgility() {
        return agility;
    }

    public int getStamina() {
        return stamina;
    }

    public int getIntellect() {
        return intellect;
    }

    public int getSpirit() {
        return spirit;
    }

    public int getArmor() {
        return armor;
    }

    // Derived stat getters
    public int getMaxHealth() {
        return maxHealth;
    }

    public int getMaxMana() {
        return maxMana;
    }

    public int getAttackPower() {
        return attackPower;
    }

    public float getCritChance() {
        return critChance;
    }

    public float getSpellPower() {
        return spellPower;
    }

    public int getManaRegenPer5() {
        return manaRegenPer5;
    }

    /**
     * Get bonus melee damage from attack power
     * Formula: Attack Power / 14
     */
    public float getBonusMeleeDamage() {
        return attackPower / 14.0f;
    }

    /**
     * Roll for a critical hit
     */
    public boolean rollCrit() {
        return Math.random() < critChance;
    }

    @Override
    public String toString() {
        return String.format("Stats[Str=%d, Agi=%d, Sta=%d, Int=%d, Spi=%d, Armor=%d | AP=%d, HP=%d, Mana=%d]",
                strength, agility, stamina, intellect, spirit, armor, attackPower, maxHealth, maxMana);
    }
}

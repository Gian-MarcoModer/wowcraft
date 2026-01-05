package com.gianmarco.wowcraft.stats;

import com.gianmarco.wowcraft.item.EquipmentManager;
import com.gianmarco.wowcraft.item.WowStats;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.world.entity.player.Player;

/**
 * Calculates character stats from class, level, and equipment.
 */
public class StatsCalculator {

    /**
     * Calculate total stats for a player
     */
    public static CharacterStats calculate(Player player, PlayerClass playerClass, int level) {
        // Get base stats for class and level
        CharacterStats baseStats = getBaseStats(playerClass, level);

        // Get bonus stats from equipped items (reads from vanilla equipment slots)
        WowStats equipmentBonus = EquipmentManager.calculateEquippedStats(player);
        System.out.println("WOWCRAFT DEBUG: Calculated equipment bonus: " + equipmentBonus);

        // Get total armor from equipped items
        int totalArmor = EquipmentManager.calculateTotalArmor(player);

        // Combine base + equipment + armor
        return baseStats.add(
                equipmentBonus.strength(),
                equipmentBonus.agility(),
                equipmentBonus.stamina(),
                equipmentBonus.intellect(),
                equipmentBonus.spirit())
                .withArmor(totalArmor);
    }

    /**
     * Get base stats for a class at a given level
     * Based on WoW Classic values (Human race baseline)
     * 
     * WoW Classic starting stats:
     * - Human base: STR 20, AGI 20, STA 20, INT 20, SPI 21
     * - Warrior bonus: +3 STR, +0 AGI, +2 STA, +0 INT, +0 SPI
     * - Mage bonus: +0 STR, +0 AGI, +0 STA, +3 INT, +2 SPI
     * 
     * Stats per level (approximate from WoW Classic):
     * - Warrior: focuses on STR/STA (about +1 each per level on average)
     * - Mage: focuses on INT/SPI (about +1 each per level on average)
     */
    public static CharacterStats getBaseStats(PlayerClass playerClass, int level) {
        // Base stats at level 1 and per-level gains
        int str, agi, sta, intel, spi;
        int strPerLvl, agiPerLvl, staPerLvl, intPerLvl, spiPerLvl;

        switch (playerClass) {
            case WARRIOR -> {
                // Human Warrior: 23 STR, 20 AGI, 22 STA, 20 INT, 21 SPI
                str = 23;
                agi = 20;
                sta = 22;
                intel = 20;
                spi = 21;
                // Warriors gain mostly STR/STA per level
                strPerLvl = 2;
                agiPerLvl = 1;
                staPerLvl = 2;
                intPerLvl = 0;
                spiPerLvl = 1;
            }
            case MAGE -> {
                // Human Mage: 20 STR, 20 AGI, 20 STA, 23 INT, 23 SPI  
                str = 20;
                agi = 20;
                sta = 20;
                intel = 23;
                spi = 23;
                // Mages gain mostly INT/SPI per level
                strPerLvl = 0;
                agiPerLvl = 1;
                staPerLvl = 1;
                intPerLvl = 2;
                spiPerLvl = 2;
            }
            default -> {
                // Default/no class - balanced
                str = 20;
                agi = 20;
                sta = 20;
                intel = 20;
                spi = 20;
                strPerLvl = 1;
                agiPerLvl = 1;
                staPerLvl = 1;
                intPerLvl = 1;
                spiPerLvl = 1;
            }
        }

        // Add per-level gains (level 1 is base, so multiply by level-1)
        int lvlBonus = level - 1;
        str += strPerLvl * lvlBonus;
        agi += agiPerLvl * lvlBonus;
        sta += staPerLvl * lvlBonus;
        intel += intPerLvl * lvlBonus;
        spi += spiPerLvl * lvlBonus;

        // Base stats have 0 armor (armor comes from equipment)
        return new CharacterStats(str, agi, sta, intel, spi, level, 0);
    }
}

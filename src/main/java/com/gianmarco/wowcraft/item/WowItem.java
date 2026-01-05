package com.gianmarco.wowcraft.item;

import java.util.UUID;

/**
 * A WoW-style item with prefix, rarity, suffix, and stats.
 * Example: "Sturdy Iron Sword of the Bear" (Uncommon, Level 8)
 */
public class WowItem {
    private final UUID id;
    private final ItemPrefix prefix;       // Level-based prefix
    private final BaseItemType baseType;
    private final ItemRarity rarity;
    private final ItemSuffix suffix;       // null for common/poor items
    private final int itemLevel;
    private final WowStats stats;
    
    // Combat stats
    private final float attackDamage;
    private final float attackSpeed;
    private final int armorValue;

    public WowItem(ItemPrefix prefix, BaseItemType baseType, ItemRarity rarity, ItemSuffix suffix, int itemLevel) {
        this.id = UUID.randomUUID();
        this.prefix = prefix;
        this.baseType = baseType;
        this.rarity = rarity;
        this.suffix = suffix;
        this.itemLevel = itemLevel;

        // Calculate primary stats based on suffix and rarity
        if (suffix != null && rarity.getStatMultiplier() > 0) {
            this.stats = WowStats.fromSuffix(suffix, itemLevel, rarity);
        } else {
            this.stats = WowStats.ZERO;
        }
        
        // Calculate combat stats
        if (baseType.isWeapon() && baseType.getWeaponClass() != null) {
            BaseItemType.WeaponClass wc = baseType.getWeaponClass();
            this.attackDamage = wc.calculateDamage(itemLevel, rarity);
            // Only set attack speed if weapon has damage (skip for POOR vendor trash)
            this.attackSpeed = (this.attackDamage > 0) ? wc.getAttackSpeed() : 0;
        } else {
            this.attackDamage = 0;
            this.attackSpeed = 0;
        }
        
        if (baseType.isArmor() || baseType.isShield()) {
            BaseItemType.ArmorClass ac = baseType.getArmorClass();
            this.armorValue = ac != null ? ac.calculateArmor(itemLevel, rarity) : 0;
        } else {
            this.armorValue = 0;
        }
    }

    public UUID getId() {
        return id;
    }

    public ItemPrefix getPrefix() {
        return prefix;
    }

    public BaseItemType getBaseType() {
        return baseType;
    }

    public ItemRarity getRarity() {
        return rarity;
    }

    public ItemSuffix getSuffix() {
        return suffix;
    }

    public int getItemLevel() {
        return itemLevel;
    }

    public WowStats getStats() {
        return stats;
    }

    public WowEquipmentSlot getSlot() {
        return baseType.getSlot();
    }
    
    public float getAttackDamage() {
        return attackDamage;
    }
    
    public float getAttackSpeed() {
        return attackSpeed;
    }
    
    public int getArmorValue() {
        return armorValue;
    }
    
    public float getDPS() {
        if (attackDamage > 0 && attackSpeed > 0) {
            return attackDamage * attackSpeed;
        }
        return 0;
    }

    /**
     * Get the full display name with rarity color, prefix, and suffix
     * Example: "§aSturdy Iron Sword of the Bear"
     */
    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        
        if (prefix != null) {
            name.append(prefix.getDisplayName()).append(" ");
        }
        
        name.append(baseType.getDisplayName());
        
        if (suffix != null) {
            name.append(" ").append(suffix.getDisplayName());
        }
        
        return rarity.formatName(name.toString());
    }

    /**
     * Get name without color codes
     */
    public String getPlainName() {
        StringBuilder name = new StringBuilder();
        
        if (prefix != null) {
            name.append(prefix.getDisplayName()).append(" ");
        }
        
        name.append(baseType.getDisplayName());
        
        if (suffix != null) {
            name.append(" ").append(suffix.getDisplayName());
        }
        
        return name.toString();
    }

    /**
     * Generate tooltip lines for this item
     */
    public String[] getTooltipLines() {
        java.util.List<String> lines = new java.util.ArrayList<>();

        // Name with color
        lines.add(getDisplayName());

        // Item level
        lines.add("§7Item Level " + itemLevel);

        // Slot
        lines.add("§7" + baseType.getSlot().getDisplayName());
        
        // Weapon stats
        if (baseType.isWeapon() && attackDamage > 0) {
            lines.add("");
            lines.add(String.format("§f%.1f Damage", attackDamage));
            lines.add(String.format("§7Speed %.2f", attackSpeed));
            lines.add(String.format("§7(%.1f damage per second)", getDPS()));
        }
        
        // Armor stats
        if (armorValue > 0) {
            lines.add("");
            lines.add(String.format("§f%d Armor", armorValue));
        }

        // Armor type if applicable
        if (baseType.getArmorClass() != null) {
            lines.add("§7" + baseType.getArmorClass().getDisplayName());
        }

        // Primary stats
        if (stats.hasStats()) {
            lines.add(""); // Empty line before stats
            if (stats.strength() > 0) {
                lines.add("§a+" + stats.strength() + " Strength");
            }
            if (stats.agility() > 0) {
                lines.add("§a+" + stats.agility() + " Agility");
            }
            if (stats.stamina() > 0) {
                lines.add("§a+" + stats.stamina() + " Stamina");
            }
            if (stats.intellect() > 0) {
                lines.add("§a+" + stats.intellect() + " Intellect");
            }
            if (stats.spirit() > 0) {
                lines.add("§a+" + stats.spirit() + " Spirit");
            }
        }

        // Rarity at bottom
        lines.add("");
        lines.add(rarity.getColorCode() + rarity.getDisplayName());

        return lines.toArray(new String[0]);
    }

    @Override
    public String toString() {
        return getPlainName() + " [iLvl " + itemLevel + ", " + rarity.getDisplayName() + "]";
    }
}

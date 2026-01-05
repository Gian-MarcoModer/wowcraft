package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.playerclass.PlayerClass;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;

/**
 * Base item types that can be generated.
 * Each type has a slot, level range, and combat stats.
 */
public enum BaseItemType {
    // ==================== HEAD ====================
    CLOTH_HOOD("Hood", WowEquipmentSlot.HEAD, 1, 30, ArmorClass.CLOTH),
    CLOTH_COWL("Cowl", WowEquipmentSlot.HEAD, 1, 30, ArmorClass.CLOTH),
    LEATHER_HELM("Leather Helm", WowEquipmentSlot.HEAD, 1, 30, ArmorClass.LEATHER),
    LEATHER_CAP("Cap", WowEquipmentSlot.HEAD, 1, 30, ArmorClass.LEATHER),
    MAIL_HELM("Mail Coif", WowEquipmentSlot.HEAD, 10, 30, ArmorClass.MAIL),
    MAIL_HEADGUARD("Headguard", WowEquipmentSlot.HEAD, 10, 30, ArmorClass.MAIL),
    PLATE_HELM("Plate Helm", WowEquipmentSlot.HEAD, 15, 30, ArmorClass.PLATE),
    PLATE_GREATHELM("Greathelm", WowEquipmentSlot.HEAD, 15, 30, ArmorClass.PLATE),

    // ==================== CHEST ====================
    CLOTH_ROBE("Robe", WowEquipmentSlot.CHEST, 1, 30, ArmorClass.CLOTH),
    CLOTH_VEST("Vest", WowEquipmentSlot.CHEST, 1, 30, ArmorClass.CLOTH),
    LEATHER_TUNIC("Tunic", WowEquipmentSlot.CHEST, 1, 30, ArmorClass.LEATHER),
    LEATHER_JERKIN("Jerkin", WowEquipmentSlot.CHEST, 1, 30, ArmorClass.LEATHER),
    MAIL_HAUBERK("Hauberk", WowEquipmentSlot.CHEST, 10, 30, ArmorClass.MAIL),
    MAIL_CHAINMAIL("Chainmail", WowEquipmentSlot.CHEST, 10, 30, ArmorClass.MAIL),
    PLATE_BREASTPLATE("Breastplate", WowEquipmentSlot.CHEST, 15, 30, ArmorClass.PLATE),
    PLATE_CUIRASS("Cuirass", WowEquipmentSlot.CHEST, 15, 30, ArmorClass.PLATE),

    // ==================== LEGS ====================
    CLOTH_PANTS("Pants", WowEquipmentSlot.LEGS, 1, 30, ArmorClass.CLOTH),
    CLOTH_LEGGINGS("Leggings", WowEquipmentSlot.LEGS, 1, 30, ArmorClass.CLOTH),
    LEATHER_BREECHES("Breeches", WowEquipmentSlot.LEGS, 1, 30, ArmorClass.LEATHER),
    LEATHER_KILT("Kilt", WowEquipmentSlot.LEGS, 1, 30, ArmorClass.LEATHER),
    MAIL_LEGGUARDS("Legguards", WowEquipmentSlot.LEGS, 10, 30, ArmorClass.MAIL),
    MAIL_GREAVES("Greaves", WowEquipmentSlot.LEGS, 10, 30, ArmorClass.MAIL),
    PLATE_LEGPLATES("Legplates", WowEquipmentSlot.LEGS, 15, 30, ArmorClass.PLATE),
    PLATE_CUISSES("Cuisses", WowEquipmentSlot.LEGS, 15, 30, ArmorClass.PLATE),

    // ==================== FEET ====================
    CLOTH_SANDALS("Sandals", WowEquipmentSlot.FEET, 1, 30, ArmorClass.CLOTH),
    CLOTH_SLIPPERS("Slippers", WowEquipmentSlot.FEET, 1, 30, ArmorClass.CLOTH),
    LEATHER_BOOTS("Boots", WowEquipmentSlot.FEET, 1, 30, ArmorClass.LEATHER),
    LEATHER_TREADS("Treads", WowEquipmentSlot.FEET, 1, 30, ArmorClass.LEATHER),
    MAIL_SABATONS("Sabatons", WowEquipmentSlot.FEET, 10, 30, ArmorClass.MAIL),
    MAIL_STOMPERS("Stompers", WowEquipmentSlot.FEET, 10, 30, ArmorClass.MAIL),
    PLATE_BOOTS("Plate Boots", WowEquipmentSlot.FEET, 15, 30, ArmorClass.PLATE),
    PLATE_WARBOOTS("Warboots", WowEquipmentSlot.FEET, 15, 30, ArmorClass.PLATE),

    // ==================== MAIN HAND WEAPONS ====================
    // Daggers - Fast, low damage
    DAGGER("Dagger", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.DAGGER),
    DIRK("Dirk", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.DAGGER),
    SHIV("Shiv", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.DAGGER),
    STILETTO("Stiletto", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.DAGGER),

    // Swords - Balanced
    SHORTSWORD("Shortsword", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.SWORD),
    LONGSWORD("Longsword", WowEquipmentSlot.MAIN_HAND, 5, 30, WeaponClass.SWORD),
    BROADSWORD("Broadsword", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.SWORD),
    CUTLASS("Cutlass", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.SWORD),
    SCIMITAR("Scimitar", WowEquipmentSlot.MAIN_HAND, 8, 30, WeaponClass.SWORD),

    // Axes - High damage, slower
    HATCHET("Hatchet", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.AXE),
    HANDAXE("Handaxe", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.AXE),
    WAR_AXE("War Axe", WowEquipmentSlot.MAIN_HAND, 8, 30, WeaponClass.AXE),
    CLEAVER("Cleaver", WowEquipmentSlot.MAIN_HAND, 5, 30, WeaponClass.AXE),

    // Maces - Medium damage, good vs armor
    CLUB("Club", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.MACE),
    MACE("Mace", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.MACE),
    HAMMER("Hammer", WowEquipmentSlot.MAIN_HAND, 5, 30, WeaponClass.MACE),
    MORNINGSTAR("Morningstar", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.MACE),
    FLAIL("Flail", WowEquipmentSlot.MAIN_HAND, 8, 30, WeaponClass.MACE),

    // Staves - Two-handed caster weapons
    STAFF("Staff", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.STAFF, WeaponHandType.TWO_HAND),
    QUARTERSTAFF("Quarterstaff", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.STAFF, WeaponHandType.TWO_HAND),
    WALKING_STICK("Walking Stick", WowEquipmentSlot.MAIN_HAND, 1, 15, WeaponClass.STAFF, WeaponHandType.TWO_HAND),
    BATTLE_STAFF("Battle Staff", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.STAFF, WeaponHandType.TWO_HAND),

    // Wands - Ranged caster (1H)
    WAND("Wand", WowEquipmentSlot.MAIN_HAND, 1, 30, WeaponClass.WAND),
    CRYSTAL_WAND("Crystal Wand", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.WAND),

    // ==================== TWO-HANDED WEAPONS ====================
    // Two-handed swords - High damage, slow
    GREATSWORD("Greatsword", WowEquipmentSlot.MAIN_HAND, 8, 30, WeaponClass.TWO_HAND_SWORD, WeaponHandType.TWO_HAND),
    CLAYMORE("Claymore", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.TWO_HAND_SWORD, WeaponHandType.TWO_HAND),
    BASTARD_SWORD("Bastard Sword", WowEquipmentSlot.MAIN_HAND, 5, 30, WeaponClass.TWO_HAND_SWORD,
            WeaponHandType.TWO_HAND),

    // Two-handed axes - Very high damage, very slow
    BATTLEAXE("Battleaxe", WowEquipmentSlot.MAIN_HAND, 8, 30, WeaponClass.TWO_HAND_AXE, WeaponHandType.TWO_HAND),
    GREATAXE("Greataxe", WowEquipmentSlot.MAIN_HAND, 12, 30, WeaponClass.TWO_HAND_AXE, WeaponHandType.TWO_HAND),

    // Two-handed maces - High damage, medium speed
    MAUL("Maul", WowEquipmentSlot.MAIN_HAND, 8, 30, WeaponClass.TWO_HAND_MACE, WeaponHandType.TWO_HAND),
    WARHAMMER("Warhammer", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.TWO_HAND_MACE, WeaponHandType.TWO_HAND),
    GREAT_MACE("Great Mace", WowEquipmentSlot.MAIN_HAND, 12, 30, WeaponClass.TWO_HAND_MACE, WeaponHandType.TWO_HAND),

    // Polearms - Long reach, versatile
    SPEAR("Spear", WowEquipmentSlot.MAIN_HAND, 5, 30, WeaponClass.POLEARM, WeaponHandType.TWO_HAND),
    PIKE("Pike", WowEquipmentSlot.MAIN_HAND, 10, 30, WeaponClass.POLEARM, WeaponHandType.TWO_HAND),
    HALBERD("Halberd", WowEquipmentSlot.MAIN_HAND, 12, 30, WeaponClass.POLEARM, WeaponHandType.TWO_HAND),
    GLAIVE("Glaive", WowEquipmentSlot.MAIN_HAND, 15, 30, WeaponClass.POLEARM, WeaponHandType.TWO_HAND),

    // ==================== OFF HAND ====================
    // Shields
    BUCKLER("Buckler", WowEquipmentSlot.OFF_HAND, 1, 30, ArmorClass.SHIELD),
    ROUND_SHIELD("Round Shield", WowEquipmentSlot.OFF_HAND, 1, 30, ArmorClass.SHIELD),
    KITE_SHIELD("Kite Shield", WowEquipmentSlot.OFF_HAND, 10, 30, ArmorClass.SHIELD),
    TOWER_SHIELD("Tower Shield", WowEquipmentSlot.OFF_HAND, 15, 30, ArmorClass.SHIELD),

    // Caster off-hands
    ORB("Orb", WowEquipmentSlot.OFF_HAND, 1, 30),
    CRYSTAL("Crystal", WowEquipmentSlot.OFF_HAND, 5, 30),
    TOME("Tome", WowEquipmentSlot.OFF_HAND, 1, 30),
    GRIMOIRE("Grimoire", WowEquipmentSlot.OFF_HAND, 10, 30),
    IDOL("Idol", WowEquipmentSlot.OFF_HAND, 5, 30);

    private final String displayName;
    private final WowEquipmentSlot slot;
    private final int minLevel;
    private final int maxLevel;
    private final ArmorClass armorClass;
    private final WeaponClass weaponClass;
    private final WeaponHandType handType;

    /**
     * Weapon hand type (1H, 2H, or off-hand only)
     */
    public enum WeaponHandType {
        ONE_HAND, // Can be used in main hand or off-hand
        TWO_HAND, // Requires both hands, prevents off-hand use
        OFF_HAND_ONLY // Can only be used in off-hand (shields, orbs, etc.)
    }

    // Armor constructor
    BaseItemType(String displayName, WowEquipmentSlot slot, int minLevel, int maxLevel, ArmorClass armorClass) {
        this.displayName = displayName;
        this.slot = slot;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.armorClass = armorClass;
        this.weaponClass = null;
        this.handType = null;
    }

    // Weapon constructor (1H by default for backwards compatibility)
    BaseItemType(String displayName, WowEquipmentSlot slot, int minLevel, int maxLevel, WeaponClass weaponClass) {
        this(displayName, slot, minLevel, maxLevel, weaponClass, WeaponHandType.ONE_HAND);
    }

    // Weapon constructor with hand type
    BaseItemType(String displayName, WowEquipmentSlot slot, int minLevel, int maxLevel, WeaponClass weaponClass,
            WeaponHandType handType) {
        this.displayName = displayName;
        this.slot = slot;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.armorClass = null;
        this.weaponClass = weaponClass;
        this.handType = handType;
    }

    // Generic item constructor (neither weapon nor armor, e.g. off-hand stats)
    BaseItemType(String displayName, WowEquipmentSlot slot, int minLevel, int maxLevel) {
        this.displayName = displayName;
        this.slot = slot;
        this.minLevel = minLevel;
        this.maxLevel = maxLevel;
        this.armorClass = null;
        this.weaponClass = null;
        this.handType = WeaponHandType.OFF_HAND_ONLY; // Off-hand items like orbs, tomes
    }

    public String getDisplayName() {
        return displayName;
    }

    public WowEquipmentSlot getSlot() {
        return slot;
    }

    public int getMinLevel() {
        return minLevel;
    }

    public int getMaxLevel() {
        return maxLevel;
    }

    public ArmorClass getArmorClass() {
        return armorClass;
    }

    public WeaponClass getWeaponClass() {
        return weaponClass;
    }

    public WeaponHandType getHandType() {
        return handType;
    }

    public boolean isWeapon() {
        return weaponClass != null;
    }

    public boolean isArmor() {
        return armorClass != null && armorClass != ArmorClass.SHIELD;
    }

    public boolean isShield() {
        return armorClass == ArmorClass.SHIELD;
    }

    public boolean isTwoHanded() {
        return handType == WeaponHandType.TWO_HAND;
    }

    public boolean isOneHanded() {
        return handType == WeaponHandType.ONE_HAND;
    }

    public boolean isOffHandOnly() {
        return handType == WeaponHandType.OFF_HAND_ONLY;
    }

    public boolean canDualWield() {
        return handType == WeaponHandType.ONE_HAND;
    }

    public boolean isAvailableAtLevel(int level) {
        return level >= minLevel && level <= maxLevel;
    }

    /**
     * Armor types with base armor values and class restrictions (WoW Classic style)
     */
    public enum ArmorClass {
        // Cloth - All classes can wear (lowest armor, best for casters)
        CLOTH(1.0f, "Cloth", PlayerClass.MAGE, PlayerClass.ROGUE, PlayerClass.WARRIOR),
        // Leather - Rogues and Warriors can wear (medium armor)
        LEATHER(2.0f, "Leather", PlayerClass.ROGUE, PlayerClass.WARRIOR),
        // Mail - Warriors can wear (heavy armor, future: Hunters, Shamans at 40)
        MAIL(4.0f, "Mail", PlayerClass.WARRIOR),
        // Plate - No one can wear yet (Warriors at level 40 in future)
        PLATE(6.0f, "Plate"),
        // Shield - Warriors only, VERY high armor since it's a single off-hand piece
        // Shields give ~2x the armor of a mail chest to match WoW where shields are major armor contributors
        // Player can actively block with right-click for Minecraft-style active defense
        SHIELD(8.0f, "Shield", PlayerClass.WARRIOR);

        private final float baseArmor;
        private final String displayName;
        private final Set<PlayerClass> allowedClasses;

        ArmorClass(float baseArmor, String displayName, PlayerClass... classes) {
            this.baseArmor = baseArmor;
            this.displayName = displayName;
            this.allowedClasses = classes.length > 0
                    ? EnumSet.copyOf(Arrays.asList(classes))
                    : EnumSet.noneOf(PlayerClass.class);
        }

        public float getBaseArmor() {
            return baseArmor;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Check if a specific class can wear this armor type
         */
        public boolean canBeWornBy(PlayerClass playerClass) {
            // If no classes specified, no one can wear it (e.g., Plate before level 40)
            if (allowedClasses.isEmpty()) {
                return false;
            }
            return allowedClasses.contains(playerClass);
        }

        /**
         * Calculate armor value for an item
         * WoW Classic-style scaling to match authentic values
         */
        public int calculateArmor(int itemLevel, ItemRarity rarity) {
            // POOR items have no combat stats (vendor trash only)
            if (rarity.getStatMultiplier() <= 0) {
                return 0;
            }

            // WoW Classic armor scaling (much higher than before!)
            // Formula: baseArmor × (10 + itemLevel × 1.5) × rarity
            // This gives realistic WoW values:
            // - Cloth at level 1: ~1 × 11.5 × 1.0 = 11 per piece
            // - Cloth at level 30: ~1 × 55 × 1.0 = 55 per piece (220 total for 4 pieces)
            // - Mail at level 1: ~4 × 11.5 × 1.0 = 46 per piece
            // - Mail at level 30: ~4 × 55 × 1.0 = 220 per piece (880 total for 4 pieces)
            float armor = baseArmor * (10 + itemLevel * 1.5f) * rarity.getStatMultiplier();
            return Math.max(1, Math.round(armor));
        }
    }

    /**
     * Weapon types with base DPS and attack speed (WoW Classic style)
     */
    public enum WeaponClass {
        // One-handed weapons
        DAGGER(2.5f, 1.8f, "Dagger"), // Fast, low damage
        SWORD(3.5f, 1.4f, "Sword"), // Balanced
        AXE(4.2f, 1.0f, "Axe"), // High damage, slow
        MACE(3.8f, 1.2f, "Mace"), // Medium-high, medium speed
        WAND(1.5f, 1.6f, "Wand"), // Ranged caster (1H)

        // Two-handed weapons (higher DPS, MUCH slower speed - ~2 sec between hits)
        TWO_HAND_SWORD(5.5f, 0.5f, "Two-Hand Sword"), // 2.0 sec between hits
        TWO_HAND_AXE(6.5f, 0.4f, "Two-Hand Axe"), // 2.5 sec between hits (slow but devastating)
        TWO_HAND_MACE(6.0f, 0.45f, "Two-Hand Mace"), // 2.2 sec between hits
        POLEARM(5.5f, 0.5f, "Polearm"), // 2.0 sec between hits
        STAFF(2.8f, 0.6f, "Staff"); // 1.7 sec (caster weapon, slightly faster)

        private final float baseDPS; // Base damage per second at level 1
        private final float attackSpeed;
        private final String displayName;

        WeaponClass(float baseDPS, float attackSpeed, String displayName) {
            this.baseDPS = baseDPS;
            this.attackSpeed = attackSpeed;
            this.displayName = displayName;
        }

        public float getBaseDPS() {
            return baseDPS;
        }

        public float getAttackSpeed() {
            return attackSpeed;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Calculate weapon damage for an item (WoW Classic formula)
         * In WoW: Damage per hit = DPS × swing time
         * Slower weapons deal MORE damage per hit (but same DPS)
         */
        public float calculateDamage(int itemLevel, ItemRarity rarity) {
            // POOR items have no combat stats (vendor trash only)
            if (rarity.getStatMultiplier() <= 0) {
                return 0;
            }

            // Calculate DPS that scales with item level
            float dpsPerLevel = 0.5f; // Tunable: how much DPS increases per level
            float dps = (baseDPS + (itemLevel * dpsPerLevel)) * rarity.getStatMultiplier();

            // Convert DPS to damage per hit: Damage = DPS × swing time
            // Swing time = 1 / attackSpeed (e.g., 0.5 speed = 2 second swing)
            // So: Damage = DPS / attackSpeed
            // This means slower weapons deal MORE damage per hit
            return dps / attackSpeed;
        }

        /**
         * Calculate DPS for tooltip display
         */
        public float calculateDPS(int itemLevel, ItemRarity rarity) {
            // POOR items have no combat stats
            if (rarity.getStatMultiplier() <= 0) {
                return 0;
            }

            float dpsPerLevel = 0.5f;
            return (baseDPS + (itemLevel * dpsPerLevel)) * rarity.getStatMultiplier();
        }
    }
}

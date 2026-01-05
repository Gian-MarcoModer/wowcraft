package com.gianmarco.wowcraft.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Data component that stores WowItem information on an ItemStack.
 * This allows us to persist WoW stats on vanilla items.
 */
public record WowItemData(
        String prefixName,      // Level-based prefix (e.g., "Sturdy", "Masterwork")
        String baseTypeName,
        String rarityName,
        String suffixName,      // empty string if no suffix
        int itemLevel,
        // Primary stats
        int strength,
        int agility,
        int stamina,
        int intellect,
        int spirit,
        // Combat stats
        float attackDamage,     // Weapon damage
        float attackSpeed,      // Attacks per second
        int armorValue          // Armor points
) {
    public static final WowItemData EMPTY = new WowItemData("", "", "", "", 0, 0, 0, 0, 0, 0, 0f, 0f, 0);

    // Codec for serialization (saving to disk)
    public static final Codec<WowItemData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.STRING.fieldOf("prefix").forGetter(WowItemData::prefixName),
            Codec.STRING.fieldOf("baseType").forGetter(WowItemData::baseTypeName),
            Codec.STRING.fieldOf("rarity").forGetter(WowItemData::rarityName),
            Codec.STRING.fieldOf("suffix").forGetter(WowItemData::suffixName),
            Codec.INT.fieldOf("itemLevel").forGetter(WowItemData::itemLevel),
            Codec.INT.fieldOf("strength").forGetter(WowItemData::strength),
            Codec.INT.fieldOf("agility").forGetter(WowItemData::agility),
            Codec.INT.fieldOf("stamina").forGetter(WowItemData::stamina),
            Codec.INT.fieldOf("intellect").forGetter(WowItemData::intellect),
            Codec.INT.fieldOf("spirit").forGetter(WowItemData::spirit),
            Codec.FLOAT.fieldOf("attackDamage").forGetter(WowItemData::attackDamage),
            Codec.FLOAT.fieldOf("attackSpeed").forGetter(WowItemData::attackSpeed),
            Codec.INT.fieldOf("armorValue").forGetter(WowItemData::armorValue)
    ).apply(instance, WowItemData::new));

    // Stream codec for network sync
    // Stream codec for network sync
    public static final StreamCodec<RegistryFriendlyByteBuf, WowItemData> STREAM_CODEC = new StreamCodec<>() {
        @Override
        public void encode(RegistryFriendlyByteBuf buf, WowItemData val) {
            ByteBufCodecs.STRING_UTF8.encode(buf, val.prefixName);
            ByteBufCodecs.STRING_UTF8.encode(buf, val.baseTypeName);
            ByteBufCodecs.STRING_UTF8.encode(buf, val.rarityName);
            ByteBufCodecs.STRING_UTF8.encode(buf, val.suffixName);
            ByteBufCodecs.INT.encode(buf, val.itemLevel);
            ByteBufCodecs.INT.encode(buf, val.strength);
            ByteBufCodecs.INT.encode(buf, val.agility);
            ByteBufCodecs.INT.encode(buf, val.stamina);
            ByteBufCodecs.INT.encode(buf, val.intellect);
            ByteBufCodecs.INT.encode(buf, val.spirit);
            ByteBufCodecs.FLOAT.encode(buf, val.attackDamage);
            ByteBufCodecs.FLOAT.encode(buf, val.attackSpeed);
            ByteBufCodecs.INT.encode(buf, val.armorValue);
        }

        @Override
        public WowItemData decode(RegistryFriendlyByteBuf buf) {
            return new WowItemData(
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.STRING_UTF8.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.INT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.FLOAT.decode(buf),
                    ByteBufCodecs.INT.decode(buf)
            );
        }
    };

    /**
     * Create from a WowItem
     */
    public static WowItemData fromWowItem(WowItem item) {
        WowStats stats = item.getStats();
        return new WowItemData(
                item.getPrefix() != null ? item.getPrefix().name() : "",
                item.getBaseType().name(),
                item.getRarity().name(),
                item.getSuffix() != null ? item.getSuffix().name() : "",
                item.getItemLevel(),
                stats.strength(),
                stats.agility(),
                stats.stamina(),
                stats.intellect(),
                stats.spirit(),
                item.getAttackDamage(),
                item.getAttackSpeed(),
                item.getArmorValue());
    }

    /**
     * Get the prefix enum (null if no prefix)
     */
    public ItemPrefix getPrefix() {
        if (prefixName == null || prefixName.isEmpty()) {
            return null;
        }
        try {
            return ItemPrefix.valueOf(prefixName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get the rarity enum
     */
    public ItemRarity getRarity() {
        try {
            return ItemRarity.valueOf(rarityName);
        } catch (Exception e) {
            return ItemRarity.COMMON;
        }
    }

    /**
     * Get the base type enum
     */
    public BaseItemType getBaseType() {
        try {
            return BaseItemType.valueOf(baseTypeName);
        } catch (Exception e) {
            return BaseItemType.SHORTSWORD;
        }
    }

    /**
     * Get suffix enum (null if no suffix)
     */
    public ItemSuffix getSuffix() {
        if (suffixName == null || suffixName.isEmpty()) {
            return null;
        }
        try {
            return ItemSuffix.valueOf(suffixName);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Get stats as WowStats record
     */
    public WowStats getStats() {
        return new WowStats(strength, agility, stamina, intellect, spirit);
    }

    /**
     * Check if this is valid WoW item data
     */
    public boolean isValid() {
        return !baseTypeName.isEmpty() && !rarityName.isEmpty() && itemLevel > 0;
    }

    /**
     * Get display name with prefix, base name, suffix, and rarity color
     * Example: "§aSturdy Iron Sword of the Bear"
     */
    public String getDisplayName() {
        StringBuilder name = new StringBuilder();

        // Add prefix if present
        ItemPrefix prefix = getPrefix();
        if (prefix != null) {
            name.append(prefix.getDisplayName()).append(" ");
        }

        // Add base type name
        name.append(getBaseType().getDisplayName());

        // Add suffix if present
        ItemSuffix suffix = getSuffix();
        if (suffix != null) {
            name.append(" ").append(suffix.getDisplayName());
        }

        return getRarity().formatName(name.toString());
    }

    /**
     * Get equipment slot
     */
    public WowEquipmentSlot getSlot() {
        return getBaseType().getSlot();
    }

    /**
     * Check if this is a weapon
     */
    public boolean isWeapon() {
        return getBaseType().isWeapon();
    }

    /**
     * Check if this is armor
     */
    public boolean isArmor() {
        return getBaseType().isArmor() || getBaseType().isShield();
    }

    /**
     * Get the armor class of this item (null for weapons and non-armor items)
     */
    public BaseItemType.ArmorClass getArmorClass() {
        return getBaseType().getArmorClass();
    }

    /**
     * Calculate DPS for tooltip
     */
    public float getDPS() {
        if (attackDamage > 0 && attackSpeed > 0) {
            return attackDamage * attackSpeed;
        }
        return 0;
    }

    /**
     * Check if this is a two-handed weapon
     */
    public boolean isTwoHanded() {
        return getBaseType().isTwoHanded();
    }

    /**
     * Check if this weapon can be dual wielded
     */
    public boolean canDualWield() {
        return getBaseType().canDualWield();
    }
}

package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.MobLevelManager;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages loot drops and player inventory of WoW items.
 */
public class LootManager {
    // Track generated items by their UUID
    private static final Map<UUID, WowItem> generatedItems = new HashMap<>();

    // Track player equipment
    private static final Map<UUID, WowItem[]> playerEquipment = new HashMap<>();

    /**
     * Called when a mob dies. Determines if loot should drop.
     */
    public static void onMobDeath(LivingEntity mob, Player killer) {
        if (!(killer instanceof ServerPlayer serverPlayer))
            return;
        if (mob.level().isClientSide())
            return;

        // Get mob level from our system
        int mobLevel = MobLevelManager.getMobLevel(mob);

        // Check if loot should drop
        if (!ItemGenerator.shouldDropLoot(mobLevel)) {
            return;
        }

        // Generate item
        PlayerClass playerClass = PlayerDataManager.getPlayerClass(serverPlayer);
        WowItem wowItem = ItemGenerator.generateItem(mobLevel, playerClass);

        // Store the item
        generatedItems.put(wowItem.getId(), wowItem);

        // Create a visual item entity with proper attributes
        ItemStack displayStack = createDisplayStack(wowItem);

        // Spawn the item in the world
        ServerLevel level = (ServerLevel) mob.level();
        ItemEntity itemEntity = new ItemEntity(
                level,
                mob.getX(), mob.getY() + 0.5, mob.getZ(),
                displayStack);
        itemEntity.setPickUpDelay(10); // Short delay before pickup
        itemEntity.setGlowingTag(wowItem.getRarity().ordinal() >= ItemRarity.RARE.ordinal()); // Rare+ items glow
        level.addFreshEntity(itemEntity);

        // Notify player
        serverPlayer.sendSystemMessage(Component.literal(
                "§7Loot: " + wowItem.getDisplayName()));

        WowCraft.LOGGER.info("Dropped {} (iLvl {}, {}) from {} for player {}",
                wowItem.getPlainName(), wowItem.getItemLevel(), wowItem.getRarity().getDisplayName(),
                mob.getType().getDescription().getString(),
                serverPlayer.getName().getString());
    }

    /**
     * Create a vanilla ItemStack to represent the WoW item with proper attributes
     * Public so commands can use it to give items to players
     */
    public static ItemStack createDisplayStack(WowItem wowItem) {
        // Choose vanilla item based on slot and type
        ItemStack stack = getVanillaItem(wowItem);

        // CRITICAL: Clear vanilla attribute modifiers FIRST before applying our own
        // This prevents stacking (e.g., vanilla sword -2.4 + wow -2.2 = broken speed)
        stack.remove(DataComponents.ATTRIBUTE_MODIFIERS);

        // Set custom name with rarity color
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(wowItem.getDisplayName()));

        // Store WoW item data on the stack
        stack.set(WowItemComponents.WOW_ITEM_DATA, WowItemData.fromWowItem(wowItem));

        // Apply attribute modifiers for weapons and armor (now without vanilla
        // interference)
        applyAttributeModifiers(stack, wowItem);

        // Apply custom model data to use custom 3D models (MCModels pack uses 2501)
        // In 1.21.4+, CustomModelData uses lists of floats, flags, strings, colors
        stack.set(DataComponents.CUSTOM_MODEL_DATA, new net.minecraft.world.item.component.CustomModelData(
                java.util.List.of(2501f), // floats - triggers predicate custom_model_data:2501
                java.util.List.of(), // flags (booleans)
                java.util.List.of(), // strings
                java.util.List.of() // colors
        ));

        return stack;
    }

    /**
     * Get the appropriate vanilla item for the WoW item type
     */
    private static ItemStack getVanillaItem(WowItem wowItem) {
        BaseItemType baseType = wowItem.getBaseType();

        // Weapons - Use actual weapon items to ensure attack cooldown works
        if (baseType.isWeapon()) {
            BaseItemType.WeaponClass wc = baseType.getWeaponClass();
            return switch (wc) {
                case DAGGER -> new ItemStack(Items.IRON_SWORD); // Fast weapon
                case SWORD -> new ItemStack(Items.IRON_SWORD);
                case AXE -> new ItemStack(Items.IRON_AXE);
                case MACE -> new ItemStack(Items.IRON_PICKAXE); // Changed from shovel to pickaxe (has attack speed)
                case WAND -> new ItemStack(Items.STICK); // Changed from blaze rod to stick (simpler visual)
                case TWO_HAND_SWORD -> new ItemStack(Items.DIAMOND_SWORD);
                case TWO_HAND_AXE -> new ItemStack(Items.DIAMOND_AXE);
                case TWO_HAND_MACE -> new ItemStack(Items.DIAMOND_PICKAXE); // Changed from shovel
                case POLEARM -> new ItemStack(Items.TRIDENT); // Already a weapon
                case STAFF -> new ItemStack(Items.STICK); // Changed from hoe to stick (caster weapon)
            };
        }

        // Armor
        if (baseType.isArmor()) {
            BaseItemType.ArmorClass ac = baseType.getArmorClass();
            WowEquipmentSlot slot = baseType.getSlot();

            return switch (slot) {
                case HEAD -> switch (ac) {
                    case CLOTH, LEATHER -> new ItemStack(Items.LEATHER_HELMET);
                    case MAIL -> new ItemStack(Items.CHAINMAIL_HELMET);
                    case PLATE -> new ItemStack(Items.IRON_HELMET);
                    default -> new ItemStack(Items.LEATHER_HELMET);
                };
                case CHEST -> switch (ac) {
                    case CLOTH, LEATHER -> new ItemStack(Items.LEATHER_CHESTPLATE);
                    case MAIL -> new ItemStack(Items.CHAINMAIL_CHESTPLATE);
                    case PLATE -> new ItemStack(Items.IRON_CHESTPLATE);
                    default -> new ItemStack(Items.LEATHER_CHESTPLATE);
                };
                case LEGS -> switch (ac) {
                    case CLOTH, LEATHER -> new ItemStack(Items.LEATHER_LEGGINGS);
                    case MAIL -> new ItemStack(Items.CHAINMAIL_LEGGINGS);
                    case PLATE -> new ItemStack(Items.IRON_LEGGINGS);
                    default -> new ItemStack(Items.LEATHER_LEGGINGS);
                };
                case FEET -> switch (ac) {
                    case CLOTH, LEATHER -> new ItemStack(Items.LEATHER_BOOTS);
                    case MAIL -> new ItemStack(Items.CHAINMAIL_BOOTS);
                    case PLATE -> new ItemStack(Items.IRON_BOOTS);
                    default -> new ItemStack(Items.LEATHER_BOOTS);
                };
                default -> new ItemStack(Items.LEATHER_CHESTPLATE);
            };
        }

        // Shields
        if (baseType.isShield()) {
            return new ItemStack(Items.SHIELD);
        }

        // Off-hand caster items
        return switch (baseType) {
            case ORB, CRYSTAL -> new ItemStack(Items.ENDER_PEARL);
            case TOME, GRIMOIRE -> new ItemStack(Items.BOOK);
            case IDOL -> new ItemStack(Items.TOTEM_OF_UNDYING);
            default -> new ItemStack(Items.BOOK);
        };
    }

    /**
     * Apply Minecraft attribute modifiers for weapon damage, attack speed, and
     * armor
     * IMPORTANT: We set show_in_tooltip to false to hide vanilla attributes and
     * only show WoW stats
     */
    private static void applyAttributeModifiers(ItemStack stack, WowItem wowItem) {
        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        // Weapon damage and attack speed
        if (wowItem.getBaseType().isWeapon() && wowItem.getAttackDamage() > 0) {
            WowEquipmentSlot slot = wowItem.getSlot();
            boolean isOffHand = (slot == WowEquipmentSlot.OFF_HAND);

            // Apply off-hand damage penalty (50% damage for dual wielding)
            float damage = wowItem.getAttackDamage();
            if (isOffHand && wowItem.getBaseType().canDualWield()) {
                damage *= DualWieldRules.getOffHandDamagePenalty(); // 50% damage in off-hand
            }

            // Attack damage (use MULTIPLY_BASE to override vanilla item damage completely)
            // First, we need to zero out any base damage from the vanilla item
            builder.add(
                    Attributes.ATTACK_DAMAGE,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "weapon_damage"),
                            damage,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);

            // Attack speed - Set absolute value by using the base 4.0 reference
            // WoW weapon speeds are attacks per second (e.g., 1.8 = 1.8 attacks/sec)
            // Minecraft base is 4.0, so we subtract to get the modifier
            // CRITICAL: This only works if the base item has an attack speed attribute!
            float speedValue = wowItem.getAttackSpeed() - 4.0f;
            builder.add(
                    Attributes.ATTACK_SPEED,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "weapon_speed"),
                            speedValue,
                            AttributeModifier.Operation.ADD_VALUE),
                    EquipmentSlotGroup.MAINHAND);
        }

        // Armor value
        if (wowItem.getArmorValue() > 0) {
            EquipmentSlotGroup slotGroup = getEquipmentSlotGroup(wowItem.getSlot());
            builder.add(
                    Attributes.ARMOR,
                    new AttributeModifier(
                            ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "armor_value"),
                            wowItem.getArmorValue(),
                            AttributeModifier.Operation.ADD_VALUE),
                    slotGroup);
        }

        // Set the modifiers on the stack
        // NOTE: We'll filter out vanilla attribute lines in the tooltip mixin instead
        stack.set(DataComponents.ATTRIBUTE_MODIFIERS, builder.build());
    }

    /**
     * Convert WowEquipmentSlot to Minecraft EquipmentSlotGroup
     */
    private static EquipmentSlotGroup getEquipmentSlotGroup(WowEquipmentSlot slot) {
        return switch (slot) {
            case HEAD -> EquipmentSlotGroup.HEAD;
            case CHEST -> EquipmentSlotGroup.CHEST;
            case LEGS -> EquipmentSlotGroup.LEGS;
            case FEET -> EquipmentSlotGroup.FEET;
            case MAIN_HAND -> EquipmentSlotGroup.MAINHAND;
            case OFF_HAND -> EquipmentSlotGroup.OFFHAND;
        };
    }

    /**
     * Get player's equipment array
     */
    public static WowItem[] getPlayerEquipment(Player player) {
        return playerEquipment.computeIfAbsent(
                player.getUUID(),
                k -> new WowItem[WowEquipmentSlot.getSlotCount()]);
    }

    /**
     * Equip an item to the appropriate slot
     */
    public static void equipItem(Player player, WowItem item) {
        WowItem[] equipment = getPlayerEquipment(player);
        equipment[item.getSlot().getSlotIndex()] = item;

        // Recalculate stats
        WowStats totalStats = calculateTotalStats(player);
        WowCraft.LOGGER.info("Player {} equipped {}. Total stats: Str={}, Sta={}, Int={}",
                player.getName().getString(), item.getPlainName(),
                totalStats.strength(), totalStats.stamina(), totalStats.intellect());
    }

    /**
     * Calculate total stats from all equipped items
     */
    public static WowStats calculateTotalStats(Player player) {
        WowItem[] equipment = getPlayerEquipment(player);
        WowStats total = WowStats.ZERO;

        for (WowItem item : equipment) {
            if (item != null) {
                total = total.add(item.getStats());
            }
        }

        return total;
    }

    /**
     * Clear cached data (for server shutdown)
     */
    public static void clear() {
        generatedItems.clear();
        playerEquipment.clear();
    }
}

package com.gianmarco.wowcraft.item;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

/**
 * Registers custom data components for storing WoW item data on ItemStacks.
 */
public class WowItemComponents {

    public static final DataComponentType<WowItemData> WOW_ITEM_DATA = DataComponentType.<WowItemData>builder()
            .persistent(WowItemData.CODEC)
            .networkSynchronized(WowItemData.STREAM_CODEC)
            .build();

    /**
     * Register all custom data components
     */
    public static void register() {
        Registry.register(
                BuiltInRegistries.DATA_COMPONENT_TYPE,
                ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "wow_item_data"),
                WOW_ITEM_DATA);
        WowCraft.LOGGER.info("Registered WowCraft item data components");
    }
}

package com.gianmarco.wowcraft.item;

/**
 * Equipment slots for WoW-style gear.
 */
public enum WowEquipmentSlot {
    HEAD("Head", 0),
    CHEST("Chest", 1),
    LEGS("Legs", 2),
    FEET("Feet", 3),
    MAIN_HAND("Main Hand", 4),
    OFF_HAND("Off Hand", 5);

    private final String displayName;
    private final int slotIndex;

    WowEquipmentSlot(String displayName, int slotIndex) {
        this.displayName = displayName;
        this.slotIndex = slotIndex;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getSlotIndex() {
        return slotIndex;
    }

    public static int getSlotCount() {
        return values().length;
    }
}

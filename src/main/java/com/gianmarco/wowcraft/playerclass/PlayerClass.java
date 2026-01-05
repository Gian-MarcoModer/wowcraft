package com.gianmarco.wowcraft.playerclass;

/**
 * Represents the available player classes in WowCraft.
 * Each class has unique abilities, resource types, and color themes.
 */
public enum PlayerClass {
    NONE("None", "No class selected", 0x808080, ResourceType.NONE),
    WARRIOR("Warrior", "A mighty melee fighter who builds rage through combat.", 0xC79C6E, ResourceType.RAGE),
    MAGE("Mage", "A powerful spellcaster who wields arcane, fire, and frost magic.", 0x69CCF0, ResourceType.MANA),
    ROGUE("Rogue", "A stealthy assassin who uses energy and combo points for devastating strikes.", 0xFFF569, ResourceType.ENERGY);

    private final String displayName;
    private final String description;
    private final int color;
    private final ResourceType resourceType;

    PlayerClass(String displayName, String description, int color, ResourceType resourceType) {
        this.displayName = displayName;
        this.description = description;
        this.color = color;
        this.resourceType = resourceType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * @return The class color in 0xRRGGBB format (WoW Classic colors)
     */
    public int getColor() {
        return color;
    }

    public ResourceType getResourceType() {
        return resourceType;
    }

    /**
     * @return The color with full opacity for rendering (0xAARRGGBB)
     */
    public int getColorWithAlpha() {
        return 0xFF000000 | color;
    }
}

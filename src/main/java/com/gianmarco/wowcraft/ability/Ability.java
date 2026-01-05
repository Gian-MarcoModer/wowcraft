package com.gianmarco.wowcraft.ability;

import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.world.entity.player.Player;

/**
 * Base class for all abilities.
 * Each ability has a name, cooldown, resource cost, and execute behavior.
 */
public abstract class Ability {
    protected final String id;
    protected final String displayName;
    protected final int cooldownTicks; // 20 ticks = 1 second
    protected final int resourceCost;
    protected final PlayerClass requiredClass;

    protected Ability(String id, String displayName, int cooldownSeconds, int resourceCost, PlayerClass requiredClass) {
        this.id = id;
        this.displayName = displayName;
        this.cooldownTicks = cooldownSeconds * 20;
        this.resourceCost = resourceCost;
        this.requiredClass = requiredClass;
    }

    public String getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getCooldownTicks() {
        return cooldownTicks;
    }

    public int getCooldownSeconds() {
        return cooldownTicks / 20;
    }

    public int getResourceCost() {
        return resourceCost;
    }

    public PlayerClass getRequiredClass() {
        return requiredClass;
    }

    /**
     * Check if this ability can be used by the player
     * 
     * @return true if the ability can be used
     */
    public abstract boolean canUse(Player player);

    /**
     * Use the ability. Called when the player activates it.
     * Override this for abilities with cast times.
     * Default implementation calls execute() immediately.
     */
    public void use(Player player) {
        execute(player);
    }

    /**
     * Execute the ability effect
     * 
     * @param player The player using the ability
     */
    public abstract void execute(Player player);

    /**
     * Get the description of this ability for tooltips
     */
    public abstract String getDescription();

    /**
     * Get the icon texture location for this ability.
     * Defaults to "wowcraft:textures/gui/spells/<id>.png"
     */
    public net.minecraft.resources.ResourceLocation getIconTexture() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("wowcraft",
                "textures/gui/spells/" + id + ".png");
    }
}

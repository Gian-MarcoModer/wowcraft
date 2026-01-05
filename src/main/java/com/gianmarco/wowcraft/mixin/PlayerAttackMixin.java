package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.stats.StatsManager;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Mixin to add bonus melee damage based on Attack Power from stats.
 */
@Mixin(Player.class)
public abstract class PlayerAttackMixin {

    /**
     * Modifies the damage dealt in attack() method.
     * Adds bonus damage from Attack Power.
     */
    @ModifyVariable(method = "attack", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private Entity wowcraft$modifyAttackTarget(Entity target) {
        // We can't modify damage directly here, but we can set up for it
        // The actual damage modification happens in the next mixin
        return target;
    }
}

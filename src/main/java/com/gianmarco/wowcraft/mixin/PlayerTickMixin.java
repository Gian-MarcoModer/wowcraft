package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.stats.StatsManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.EnumMap;
import java.util.Map;

/**
 * Monitors equipment changes every tick and triggers stat recalculation.
 * This approach avoids Mixin conflicts with other mods (like Trinkets).
 */
@Mixin(Player.class)
public class PlayerTickMixin {

    @Unique
    private final Map<EquipmentSlot, ItemStack> lastEquipment = new EnumMap<>(EquipmentSlot.class);

    @Inject(method = "tick", at = @At("TAIL"))
    private void wowcraft$onTick(CallbackInfo ci) {
        Player player = (Player) (Object) this;

        // Only run on server side to avoid duplicate recalculation
        if (player.level().isClientSide()) {
            return;
        }

        boolean changed = false;

        // Check all equipment slots for changes
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            ItemStack current = player.getItemBySlot(slot);
            ItemStack last = lastEquipment.getOrDefault(slot, ItemStack.EMPTY);

            if (!ItemStack.matches(last, current)) {
                lastEquipment.put(slot, current.copy());
                changed = true;
            }
        }

        if (changed) {
            StatsManager.recalculateStats(player);
        }
    }
}

package com.gianmarco.wowcraft.mixin;

import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.item.LootManager;
import com.gianmarco.wowcraft.mobpack.MobPackManager;
import com.gianmarco.wowcraft.mobpack.SocialAggroHandler;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import com.gianmarco.wowcraft.stats.ExperienceManager;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixin to hook into mob death events for WoW-style loot drops,
 * pack respawn tracking, and social aggro triggering.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {

    @Inject(method = "die", at = @At("HEAD"))
    private void wowcraft$onDeath(DamageSource damageSource, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Only process on server side
        if (self.level().isClientSide())
            return;

        // Don't drop loot for players
        if (self instanceof Player)
            return;

        // Get the killer
        if (damageSource.getEntity() instanceof Player killer) {
            LootManager.onMobDeath(self, killer);
            ExperienceManager.awardXpForKill(killer, self);
        }

        // Track pack mob death for respawning
        if (self.level() instanceof ServerLevel serverLevel) {
            MobPackManager.onMobDeath(self, serverLevel);
        }
    }

    /**
     * Prevent hurt animation from zero/negative damage (armor changes, etc.)
     * and trigger social aggro for pack mobs.
     */
    @Inject(method = "actuallyHurt", at = @At("HEAD"), cancellable = true)
    private void wowcraft$onHurt(ServerLevel level, DamageSource source, float amount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;

        // Handle player being hurt - prevent false hurt animations
        if (self instanceof Player) {
            if (amount <= 0) {
                ci.cancel();
            }
            return;
        }

        // Handle mob being hurt by player - trigger social aggro
        if (source.getEntity() instanceof Player player && !level.isClientSide()) {
            // Check if this is a pack mob
            MobData data = self.getAttached(PlayerDataRegistry.MOB_DATA);
            if (data != null && data.packId() != null) {
                SocialAggroHandler.onPlayerDamageMob(player, self, level);
            }
        }
    }
}

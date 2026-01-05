package com.gianmarco.wowcraft.ability.rogue;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.combat.DamagePipeline;
import com.gianmarco.wowcraft.combat.DamageResult;
import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerData;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsCalculator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Backstab - Powerful positional attack that must be used from behind the target.
 * Generates 2 combo points and deals bonus damage from stealth.
 * Damage scales with Attack Power.
 * Formula: BASE_DAMAGE + (AttackPower * SCALING_COEFFICIENT)
 * Bonus: 50% more damage from stealth
 */
public class Backstab extends Ability {

    private static final float BASE_DAMAGE = 24.0f;
    private static final float AP_SCALING = 1.2f; // 120% of Attack Power
    private static final float RANGE = 4.0f;
    private static final int COMBO_POINTS_GENERATED = 2;
    private static final float STEALTH_DAMAGE_MULTIPLIER = 1.5f;

    public Backstab() {
        super("backstab", "Backstab", 5, 40, PlayerClass.ROGUE);
    }

    @Override
    public boolean canUse(Player player) {
        LivingEntity target = getTargetInRange(player);
        if (target == null)
            return false;

        // Must be behind the target
        boolean isBehind = isBehindTarget(player, target);

        // Optional: Show feedback when not in position (client-side only to avoid spam)
        if (!isBehind && player.level().isClientSide) {
            // Could add a subtle indicator here if desired
            // For now, the ability simply won't be usable
        }

        return isBehind;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        LivingEntity target = getTargetInRange(player);
        if (target == null || !isBehindTarget(player, target))
            return;

        // Calculate damage with Attack Power scaling
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.ROGUE, level);

        float attackPower = stats.getAttackPower();
        float bonusDamage = stats.getBonusMeleeDamage();
        float baseDamage = BASE_DAMAGE + (attackPower * AP_SCALING) + bonusDamage;

        // Bonus damage from stealth
        boolean fromStealth = player.hasEffect(MobEffects.INVISIBILITY);
        if (fromStealth) {
            baseDamage *= STEALTH_DAMAGE_MULTIPLIER;
            // Break stealth when using backstab
            player.removeEffect(MobEffects.INVISIBILITY);
            player.removeEffect(MobEffects.SLOWNESS);
        }

        // Deal damage through the pipeline (handles crits, events, FCT)
        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
        DamageResult result = DamagePipeline.deal(source, target, baseDamage);

        // Generate combo points
        PlayerData data = PlayerDataManager.getData(player);
        int newComboPoints = Math.min(5, data.comboPoints() + COMBO_POINTS_GENERATED);
        PlayerData newData = data.withComboPoints(newComboPoints);
        PlayerDataManager.setData(player, newData);
        com.gianmarco.wowcraft.network.NetworkHandler.syncPlayerData((net.minecraft.server.level.ServerPlayer) player);

        // Play backstab sound (different for crit or from stealth)
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                result.isCritical() || fromStealth ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0f, fromStealth ? 0.7f : 0.9f);

        // Spawn particles (dramatic red/dark particles for backstab)
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                result.isCritical() ? 30 : 20, 0.5, 0.5, 0.5, 0.2);

        // Extra particles for stealth backstab
        if (fromStealth) {
            serverLevel.sendParticles(ParticleTypes.SMOKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.3, 0.4, 0.3, 0.05);
        }
    }

    private LivingEntity getTargetInRange(Player player) {
        AABB searchBox = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));

        return entities.stream()
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(player),
                        b.distanceToSqr(player)))
                .orElse(null);
    }

    /**
     * Check if player is behind the target (positional requirement)
     * Uses the target's body rotation (yaw) instead of look angle for more reliable detection
     */
    private boolean isBehindTarget(Player player, Entity target) {
        // Get the direction from target to player (horizontal plane only)
        Vec3 targetPos = target.position();
        Vec3 playerPos = player.position();

        double dx = playerPos.x - targetPos.x;
        double dz = playerPos.z - targetPos.z;

        // Calculate angle from target to player (in radians)
        double angleToPlayer = Math.atan2(-dx, dz); // -dx because of Minecraft's coordinate system

        // Get target's body rotation (yaw) in radians
        // yaw is in degrees, 0 = south, 90 = west, -90 = east, 180/-180 = north
        double targetYaw = Math.toRadians(target.getYRot());

        // Normalize angles to -PI to PI range
        while (angleToPlayer > Math.PI) angleToPlayer -= 2 * Math.PI;
        while (angleToPlayer < -Math.PI) angleToPlayer += 2 * Math.PI;
        while (targetYaw > Math.PI) targetYaw -= 2 * Math.PI;
        while (targetYaw < -Math.PI) targetYaw += 2 * Math.PI;

        // Calculate the difference between where target is facing and where player is
        double angleDiff = Math.abs(angleToPlayer - targetYaw);
        if (angleDiff > Math.PI) angleDiff = 2 * Math.PI - angleDiff;

        // Player is behind if within 90 degrees of target's back (±45 degrees from directly behind)
        // More strict than before: requires angle difference < 90 degrees (PI/2 radians)
        return angleDiff < Math.PI / 2;
    }

    @Override
    public String getDescription() {
        return String.format("Positional attack from behind dealing %.0f + 120%% AP damage. +50%% damage from stealth. Generates %d combo points. %d energy, %ds cooldown.",
                BASE_DAMAGE, COMBO_POINTS_GENERATED, resourceCost, getCooldownSeconds());
    }
}

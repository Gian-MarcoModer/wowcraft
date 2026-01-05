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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Eviscerate - Powerful finishing move that consumes all combo points.
 * Damage scales with combo points and Attack Power.
 * Formula: (BASE_DAMAGE + (AttackPower * SCALING_COEFFICIENT)) * comboPoints
 */
public class Eviscerate extends Ability {

    private static final float BASE_DAMAGE_PER_CP = 15.0f;
    private static final float AP_SCALING_PER_CP = 0.5f; // 50% of AP per combo point
    private static final float RANGE = 4.0f;

    public Eviscerate() {
        super("eviscerate", "Eviscerate", 6, 50, PlayerClass.ROGUE);
    }

    @Override
    public boolean canUse(Player player) {
        // Need at least 1 combo point and a target in range
        PlayerData data = PlayerDataManager.getData(player);
        return data.comboPoints() > 0 && getTargetInRange(player) != null;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        LivingEntity target = getTargetInRange(player);
        if (target == null)
            return;

        PlayerData data = PlayerDataManager.getData(player);
        int comboPoints = data.comboPoints();

        if (comboPoints == 0)
            return;

        // Calculate damage with Attack Power scaling and combo points
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.ROGUE, level);

        float attackPower = stats.getAttackPower();
        float bonusDamage = stats.getBonusMeleeDamage();
        float damagePerCP = BASE_DAMAGE_PER_CP + (attackPower * AP_SCALING_PER_CP) + (bonusDamage / 5);
        float totalDamage = damagePerCP * comboPoints;

        // Deal damage through the pipeline (handles crits, events, FCT)
        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
        DamageResult result = DamagePipeline.deal(source, target, totalDamage);

        // Consume all combo points
        PlayerData newData = data.withComboPoints(0);
        PlayerDataManager.setData(player, newData);
        com.gianmarco.wowcraft.network.NetworkHandler.syncPlayerData((net.minecraft.server.level.ServerPlayer) player);

        // Play sound (different for crit and more dramatic for more combo points)
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                result.isCritical() ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_KNOCKBACK,
                SoundSource.PLAYERS, 1.0f + (comboPoints * 0.1f), 0.8f - (comboPoints * 0.05f));

        // Spawn particles (more for higher combo points)
        int particleCount = 15 + (comboPoints * 5);
        serverLevel.sendParticles(result.isCritical() ? ParticleTypes.CRIT : ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                particleCount, 0.5, 0.5, 0.5, 0.15);

        // Additional blood particles for high combo points
        if (comboPoints >= 4) {
            serverLevel.sendParticles(ParticleTypes.CRIMSON_SPORE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    comboPoints * 3, 0.3, 0.3, 0.3, 0.1);
        }
    }

    private LivingEntity getTargetInRange(Player player) {
        AABB searchBox = player.getBoundingBox().inflate(RANGE);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive() && player.hasLineOfSight(e));

        return entities.stream()
                .filter(e -> isInFront(player, e))
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(player),
                        b.distanceToSqr(player)))
                .orElse(null);
    }

    private boolean isInFront(Player player, Entity target) {
        var toTarget = target.position().subtract(player.position()).normalize();
        var look = player.getLookAngle();
        return toTarget.dot(look) > 0.5;
    }

    @Override
    public String getDescription() {
        return String.format("Devastating finisher dealing (%.0f + 50%% AP) × combo points damage. Consumes all combo points. %d energy, %ds cooldown.",
                BASE_DAMAGE_PER_CP, resourceCost, getCooldownSeconds());
    }
}

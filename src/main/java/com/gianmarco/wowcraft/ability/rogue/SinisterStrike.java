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
 * Sinister Strike - Quick melee attack that generates 1 combo point.
 * Damage scales with Attack Power.
 * Formula: BASE_DAMAGE + (AttackPower * SCALING_COEFFICIENT)
 */
public class SinisterStrike extends Ability {

    private static final float BASE_DAMAGE = 12.0f;
    private static final float AP_SCALING = 0.6f; // 60% of Attack Power
    private static final float RANGE = 4.0f;
    private static final int COMBO_POINTS_GENERATED = 1;

    public SinisterStrike() {
        super("sinister_strike", "Sinister Strike", 2, 30, PlayerClass.ROGUE);
    }

    @Override
    public boolean canUse(Player player) {
        return getTargetInRange(player) != null;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        LivingEntity target = getTargetInRange(player);
        if (target == null)
            return;

        // Calculate damage with Attack Power scaling
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.ROGUE, level);

        float attackPower = stats.getAttackPower();
        float bonusDamage = stats.getBonusMeleeDamage();
        float baseDamage = BASE_DAMAGE + (attackPower * AP_SCALING) + bonusDamage;

        // Deal damage through the pipeline (handles crits, events, FCT)
        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
        DamageResult result = DamagePipeline.deal(source, target, baseDamage);

        // Generate combo point
        PlayerData data = PlayerDataManager.getData(player);
        int newComboPoints = Math.min(5, data.comboPoints() + COMBO_POINTS_GENERATED);
        PlayerData newData = data.withComboPoints(newComboPoints);
        PlayerDataManager.setData(player, newData);
        com.gianmarco.wowcraft.network.NetworkHandler.syncPlayerData((net.minecraft.server.level.ServerPlayer) player);

        // Play sound (different for crit)
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                result.isCritical() ? SoundEvents.PLAYER_ATTACK_CRIT : SoundEvents.PLAYER_ATTACK_STRONG,
                SoundSource.PLAYERS, 1.0f, 1.2f);

        // Spawn particles (yellow for rogue abilities)
        serverLevel.sendParticles(result.isCritical() ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                result.isCritical() ? 20 : 10, 0.4, 0.4, 0.4, 0.1);
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
        return String.format("Quick melee strike dealing %.0f + 60%% AP damage. Generates %d combo point. %d energy, %ds cooldown.",
                BASE_DAMAGE, COMBO_POINTS_GENERATED, resourceCost, getCooldownSeconds());
    }
}

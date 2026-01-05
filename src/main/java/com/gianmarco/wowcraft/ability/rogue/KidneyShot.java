package com.gianmarco.wowcraft.ability.rogue;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.combat.DamagePipeline;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Kidney Shot - Stun finisher that consumes all combo points.
 * Stun duration scales with combo points.
 * Formula: 1 second base + 0.5 seconds per combo point
 * (1 CP = 1.5s, 2 CP = 2s, 3 CP = 2.5s, 4 CP = 3s, 5 CP = 3.5s)
 */
public class KidneyShot extends Ability {

    private static final float BASE_DAMAGE = 10.0f;
    private static final float AP_SCALING = 0.3f; // 30% of Attack Power
    private static final float RANGE = 4.0f;
    private static final int BASE_STUN_TICKS = 20; // 1 second
    private static final int STUN_TICKS_PER_CP = 10; // 0.5 seconds per combo point

    public KidneyShot() {
        super("kidney_shot", "Kidney Shot", 20, 50, PlayerClass.ROGUE);
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

        // Calculate damage with Attack Power scaling
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.ROGUE, level);

        float attackPower = stats.getAttackPower();
        float bonusDamage = stats.getBonusMeleeDamage();
        float totalDamage = BASE_DAMAGE + (attackPower * AP_SCALING) + bonusDamage;

        // Deal damage through the pipeline (handles crits, events, FCT)
        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
        DamagePipeline.deal(source, target, totalDamage);

        // Apply stun based on combo points
        int stunDuration = BASE_STUN_TICKS + (STUN_TICKS_PER_CP * comboPoints);
        MobEffectInstance stun = new MobEffectInstance(
                MobEffects.SLOWNESS,
                stunDuration,
                255, // Maximum slowness = stun
                false,
                true // Show particles
        );
        target.addEffect(stun);

        // Also apply weakness to simulate incapacitation
        MobEffectInstance weakness = new MobEffectInstance(
                MobEffects.WEAKNESS,
                stunDuration,
                2,
                false,
                false
        );
        target.addEffect(weakness);

        // Stop target movement
        target.setDeltaMovement(0, target.getDeltaMovement().y, 0);

        // Consume all combo points
        PlayerData newData = data.withComboPoints(0);
        PlayerDataManager.setData(player, newData);
        com.gianmarco.wowcraft.network.NetworkHandler.syncPlayerData((net.minecraft.server.level.ServerPlayer) player);

        // Play stun sound
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.6f, 1.8f);

        // Spawn impact particles (stars for stun effect)
        serverLevel.sendParticles(ParticleTypes.CRIT,
                target.getX(), target.getY() + target.getBbHeight() + 0.5, target.getZ(),
                15 + (comboPoints * 3), 0.3, 0.3, 0.3, 0.1);

        // Spawn damage particles
        serverLevel.sendParticles(ParticleTypes.DAMAGE_INDICATOR,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                10, 0.3, 0.3, 0.3, 0.1);

        // Create a ring of particles around stunned target
        for (int i = 0; i < 12; i++) {
            double angle = (i / 12.0) * Math.PI * 2;
            double offsetX = Math.cos(angle) * 0.8;
            double offsetZ = Math.sin(angle) * 0.8;
            serverLevel.sendParticles(ParticleTypes.END_ROD,
                    target.getX() + offsetX, target.getY() + 1.5, target.getZ() + offsetZ,
                    1, 0, 0, 0, 0);
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
        return String.format("Stun finisher dealing %.0f + 30%% AP damage. Stuns for 1-3.5s based on combo points. Consumes all combo points. %d energy, %ds cooldown.",
                BASE_DAMAGE, resourceCost, getCooldownSeconds());
    }
}

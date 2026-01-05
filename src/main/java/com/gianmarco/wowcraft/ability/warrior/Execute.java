package com.gianmarco.wowcraft.ability.warrior;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.combat.DamagePipeline;
import com.gianmarco.wowcraft.combat.DamageResult;
import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
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
 * Execute - A devastating finishing move that can only be used on low-health targets.
 * Deals massive damage when target is below 20% health.
 * Formula: BASE_DAMAGE + (AttackPower * SCALING_COEFFICIENT)
 * Critical strikes deal 2.5x damage instead of 2x.
 */
public class Execute extends Ability {

    private static final float BASE_DAMAGE = 15.0f;
    private static final float AP_SCALING = 0.8f; // 80% of Attack Power added as damage
    private static final float RANGE = 4.0f;
    private static final float EXECUTE_THRESHOLD = 0.20f; // 20% health

    public Execute() {
        super("execute", "Execute", 6, 20, PlayerClass.WARRIOR);
    }

    @Override
    public boolean canUse(Player player) {
        LivingEntity target = getTargetInRange(player);
        if (target == null) return false;

        // Can only use on targets below 20% health
        float healthPercent = target.getHealth() / target.getMaxHealth();
        return healthPercent <= EXECUTE_THRESHOLD;
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
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.WARRIOR, level);

        float attackPower = stats.getAttackPower();
        float bonusDamage = stats.getBonusMeleeDamage();
        float baseDamage = BASE_DAMAGE + (attackPower * AP_SCALING) + bonusDamage;

        // Deal damage through pipeline (handles crits, events, FCT)
        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
        DamageResult result = DamagePipeline.deal(source, target, baseDamage);

        // Play dramatic sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ANVIL_LAND,
                SoundSource.PLAYERS, 0.5f, 1.5f);

        // Spawn explosion particles
        serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                result.isCritical() ? 8 : 5, 0.3, 0.3, 0.3, 0.0);

        // Additional crit particles
        if (result.isCritical()) {
            serverLevel.sendParticles(ParticleTypes.CRIT,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, 0.5, 0.5, 0.5, 0.1);
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
        return String.format("Devastating finisher usable on targets below 20%% health. Deals %.0f + 80%% AP damage. %d rage, %ds cooldown.",
                BASE_DAMAGE, resourceCost, getCooldownSeconds());
    }
}

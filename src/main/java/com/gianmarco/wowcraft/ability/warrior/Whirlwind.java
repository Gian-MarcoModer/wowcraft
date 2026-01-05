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
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Whirlwind - A spinning AoE attack that hits all nearby enemies.
 * Deals damage in waves over a short duration.
 * Formula: BASE_DAMAGE + (AttackPower * SCALING_COEFFICIENT) per hit
 */
public class Whirlwind extends Ability {

    private static final float BASE_DAMAGE = 3.0f;
    private static final float AP_SCALING = 0.2f; // 20% of Attack Power per hit
    private static final float RADIUS = 4.0f;
    private static final int NUM_HITS = 3;
    private static final int TICKS_BETWEEN_HITS = 10; // 0.5 seconds between each hit

    public Whirlwind() {
        super("whirlwind", "Whirlwind", 10, 25, PlayerClass.WARRIOR);
    }

    @Override
    public boolean canUse(Player player) {
        // Always usable as long as cooldown and rage are available
        return true;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        // Schedule the whirlwind hits
        scheduleWhirlwindHits(player, serverLevel, 0);
    }

    private void scheduleWhirlwindHits(Player player, ServerLevel serverLevel, int hitNumber) {
        if (hitNumber >= NUM_HITS) {
            return;
        }

        // Perform hit immediately if first, otherwise schedule
        if (hitNumber == 0) {
            performWhirlwindHit(player, serverLevel, hitNumber);
        } else {
            // Schedule next hit
            serverLevel.getServer().execute(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + TICKS_BETWEEN_HITS,
                    () -> {
                        if (player.isAlive() && player.level() == serverLevel) {
                            performWhirlwindHit(player, serverLevel, hitNumber);
                        }
                    }
            ));
        }
    }

    private void performWhirlwindHit(Player player, ServerLevel serverLevel, int hitNumber) {
        // Calculate damage
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.WARRIOR, level);

        float attackPower = stats.getAttackPower();
        float bonusDamage = stats.getBonusMeleeDamage();
        float damage = BASE_DAMAGE + (attackPower * AP_SCALING) + bonusDamage;

        // Find all enemies in range
        AABB searchBox = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            // Deal damage through pipeline (handles crits, events, FCT)
            WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
            DamageResult result = DamagePipeline.deal(source, target, damage);
            hitCount++;

            // Spawn hit particles on each target
            serverLevel.sendParticles(result.isCritical() ? ParticleTypes.CRIT : ParticleTypes.SWEEP_ATTACK,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    result.isCritical() ? 15 : 8, 0.3, 0.3, 0.3, 0.1);
        }

        // Play sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS, 1.0f, 1.0f + (hitNumber * 0.1f)); // Slightly higher pitch each hit

        // Spawn spiral particles around player
        spawnSpiralParticles(serverLevel, player, hitNumber);

        // Show message on first hit
        if (hitNumber == 0 && player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(String.format("§eWhirlwind! Hit %d enemies", hitCount)));
        }

        // Schedule next hit
        scheduleWhirlwindHits(player, serverLevel, hitNumber + 1);
    }

    private void spawnSpiralParticles(ServerLevel serverLevel, Player player, int hitNumber) {
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Create a spinning particle effect
        int particleCount = 12;
        double radius = RADIUS * 0.8;
        double angleOffset = (hitNumber * Math.PI / 3); // Rotate pattern each hit

        for (int i = 0; i < particleCount; i++) {
            double angle = (2 * Math.PI * i / particleCount) + angleOffset;
            double offsetX = Math.cos(angle) * radius;
            double offsetZ = Math.sin(angle) * radius;

            serverLevel.sendParticles(ParticleTypes.SWEEP_ATTACK,
                    x + offsetX, y, z + offsetZ,
                    1, 0, 0, 0, 0);
        }
    }

    @Override
    public String getDescription() {
        return String.format("Spin in a whirlwind, hitting all nearby enemies %d times. Deals %.0f + 20%% AP per hit. %d rage, %ds cooldown.",
                NUM_HITS, BASE_DAMAGE, resourceCost, getCooldownSeconds());
    }
}

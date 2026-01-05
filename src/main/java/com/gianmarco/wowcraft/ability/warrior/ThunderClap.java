package com.gianmarco.wowcraft.ability.warrior;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.combat.DamagePipeline;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Thunder Clap - Slams the ground, dealing AoE damage and slowing all nearby enemies.
 * Applies Slowness II for crowd control.
 * Formula: BASE_DAMAGE + (AttackPower * SCALING_COEFFICIENT)
 */
public class ThunderClap extends Ability {

    private static final float BASE_DAMAGE = 4.0f;
    private static final float AP_SCALING = 0.3f; // 30% of Attack Power
    private static final float RADIUS = 5.0f;
    private static final int SLOW_DURATION_TICKS = 120; // 6 seconds
    private static final int SLOW_AMPLIFIER = 1; // Slowness II

    public ThunderClap() {
        super("thunder_clap", "Thunder Clap", 8, 20, PlayerClass.WARRIOR);
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
            DamagePipeline.deal(source, target, damage);

            // Apply Slowness II
            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    SLOW_DURATION_TICKS,
                    SLOW_AMPLIFIER,
                    false,
                    true));

            hitCount++;

            // Spawn particles on each target
            serverLevel.sendParticles(ParticleTypes.CLOUD,
                    target.getX(), target.getY() + 0.1, target.getZ(),
                    10, 0.3, 0.1, 0.3, 0.05);
        }

        // Play ground slam sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.0f, 0.8f);

        // Spawn ground impact particles in a circle
        spawnGroundImpactParticles(serverLevel, player);

        // Show message
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(
                    String.format("§6Thunder Clap! Hit and slowed %d enemies", hitCount)));
        }
    }

    private void spawnGroundImpactParticles(ServerLevel serverLevel, Player player) {
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();

        // Create expanding ring of particles at ground level
        int particleCount = 24;
        for (int ring = 0; ring < 3; ring++) {
            double radius = RADIUS * (ring + 1) / 3.0;

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;

                // Explosion particles for impact
                serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                        x + offsetX, y + 0.1, z + offsetZ,
                        1, 0, 0, 0, 0);

                // Poof particles for dust cloud
                if (ring == 2) {
                    serverLevel.sendParticles(ParticleTypes.POOF,
                            x + offsetX, y + 0.2, z + offsetZ,
                            2, 0.1, 0.1, 0.1, 0.02);
                }
            }
        }

        // Central explosion effect
        serverLevel.sendParticles(ParticleTypes.EXPLOSION_EMITTER,
                x, y + 0.1, z,
                1, 0, 0, 0, 0);
    }

    @Override
    public String getDescription() {
        return String.format("Slam the ground, dealing %.0f + 30%% AP damage to all enemies within %.0f blocks and slowing them for 6 seconds. %d rage, %ds cooldown.",
                BASE_DAMAGE, RADIUS, resourceCost, getCooldownSeconds());
    }
}

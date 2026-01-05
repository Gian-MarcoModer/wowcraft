package com.gianmarco.wowcraft.ability.mage;

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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arcane Explosion - Blasts all nearby enemies with arcane energy.
 * Close-range AoE defense ability with knockback.
 * Formula: BASE_DAMAGE + (SpellPower * SCALING_COEFFICIENT)
 */
public class ArcaneExplosion extends Ability {

    private static final float BASE_DAMAGE = 8.0f;
    private static final float SP_SCALING = 0.5f; // 50% of Spell Power
    private static final float RADIUS = 5.0f;
    private static final float KNOCKBACK_STRENGTH = 0.3f;

    public ArcaneExplosion() {
        super("arcane_explosion", "Arcane Explosion", 6, 25, PlayerClass.MAGE);
    }

    @Override
    public boolean canUse(Player player) {
        return true;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        // Calculate damage with Spell Power scaling
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.MAGE, level);

        float spellPower = stats.getSpellPower();
        float totalDamage = BASE_DAMAGE + (spellPower * SP_SCALING);

        // Find all enemies in range
        AABB searchBox = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        int hitCount = 0;
        for (LivingEntity target : targets) {
            // Deal arcane damage through pipeline (handles crits, events, FCT)
            WowDamageSource source = WowDamageSource.spellAoe(player, abilityId, WowDamageSource.DamageSchool.ARCANE);
            DamagePipeline.deal(source, target, totalDamage);

            // Apply knockback away from player
            Vec3 knockbackDirection = target.position().subtract(player.position()).normalize();
            target.setDeltaMovement(target.getDeltaMovement().add(
                    knockbackDirection.x * KNOCKBACK_STRENGTH,
                    KNOCKBACK_STRENGTH * 0.5, // Slight upward knock
                    knockbackDirection.z * KNOCKBACK_STRENGTH
            ));

            hitCount++;

            // Spawn particles on each target
            serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    15, 0.3, 0.3, 0.3, 0.05);
        }

        // Play explosion sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.PLAYERS, 1.0f, 1.2f);

        // Spawn explosion particle effect radiating from player
        spawnExplosionParticles(serverLevel, player);

        // Show message
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal(
                    String.format("§dArcane Explosion! Hit %d enemies", hitCount)));
        }
    }

    private void spawnExplosionParticles(ServerLevel serverLevel, Player player) {
        double x = player.getX();
        double y = player.getY() + 1.0;
        double z = player.getZ();

        // Central explosion burst
        serverLevel.sendParticles(ParticleTypes.DRAGON_BREATH,
                x, y, z,
                50, 0.1, 0.1, 0.1, 0.15);

        // Expanding ring effect with multiple layers
        for (int ring = 0; ring < 3; ring++) {
            double radius = RADIUS * (ring + 1) / 3.0;
            int particleCount = 20 + (ring * 10);

            for (int i = 0; i < particleCount; i++) {
                double angle = 2 * Math.PI * i / particleCount;
                double offsetX = Math.cos(angle) * radius;
                double offsetZ = Math.sin(angle) * radius;

                // Enchant particles for arcane energy
                serverLevel.sendParticles(ParticleTypes.ENCHANT,
                        x + offsetX, y, z + offsetZ,
                        2, 0.1, 0.2, 0.1, 0.05);

                // End rod particles for arcane beams
                if (ring == 2) {
                    serverLevel.sendParticles(ParticleTypes.END_ROD,
                            x + offsetX, y, z + offsetZ,
                            1, 0, 0, 0, 0);
                }
            }
        }

        // Upward burst of portal particles
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                x, y, z,
                30, 0.3, 0.5, 0.3, 0.2);
    }

    @Override
    public String getDescription() {
        return String.format("Unleash arcane energy, dealing %.0f + 50%% SP damage to all enemies within %.0f blocks and knocking them back. %d mana, %ds cooldown.",
                BASE_DAMAGE, RADIUS, resourceCost, getCooldownSeconds());
    }
}

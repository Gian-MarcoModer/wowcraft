package com.gianmarco.wowcraft.ability.mage;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.combat.DamagePipeline;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import com.gianmarco.wowcraft.entity.SpellEffectEntity;

import java.util.List;

/**
 * Frost Nova - Freezes all enemies in range, rooting them in place.
 * Damage scales with Spell Power (50% coefficient).
 */
public class FrostNova extends Ability {

    private static final float RADIUS = 8.0f;
    private static final int FREEZE_DURATION_SECONDS = 4;
    private static final float BASE_DAMAGE = 5.0f;
    private static final float SP_SCALING = 0.5f; // 50% of Spell Power

    public FrostNova() {
        super("frost_nova", "Frost Nova", 20, 30, PlayerClass.MAGE);
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

        AABB effectArea = player.getBoundingBox().inflate(RADIUS);
        List<LivingEntity> targets = serverLevel.getEntitiesOfClass(LivingEntity.class, effectArea,
                e -> e != player && e.isAlive());

        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:" + getId());
        int freezeTicks = FREEZE_DURATION_SECONDS * 20;

        for (LivingEntity target : targets) {
            // Deal frost damage through pipeline (handles crits, events, FCT)
            WowDamageSource source = WowDamageSource.spellAoe(player, abilityId, WowDamageSource.DamageSchool.FROST);
            DamagePipeline.deal(source, target, totalDamage);

            // Apply slowness (simulates root/freeze)
            target.addEffect(new MobEffectInstance(
                    MobEffects.SLOWNESS,
                    freezeTicks,
                    100, // Extreme slowness = essentially rooted
                    false,
                    true,
                    true));

            // Freeze particles on each target
            serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                    target.getX(), target.getY() + target.getBbHeight() / 2, target.getZ(),
                    30, 0.5, 0.8, 0.5, 0.1);
        }

        // Spawn WoW-style ground decal effect FIRST (so texture appears before particles)
        SpellEffectEntity effect = new SpellEffectEntity(
                serverLevel,
                new Vec3(player.getX(), player.getY(), player.getZ()),
                SpellEffectEntity.EffectType.FROST_NOVA);
        effect.setMaxRadius(RADIUS);
        effect.setLifetime(25); // 1.25 seconds - quick expand and fade
        serverLevel.addFreshEntity(effect);

        // Nova explosion particles in a ring
        for (int i = 0; i < 360; i += 10) {
            double angle = Math.toRadians(i);
            for (float r = 1; r <= RADIUS; r += 2) {
                double x = player.getX() + Math.cos(angle) * r;
                double z = player.getZ() + Math.sin(angle) * r;
                serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                        x, player.getY() + 0.5, z,
                        2, 0.1, 0.1, 0.1, 0);
            }
        }

        // Ice crack particles at player feet
        serverLevel.sendParticles(ParticleTypes.ITEM_SNOWBALL,
                player.getX(), player.getY(), player.getZ(),
                50, 0.5, 0.1, 0.5, 0.3);

        // Play frost sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.5f);

        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 2.0f, 0.5f);
    }

    @Override
    public String getDescription() {
        return String.format(
                "Blast enemies within %.0f blocks with cold, freezing them for %d seconds. %d mana, %ds cooldown.",
                RADIUS, FREEZE_DURATION_SECONDS, resourceCost, getCooldownSeconds());
    }
}

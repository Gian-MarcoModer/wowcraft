package com.gianmarco.wowcraft.ability.rogue;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

public class Sprint extends Ability {
    private static final int SPEED_AMPLIFIER = 2; // Speed III (200% speed boost)
    private static final int DURATION_TICKS = 240; // 12 seconds
    private static final int COOLDOWN_SECONDS = 120; // 2 minute cooldown
    private static final int ENERGY_COST = 20; // 20 energy cost

    public Sprint() {
        super("sprint", "Sprint", COOLDOWN_SECONDS, ENERGY_COST, PlayerClass.ROGUE);
    }

    @Override
    public boolean canUse(Player player) {
        // Don't allow if already sprinting to prevent wasting energy
        if (player.hasEffect(MobEffects.SPEED)) {
            MobEffectInstance effect = player.getEffect(MobEffects.SPEED);
            if (effect != null && effect.getAmplifier() >= SPEED_AMPLIFIER) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void execute(Player player) {
        // Apply speed buff
        player.addEffect(new MobEffectInstance(
            MobEffects.SPEED,
            DURATION_TICKS,
            SPEED_AMPLIFIER,
            false, // not ambient
            true   // show particles
        ));

        // Visual and audio effects
        if (player.level() instanceof ServerLevel serverLevel) {
            // Spawn speed particles around player
            serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                player.getX(),
                player.getY() + 0.5,
                player.getZ(),
                15, // particle count
                0.5, // x spread
                0.3, // y spread
                0.5, // z spread
                0.05 // speed
            );

            // Play whoosh sound
            serverLevel.playSound(
                null,
                player.getX(),
                player.getY(),
                player.getZ(),
                SoundEvents.ENDER_DRAGON_FLAP,
                SoundSource.PLAYERS,
                0.5f, // volume
                1.5f  // pitch
            );
        }
    }

    @Override
    public String getDescription() {
        return "Increases movement speed by 200% for 12 seconds.";
    }
}

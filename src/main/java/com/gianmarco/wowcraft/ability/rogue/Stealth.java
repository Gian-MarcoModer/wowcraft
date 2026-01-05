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

/**
 * Stealth - Enter stealth mode, becoming invisible for 20 seconds.
 * Breaks on taking damage or using offensive abilities.
 * Classic WoW stealth mechanic using Minecraft's invisibility effect.
 */
public class Stealth extends Ability {

    private static final int STEALTH_DURATION_TICKS = 400; // 20 seconds
    private static final int INVISIBILITY_AMPLIFIER = 0;

    public Stealth() {
        super("stealth", "Stealth", 30, 0, PlayerClass.ROGUE);
    }

    @Override
    public boolean canUse(Player player) {
        // Can't use if already in stealth (has invisibility effect)
        return !player.hasEffect(MobEffects.INVISIBILITY);
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        // Apply invisibility effect (stealth)
        MobEffectInstance invisibility = new MobEffectInstance(
                MobEffects.INVISIBILITY,
                STEALTH_DURATION_TICKS,
                INVISIBILITY_AMPLIFIER,
                false, // Not ambient
                false  // Don't show particles (would reveal position)
        );
        player.addEffect(invisibility);

        // Also add slight slowdown while stealthed (WoW Classic stealth movement)
        MobEffectInstance slowdown = new MobEffectInstance(
                MobEffects.SLOWNESS,
                STEALTH_DURATION_TICKS,
                0, // Slowness I (stealth walk speed)
                false,
                false
        );
        player.addEffect(slowdown);

        // Play stealth sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.POWDER_SNOW_STEP, SoundSource.PLAYERS, 0.5f, 0.6f);

        // Spawn subtle smoke particles when entering stealth
        serverLevel.sendParticles(ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1.0, player.getZ(),
                15, 0.3, 0.5, 0.3, 0.02);
    }

    @Override
    public String getDescription() {
        return String.format("Enter stealth, becoming invisible for %d seconds. Breaks on damage or attacks. %ds cooldown.",
                STEALTH_DURATION_TICKS / 20, getCooldownSeconds());
    }
}

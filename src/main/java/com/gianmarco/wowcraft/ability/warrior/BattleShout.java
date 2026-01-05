package com.gianmarco.wowcraft.ability.warrior;

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
 * Battle Shout - Buff that increases the warrior's attack power and speed.
 * Classic WoW: Increases attack power of nearby party members.
 */
public class BattleShout extends Ability {

    private static final int BUFF_DURATION_SECONDS = 30;
    private static final int STRENGTH_LEVEL = 1; // Strength II
    private static final int SPEED_LEVEL = 0; // Speed I

    public BattleShout() {
        super("battle_shout", "Battle Shout", 60, 10, PlayerClass.WARRIOR);
    }

    @Override
    public boolean canUse(Player player) {
        // Can always use if not on cooldown and have rage
        return true;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        int durationTicks = BUFF_DURATION_SECONDS * 20;

        // Apply Strength effect (increases attack damage)
        player.addEffect(new MobEffectInstance(
                MobEffects.STRENGTH,
                durationTicks,
                STRENGTH_LEVEL,
                false, // ambient
                true, // visible
                true // show icon
        ));

        // Apply Speed effect (increases movement speed)
        player.addEffect(new MobEffectInstance(
                MobEffects.SPEED,
                durationTicks,
                SPEED_LEVEL,
                false,
                true,
                true));

        // Play roar sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.ENDER_DRAGON_GROWL, SoundSource.PLAYERS, 0.7f, 1.5f);

        // Shout particles
        for (int i = 0; i < 360; i += 30) {
            double angle = Math.toRadians(i);
            double x = player.getX() + Math.cos(angle) * 2;
            double z = player.getZ() + Math.sin(angle) * 2;
            serverLevel.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                    x, player.getY() + 1, z,
                    3, 0.2, 0.3, 0.2, 0);
        }
    }

    @Override
    public String getDescription() {
        return String.format(
                "Let out a battle cry, gaining Strength II and Speed for %d seconds. %d rage, %ds cooldown.",
                BUFF_DURATION_SECONDS, resourceCost, getCooldownSeconds());
    }
}

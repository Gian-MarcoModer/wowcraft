package com.gianmarco.wowcraft.ability.mage;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.entity.IceLanceProjectile;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsCalculator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Ice Lance - A fast frost projectile that deals bonus damage to slowed targets.
 * Synergizes with Frost Nova and other slowing effects.
 * Formula: BASE_DAMAGE + (SpellPower * SCALING_COEFFICIENT), triple damage if target is slowed/frozen
 */
public class IceLance extends Ability {

    private static final float BASE_DAMAGE = 6.0f;
    private static final float SP_SCALING = 0.6f; // 60% of Spell Power
    private static final float PROJECTILE_SPEED = 1.5f;
    private static final float FROZEN_DAMAGE_MULTIPLIER = 3.0f;

    public IceLance() {
        super("ice_lance", "Ice Lance", 3, 15, PlayerClass.MAGE);
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

        // Check for critical hit
        boolean isCrit = stats.rollCrit();

        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();

        // Create ice lance projectile
        IceLanceProjectile iceLance = new IceLanceProjectile(player, serverLevel);
        iceLance.setPos(start.x + look.x * 0.5, start.y, start.z + look.z * 0.5);
        iceLance.setDamage(totalDamage);
        iceLance.setCanCrit(true);
        iceLance.setIsCrit(isCrit);

        // Set velocity - faster than fireball
        iceLance.shoot(look.x, look.y, look.z, PROJECTILE_SPEED, 0.0f);

        serverLevel.addFreshEntity(iceLance);

        // Play cast sound - ice cracking sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 0.5f, 1.5f);

        // Cast particles - frost effect from hands
        double offsetX = look.z * 0.5;
        double offsetZ = -look.x * 0.5;
        serverLevel.sendParticles(ParticleTypes.SNOWFLAKE,
                player.getX() + offsetX, player.getY() + 1.2, player.getZ() + offsetZ,
                12, 0.1, 0.1, 0.1, 0.02);
    }

    @Override
    public String getDescription() {
        return String.format(
                "Launch a shard of ice dealing %.0f + 60%% SP frost damage. Deals 3x damage to slowed targets. %d mana, %ds cooldown.",
                BASE_DAMAGE, resourceCost, getCooldownSeconds());
    }

    /**
     * Check if a target is frozen/slowed
     */
    public static boolean isTargetSlowed(LivingEntity target) {
        return target.hasEffect(MobEffects.SLOWNESS);
    }
}

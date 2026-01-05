package com.gianmarco.wowcraft.ability.mage;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.entity.FireballProjectile;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsCalculator;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Fireball - Launches a fiery projectile that deals fire damage ON HIT.
 * Damage scales with Spell Power.
 * Formula: BASE_DAMAGE + (SpellPower * SCALING_COEFFICIENT)
 */
public class Fireball extends Ability {

    private static final float BASE_DAMAGE = 25.0f; // Increased from 15 for better early game balance
    private static final float SP_SCALING = 1.0f; // 100% of Spell Power added as damage
    private static final float PROJECTILE_SPEED = 1.2f;

    public Fireball() {
        super("fireball", "Fireball", 2, 20, PlayerClass.MAGE);
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

        // Create our custom fireball projectile
        FireballProjectile fireball = new FireballProjectile(player, serverLevel);
        fireball.setPos(start.x + look.x * 0.5, start.y, start.z + look.z * 0.5);
        fireball.setDamage(totalDamage);
        fireball.setCanCrit(true);
        fireball.setIsCrit(isCrit);

        // Set velocity using shoot method
        fireball.shoot(look.x, look.y, look.z, PROJECTILE_SPEED, 0.0f);

        serverLevel.addFreshEntity(fireball);

        // Play cast sound
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Cast particles from hands
        double offsetX = look.z * 0.5;
        double offsetZ = -look.x * 0.5;
        serverLevel.sendParticles(ParticleTypes.FLAME,
                player.getX() + offsetX, player.getY() + 1.2, player.getZ() + offsetZ,
                8, 0.1, 0.1, 0.1, 0.02);
    }

    @Override
    public String getDescription() {
        return String.format(
                "Hurl a ball of fire dealing %.0f + 100%% SP fire damage on impact. %d mana, %ds cooldown.",
                BASE_DAMAGE, resourceCost, getCooldownSeconds());
    }
}

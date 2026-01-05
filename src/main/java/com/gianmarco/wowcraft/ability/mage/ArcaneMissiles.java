package com.gianmarco.wowcraft.ability.mage;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.entity.ArcaneMissileProjectile;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import com.gianmarco.wowcraft.stats.CharacterStats;
import com.gianmarco.wowcraft.stats.StatsCalculator;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Arcane Missiles - Channels a barrage of 5 magic missiles at a target.
 * Each missile can crit independently.
 * Formula: BASE_DAMAGE + (SpellPower * SCALING_COEFFICIENT) per missile
 */
public class ArcaneMissiles extends Ability {

    private static final float BASE_DAMAGE = 4.0f;
    private static final float SP_SCALING = 0.4f; // 40% of Spell Power per missile
    private static final float PROJECTILE_SPEED = 1.3f;
    private static final float RANGE = 30.0f;
    private static final int NUM_MISSILES = 5;
    private static final int TICKS_BETWEEN_MISSILES = 10; // 0.5 seconds between each missile

    public ArcaneMissiles() {
        super("arcane_missiles", "Arcane Missiles", 8, 30, PlayerClass.MAGE);
    }

    @Override
    public boolean canUse(Player player) {
        return getTargetInRange(player) != null;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        LivingEntity target = getTargetInRange(player);
        if (target == null)
            return;

        // Notify player
        if (player instanceof net.minecraft.server.level.ServerPlayer sp) {
            sp.sendSystemMessage(Component.literal("§dChanneling Arcane Missiles..."));
        }

        // Schedule the missiles
        scheduleMissile(player, serverLevel, target, 0);
    }

    private void scheduleMissile(Player player, ServerLevel serverLevel, LivingEntity initialTarget, int missileNumber) {
        if (missileNumber >= NUM_MISSILES) {
            return;
        }

        // Fire missile immediately if first, otherwise schedule
        if (missileNumber == 0) {
            fireMissile(player, serverLevel, initialTarget, missileNumber);
        } else {
            // Schedule next missile
            serverLevel.getServer().execute(new net.minecraft.server.TickTask(
                    serverLevel.getServer().getTickCount() + TICKS_BETWEEN_MISSILES,
                    () -> {
                        if (player.isAlive() && player.level() == serverLevel) {
                            // Try to re-acquire target (might have died or moved out of range)
                            LivingEntity target = getTargetInRange(player);
                            if (target != null) {
                                fireMissile(player, serverLevel, target, missileNumber);
                            }
                        }
                    }
            ));
        }
    }

    private void fireMissile(Player player, ServerLevel serverLevel, LivingEntity target, int missileNumber) {
        // Calculate damage with Spell Power scaling
        int level = PlayerDataManager.getLevel(player);
        CharacterStats stats = StatsCalculator.getBaseStats(PlayerClass.MAGE, level);

        float spellPower = stats.getSpellPower();
        float totalDamage = BASE_DAMAGE + (spellPower * SP_SCALING);

        // Check for critical hit (per missile)
        boolean isCrit = stats.rollCrit();

        // Calculate direction to target with slight randomness for visual variety
        Vec3 targetPos = target.position().add(0, target.getBbHeight() / 2, 0);
        Vec3 start = player.getEyePosition();
        Vec3 direction = targetPos.subtract(start).normalize();

        // Add slight random offset for visual variety
        double spread = 0.05;
        direction = direction.add(
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * spread,
                (Math.random() - 0.5) * spread
        ).normalize();

        // Create arcane missile projectile
        ArcaneMissileProjectile missile = new ArcaneMissileProjectile(player, serverLevel);
        missile.setPos(start.x + direction.x * 0.5, start.y, start.z + direction.z * 0.5);
        missile.setDamage(totalDamage);
        missile.setCanCrit(true);
        missile.setIsCrit(isCrit);

        // Set velocity
        missile.shoot(direction.x, direction.y, direction.z, PROJECTILE_SPEED, 0.0f);

        serverLevel.addFreshEntity(missile);

        // Play sound (higher pitch for each successive missile)
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.NOTE_BLOCK_PLING.value(), SoundSource.PLAYERS,
                0.5f, 1.0f + (missileNumber * 0.1f));

        // Schedule next missile
        scheduleMissile(player, serverLevel, target, missileNumber + 1);
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
        return String.format("Channel a barrage of %d arcane missiles, each dealing %.0f + 40%% SP damage. %d mana, %ds cooldown.",
                NUM_MISSILES, BASE_DAMAGE, resourceCost, getCooldownSeconds());
    }
}

package com.gianmarco.wowcraft.ability.rogue;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Shadowstep - Teleport behind target enemy and gain a speed boost.
 * Classic Rogue mobility ability.
 * Range: 5-20 blocks
 */
public class Shadowstep extends Ability {

    private static final float MIN_RANGE = 5.0f;
    private static final float MAX_RANGE = 20.0f;
    private static final int SPEED_DURATION_TICKS = 60; // 3 seconds
    private static final int SPEED_AMPLIFIER = 1; // Speed II

    public Shadowstep() {
        super("shadowstep", "Shadowstep", 12, 25, PlayerClass.ROGUE);
    }

    @Override
    public boolean canUse(Player player) {
        LivingEntity target = getShadowstepTarget(player);
        if (target == null)
            return false;

        double distance = player.distanceTo(target);
        return distance >= MIN_RANGE && distance <= MAX_RANGE;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        LivingEntity target = getShadowstepTarget(player);
        if (target == null)
            return;

        // Store starting position for particles
        Vec3 startPos = player.position();

        // Calculate position behind target using body rotation (2 blocks behind)
        // Use target's yaw (body rotation) for consistent positioning
        float targetYawDegrees = target.getYRot();
        double targetYawRadians = Math.toRadians(targetYawDegrees);

        // Calculate offset: 2 blocks in the direction the target is facing
        // Minecraft yaw: 0 = south (+Z), 90 = west (-X), -90 = east (+X), 180 = north (-Z)
        double offsetX = -Math.sin(targetYawRadians) * 2.0;
        double offsetZ = Math.cos(targetYawRadians) * 2.0;

        Vec3 targetPos = target.position();
        Vec3 destination = new Vec3(
                targetPos.x - offsetX, // Behind means opposite direction
                targetPos.y,
                targetPos.z - offsetZ
        );

        // Spawn particles at starting position (dark smoke)
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                startPos.x, startPos.y + 1.0, startPos.z,
                20, 0.3, 0.5, 0.3, 0.05);

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                startPos.x, startPos.y + 1.0, startPos.z,
                30, 0.5, 0.5, 0.5, 0.5);

        // Play teleport sound at start
        serverLevel.playSound(null, startPos.x, startPos.y, startPos.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Teleport player behind target
        player.teleportTo(destination.x, destination.y, destination.z);

        // Make player face the target's back (same direction as target is facing)
        // This ensures perfect Backstab positioning
        player.setYRot(targetYawDegrees);
        player.setXRot(0); // Keep pitch level for easier aiming

        // Apply speed boost
        MobEffectInstance speed = new MobEffectInstance(
                MobEffects.SPEED,
                SPEED_DURATION_TICKS,
                SPEED_AMPLIFIER,
                false,
                true // Show particles for speed buff
        );
        player.addEffect(speed);

        // Spawn particles at destination (dark smoke)
        serverLevel.sendParticles(ParticleTypes.LARGE_SMOKE,
                destination.x, destination.y + 1.0, destination.z,
                20, 0.3, 0.5, 0.3, 0.05);

        serverLevel.sendParticles(ParticleTypes.PORTAL,
                destination.x, destination.y + 1.0, destination.z,
                30, 0.5, 0.5, 0.5, 0.5);

        // Play teleport sound at destination
        serverLevel.playSound(null, destination.x, destination.y, destination.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.8f);
    }

    private LivingEntity getShadowstepTarget(Player player) {
        Vec3 look = player.getLookAngle();

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(MAX_RANGE)).inflate(5);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        // Find entity closest to where player is looking within range
        return entities.stream()
                .filter(e -> {
                    double distance = player.distanceTo(e);
                    if (distance < MIN_RANGE || distance > MAX_RANGE)
                        return false;

                    Vec3 toEntity = e.position().subtract(player.position()).normalize();
                    return toEntity.dot(look) > 0.7; // Must be looking at target
                })
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(player),
                        b.distanceToSqr(player)))
                .orElse(null);
    }

    @Override
    public String getDescription() {
        return String.format("Teleport behind target (%.0f-%.0f blocks) and gain Speed II for %d seconds. %d energy, %ds cooldown.",
                MIN_RANGE, MAX_RANGE, SPEED_DURATION_TICKS / 20, resourceCost, getCooldownSeconds());
    }
}

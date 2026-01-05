package com.gianmarco.wowcraft.ability.warrior;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.combat.DamagePipeline;
import com.gianmarco.wowcraft.combat.WowDamageSource;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import com.gianmarco.wowcraft.playerclass.PlayerDataManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Charge - Rush toward an enemy, generating rage and stunning briefly.
 * Classic WoW: Charge to enemy, gain rage, stun for 1 second.
 */
public class Charge extends Ability {

    private static final float CHARGE_RANGE = 25.0f;
    private static final float MIN_RANGE = 8.0f; // Must be at least this far
    private static final int RAGE_GENERATED = 15;
    private static final int CHARGE_DURATION_TICKS = 16; // 0.8 second animation duration

    // Track active charges
    private static final Map<UUID, ActiveCharge> activeCharges = new HashMap<>();

    private static class ActiveCharge {
        final Vec3 startPos;
        final Vec3 destination;
        final LivingEntity target;
        final int startTick;

        ActiveCharge(Vec3 startPos, Vec3 destination, LivingEntity target, int startTick) {
            this.startPos = startPos;
            this.destination = destination;
            this.target = target;
            this.startTick = startTick;
        }

        float getProgress(int currentTick) {
            int elapsed = currentTick - startTick;
            return Math.min(1.0f, (float) elapsed / CHARGE_DURATION_TICKS);
        }

        boolean isComplete(int currentTick) {
            return currentTick - startTick >= CHARGE_DURATION_TICKS;
        }
    }

    public Charge() {
        super("charge", "Charge", 15, 0, PlayerClass.WARRIOR); // No rage cost, generates rage
    }

    /**
     * Register the charge tick handler - should be called once during mod initialization
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            int currentTick = server.getTickCount();

            Iterator<Map.Entry<UUID, ActiveCharge>> iter = activeCharges.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry<UUID, ActiveCharge> entry = iter.next();
                UUID playerId = entry.getKey();
                ActiveCharge charge = entry.getValue();

                ServerPlayer player = server.getPlayerList().getPlayer(playerId);
                if (player == null || !player.isAlive()) {
                    iter.remove();
                    continue;
                }

                ServerLevel serverLevel = (ServerLevel) player.level();

                // Calculate direction to target (horizontal only for ground movement)
                Vec3 currentPos = player.position();
                Vec3 toTarget = new Vec3(
                        charge.destination.x - currentPos.x,
                        0, // No vertical velocity - follow ground
                        charge.destination.z - currentPos.z
                );

                double distanceToTarget = Math.sqrt(toTarget.x * toTarget.x + toTarget.z * toTarget.z);

                // If very close to target, finish charge
                if (distanceToTarget < 1.5 || charge.isComplete(currentTick)) {
                    iter.remove();
                    finishCharge(player, charge.target, serverLevel);
                } else {
                    // Calculate next step position (smaller increments for smoother movement)
                    float progress = (float) (currentTick - charge.startTick) / CHARGE_DURATION_TICKS;
                    Vec3 nextPos = charge.startPos.lerp(charge.destination, Math.min(1.0f, progress + 0.08f));

                    // Teleport toward target progressively (small steps for smooth movement)
                    player.teleportTo(nextPos.x, nextPos.y, nextPos.z);

                    // Apply forward velocity for visual smoothness
                    Vec3 direction = toTarget.normalize();
                    player.setDeltaMovement(direction.x * 2.0, player.getDeltaMovement().y, direction.z * 2.0);

                    // Make player invulnerable during charge
                    player.setInvulnerable(true);

                    // Spawn particle trail
                    serverLevel.sendParticles(ParticleTypes.CLOUD,
                            currentPos.x, currentPos.y + 0.5, currentPos.z,
                            5, 0.3, 0.3, 0.3, 0.05);

                    serverLevel.sendParticles(ParticleTypes.FLAME,
                            currentPos.x, currentPos.y + 1.0, currentPos.z,
                            2, 0.2, 0.4, 0.2, 0.01);
                }
            }
        });

        WowCraft.LOGGER.info("Registered Charge ability tick handler");
    }

    @Override
    public boolean canUse(Player player) {
        // Need an enemy in charge range (but not too close)
        LivingEntity target = getChargeTarget(player);
        if (target == null)
            return false;

        double distance = player.distanceTo(target);
        return distance >= MIN_RANGE && distance <= CHARGE_RANGE;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        LivingEntity target = getChargeTarget(player);
        if (target == null)
            return;

        // Calculate charge destination (2 blocks in front of target)
        Vec3 direction = player.position().subtract(target.position()).normalize();
        Vec3 destination = target.position().add(direction.scale(2.0));
        Vec3 startPos = player.position();

        int currentTick = serverLevel.getServer().getTickCount();

        // Store active charge
        activeCharges.put(player.getUUID(), new ActiveCharge(startPos, destination, target, currentTick));

        // Play charge sound at start
        serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
                SoundEvents.RAVAGER_ATTACK, SoundSource.PLAYERS, 1.0f, 1.2f);

        // Apply speed effect during charge for visual feedback
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, CHARGE_DURATION_TICKS, 10, false, false));
    }

    /**
     * Complete the charge - deal damage, generate rage, apply stun
     */
    private static void finishCharge(Player player, LivingEntity target, ServerLevel serverLevel) {
        // Remove invulnerability
        player.setInvulnerable(false);

        // Stop player movement
        player.setDeltaMovement(0, 0, 0);

        // Slow the target briefly (simulates stun)
        target.setDeltaMovement(0, 0, 0);

        // Generate rage
        PlayerDataManager.modifyResource(player, RAGE_GENERATED);

        // Deal small damage through pipeline (handles crits, events, FCT)
        ResourceLocation abilityId = ResourceLocation.parse("wowcraft:charge");
        WowDamageSource source = WowDamageSource.meleeAbility(player, abilityId);
        DamagePipeline.deal(source, target, 2.0f);

        // Play impact sound
        serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
                SoundEvents.SHIELD_BLOCK, SoundSource.PLAYERS, 1.0f, 0.8f);

        // Impact particles
        for (int i = 0; i < 10; i++) {
            double angle = (i / 10.0) * Math.PI * 2;
            double offsetX = Math.cos(angle) * 0.5;
            double offsetZ = Math.sin(angle) * 0.5;
            serverLevel.sendParticles(ParticleTypes.EXPLOSION,
                    target.getX() + offsetX, target.getY() + 1, target.getZ() + offsetZ,
                    1, 0, 0, 0, 0);
        }
    }

    private LivingEntity getChargeTarget(Player player) {
        Vec3 look = player.getLookAngle();
        Vec3 start = player.getEyePosition();

        AABB searchBox = player.getBoundingBox().expandTowards(look.scale(CHARGE_RANGE)).inflate(2);
        List<LivingEntity> entities = player.level().getEntitiesOfClass(LivingEntity.class, searchBox,
                e -> e != player && e.isAlive());

        // Find entity closest to where player is looking
        return entities.stream()
                .filter(e -> {
                    Vec3 toEntity = e.position().subtract(player.position()).normalize();
                    return toEntity.dot(look) > 0.8; // Must be looking at target
                })
                .min((a, b) -> Double.compare(
                        a.distanceToSqr(player),
                        b.distanceToSqr(player)))
                .orElse(null);
    }

    @Override
    public String getDescription() {
        return String.format("Charge at an enemy (%.0f-%.0f range), generating %d rage. %ds cooldown.",
                MIN_RANGE, CHARGE_RANGE, RAGE_GENERATED, getCooldownSeconds());
    }
}

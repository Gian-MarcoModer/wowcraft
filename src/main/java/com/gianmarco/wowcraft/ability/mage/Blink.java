package com.gianmarco.wowcraft.ability.mage;

import com.gianmarco.wowcraft.ability.Ability;
import com.gianmarco.wowcraft.playerclass.PlayerClass;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

/**
 * Blink - Teleport forward a short distance.
 * Classic WoW: Instant teleport in the direction you're facing.
 */
public class Blink extends Ability {

    private static final float BLINK_DISTANCE = 15.0f;

    public Blink() {
        super("blink", "Blink", 15, 25, PlayerClass.MAGE);
    }

    @Override
    public boolean canUse(Player player) {
        // Can always blink (will find valid position)
        return true;
    }

    @Override
    public void execute(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel))
            return;

        Vec3 start = player.position();

        // Get horizontal direction only (ignore vertical pitch)
        float yaw = player.getYRot();
        double yawRad = Math.toRadians(yaw);
        Vec3 horizontalDirection = new Vec3(-Math.sin(yawRad), 0, Math.cos(yawRad)).normalize();

        // Calculate target position
        Vec3 targetPos = findBlinkPosition(player, horizontalDirection, BLINK_DISTANCE);

        // Particles at start position
        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL,
                start.x, start.y + 1, start.z,
                30, 0.3, 0.5, 0.3, 0.1);

        // Play vanish sound
        serverLevel.playSound(null, start.x, start.y, start.z,
                SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.5f);

        // Teleport the player
        player.teleportTo(targetPos.x, targetPos.y, targetPos.z);

        // Particles at destination
        serverLevel.sendParticles(ParticleTypes.PORTAL,
                targetPos.x, targetPos.y + 1, targetPos.z,
                30, 0.3, 0.5, 0.3, 0.1);

        // Arcane particles trail
        int steps = (int) (start.distanceTo(targetPos) * 2);
        for (int i = 0; i < steps; i++) {
            double progress = i / (double) steps;
            Vec3 trailPos = start.lerp(targetPos, progress);
            serverLevel.sendParticles(ParticleTypes.WITCH,
                    trailPos.x, trailPos.y + 1, trailPos.z,
                    1, 0, 0, 0, 0);
        }

        // Arrival sound
        serverLevel.playSound(null, targetPos.x, targetPos.y, targetPos.z,
                SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 0.8f, 1.2f);
    }

    private Vec3 findBlinkPosition(Player player, Vec3 direction, float maxDistance) {
        Vec3 start = player.position();
        ServerLevel level = (ServerLevel) player.level();

        // Calculate the target position in the direction you're looking
        Vec3 targetPos = start.add(direction.scale(maxDistance));

        // Check if there's a solid block at the target position (wall blocking)
        // Walk backwards from target toward player to find first valid air space
        Vec3 currentCheck = targetPos;
        for (float backtrack = 0; backtrack <= maxDistance; backtrack += 0.5f) {
            currentCheck = targetPos.subtract(direction.scale(backtrack));
            BlockPos checkBlock = BlockPos.containing(currentCheck.x, currentCheck.y, currentCheck.z);

            // Check if this position has enough space for the player (2 blocks tall)
            boolean feetClear = !level.getBlockState(checkBlock).isSolid();
            boolean headClear = !level.getBlockState(checkBlock.above()).isSolid();

            if (feetClear && headClear) {
                // Found valid air space - now find safe ground below or above
                Vec3 safePos = findSafeGround(level, currentCheck, checkBlock);
                if (safePos != null) {
                    return safePos;
                }
            }
        }

        // If no valid position found, stay at current position
        return start;
    }

    /**
     * Find safe ground near the target position.
     * Searches downward first (for landing), then upward (if in a block).
     */
    private Vec3 findSafeGround(ServerLevel level, Vec3 targetPos, BlockPos targetBlock) {
        // First, check if we're already on solid ground
        if (level.getBlockState(targetBlock.below()).isSolid()) {
            return new Vec3(targetPos.x, targetBlock.getY(), targetPos.z);
        }

        // Search downward for ground (up to 10 blocks)
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos checkGround = targetBlock.below(dy);
            if (level.getBlockState(checkGround).isSolid()) {
                // Found ground - make sure there's space above it
                if (!level.getBlockState(checkGround.above()).isSolid()
                        && !level.getBlockState(checkGround.above(2)).isSolid()) {
                    return new Vec3(targetPos.x, checkGround.getY() + 1, targetPos.z);
                }
            }
        }

        // If no ground found below, check upward (in case we're underground)
        for (int dy = 1; dy <= 10; dy++) {
            BlockPos checkUp = targetBlock.above(dy);
            if (level.getBlockState(checkUp.below()).isSolid()
                    && !level.getBlockState(checkUp).isSolid()
                    && !level.getBlockState(checkUp.above()).isSolid()) {
                return new Vec3(targetPos.x, checkUp.getY(), targetPos.z);
            }
        }

        // No safe position found
        return null;
    }

    @Override
    public String getDescription() {
        return String.format("Teleport forward up to %.0f blocks. %d mana, %ds cooldown.",
                BLINK_DISTANCE, resourceCost, getCooldownSeconds());
    }
}

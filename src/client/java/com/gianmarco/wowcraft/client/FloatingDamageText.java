package com.gianmarco.wowcraft.client;

/**
 * Represents a single floating damage text instance.
 * Tracks position, damage amount, lifetime, and visual properties.
 */
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Represents a single floating damage text instance.
 * Tracks position relative to an entity.
 */
public class FloatingDamageText {
    private final float damage;
    private final boolean isCritical;
    private final int entityId;
    private final long spawnTime;

    // Store initial position in case entity disappears
    private double lastX;
    private double lastY;
    private double lastZ;

    // Random offsets for visual stacking/noise
    private final float offsetX;
    private final float offsetZ;

    private static final long LIFETIME_MS = 2000; // 2 seconds
    private static final float RISE_SPEED = 1.2f; // Blocks to rise over lifetime

    public FloatingDamageText(int entityId, float damage, boolean isCritical, double x, double y, double z) {
        this.entityId = entityId;
        this.damage = damage;
        this.isCritical = isCritical;
        this.lastX = x;
        this.lastY = y;
        this.lastZ = z;
        this.spawnTime = System.currentTimeMillis();

        // Randomize horizontal offset slightly
        this.offsetX = (float) (Math.random() - 0.5) * 0.5f;
        this.offsetZ = (float) (Math.random() - 0.5) * 0.5f;
    }

    public float getDamage() {
        return damage;
    }

    public boolean isCritical() {
        return isCritical;
    }

    /**
     * Get current 3D position for rendering
     */
    public Vec3 getPosition(float partialTicks) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level != null) {
            Entity entity = mc.level.getEntity(entityId);
            if (entity != null && entity.isAlive()) {
                // Update last known position
                // Interpolate position for smooth rendering
                double x = net.minecraft.util.Mth.lerp(partialTicks, entity.xOld, entity.getX());
                double y = net.minecraft.util.Mth.lerp(partialTicks, entity.yOld, entity.getY()) + entity.getBbHeight();
                double z = net.minecraft.util.Mth.lerp(partialTicks, entity.zOld, entity.getZ());

                this.lastX = x;
                this.lastY = y;
                this.lastZ = z;
            }
        }

        return new Vec3(
                lastX + offsetX,
                lastY + 0.5 + getYOffset(partialTicks), // Start 0.5 blocks above head/last pos
                lastZ + offsetZ);
    }

    public double getYOffset(float partialTick) {
        long elapsed = System.currentTimeMillis() - spawnTime;
        float progress = Math.min(1.0f, elapsed / (float) LIFETIME_MS);
        return (double) (progress * RISE_SPEED); // Rises 1.2 blocks over 2 seconds
    }

    public int getEntityId() {
        return entityId;
    }

    public double getX() {
        return lastX + offsetX;
    }

    public double getY() {
        return lastY;
    }

    public double getZ() {
        return lastZ + offsetZ;
    }

    public float getAlpha() {
        long elapsed = System.currentTimeMillis() - spawnTime;
        float progress = elapsed / (float) LIFETIME_MS;

        if (progress >= 1.0f) {
            return 0.0f; // Fully faded
        }

        // Fade out in last 50% of lifetime
        if (progress > 0.5f) {
            return 1.0f - ((progress - 0.5f) * 2.0f);
        }

        return 1.0f; // Fully visible
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - spawnTime >= LIFETIME_MS;
    }

    public float getScale() {
        long elapsed = System.currentTimeMillis() - spawnTime;
        float progress = elapsed / (float) LIFETIME_MS;

        // Start slightly larger for crits, grow slightly in first 20% of lifetime
        float baseScale = isCritical ? 1.5f : 1.0f;

        // Add a "pop" effect at start
        if (progress < 0.2f) {
            return baseScale * (0.5f + (progress / 0.2f) * 0.5f);
        }

        return baseScale;
    }
}

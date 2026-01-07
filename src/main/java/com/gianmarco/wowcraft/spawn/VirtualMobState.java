package com.gianmarco.wowcraft.spawn;

import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.entity.ModEntities;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a "virtual" mob that exists in theory but not as an entity.
 * Used when players are far away to save memory.
 */
public class VirtualMobState {
    private final ResourceLocation mobType;
    private final BlockPos position;
    private final int level;
    private final UUID originalEntityId;  // If this mob was previously spawned
    private final long lastSeenTick;  // When entity was unloaded

    public VirtualMobState(
            ResourceLocation mobType,
            BlockPos position,
            int level,
            @Nullable UUID originalEntityId,
            long lastSeenTick) {
        this.mobType = mobType;
        this.position = position;
        this.level = level;
        this.originalEntityId = originalEntityId;
        this.lastSeenTick = lastSeenTick;
    }

    public ResourceLocation getMobType() {
        return mobType;
    }

    public BlockPos getPosition() {
        return position;
    }

    public int getLevel() {
        return level;
    }

    /**
     * Create entity from virtual state when player approaches.
     * Uses custom pack entities.
     */
    @Nullable
    public Mob spawnEntity(ServerLevel level, UUID packId) {
        EntityType<?> entityType = mapToPackEntity(mobType);
        if (entityType == null) {
            return null;
        }

        Entity entity = entityType.create(
            level,
            null,
            position,
            EntitySpawnReason.MOB_SUMMONED,
            true,
            false
        );

        if (entity instanceof Mob mob) {
            // Restore level and stats
            applyMobLevel(mob, this.level, packId);
            mob.setPersistenceRequired();

            level.addFreshEntity(mob);
            return mob;
        }

        return null;
    }

    /**
     * Map vanilla entity types to custom pack entities.
     */
    @Nullable
    private EntityType<?> mapToPackEntity(ResourceLocation vanillaType) {
        String path = vanillaType.getPath();

        return switch (path) {
            // Zombie variants
            case "zombie" -> ModEntities.PACK_ZOMBIE;
            case "husk" -> ModEntities.PACK_HUSK;
            case "drowned" -> ModEntities.PACK_DROWNED;
            case "zombie_villager" -> ModEntities.PACK_ZOMBIE_VILLAGER;

            // Skeleton variants
            case "skeleton" -> ModEntities.PACK_SKELETON;
            case "stray" -> ModEntities.PACK_STRAY;

            // Spider variants
            case "spider" -> ModEntities.PACK_SPIDER;
            case "cave_spider" -> ModEntities.PACK_CAVE_SPIDER;

            // Other hostiles
            case "creeper" -> ModEntities.PACK_CREEPER;
            case "witch" -> ModEntities.PACK_WITCH;
            case "slime" -> ModEntities.PACK_SLIME;

            // Illagers
            case "vindicator" -> ModEntities.PACK_VINDICATOR;
            case "pillager" -> ModEntities.PACK_PILLAGER;

            default -> null;  // Unsupported type
        };
    }

    /**
     * Apply level-based stats to a mob.
     */
    private void applyMobLevel(Mob mob, int level, UUID packId) {
        var hpAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        var dmgAttr = mob.getAttribute(Attributes.ATTACK_DAMAGE);

        // Health Scaling: Base * 4 * (1.08 ^ (Level - 1))
        double hpMultiplier = 4.0 * Math.pow(1.08, level - 1);
        if (hpAttr != null) {
            double baseHp = hpAttr.getBaseValue();
            if (baseHp <= 100) {
                hpAttr.setBaseValue(baseHp * hpMultiplier);
            }
        }

        // Damage Scaling: Base * 1.5 * (1.05 ^ (Level - 1))
        double dmgMultiplier = 1.5 * Math.pow(1.05, level - 1);
        if (dmgAttr != null) {
            double baseDmg = dmgAttr.getBaseValue();
            if (baseDmg <= 20) {
                dmgAttr.setBaseValue(baseDmg * dmgMultiplier);
            }
        }

        // Heal to full
        mob.setHealth(mob.getMaxHealth());

        // Attach pack data
        mob.setAttached(PlayerDataRegistry.MOB_DATA, new MobData(level, 0, packId));

        // Update nameplate
        String name = mob.getType().getDescription().getString();
        // Virtual pack mobs are hostile -> RED nameplate
        mob.setCustomName(net.minecraft.network.chat.Component.literal("§c[Lv." + level + "] " + name));
        mob.setCustomNameVisible(true);
    }
}

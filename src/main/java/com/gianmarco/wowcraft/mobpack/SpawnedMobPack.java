package com.gianmarco.wowcraft.mobpack;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.ModEntities;
import com.gianmarco.wowcraft.entity.pack.IPackMob;
import com.gianmarco.wowcraft.playerclass.PlayerDataRegistry;
import com.gianmarco.wowcraft.entity.MobData;
import com.gianmarco.wowcraft.zone.BiomeGroup;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an active pack instance spawned in the world.
 * Tracks all mobs in the pack and handles respawning.
 */
public class SpawnedMobPack {
    private final UUID packId;
    private final String templateId;
    private final BlockPos centerPos;
    private final int targetLevel;
    private final float socialAggroRadius;
    private final int respawnDelayTicks;
    private final BiomeGroup zone;
    private final List<SpawnedMob> mobs;

    public SpawnedMobPack(UUID packId, String templateId, BlockPos centerPos,
            int targetLevel, float socialAggroRadius,
            int respawnDelaySeconds, BiomeGroup zone) {
        this.packId = packId;
        this.templateId = templateId;
        this.centerPos = centerPos;
        this.targetLevel = targetLevel;
        this.socialAggroRadius = socialAggroRadius;
        this.respawnDelayTicks = respawnDelaySeconds * 20; // Convert to ticks
        this.zone = zone;
        this.mobs = new ArrayList<>();
    }

    // === Getters ===

    public UUID getPackId() {
        return packId;
    }

    public String getTemplateId() {
        return templateId;
    }

    public BlockPos getCenterPos() {
        return centerPos;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public float getSocialAggroRadius() {
        return socialAggroRadius;
    }

    public BiomeGroup getZone() {
        return zone;
    }

    public List<SpawnedMob> getMobs() {
        return mobs;
    }

    // === Mob Management ===

    /**
     * Adds a mob slot to this pack.
     */
    public void addMob(SpawnedMob mob) {
        mobs.add(mob);
    }

    /**
     * Spawns all mobs that are ready (dead mobs that passed respawn delay).
     */
    public void spawnReadyMobs(ServerLevel level, long currentTick) {
        for (SpawnedMob mobSlot : mobs) {
            if (mobSlot.isReadyToRespawn(currentTick, respawnDelayTicks)) {
                spawnMob(level, mobSlot);
            }
        }
    }

    /**
     * Spawns a single mob into the world.
     */
    private void spawnMob(ServerLevel level, SpawnedMob mobSlot) {
        BlockPos targetPos = mobSlot.getSpawnPos();

        // Check if chunk is loaded
        if (!level.isLoaded(targetPos)) {
            WowCraft.LOGGER.debug("Chunk not loaded for pack {} mob at {}, deferring spawn",
                    packId, targetPos);
            return; // Will retry on next respawn check
        }

        BlockPos spawnPos = findValidSpawnPosition(level, targetPos);
        if (spawnPos == null) {
            WowCraft.LOGGER.warn("Could not find valid spawn position for pack {} mob near {}",
                    packId, targetPos);
            return;
        }

        // Map vanilla mob types to custom pack entities
        ResourceLocation requestedType = mobSlot.getMobType();
        EntityType<?> entityType = mapToPackEntity(requestedType);

        if (entityType == null) {
            WowCraft.LOGGER.error("Unknown mob type: {}", requestedType);
            return;
        }

        // Spawn the entity using create() with proper 1.21.5 signature
        Entity entity = entityType.create(
                level,
                null, // no custom data consumer
                spawnPos,
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED,
                true, // adjust position
                false // don't adjust for surface
        );
        if (entity == null) {
            WowCraft.LOGGER.error("Failed to create entity: {}", mobSlot.getMobType());
            return;
        }

        if (entity instanceof Mob mob) {
            // Mark as persistent (won't despawn)
            mob.setPersistenceRequired();

            // Attach pack data - we override the level from MobLevelManager
            // The mob will already have been initialized by MobMixin, but we override with
            // pack level
            MobData existingData = mob.getAttached(PlayerDataRegistry.MOB_DATA);
            int zoneTier = existingData != null ? existingData.zoneTier() : 0;
            mob.setAttached(PlayerDataRegistry.MOB_DATA, new MobData(targetLevel, zoneTier, packId));

            // Re-apply level stats with our target level
            applyPackLevel(mob, targetLevel, requestedType);

            // Make mob stay near spawn position (leash to pack area)
            makePackMobTerritorial(mob, mobSlot.getSpawnPos());

            // Add to world
            level.addFreshEntity(mob);

            mobSlot.onSpawned(mob.getUUID());

            WowCraft.LOGGER.debug("Spawned pack mob {} at {} (Level {})",
                    mobSlot.getMobType(), spawnPos, targetLevel);
        } else {
            WowCraft.LOGGER.warn("Created entity is not a Mob: {}", mobSlot.getMobType());
            entity.discard();
        }
    }

    /**
     * Map vanilla entity types to custom pack entities.
     * Returns null if the mob type is not supported for packs.
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

            default -> {
                // For unsupported types (passive mobs, warden), use vanilla
                var opt = BuiltInRegistries.ENTITY_TYPE.getOptional(vanillaType);
                yield opt.orElse(null);
            }
        };
    }

    /**
     * Makes a pack mob territorial by restricting its movement range.
     * Mobs stay stationary when idle, but can chase when aggroed.
     */
    private void makePackMobTerritorial(Mob mob, BlockPos homePos) {
        // Set home position with 24 block leash - they can chase but will return
        mob.restrictTo(homePos, 24);

        // If this is a custom pack mob, set its home position for the behavior component
        if (mob instanceof com.gianmarco.wowcraft.entity.pack.IPackMob packMob) {
            packMob.setHomePosition(homePos);
            WowCraft.LOGGER.debug("Set pack mob home position at {} with custom AI", homePos);
        } else {
            // Fallback: Stationary behavior is added via PackMobGoalMixin for vanilla mobs
            WowCraft.LOGGER.debug("Made vanilla mob territorial at {} with 24 block leash", homePos);
        }
    }

    /**
     * Apply level-based stats to a pack mob.
     * Mirrors MobLevelManager but uses our target level.
     */
    private void applyPackLevel(LivingEntity mob, int level, ResourceLocation mobType) {
        var hpAttr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MAX_HEALTH);
        var dmgAttr = mob.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);

        // Health Scaling: Base * 4 * (1.08 ^ (Level - 1))
        // Use current base value as the original (before our scaling)
        double hpMultiplier = 4.0 * Math.pow(1.08, level - 1);
        if (hpAttr != null) {
            // Get the base value (vanilla default is 20 for most mobs)
            double baseHp = hpAttr.getBaseValue();
            // Only apply if not already scaled (check if it looks vanilla-ish)
            if (baseHp <= 100) { // Assume vanilla mobs have <100 base HP
                hpAttr.setBaseValue(baseHp * hpMultiplier);
            }
        }

        // Damage Scaling: Base * 1.5 * (1.05 ^ (Level - 1))
        double dmgMultiplier = 1.5 * Math.pow(1.05, level - 1);
        if (dmgAttr != null) {
            double baseDmg = dmgAttr.getBaseValue();
            if (baseDmg <= 20) { // Assume vanilla mobs have <20 base damage
                dmgAttr.setBaseValue(baseDmg * dmgMultiplier);
            }
        }

        // Heal to full
        mob.setHealth(mob.getMaxHealth());

        // Update nameplate with creative name
        String name = MobNameGenerator.getName(mobType, zone);
        // Pack mobs are always hostile -> RED nameplate
        // Level display is white/grey for now (will be colored based on player level dynamically)
        mob.setCustomName(net.minecraft.network.chat.Component.literal("§c[Lv." + level + "] " + name));
        mob.setCustomNameVisible(true);
    }

    /**
     * Finds a valid spawn position near the target, adjusting for terrain changes.
     */
    @Nullable
    private BlockPos findValidSpawnPosition(ServerLevel level, BlockPos target) {
        // Try original position first
        if (isValidSpawnPos(level, target)) {
            return target;
        }

        // Search in expanding radius
        for (int radius = 1; radius <= 5; radius++) {
            for (int dx = -radius; dx <= radius; dx++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    BlockPos candidate = target.offset(dx, 0, dz);
                    // Get surface Y
                    BlockPos surface = level.getHeightmapPos(
                            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                            candidate);
                    if (isValidSpawnPos(level, surface)) {
                        return surface;
                    }
                }
            }
        }
        return null;
    }

    private boolean isValidSpawnPos(ServerLevel level, BlockPos pos) {
        // Check Y bounds (Minecraft 1.21.5 has world height from -64 to 320)
        if (pos.getY() < level.getMinY() || pos.getY() > level.getMaxY() - 2) {
            return false;
        }

        BlockState below = level.getBlockState(pos.below());
        BlockState at = level.getBlockState(pos);
        BlockState above = level.getBlockState(pos.above());

        // Need solid ground, passable at spawn height
        // Use isSuffocating for better ground check (includes full blocks like grass, stone, etc.)
        boolean hasGround = below.isSuffocating(level, pos.below()) || below.blocksMotion();
        boolean canFitMob = !at.isSuffocating(level, pos) && at.getFluidState().isEmpty();
        boolean hasHeadroom = !above.isSuffocating(level, pos);

        return hasGround && canFitMob && hasHeadroom;
    }

    /**
     * Called when a mob from this pack dies.
     */
    public void onMobDeath(UUID entityId, long currentTick) {
        for (SpawnedMob mob : mobs) {
            if (entityId.equals(mob.getEntityId())) {
                mob.onDeath(currentTick);
                WowCraft.LOGGER.debug("Pack {} mob died, will respawn in {} seconds",
                        packId, respawnDelayTicks / 20);
                return;
            }
        }
    }

    /**
     * Gets all alive mobs within the social aggro radius of a position.
     * Uses live entity positions for accurate distance checks.
     */
    public List<UUID> getAliveMobsWithinRadius(ServerLevel level, BlockPos center, float radius) {
        List<UUID> result = new ArrayList<>();
        double radiusSq = radius * radius;

        for (SpawnedMob mob : mobs) {
            if (mob.isAlive() && mob.getEntityId() != null) {
                // Look up the actual entity to get its current position
                Entity entity = level.getEntity(mob.getEntityId());
                if (entity != null && entity.isAlive()) {
                    double distSq = entity.blockPosition().distSqr(center);
                    if (distSq <= radiusSq) {
                        result.add(mob.getEntityId());
                    }
                }
            }
        }
        return result;
    }

    // === Serialization ===

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("packId", packId.toString());
        json.addProperty("templateId", templateId);
        json.addProperty("centerX", centerPos.getX());
        json.addProperty("centerY", centerPos.getY());
        json.addProperty("centerZ", centerPos.getZ());
        json.addProperty("targetLevel", targetLevel);
        json.addProperty("socialAggroRadius", socialAggroRadius);
        json.addProperty("respawnDelayTicks", respawnDelayTicks);
        json.addProperty("zone", zone.name());

        JsonArray mobsArray = new JsonArray();
        for (SpawnedMob mob : mobs) {
            mobsArray.add(mob.toJson());
        }
        json.add("mobs", mobsArray);

        return json;
    }

    public static SpawnedMobPack fromJson(JsonObject json) {
        UUID packId = UUID.fromString(json.get("packId").getAsString());
        String templateId = json.get("templateId").getAsString();
        BlockPos center = new BlockPos(
                json.get("centerX").getAsInt(),
                json.get("centerY").getAsInt(),
                json.get("centerZ").getAsInt());
        int targetLevel = json.get("targetLevel").getAsInt();
        float socialAggroRadius = json.get("socialAggroRadius").getAsFloat();
        int respawnDelayTicks = json.get("respawnDelayTicks").getAsInt();
        BiomeGroup zone = BiomeGroup.valueOf(json.get("zone").getAsString());

        SpawnedMobPack pack = new SpawnedMobPack(
                packId, templateId, center, targetLevel,
                socialAggroRadius, respawnDelayTicks / 20, zone);

        JsonArray mobsArray = json.getAsJsonArray("mobs");
        for (var element : mobsArray) {
            pack.addMob(SpawnedMob.fromJson(element.getAsJsonObject()));
        }

        return pack;
    }
}

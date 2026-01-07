package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import com.gianmarco.wowcraft.entity.pack.*;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.monster.*;

/**
 * Registry for all custom WowCraft entities.
 */
public class ModEntities {

    private static final ResourceLocation FIREBALL_ID = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "fireball_projectile");
    private static final ResourceLocation ARCANE_MISSILE_ID = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "arcane_missile_projectile");
    private static final ResourceLocation ICECANE_ID = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "ice_lance_projectile");

    public static final EntityType<FireballProjectile> FIREBALL_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            FIREBALL_ID,
            EntityType.Builder.<FireballProjectile>of(FireballProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, FIREBALL_ID)));

    public static final EntityType<ArcaneMissileProjectile> ARCANE_MISSILE_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ARCANE_MISSILE_ID,
            EntityType.Builder.<ArcaneMissileProjectile>of(ArcaneMissileProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ARCANE_MISSILE_ID)));

    public static final EntityType<IceLanceProjectile> ICE_LANCE_PROJECTILE = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            ICECANE_ID,
            EntityType.Builder.<IceLanceProjectile>of(IceLanceProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ICECANE_ID)));

    // Spell Effect Entity for ground decals
    private static final ResourceLocation SPELL_EFFECT_ID = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "spell_effect");

    public static final EntityType<SpellEffectEntity> SPELL_EFFECT = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            SPELL_EFFECT_ID,
            EntityType.Builder.<SpellEffectEntity>of(SpellEffectEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(64)
                    .updateInterval(5)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, SPELL_EFFECT_ID)));

    // ===== Pack Mob Entities with Custom AI =====

    // Zombie variants
    private static final ResourceLocation PACK_ZOMBIE_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_zombie");
    private static final ResourceLocation PACK_HUSK_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_husk");
    private static final ResourceLocation PACK_DROWNED_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_drowned");
    private static final ResourceLocation PACK_ZOMBIE_VILLAGER_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_zombie_villager");

    public static final EntityType<PackZombie> PACK_ZOMBIE = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_ZOMBIE_ID,
            EntityType.Builder.<PackZombie>of(PackZombie::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_ZOMBIE_ID)));
    public static final EntityType<PackHusk> PACK_HUSK = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_HUSK_ID,
            EntityType.Builder.<PackHusk>of(PackHusk::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_HUSK_ID)));
    public static final EntityType<PackDrowned> PACK_DROWNED = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_DROWNED_ID,
            EntityType.Builder.<PackDrowned>of(PackDrowned::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_DROWNED_ID)));
    public static final EntityType<PackZombieVillager> PACK_ZOMBIE_VILLAGER = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_ZOMBIE_VILLAGER_ID,
            EntityType.Builder.<PackZombieVillager>of(PackZombieVillager::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_ZOMBIE_VILLAGER_ID)));

    // Skeleton variants
    private static final ResourceLocation PACK_SKELETON_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_skeleton");
    private static final ResourceLocation PACK_STRAY_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_stray");

    public static final EntityType<PackSkeleton> PACK_SKELETON = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_SKELETON_ID,
            EntityType.Builder.<PackSkeleton>of(PackSkeleton::new, MobCategory.MONSTER).sized(0.6f, 1.99f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_SKELETON_ID)));
    public static final EntityType<PackStray> PACK_STRAY = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_STRAY_ID,
            EntityType.Builder.<PackStray>of(PackStray::new, MobCategory.MONSTER).sized(0.6f, 1.99f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_STRAY_ID)));

    // Spider variants
    private static final ResourceLocation PACK_SPIDER_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_spider");
    private static final ResourceLocation PACK_CAVE_SPIDER_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_cave_spider");

    public static final EntityType<PackSpider> PACK_SPIDER = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_SPIDER_ID,
            EntityType.Builder.<PackSpider>of(PackSpider::new, MobCategory.MONSTER).sized(1.4f, 0.9f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_SPIDER_ID)));
    public static final EntityType<PackCaveSpider> PACK_CAVE_SPIDER = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_CAVE_SPIDER_ID,
            EntityType.Builder.<PackCaveSpider>of(PackCaveSpider::new, MobCategory.MONSTER).sized(0.7f, 0.5f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_CAVE_SPIDER_ID)));

    // Other hostiles
    private static final ResourceLocation PACK_CREEPER_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_creeper");
    private static final ResourceLocation PACK_WITCH_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_witch");
    private static final ResourceLocation PACK_SLIME_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_slime");

    public static final EntityType<PackCreeper> PACK_CREEPER = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_CREEPER_ID,
            EntityType.Builder.<PackCreeper>of(PackCreeper::new, MobCategory.MONSTER).sized(0.6f, 1.7f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_CREEPER_ID)));
    public static final EntityType<PackWitch> PACK_WITCH = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_WITCH_ID,
            EntityType.Builder.<PackWitch>of(PackWitch::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_WITCH_ID)));
    public static final EntityType<PackSlime> PACK_SLIME = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_SLIME_ID,
            EntityType.Builder.<PackSlime>of(PackSlime::new, MobCategory.MONSTER).sized(2.04f, 2.04f).clientTrackingRange(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_SLIME_ID)));

    // Illagers
    private static final ResourceLocation PACK_VINDICATOR_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_vindicator");
    private static final ResourceLocation PACK_PILLAGER_ID = ResourceLocation.fromNamespaceAndPath(WowCraft.MOD_ID, "pack_pillager");

    public static final EntityType<PackVindicator> PACK_VINDICATOR = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_VINDICATOR_ID,
            EntityType.Builder.<PackVindicator>of(PackVindicator::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_VINDICATOR_ID)));
    public static final EntityType<PackPillager> PACK_PILLAGER = Registry.register(BuiltInRegistries.ENTITY_TYPE, PACK_PILLAGER_ID,
            EntityType.Builder.<PackPillager>of(PackPillager::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(8)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, PACK_PILLAGER_ID)));

    /**
     * Initialize entity registration
     */
    public static void register() {
        // Register entity attributes (required for custom entities)
        FabricDefaultAttributeRegistry.register(PACK_ZOMBIE, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_HUSK, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_DROWNED, Zombie.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_ZOMBIE_VILLAGER, Zombie.createAttributes());

        FabricDefaultAttributeRegistry.register(PACK_SKELETON, AbstractSkeleton.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_STRAY, AbstractSkeleton.createAttributes());

        FabricDefaultAttributeRegistry.register(PACK_SPIDER, Spider.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_CAVE_SPIDER, Spider.createAttributes());

        FabricDefaultAttributeRegistry.register(PACK_CREEPER, Creeper.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_WITCH, Witch.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_SLIME, Monster.createMonsterAttributes());

        FabricDefaultAttributeRegistry.register(PACK_VINDICATOR, Vindicator.createAttributes());
        FabricDefaultAttributeRegistry.register(PACK_PILLAGER, Pillager.createAttributes());

        WowCraft.LOGGER.info("Registered WowCraft entities");
    }
}

package com.gianmarco.wowcraft.entity;

import com.gianmarco.wowcraft.WowCraft;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Registry for all custom WowCraft entities.
 */
public class ModEntities {

    private static final ResourceLocation FIREBALL_ID = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "fireball_projectile");
    private static final ResourceLocation ARCANE_MISSILE_ID = ResourceLocation.fromNamespaceAndPath(
            WowCraft.MOD_ID, "arcane_missile_projectile");
    private static final ResourceLocation ICE_LANCE_ID = ResourceLocation.fromNamespaceAndPath(
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
            ICE_LANCE_ID,
            EntityType.Builder.<IceLanceProjectile>of(IceLanceProjectile::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(10)
                    .build(ResourceKey.create(Registries.ENTITY_TYPE, ICE_LANCE_ID)));

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

    /**
     * Initialize entity registration
     */
    public static void register() {
        WowCraft.LOGGER.info("Registered WowCraft entities");
    }
}

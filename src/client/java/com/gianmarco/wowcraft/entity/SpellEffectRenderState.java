package com.gianmarco.wowcraft.entity;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

/**
 * Render state for SpellEffectEntity.
 * Holds animation data extracted from entity for rendering.
 */
@Environment(EnvType.CLIENT)
public class SpellEffectRenderState extends EntityRenderState {
    public SpellEffectEntity.EffectType effectType = SpellEffectEntity.EffectType.FROST_NOVA;
    public float currentRadius = 1.0f;
    public float alpha = 1.0f;
    public float rotation = 0.0f;
}

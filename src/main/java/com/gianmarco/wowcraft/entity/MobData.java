package com.gianmarco.wowcraft.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.UUIDUtil;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/**
 * Stores level, zone, and pack data for mobs.
 */
public record MobData(int level, int zoneTier, @Nullable UUID packId) {

    // Default is level 1, zone 0, no pack
    public static final MobData DEFAULT = new MobData(1, 0, null);

    // Codec for serialization (packId is optional)
    public static final Codec<MobData> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("level").forGetter(MobData::level),
            Codec.INT.fieldOf("zoneTier").forGetter(MobData::zoneTier),
            UUIDUtil.CODEC.optionalFieldOf("packId").forGetter(d -> Optional.ofNullable(d.packId())))
            .apply(instance, (level, zoneTier, packIdOpt) -> new MobData(level, zoneTier, packIdOpt.orElse(null))));

    // Constructor without packId for backward compatibility
    public MobData(int level, int zoneTier) {
        this(level, zoneTier, null);
    }

    public MobData withLevel(int newLevel) {
        return new MobData(newLevel, this.zoneTier, this.packId);
    }

    public MobData withZoneTier(int newZoneTier) {
        return new MobData(this.level, newZoneTier, this.packId);
    }

    public MobData withPackId(@Nullable UUID newPackId) {
        return new MobData(this.level, this.zoneTier, newPackId);
    }

    public boolean isPackMob() {
        return packId != null;
    }
}

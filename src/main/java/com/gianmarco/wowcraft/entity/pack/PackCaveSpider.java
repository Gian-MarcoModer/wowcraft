package com.gianmarco.wowcraft.entity.pack;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.CaveSpider;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

/**
 * Pack cavespider with built-in leash/evade behavior.
 */
public class PackCaveSpider extends CaveSpider implements IPackMob {

    private final PackMobBehavior packBehavior;

    public PackCaveSpider(EntityType<? extends CaveSpider> entityType, Level level) {
        super(entityType, level);
        this.packBehavior = new PackMobBehavior(this);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);
        packBehavior.tick();
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        if (packBehavior.isEvading() && target != null) {
            return;
        }
        super.setTarget(target);
    }

    @Override
    public BlockPos getHomePosition() {
        return packBehavior.getHomePosition();
    }

    @Override
    public void setHomePosition(BlockPos pos) {
        packBehavior.setHomePosition(pos);
    }

    @Override
    public boolean isEvading() {
        return packBehavior.isEvading();
    }

    @Override
    public void startEvade() {
        packBehavior.startEvade();
    }

    @Override
    public void stopEvade() {
        packBehavior.stopEvade();
    }

    @Override
    public double getLeashRange() {
        return packBehavior.getLeashRange();
    }

    @Override
    public int getLeashResetTicks() {
        return packBehavior.getLeashResetTicks();
    }
}

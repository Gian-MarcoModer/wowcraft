package com.gianmarco.wowcraft.entity.pack;

import net.minecraft.core.BlockPos;

/**
 * Interface for pack mobs with custom AI behavior.
 */
public interface IPackMob {

    BlockPos getHomePosition();

    void setHomePosition(BlockPos pos);

    boolean isEvading();

    void startEvade();

    void stopEvade();

    double getLeashRange();

    int getLeashResetTicks();
}

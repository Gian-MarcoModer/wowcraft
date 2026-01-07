package com.gianmarco.wowcraft.roads;

import net.minecraft.core.BlockPos;

/**
 * Represents a planned road segment between two points.
 */
public class RoadSegment {
    private final BlockPos start;
    private final BlockPos end;
    private BlockPos currentPos;
    private int xRemaining;
    private int zRemaining;
    private boolean complete;

    public RoadSegment(BlockPos start, BlockPos end) {
        this.start = start;
        this.end = end;
        this.currentPos = start;
        this.xRemaining = end.getX() - start.getX();
        this.zRemaining = end.getZ() - start.getZ();
        this.complete = false;
    }

    public BlockPos getStart() {
        return start;
    }

    public BlockPos getEnd() {
        return end;
    }

    public BlockPos getCurrentPos() {
        return currentPos;
    }

    public boolean isComplete() {
        return complete;
    }

    /**
     * Advance the road by one step.
     * Returns the new position to place road blocks at.
     */
    public BlockPos advance() {
        if (complete) {
            return null;
        }

        // Move along whichever axis has more distance remaining
        if (Math.abs(xRemaining) >= Math.abs(zRemaining)) {
            if (xRemaining > 0) {
                currentPos = currentPos.east();
                xRemaining--;
            } else if (xRemaining < 0) {
                currentPos = currentPos.west();
                xRemaining++;
            }
        } else {
            if (zRemaining > 0) {
                currentPos = currentPos.south();
                zRemaining--;
            } else if (zRemaining < 0) {
                currentPos = currentPos.north();
                zRemaining++;
            }
        }

        // Check if we've reached the destination
        if (xRemaining == 0 && zRemaining == 0) {
            complete = true;
        }

        return currentPos;
    }

    /**
     * Get total length of this road segment.
     */
    public int getTotalLength() {
        return Math.abs(end.getX() - start.getX()) + Math.abs(end.getZ() - start.getZ());
    }

    /**
     * Get remaining distance to complete.
     */
    public int getRemainingDistance() {
        return Math.abs(xRemaining) + Math.abs(zRemaining);
    }

    @Override
    public String toString() {
        return String.format("RoadSegment[%s -> %s, %d remaining]", start, end, getRemainingDistance());
    }
}

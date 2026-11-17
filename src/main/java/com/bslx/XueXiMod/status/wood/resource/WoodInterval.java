package com.bslx.XueXiMod.status.wood.resource;

/** 区间潜力。公式是 floor(P*high/E) - floor(P*low/E)，中间乘积用 long。 */
public final class WoodInterval {
    private WoodInterval() {
    }

    public static int potential(int potentialUnits, int effectiveUnits, int lowInclusive, int highExclusive) {
        if (effectiveUnits <= 0 || potentialUnits <= 0 || lowInclusive < 0
                || highExclusive <= lowInclusive || highExclusive > effectiveUnits) {
            return 0;
        }
        long highPart = (long) potentialUnits * highExclusive / effectiveUnits;
        long lowPart = (long) potentialUnits * lowInclusive / effectiveUnits;
        long difference = highPart - lowPart;
        if (difference <= 0L) {
            return 0;
        }
        if (difference > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) difference;
    }

    public static boolean addressable(double coordX, double coordY, double coordZ) {
        return horizontalBlock(coordX) != null && verticalBlock(coordY) != null && horizontalBlock(coordZ) != null;
    }

    /** 水平方块坐标。超出原版可寻址范围时返回 null，调用方不得加载区块去凑这个点。 */
    public static Integer horizontalBlock(double coordinate) {
        if (!Double.isFinite(coordinate)) {
            return null;
        }
        double floored = Math.floor(coordinate);
        double limit = WoodLimits.HORIZONTAL_BLOCK_LIMIT;
        if (floored < -limit || floored > limit) {
            return null;
        }
        return (int) floored;
    }

    public static Integer verticalBlock(double coordinate) {
        if (!Double.isFinite(coordinate)) {
            return null;
        }
        double floored = Math.floor(coordinate);
        if (floored < Integer.MIN_VALUE || floored > Integer.MAX_VALUE) {
            return null;
        }
        return (int) floored;
    }

    public static int chunkCoordinate(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, 16);
    }
}

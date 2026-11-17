package com.bslx.XueXiMod.shu.technique.enchant.resource;

import java.util.ArrayList;
import java.util.List;

/**
 * 崩地浅碗的相对方块。只给出形状和顺序，不检查世界、不破坏方块。
 * 块心水平距离平方不超过 4，向下深度满足深度的两倍不超过 4 减该平方。
 */
public final class BreakGroundShape {
    public static final int HORIZONTAL_LIMIT = 2;
    public static final int DEPTH_LIMIT = 2;
    public static final int BLOCK_CAP = 32;
    private static final List<BreakOffset> OFFSETS = List.copyOf(build());

    private BreakGroundShape() {
    }

    public static List<BreakOffset> offsets() {
        return OFFSETS;
    }

    private static List<BreakOffset> build() {
        ArrayList<BreakOffset> gathered = new ArrayList<>();
        for (int east = -HORIZONTAL_LIMIT; east <= HORIZONTAL_LIMIT; east++) {
            for (int south = -HORIZONTAL_LIMIT; south <= HORIZONTAL_LIMIT; south++) {
                long span = (long) east * east + (long) south * south;
                if (span > (long) HORIZONTAL_LIMIT * HORIZONTAL_LIMIT) {
                    continue;
                }
                for (int down = 0; down <= DEPTH_LIMIT; down++) {
                    if (down * 2L <= (HORIZONTAL_LIMIT * HORIZONTAL_LIMIT) - span) {
                        gathered.add(new BreakOffset(east, down, south));
                    }
                }
            }
        }
        gathered.sort(BreakGroundShape::nearer);
        if (gathered.size() > BLOCK_CAP) {
            return new ArrayList<>(gathered.subList(0, BLOCK_CAP));
        }
        return gathered;
    }

    private static int nearer(BreakOffset left, BreakOffset right) {
        int spanOrder = Long.compare(spanOf(left), spanOf(right));
        if (spanOrder != 0) {
            return spanOrder;
        }
        if (left.downSteps() != right.downSteps()) {
            return Integer.compare(left.downSteps(), right.downSteps());
        }
        if (left.eastSteps() != right.eastSteps()) {
            return Integer.compare(left.eastSteps(), right.eastSteps());
        }
        return Integer.compare(left.southSteps(), right.southSteps());
    }

    private static long spanOf(BreakOffset offset) {
        return (long) offset.eastSteps() * offset.eastSteps() + (long) offset.southSteps() * offset.southSteps();
    }

    public record BreakOffset(int eastSteps, int downSteps, int southSteps) {
    }
}

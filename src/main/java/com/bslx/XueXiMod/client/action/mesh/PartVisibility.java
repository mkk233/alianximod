package com.bslx.XueXiMod.client.action.mesh;

import java.util.Arrays;

/** 一次绘制自己的分区开关。不写回共享网格。 */
public final class PartVisibility {
    private final boolean[] shown;

    private PartVisibility(boolean[] shown) {
        this.shown = shown;
    }

    public static PartVisibility allShown() {
        boolean[] shown = new boolean[PlayerMeshPart.values().length];
        Arrays.fill(shown, true);
        return new PartVisibility(shown);
    }

    public static PartVisibility noneShown() {
        return new PartVisibility(new boolean[PlayerMeshPart.values().length]);
    }

    public PartVisibility with(PlayerMeshPart part, boolean visible) {
        if (part == null) {
            throw new SkinDiagnostic("part-missing", "分区开关缺少部位");
        }
        boolean[] next = this.shown.clone();
        next[part.ordinal()] = visible;
        return new PartVisibility(next);
    }

    public boolean shows(PlayerMeshPart part) {
        if (part == null) {
            return false;
        }
        return this.shown[part.ordinal()];
    }
}

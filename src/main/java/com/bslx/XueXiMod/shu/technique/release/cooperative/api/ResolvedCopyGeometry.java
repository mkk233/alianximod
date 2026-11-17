package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.world.phys.Vec3;

/** 统一链已经解出的一份副本几何，再加玩家基准眼点。方向不在这里按演员拆开。 */
public record ResolvedCopyGeometry(Vec3 origin, Vec3 bladeNormal, float roll, boolean pose3d,
                                   boolean overrideGeometry, Vec3 playerEye) {
    public ResolvedCopyGeometry {
        origin = FrozenValues.finite(origin, "副本原点");
        bladeNormal = FrozenValues.unit(bladeNormal, "刀面法线");
        playerEye = FrozenValues.finite(playerEye, "玩家基准眼点");
        if (!Float.isFinite(roll)) {
            throw new IllegalArgumentException("刀面滚转必须有限");
        }
    }
}

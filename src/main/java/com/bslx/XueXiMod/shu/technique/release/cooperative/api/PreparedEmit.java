package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.resources.ResourceLocation;

/** 一名演员、一份逻辑副本上的恰好一个发射。不含伤害单价。 */
public record PreparedEmit(ExplicitVolumeGeometry geometry, int lifetimeTicks, ResourceLocation presentationId,
                           LogicalCopyKey copy, int actorOrdinal) {
    public PreparedEmit {
        if (geometry == null || presentationId == null || copy == null) {
            throw new IllegalArgumentException("准备发射不完整");
        }
        if (lifetimeTicks < 1 || actorOrdinal < 0) {
            throw new IllegalArgumentException("准备发射的寿命或演员序号无效");
        }
    }
}

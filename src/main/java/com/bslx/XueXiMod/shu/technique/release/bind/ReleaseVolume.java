package com.bslx.XueXiMod.shu.technique.release.bind;

import net.minecraft.world.phys.Vec3;

/** 把点收进本轮冻结的真实范围。不暴露领域几何类型。 */
public interface ReleaseVolume {
    /** 返回收进冻结范围后的点。 */
    Vec3 clampPoint(Vec3 point);
}

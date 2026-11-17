package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** 领域的固定范围。完整包含和体积相交是两件事。 */
public interface DomainGeometry {
    AABB bounds();

    boolean fullyContains(AABB body);

    boolean intersects(AABB body);

    /** 位移过程中身体完整位于域内的参数区间。放不进去时为空。 */
    double[] fullInterval(AABB body, Vec3 displacement);

    /** 已完全进入后，只缩短会穿出边界的轴，其余方向保持原位移。 */
    default Vec3 clipInside(AABB body, Vec3 movement) {
        return movement;
    }

    boolean fits(AABB body);
}

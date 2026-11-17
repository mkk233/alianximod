package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 固定领域范围合同。实现保存几何，领域实例决定它的生命周期。
 * 球形场地用锚点和半径，护体用随身环，都不实现这份盒子合同。
 */
public interface DomainGeometry {
    AABB bounds();

    /** 在真实包围盒内按轴独立均匀取点，并离开盒的浮点边缘。 */
    Vec3 samplePoint(RandomSource random);

    /** 把点收到真实包围盒的内缩范围。空点仍返回空。已经落在范围内的坐标保持不变。 */
    Vec3 clampPoint(Vec3 point);

    boolean fullyContains(AABB body);

    boolean intersects(AABB body);

    /**
     * 为真时，绑定选目标用体积相交。默认仍是完整包含，旧领域不变。
     */
    default boolean selectsByIntersection() {
        return false;
    }

    /** 位移过程中身体完整位于域内的参数区间。放不进去时为空。 */
    double[] fullInterval(AABB body, Vec3 displacement);

    /** 已完全进入后，只缩短会穿出边界的轴，其余方向保持原位移。不扣资源；放不下时由实现自行返回。 */
    default Vec3 clipInside(AABB body, Vec3 movement) {
        return movement;
    }

    boolean fits(AABB body);
}

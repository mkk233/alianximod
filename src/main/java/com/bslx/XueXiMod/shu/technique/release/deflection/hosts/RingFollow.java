package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import net.minecraft.world.phys.Vec3;

/**
 * 水环当前这一拍的真实姿态。环心跟主人，法线跟护体姿态。
 * 主人离场、死亡、换维或这一代护体失效时 {@link #live()} 为假，路线不再追。
 */
public interface RingFollow {
    /** 这一代水环是否仍跟着活着的主人。 */
    boolean live();

    /** 当前环心。 */
    Vec3 center();

    /** 当前环平面法线。 */
    Vec3 planeNormal();

    /** 主人锚点，用来避开朝向主人的出口。 */
    Vec3 ownerAnchor();
}

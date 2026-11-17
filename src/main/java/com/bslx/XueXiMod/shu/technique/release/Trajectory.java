package com.bslx.XueXiMod.shu.technique.release;

/** 弹道种类。施法帧和命中规格按它选路径；速度、重力和采样不写在枚举里。 */
public enum Trajectory {
    STRAIGHT,
    DROP,
    FOLLOW_LOOK,
    FUNCTION,
    /** 固定半径螺旋。轴向速度用攻击自己的速度，不改斩击形状。 */
    VORTEX,
    /** 出手方向上仰后的抛物线。具体初速、重力和仰角读运动参数。 */
    PARABOLA,
    /** 单份沿轴向，多份在锥面上等分。具体锥角和水平限制读运动参数。 */
    UPWELL
    ,
    /** 施放朝向的水平投影。速度仍用宿主自己的速度，不另建运动。 */
    PARALLEL
}

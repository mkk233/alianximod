package com.bslx.XueXiMod.shu.technique.release.deflection;

/**
 * 宿主自己的捕获路线。本包没有水幕、水环或缺月的实现。
 * 水幕应立即给出反射样本；水环走六拍；缺月沿剩余活动弧到末端。
 */
public interface CaptureRoute {
    /** 这一拍路线上的位置。结束时由样本标明。 */
    RouteSample sample(long gameTick);

    /** 现在放出的位置和单位方向。 */
    ReleaseSample emergencyRelease(long gameTick);

    /** 路线是否还能继续携带弹体。 */
    boolean active();
}

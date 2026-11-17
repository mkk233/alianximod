package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import net.minecraft.world.phys.Vec3;

/**
 * 水幕当拍反射。冻结合同没有单独的正常放出方法，所以 {@link #emergencyRelease(long)} 兼做正常出口：
 * {@link #sample(long)} 已经 finished 时，它返回这次算好的反射点和单位反射方向，不是把宿主丢失当成退出。
 * 多次调用返回同一结果，不重新反射。速度幅值由弹体批乘回，这里只给单位方向，因此不改速率，也不朝射手转。
 */
public final class WallReflectRoute implements CaptureRoute {
    private final Vec3 exitPoint;
    private final Vec3 unitDirection;

    public WallReflectRoute(Vec3 exitPoint, Vec3 unitDirection) {
        this.exitPoint = exitPoint == null ? Vec3.ZERO : exitPoint;
        this.unitDirection = unitDirection == null || unitDirection.lengthSqr() < 1.0E-16
                ? new Vec3(0.0, 1.0, 0.0)
                : unitDirection.normalize();
    }

    /** 当拍给出结束样本。 */
    @Override
    public RouteSample sample(long gameTick) {
        return new RouteSample(exitPoint, true);
    }

    /** 放出位置和反射后的单位方向。 */
    @Override
    public ReleaseSample emergencyRelease(long gameTick) {
        return new ReleaseSample(exitPoint, unitDirection);
    }

    /** 反射路线在样本发出前保持有效。 */
    @Override
    public boolean active() {
        return true;
    }
}

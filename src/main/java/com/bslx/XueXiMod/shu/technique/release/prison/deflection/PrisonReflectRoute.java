package com.bslx.XueXiMod.shu.technique.release.prison.deflection;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import net.minecraft.world.phys.Vec3;

/**
 * 当拍向内反射。样本立刻结束，不卷绕、不追踪。
 * 速度幅值由弹体批乘回，这里只给单位方向。
 */
public final class PrisonReflectRoute implements CaptureRoute {
    private final Vec3 exitPoint;
    private final Vec3 unitDirection;

    public PrisonReflectRoute(Vec3 exitPoint, Vec3 unitDirection) {
        this.exitPoint = exitPoint == null ? Vec3.ZERO : exitPoint;
        this.unitDirection = unitDirection == null || unitDirection.lengthSqr() < 1.0E-16
                ? new Vec3(0.0, 1.0, 0.0)
                : unitDirection.normalize();
    }

    /** 当拍给出结束样本。方向不在这里。 */
    @Override
    public RouteSample sample(long gameTick) {
        return new RouteSample(exitPoint, true);
    }

    /** 放出位置和单位方向。速度幅值仍由弹体持有。 */
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

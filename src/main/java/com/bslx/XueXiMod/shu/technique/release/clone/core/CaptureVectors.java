package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

final class CaptureVectors {
    private static final double UNIT_SLACK = 1.0E-3D;
    private static final double DEGENERATE_SEPARATION = 1.0E-8D;

    private CaptureVectors() {
    }

    static boolean finite(@Nullable Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    static boolean unit(@Nullable Vec3 look) {
        if (!finite(look)) {
            return false;
        }
        return Math.abs(look.lengthSqr() - 1.0D) <= UNIT_SLACK;
    }

    static Vec3 aimOrFrozen(Vec3 eyeOrigin, Vec3 targetCenter, Vec3 frozenLook) {
        if (!finite(eyeOrigin) || !finite(targetCenter)) {
            return frozenLook;
        }
        Vec3 delta = targetCenter.subtract(eyeOrigin);
        if (!finite(delta) || delta.lengthSqr() < DEGENERATE_SEPARATION) {
            return frozenLook;
        }
        Vec3 aimed = delta.normalize();
        return unit(aimed) ? aimed : frozenLook;
    }
}

package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

/**
 * 六刚性 fallback 的连接。只改枢轴和欧拉角，不改腿长、缩放或源根位移。
 * D 已是模型全局增量。T = L * D_body，身体缺增量时 D_body 按单位旋转。
 */
final class RigidFallback {
    private RigidFallback() {
    }

    static Placed place(
            Sample vanilla,
            float centerAcross,
            float centerUpward,
            float centerAhead,
            OpenMatrix4f alignment,
            OpenMatrix4f delta,
            OpenMatrix4f trunk,
            boolean bodyPivot) {
        float nextAcross = vanilla.across();
        float nextUpward = vanilla.upward();
        float nextAhead = vanilla.ahead();
        if (!bodyPivot && !identity(trunk)) {
            Vec3f turned = MatrixFrames.apply(
                    trunk,
                    vanilla.across() - centerAcross,
                    vanilla.upward() - centerUpward,
                    vanilla.ahead() - centerAhead);
            nextAcross = centerAcross + turned.coordinateX;
            nextUpward = centerUpward + turned.coordinateY;
            nextAhead = centerAhead + turned.coordinateZ;
        }
        boolean driven = delta != null;
        boolean preserveAngles = driven ? identity(alignment) && identity(delta) : identity(trunk);
        if (preserveAngles) {
            return new Placed(
                    nextAcross,
                    nextUpward,
                    nextAhead,
                    vanilla.pitchRadians(),
                    vanilla.yawRadians(),
                    vanilla.rollRadians());
        }
        OpenMatrix4f basis = VanillaEuler.zyx(vanilla.pitchRadians(), vanilla.yawRadians(), vanilla.rollRadians());
        OpenMatrix4f spin = driven
                ? MatrixFrames.product(alignment, MatrixFrames.product(delta, basis))
                : MatrixFrames.product(trunk, basis);
        VanillaEuler euler = VanillaEuler.from(spin);
        return new Placed(
                nextAcross,
                nextUpward,
                nextAhead,
                euler.pitchRadians(),
                euler.yawRadians(),
                euler.rollRadians());
    }

    static boolean identity(OpenMatrix4f matrix) {
        if (matrix == null) {
            return true;
        }
        return matrix.m00 == 1.0F && matrix.m01 == 0.0F && matrix.m02 == 0.0F && matrix.m03 == 0.0F
                && matrix.m10 == 0.0F && matrix.m11 == 1.0F && matrix.m12 == 0.0F && matrix.m13 == 0.0F
                && matrix.m20 == 0.0F && matrix.m21 == 0.0F && matrix.m22 == 1.0F && matrix.m23 == 0.0F
                && matrix.m30 == 0.0F && matrix.m31 == 0.0F && matrix.m32 == 0.0F && matrix.m33 == 1.0F;
    }

    record Sample(
            float across,
            float upward,
            float ahead,
            float pitchRadians,
            float yawRadians,
            float rollRadians) {
    }

    record Placed(
            float across,
            float upward,
            float ahead,
            float pitchRadians,
            float yawRadians,
            float rollRadians) {
    }
}

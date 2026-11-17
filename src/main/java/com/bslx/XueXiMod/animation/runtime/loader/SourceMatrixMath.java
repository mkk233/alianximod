package com.bslx.XueXiMod.animation.runtime.loader;

import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

/**
 * 固定提交 9d25f19c 的主动作入口语义。
 * 绝对矩阵先转置；只有根校正标志仍为真时左乘绕 X 轴 -90 度。
 * 增量是逆绑定左乘绝对矩阵。不按骨骼名字再乘一次校正。
 */
public final class SourceMatrixMath {
    public static final OpenMatrix4f BLENDER_TO_MINECRAFT = OpenMatrix4f.createRotatorDeg(-90.0F, Vec3f.X_AXIS).unmodifiable();

    private SourceMatrixMath() {
    }

    public static OpenMatrix4f convertedAbsolute(float[] rowMajor, boolean rootCorrection) {
        if (rowMajor == null || rowMajor.length != 16) {
            throw new IllegalArgumentException("matrix row must contain 16 components");
        }
        OpenMatrix4f absolute = OpenMatrix4f.load(null, rowMajor);
        absolute.transpose();
        if (rootCorrection) {
            absolute.mulFront(new OpenMatrix4f(BLENDER_TO_MINECRAFT));
        }
        if (!absolute.hasFiniteElements()) {
            throw new IllegalArgumentException("converted matrix must be finite");
        }
        return absolute;
    }

    public static JointTransform delta(float[] rowMajor, OpenMatrix4f inverseBind, boolean rootCorrection) {
        if (inverseBind == null || !inverseBind.hasFiniteElements()) {
            throw new IllegalArgumentException("inverse bind is missing");
        }
        OpenMatrix4f absolute = convertedAbsolute(rowMajor, rootCorrection);
        absolute.mulFront(inverseBind);
        JointTransform transform = JointTransform.fromMatrix(absolute);
        transform.rotation().normalize();
        transform.requireFinite();
        return transform;
    }
}

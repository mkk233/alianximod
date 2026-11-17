package com.bslx.XueXiMod.client.action.skinned.attachments;

import org.joml.Matrix4f;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

/** OpenMatrix4f 和 JOML 的乘向量约定相同，按字段抄过去。 */
final class JomlCopy {
    private JomlCopy() {
    }

    static Matrix4f matrix(OpenMatrix4f source) {
        return new Matrix4f(
                source.m00, source.m01, source.m02, source.m03,
                source.m10, source.m11, source.m12, source.m13,
                source.m20, source.m21, source.m22, source.m23,
                source.m30, source.m31, source.m32, source.m33);
    }
}

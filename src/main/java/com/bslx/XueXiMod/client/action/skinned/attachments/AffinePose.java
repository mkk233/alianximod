package com.bslx.XueXiMod.client.action.skinned.attachments;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

import com.mojang.blaze3d.vertex.PoseStack;

/**
 * 一次仿射同时右乘 pose，并用线性部分的逆转置右乘 normal。
 * 先确认有限且可逆，再改栈。平移不进入法线。
 */
final class AffinePose {
    private AffinePose() {
    }

    static void apply(PoseStack poseStack, OpenMatrix4f matrix) {
        if (poseStack == null || matrix == null || !matrix.hasFiniteElements()) {
            throw new SkinDiagnostic("affine", "仿射矩阵缺失或不是有限数");
        }
        OpenMatrix4f inverse;
        try {
            inverse = OpenMatrix4f.invert(matrix, null);
        } catch (IllegalArgumentException failure) {
            throw new SkinDiagnostic("affine", "仿射矩阵不可逆");
        }
        if (inverse == null || !inverse.hasFiniteElements()) {
            throw new SkinDiagnostic("affine", "仿射逆矩阵不是有限数");
        }
        Matrix4f poseMatrix = JomlCopy.matrix(matrix);
        Matrix3f normalLinear = new Matrix3f(
                inverse.m00, inverse.m10, inverse.m20,
                inverse.m01, inverse.m11, inverse.m21,
                inverse.m02, inverse.m12, inverse.m22);
        poseStack.last().pose().mul(poseMatrix);
        poseStack.last().normal().mul(normalLinear);
    }
}

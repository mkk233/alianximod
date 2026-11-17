package com.bslx.XueXiMod.client.action.spacecut;

import org.joml.Matrix3f;
import org.joml.Matrix4f;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

import com.mojang.blaze3d.vertex.PoseStack;

/** 把模型空间矩阵乘进当前绘制栈，法线用线性部分的逆转置。 */
public final class SpaceCutMatrices {
    private SpaceCutMatrices() {
    }

    public static void apply(PoseStack poseStack, OpenMatrix4f matrix) {
        if (poseStack == null || matrix == null || !matrix.hasFiniteElements()) {
            return;
        }
        OpenMatrix4f inverse;
        try {
            inverse = OpenMatrix4f.invert(matrix, null);
        } catch (IllegalArgumentException failure) {
            return;
        }
        if (inverse == null || !inverse.hasFiniteElements()) {
            return;
        }
        Matrix4f pose = new Matrix4f(
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33);
        Matrix3f normal = new Matrix3f(
                inverse.m00, inverse.m10, inverse.m20,
                inverse.m01, inverse.m11, inverse.m21,
                inverse.m02, inverse.m12, inverse.m22);
        poseStack.last().pose().mul(pose);
        poseStack.last().normal().mul(normal);
    }

    public static OpenMatrix4f placeModel(SpaceCutFrame.SpaceCutBone bone) {
        OpenMatrix4f placed = new OpenMatrix4f(bone.rotation());
        placed.m30 = bone.pivotX() * SpaceCutSampler.PIXEL;
        placed.m31 = bone.pivotY() * SpaceCutSampler.PIXEL;
        placed.m32 = bone.pivotZ() * SpaceCutSampler.PIXEL;
        return placed;
    }

    /** 第一人称相机：右为 +X，上为 +Y，朝观察者一侧为 +Z。眼睛在模型像素 (0, -1.92, -4)。 */
    public static OpenMatrix4f placeCamera(SpaceCutFrame.SpaceCutBone bone) {
        OpenMatrix4f source = bone.rotation();
        OpenMatrix4f placed = new OpenMatrix4f();
        placed.m00 = -source.m00;
        placed.m10 = -source.m10;
        placed.m20 = -source.m20;
        placed.m01 = -source.m01;
        placed.m11 = -source.m11;
        placed.m21 = -source.m21;
        placed.m02 = source.m02;
        placed.m12 = source.m12;
        placed.m22 = source.m22;
        float[] camera = cameraPoint(bone.pivotX(), bone.pivotY(), bone.pivotZ());
        placed.m30 = camera[0];
        placed.m31 = camera[1];
        placed.m32 = camera[2];
        return placed;
    }

    public static float[] cameraPoint(float modelX, float modelY, float modelZ) {
        float relativeX = modelX - 0.0F;
        float relativeY = modelY - (-1.92F);
        float relativeZ = modelZ - (-4.0F);
        return new float[] {
                -relativeX * SpaceCutSampler.PIXEL,
                -relativeY * SpaceCutSampler.PIXEL,
                relativeZ * SpaceCutSampler.PIXEL
        };
    }

    public static void flipCamera(float modelX, float modelY, float modelZ, float[] destination) {
        destination[0] = -modelX;
        destination[1] = -modelY;
        destination[2] = modelZ;
    }
}

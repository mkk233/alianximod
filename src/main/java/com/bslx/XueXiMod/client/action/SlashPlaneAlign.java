package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.runtime.ActionFrame;

/**
 * 把冻结的世界挥斩架变到当前模型空间。
 * 有渲染入口矩阵时用那份完整线性变换；否则只抵消身体偏航和 scale(-1,-1,1)。
 * 参考架来自调用方给出的素材 F/N，对齐是目标架乘参考架的逆。
 */
public final class SlashPlaneAlign {
    private SlashPlaneAlign() {
    }

    public static OpenMatrix4f modelAlignment(
            float forwardAcross,
            float forwardUpward,
            float forwardAhead,
            float normalAcross,
            float normalUpward,
            float normalAhead,
            float bodyYawDegrees) {
        return modelAlignment(
                forwardAcross, forwardUpward, forwardAhead,
                normalAcross, normalUpward, normalAhead,
                bodyYawDegrees,
                null,
                new Vec3f(0.0F, 0.0F, -1.0F),
                new Vec3f(0.0F, -1.0F, 0.0F));
    }

    public static OpenMatrix4f modelAlignment(
            float forwardAcross,
            float forwardUpward,
            float forwardAhead,
            float normalAcross,
            float normalUpward,
            float normalAhead,
            float bodyYawDegrees,
            OpenMatrix4f capturedWorldToModel,
            Vec3f referenceForward,
            Vec3f referenceNormal) {
        Vec3f modelForward;
        Vec3f modelNormal;
        if (capturedWorldToModel != null) {
            modelForward = MatrixFrames.apply(capturedWorldToModel, forwardAcross, forwardUpward, forwardAhead);
            modelNormal = MatrixFrames.apply(capturedWorldToModel, normalAcross, normalUpward, normalAhead);
        } else {
            modelForward = worldToModel(forwardAcross, forwardUpward, forwardAhead, bodyYawDegrees);
            modelNormal = worldToModel(normalAcross, normalUpward, normalAhead, bodyYawDegrees);
        }
        modelForward = MatrixFrames.normalized(modelForward);
        modelNormal = MatrixFrames.normalized(modelNormal);
        if (modelForward == null || modelNormal == null) {
            return null;
        }
        Vec3f modelRight = MatrixFrames.normalized(MatrixFrames.cross(modelNormal, modelForward));
        if (modelRight == null) {
            return null;
        }
        modelNormal = MatrixFrames.normalized(MatrixFrames.cross(modelForward, modelRight));
        if (modelNormal == null) {
            return null;
        }
        OpenMatrix4f frozen = MatrixFrames.basis(modelRight, modelNormal, modelForward);
        OpenMatrix4f inverseReference = referenceBasis(referenceForward, referenceNormal);
        if (inverseReference == null) {
            return null;
        }
        inverseReference.invert();
        return MatrixFrames.pureRotation(OpenMatrix4f.mul(frozen, inverseReference, null));
    }

    public static Vec3f contractRight(float forwardAcross, float forwardUpward, float forwardAhead, float normalAcross, float normalUpward, float normalAhead) {
        return new Vec3f(
                ActionFrame.crossX(normalAcross, normalUpward, normalAhead, forwardAcross, forwardUpward, forwardAhead),
                ActionFrame.crossY(normalAcross, normalUpward, normalAhead, forwardAcross, forwardUpward, forwardAhead),
                ActionFrame.crossZ(normalAcross, normalUpward, normalAhead, forwardAcross, forwardUpward, forwardAhead));
    }

    public static Vec3f worldToModel(float across, float upward, float ahead, float bodyYawDegrees) {
        OpenMatrix4f combined = OpenMatrix4f.mul(modelScale(), rotateYaw(bodyYawDegrees - 180.0F), null);
        return MatrixFrames.apply(combined, across, upward, ahead);
    }

    public static Vec3f modelToWorld(float across, float upward, float ahead, float bodyYawDegrees) {
        OpenMatrix4f combined = OpenMatrix4f.mul(rotateYaw(180.0F - bodyYawDegrees), modelScale(), null);
        return MatrixFrames.apply(combined, across, upward, ahead);
    }

    private static OpenMatrix4f referenceBasis(Vec3f forward, Vec3f normal) {
        forward = MatrixFrames.normalized(forward);
        normal = MatrixFrames.normalized(normal);
        if (forward == null || normal == null) {
            return null;
        }
        Vec3f right = MatrixFrames.normalized(MatrixFrames.cross(normal, forward));
        if (right == null) {
            return null;
        }
        normal = MatrixFrames.normalized(MatrixFrames.cross(forward, right));
        if (normal == null) {
            return null;
        }
        return MatrixFrames.basis(right, normal, forward);
    }

    private static OpenMatrix4f modelScale() {
        return MatrixFrames.axisFlip(-1.0F, -1.0F, 1.0F);
    }

    static OpenMatrix4f rotateYaw(float degrees) {
        double radians = Math.toRadians(degrees);
        float cosine = (float) Math.cos(radians);
        float sine = (float) Math.sin(radians);
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m00 = cosine;
        matrix.m20 = sine;
        matrix.m02 = -sine;
        matrix.m22 = cosine;
        return matrix;
    }
}

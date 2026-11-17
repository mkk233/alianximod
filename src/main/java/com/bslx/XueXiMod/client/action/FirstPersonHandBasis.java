package com.bslx.XueXiMod.client.action;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

/**
 * 第一人称手持基准。模型旋转先按视角轴共轭，再绕原版手部平移后的枢轴对齐冻结架。
 * 不把第三人称矩阵原样乘进相机。
 */
public final class FirstPersonHandBasis {
    private static final float HAND_ACROSS = 0.56F;
    private static final float HAND_UPWARD = -0.52F;
    private static final float HAND_AHEAD = -0.72F;
    private static final float EQUIP_UPWARD = -0.6F;

    private FirstPersonHandBasis() {
    }

    public static OpenMatrix4f cameraRotation(OpenMatrix4f modelRotation) {
        OpenMatrix4f viewFlip = MatrixFrames.axisFlip(-1.0F, -1.0F, 1.0F);
        return MatrixFrames.conjugate(viewFlip, MatrixFrames.pureRotation(modelRotation));
    }

    public static OpenMatrix4f cameraAlignment(
            float forwardAcross,
            float forwardUpward,
            float forwardAhead,
            float normalAcross,
            float normalUpward,
            float normalAhead,
            float lookAcross,
            float lookUpward,
            float lookAhead,
            float upAcross,
            float upUpward,
            float upAhead,
            float leftAcross,
            float leftUpward,
            float leftAhead) {
        return cameraAlignment(
                forwardAcross, forwardUpward, forwardAhead,
                normalAcross, normalUpward, normalAhead,
                lookAcross, lookUpward, lookAhead,
                upAcross, upUpward, upAhead,
                leftAcross, leftUpward, leftAhead,
                new Vec3f(0.0F, 0.0F, -1.0F),
                new Vec3f(0.0F, -1.0F, 0.0F));
    }

    public static OpenMatrix4f cameraAlignment(
            float forwardAcross,
            float forwardUpward,
            float forwardAhead,
            float normalAcross,
            float normalUpward,
            float normalAhead,
            float lookAcross,
            float lookUpward,
            float lookAhead,
            float upAcross,
            float upUpward,
            float upAhead,
            float leftAcross,
            float leftUpward,
            float leftAhead,
            Vec3f modelForward,
            Vec3f modelNormal) {
        float handRightAcross = -leftAcross;
        float handRightUpward = -leftUpward;
        float handRightAhead = -leftAhead;
        Vec3f cameraForward = cameraOf(forwardAcross, forwardUpward, forwardAhead,
                lookAcross, lookUpward, lookAhead, upAcross, upUpward, upAhead, handRightAcross, handRightUpward, handRightAhead);
        Vec3f cameraNormal = cameraOf(normalAcross, normalUpward, normalAhead,
                lookAcross, lookUpward, lookAhead, upAcross, upUpward, upAhead, handRightAcross, handRightUpward, handRightAhead);
        cameraForward = MatrixFrames.normalized(cameraForward);
        cameraNormal = MatrixFrames.normalized(cameraNormal);
        if (cameraForward == null || cameraNormal == null) {
            return null;
        }
        Vec3f cameraRight = MatrixFrames.normalized(MatrixFrames.cross(cameraNormal, cameraForward));
        if (cameraRight == null) {
            return null;
        }
        cameraNormal = MatrixFrames.normalized(MatrixFrames.cross(cameraForward, cameraRight));
        if (cameraNormal == null) {
            return null;
        }
        OpenMatrix4f frozen = MatrixFrames.basis(cameraRight, cameraNormal, cameraForward);
        OpenMatrix4f inverseReference = referenceBasis(flipModel(modelForward), flipModel(modelNormal));
        if (inverseReference == null) {
            return null;
        }
        inverseReference.invert();
        return MatrixFrames.pureRotation(OpenMatrix4f.mul(frozen, inverseReference, null));
    }

    public static OpenMatrix4f heldRotation(OpenMatrix4f modelRotation, OpenMatrix4f cameraAim) {
        return MatrixFrames.premultiply(cameraAim, cameraRotation(modelRotation));
    }

    public static Vec3f itemAxis(OpenMatrix4f modelRotation, Vec3f restAxis) {
        return MatrixFrames.apply(cameraRotation(modelRotation), restAxis.coordinateX, restAxis.coordinateY, restAxis.coordinateZ);
    }

    public static Vec3f aimedItemAxis(OpenMatrix4f modelRotation, OpenMatrix4f cameraAim, Vec3f restAxis) {
        OpenMatrix4f held = heldRotation(modelRotation, cameraAim);
        return MatrixFrames.apply(held, restAxis.coordinateX, restAxis.coordinateY, restAxis.coordinateZ);
    }

    public static Vec3f pivot(boolean rightHand, float equipProgress) {
        float side = rightHand ? 1.0F : -1.0F;
        float equip = Float.isFinite(equipProgress) ? equipProgress : 0.0F;
        return new Vec3f(side * HAND_ACROSS, HAND_UPWARD + equip * EQUIP_UPWARD, HAND_AHEAD);
    }

    public static Vec3f pointOnAxis(boolean rightHand, float equipProgress, OpenMatrix4f heldRotation, Vec3f restAxis) {
        Vec3f origin = pivot(rightHand, equipProgress);
        Vec3f turned = MatrixFrames.apply(heldRotation, restAxis.coordinateX, restAxis.coordinateY, restAxis.coordinateZ);
        return new Vec3f(origin.coordinateX + turned.coordinateX, origin.coordinateY + turned.coordinateY, origin.coordinateZ + turned.coordinateZ);
    }

    /**
     * 铁剑第一人称显示旋转之后的剑身轴。
     * 右手模型是 [0, -90, 25]。左手模型是 [0, 90, -25]，原版再把偏航和滚转取反，有效旋转与右手相同。
     * 挥砍进度为 0 时的 +45/-45 不在这里，它们已经互相抵消。比较轴是物品模型 +Y 经过这次显示旋转，再经过动作旋转。
     */
    public static Vec3f displayedBlade(OpenMatrix4f heldRotation, boolean rightHand) {
        float yawDegrees = rightHand ? -90.0F : 90.0F;
        float rollDegrees = rightHand ? 25.0F : -25.0F;
        if (!rightHand) {
            yawDegrees = -yawDegrees;
            rollDegrees = -rollDegrees;
        }
        Quaternionf display = new Quaternionf().rotationXYZ(
                0.0F,
                (float) Math.toRadians(yawDegrees),
                (float) Math.toRadians(rollDegrees));
        Vector3f blade = display.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        OpenMatrix4f held = heldRotation == null ? new OpenMatrix4f() : heldRotation;
        return MatrixFrames.apply(held, blade.x(), blade.y(), blade.z());
    }

    public static Quaternionf quaternion(OpenMatrix4f matrix) {
        Matrix4f copied = new Matrix4f(
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33);
        Quaternionf rotation = new Quaternionf();
        copied.getNormalizedRotation(rotation);
        return rotation;
    }

    private static Vec3f cameraOf(
            float worldAcross,
            float worldUpward,
            float worldAhead,
            float lookAcross,
            float lookUpward,
            float lookAhead,
            float upAcross,
            float upUpward,
            float upAhead,
            float handRightAcross,
            float handRightUpward,
            float handRightAhead) {
        float componentAcross = worldAcross * handRightAcross + worldUpward * handRightUpward + worldAhead * handRightAhead;
        float componentUpward = worldAcross * upAcross + worldUpward * upUpward + worldAhead * upAhead;
        float componentAhead = -(worldAcross * lookAcross + worldUpward * lookUpward + worldAhead * lookAhead);
        return new Vec3f(componentAcross, componentUpward, componentAhead);
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

    private static Vec3f flipModel(Vec3f source) {
        if (source == null) {
            return null;
        }
        return new Vec3f(-source.coordinateX, -source.coordinateY, source.coordinateZ);
    }
}

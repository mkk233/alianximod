package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

/** 原版 ModelPart 的 ZYX 欧拉角。xRot、yRot、zRot 分别对应俯仰、偏航、翻滚。 */
public final class VanillaEuler {
    private static final float GIMBAL_LIMIT = 0.9999F;
    private final float pitchRadians;
    private final float yawRadians;
    private final float rollRadians;

    private VanillaEuler(float pitchRadians, float yawRadians, float rollRadians) {
        this.pitchRadians = pitchRadians;
        this.yawRadians = yawRadians;
        this.rollRadians = rollRadians;
    }

    public static VanillaEuler from(OpenMatrix4f rotation) {
        OpenMatrix4f pure = MatrixFrames.pureRotation(rotation);
        float yawSine = -pure.m02;
        if (yawSine > 1.0F) {
            yawSine = 1.0F;
        } else if (yawSine < -1.0F) {
            yawSine = -1.0F;
        }
        float yaw = (float) Math.asin(yawSine);
        float pitch;
        float roll;
        if (Math.abs(pure.m02) < GIMBAL_LIMIT) {
            pitch = (float) Math.atan2(pure.m12, pure.m22);
            roll = (float) Math.atan2(pure.m01, pure.m00);
        } else {
            pitch = (float) Math.atan2(-pure.m21, pure.m11);
            roll = 0.0F;
        }
        if (!Float.isFinite(pitch) || !Float.isFinite(yaw) || !Float.isFinite(roll)) {
            throw new IllegalArgumentException("euler extraction is not finite");
        }
        return new VanillaEuler(pitch, yaw, roll);
    }

    public float pitchRadians() {
        return this.pitchRadians;
    }

    public float yawRadians() {
        return this.yawRadians;
    }

    public float rollRadians() {
        return this.rollRadians;
    }

    /** 先俯仰，再偏航，再翻滚。与 from 使用同一套 ZYX。 */
    public static OpenMatrix4f zyx(float pitchRadians, float yawRadians, float rollRadians) {
        if (!Float.isFinite(pitchRadians) || !Float.isFinite(yawRadians) || !Float.isFinite(rollRadians)) {
            throw new IllegalArgumentException("euler angles are not finite");
        }
        return MatrixFrames.product(rollMatrix(rollRadians), MatrixFrames.product(yawMatrix(yawRadians), pitchMatrix(pitchRadians)));
    }

    private static OpenMatrix4f pitchMatrix(float pitchRadians) {
        float cosine = (float) Math.cos(pitchRadians);
        float sine = (float) Math.sin(pitchRadians);
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m11 = cosine;
        matrix.m21 = -sine;
        matrix.m12 = sine;
        matrix.m22 = cosine;
        return matrix;
    }

    private static OpenMatrix4f yawMatrix(float yawRadians) {
        float cosine = (float) Math.cos(yawRadians);
        float sine = (float) Math.sin(yawRadians);
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m00 = cosine;
        matrix.m20 = sine;
        matrix.m02 = -sine;
        matrix.m22 = cosine;
        return matrix;
    }

    private static OpenMatrix4f rollMatrix(float rollRadians) {
        float cosine = (float) Math.cos(rollRadians);
        float sine = (float) Math.sin(rollRadians);
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m00 = cosine;
        matrix.m10 = -sine;
        matrix.m01 = sine;
        matrix.m11 = cosine;
        return matrix;
    }
}

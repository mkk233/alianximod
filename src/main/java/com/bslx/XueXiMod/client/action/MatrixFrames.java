package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;

/** 刚性旋转的读取、共轭和绕轴重建。平移留在原版枢轴上。 */
final class MatrixFrames {
    private static final float AXIS_FLOOR = 1.0E-5F;

    private MatrixFrames() {
    }

    static OpenMatrix4f axisFlip(float acrossSign, float upwardSign, float aheadSign) {
        return OpenMatrix4f.createScale(acrossSign, upwardSign, aheadSign);
    }

    static OpenMatrix4f pureRotation(OpenMatrix4f source) {
        if (source == null || !source.hasFiniteElements()) {
            throw new IllegalArgumentException("rotation matrix is missing");
        }
        float acrossX = source.m00;
        float upwardX = source.m01;
        float aheadX = source.m02;
        float acrossLength = length(acrossX, upwardX, aheadX);
        if (!(acrossLength > AXIS_FLOOR)) {
            throw new IllegalArgumentException("rotation axis is degenerate");
        }
        acrossX /= acrossLength;
        upwardX /= acrossLength;
        aheadX /= acrossLength;
        float acrossY = source.m10 - (source.m10 * acrossX + source.m11 * upwardX + source.m12 * aheadX) * acrossX;
        float upwardY = source.m11 - (source.m10 * acrossX + source.m11 * upwardX + source.m12 * aheadX) * upwardX;
        float aheadY = source.m12 - (source.m10 * acrossX + source.m11 * upwardX + source.m12 * aheadX) * aheadX;
        float upwardLength = length(acrossY, upwardY, aheadY);
        if (!(upwardLength > AXIS_FLOOR)) {
            throw new IllegalArgumentException("rotation axis is degenerate");
        }
        acrossY /= upwardLength;
        upwardY /= upwardLength;
        aheadY /= upwardLength;
        float acrossZ = upwardX * aheadY - aheadX * upwardY;
        float upwardZ = aheadX * acrossY - acrossX * aheadY;
        float aheadZ = acrossX * upwardY - upwardX * acrossY;
        OpenMatrix4f rotation = new OpenMatrix4f();
        rotation.m00 = acrossX;
        rotation.m01 = upwardX;
        rotation.m02 = aheadX;
        rotation.m10 = acrossY;
        rotation.m11 = upwardY;
        rotation.m12 = aheadY;
        rotation.m20 = acrossZ;
        rotation.m21 = upwardZ;
        rotation.m22 = aheadZ;
        if (!rotation.hasFiniteElements()) {
            throw new IllegalArgumentException("rotation matrix is not finite");
        }
        return rotation;
    }

    static OpenMatrix4f conjugate(OpenMatrix4f axisFlip, OpenMatrix4f rotation) {
        OpenMatrix4f inverseFlip = new OpenMatrix4f(axisFlip);
        inverseFlip.invert();
        return OpenMatrix4f.mul(axisFlip, OpenMatrix4f.mul(rotation, inverseFlip, null), null);
    }

    static OpenMatrix4f relative(OpenMatrix4f current, OpenMatrix4f bind) {
        OpenMatrix4f inverseBind = pureRotation(bind);
        inverseBind.invert();
        return OpenMatrix4f.mul(pureRotation(current), inverseBind, null);
    }

    static OpenMatrix4f premultiply(OpenMatrix4f outer, OpenMatrix4f inner) {
        if (inner == null) {
            return null;
        }
        if (outer == null) {
            return inner;
        }
        return OpenMatrix4f.mul(outer, inner, null);
    }

    /** null 当作单位旋转。缺部件仍用 premultiply，避免把空槽乘成矩阵。 */
    static OpenMatrix4f product(OpenMatrix4f outer, OpenMatrix4f inner) {
        if (outer == null) {
            return inner;
        }
        if (inner == null) {
            return outer;
        }
        return OpenMatrix4f.mul(outer, inner, null);
    }

    static OpenMatrix4f basis(Vec3f imageX, Vec3f imageY, Vec3f imageZ) {
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m00 = imageX.coordinateX;
        matrix.m01 = imageX.coordinateY;
        matrix.m02 = imageX.coordinateZ;
        matrix.m10 = imageY.coordinateX;
        matrix.m11 = imageY.coordinateY;
        matrix.m12 = imageY.coordinateZ;
        matrix.m20 = imageZ.coordinateX;
        matrix.m21 = imageZ.coordinateY;
        matrix.m22 = imageZ.coordinateZ;
        return matrix;
    }

    static Vec3f apply(OpenMatrix4f matrix, float across, float upward, float ahead) {
        return OpenMatrix4f.transform3v(matrix, new Vec3f(across, upward, ahead), null);
    }

    static Vec3f cross(Vec3f left, Vec3f right) {
        return new Vec3f(
                left.coordinateY * right.coordinateZ - left.coordinateZ * right.coordinateY,
                left.coordinateZ * right.coordinateX - left.coordinateX * right.coordinateZ,
                left.coordinateX * right.coordinateY - left.coordinateY * right.coordinateX);
    }

    static Vec3f normalized(Vec3f source) {
        float extent = length(source.coordinateX, source.coordinateY, source.coordinateZ);
        if (!(extent > AXIS_FLOOR)) {
            return null;
        }
        return new Vec3f(source.coordinateX / extent, source.coordinateY / extent, source.coordinateZ / extent);
    }

    static float length(float across, float upward, float ahead) {
        return (float) Math.sqrt(across * across + upward * upward + ahead * ahead);
    }
}

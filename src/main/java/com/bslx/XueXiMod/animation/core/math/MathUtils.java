/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/utils/math/MathUtils.java
 * Modified: 2026-10-24
 *
 * 修改说明：只保留 Vec3f 线性插值（上游约 349–363 行）和四元数插值（上游约 425–465 行）。
 * 另保留 TransformSheet.correctAnimationByNewPosition 实际调用的 Vec3f 夹角（上游约 312–317 行）。
 * 任一向量长度不超过 1e-5 时夹角为 0，避免零向量归一化后变成 90 度。其余余弦夹到 [-1, 1]。
 * 不移植 Camera、PoseStack、世界射线、Minecraft Vec3 重载和其余工具方法。
 */
package com.bslx.XueXiMod.animation.core.math;

import org.joml.Quaternionf;

public final class MathUtils {
    public static final float NEAR_ZERO_LENGTH = 1.0E-5F;

    private MathUtils() {
    }

    public static double getAngleBetween(Vec3f left, Vec3f right) {
        if (left == null || right == null || !left.validateValues() || !right.validateValues()) {
            throw new IllegalArgumentException("angle input must be finite");
        }
        if (left.length() <= NEAR_ZERO_LENGTH || right.length() <= NEAR_ZERO_LENGTH) {
            return 0.0D;
        }
        Vec3f normalLeft = Vec3f.normalize(left, null);
        Vec3f normalRight = Vec3f.normalize(right, null);
        double cosine = (double) normalLeft.coordinateX * normalRight.coordinateX
                + (double) normalLeft.coordinateY * normalRight.coordinateY
                + (double) normalLeft.coordinateZ * normalRight.coordinateZ;
        if (cosine > 1.0D) {
            cosine = 1.0D;
        } else if (cosine < -1.0D) {
            cosine = -1.0D;
        }
        if (!Double.isFinite(cosine)) {
            throw new IllegalArgumentException("angle between vectors is not finite");
        }
        return Math.toDegrees(Math.acos(cosine));
    }

    public static Vec3f lerpVector(Vec3f start, Vec3f end, float delta) {
        return lerpVector(start, end, delta, new Vec3f());
    }

    public static Vec3f lerpVector(Vec3f start, Vec3f end, float delta, Vec3f destination) {
        if (start == null || end == null || !start.validateValues() || !end.validateValues()) {
            throw new IllegalArgumentException("vector interpolation input must be finite");
        }
        FiniteChecks.requireFinite("vector delta", delta);
        if (destination == null) {
            destination = new Vec3f();
        }
        float nextX = start.coordinateX + (end.coordinateX - start.coordinateX) * delta;
        float nextY = start.coordinateY + (end.coordinateY - start.coordinateY) * delta;
        float nextZ = start.coordinateZ + (end.coordinateZ - start.coordinateZ) * delta;
        FiniteChecks.requireFiniteTriple("vector interpolation", nextX, nextY, nextZ);
        destination.coordinateX = nextX;
        destination.coordinateY = nextY;
        destination.coordinateZ = nextZ;
        return destination;
    }

    public static Quaternionf lerpQuaternion(Quaternionf from, Quaternionf toward, float delta) {
        return lerpQuaternion(from, toward, delta, null);
    }

    public static Quaternionf lerpQuaternion(Quaternionf from, Quaternionf toward, float delta, Quaternionf destination) {
        if (from == null || toward == null) {
            throw new IllegalArgumentException("quaternion interpolation input is missing");
        }
        FiniteChecks.requireFinite("quaternion delta", delta);
        FiniteChecks.requireFiniteQuaternion("quaternion from", from.x(), from.y(), from.z(), from.w());
        FiniteChecks.requireFiniteQuaternion("quaternion toward", toward.x(), toward.y(), toward.z(), toward.w());
        if (destination == null) {
            destination = new Quaternionf();
        }

        float fromX = from.x();
        float fromY = from.y();
        float fromZ = from.z();
        float fromW = from.w();
        float toX = toward.x();
        float toY = toward.y();
        float toZ = toward.z();
        float toW = toward.w();
        float dot = fromW * toW + fromX * toX + fromY * toY + fromZ * toZ;
        float blendInverse = 1.0F - delta;
        float resultX;
        float resultY;
        float resultZ;
        float resultW;
        if (dot < 0.0F) {
            resultW = blendInverse * fromW + delta * -toW;
            resultX = blendInverse * fromX + delta * -toX;
            resultY = blendInverse * fromY + delta * -toY;
            resultZ = blendInverse * fromZ + delta * -toZ;
        } else {
            resultW = blendInverse * fromW + delta * toW;
            resultX = blendInverse * fromX + delta * toX;
            resultY = blendInverse * fromY + delta * toY;
            resultZ = blendInverse * fromZ + delta * toZ;
        }

        destination.set(resultX, resultY, resultZ, resultW);
        destination.normalize();
        FiniteChecks.requireFiniteQuaternion(
                "quaternion interpolation",
                destination.x(),
                destination.y(),
                destination.z(),
                destination.w());
        return destination;
    }
}

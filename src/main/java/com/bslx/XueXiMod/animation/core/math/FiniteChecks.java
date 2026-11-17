/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Local original helper for the Epic Fight action-core port.
 * Copyright (C) 2026 XueXi Mod contributors.
 * Modified: 2026-10-24
 *
 * 与 GPLv3 兼容的本地数值门禁。不是 Epic Fight 源文件。
 * 采样、关键帧和变换在进入插值或矩阵前必须是有限数。
 */
package com.bslx.XueXiMod.animation.core.math;

public final class FiniteChecks {
    public static final float QUATERNION_LENGTH_EPSILON = 1.0E-8F;

    private FiniteChecks() {
    }

    public static float requireFinite(String label, float value) {
        if (!Float.isFinite(value)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        return value;
    }

    public static void requireFiniteTriple(String label, float first, float second, float third) {
        if (!Float.isFinite(first) || !Float.isFinite(second) || !Float.isFinite(third)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
    }

    public static void requireFiniteQuaternion(String label, float rotationX, float rotationY, float rotationZ, float rotationW) {
        if (!Float.isFinite(rotationX) || !Float.isFinite(rotationY) || !Float.isFinite(rotationZ) || !Float.isFinite(rotationW)) {
            throw new IllegalArgumentException(label + " must be finite");
        }
        float lengthSquared = rotationX * rotationX + rotationY * rotationY + rotationZ * rotationZ + rotationW * rotationW;
        if (!(lengthSquared > QUATERNION_LENGTH_EPSILON)) {
            throw new IllegalArgumentException(label + " length is too small to interpolate");
        }
    }

    public static void requireAllFinite(String label, float... elements) {
        if (elements == null) {
            throw new IllegalArgumentException(label + " is missing");
        }
        for (float element : elements) {
            if (!Float.isFinite(element)) {
                throw new IllegalArgumentException(label + " must be finite");
            }
        }
    }
}

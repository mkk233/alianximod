/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/utils/math/AnimationTransformEntry.java
 * Modified: 2026-10-24
 *
 * 修改说明：绑定顺序仍是 parent、关节局部、动画变换、front result、overwrite rotation。
 * Mojang Pair 换成本地记录，避免数据修复器依赖。同一槽位再次放入时仍按该槽已记录的乘法策略合并。
 */
package com.bslx.XueXiMod.animation.core.math;

import java.util.HashMap;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.JointTransform;

public class AnimationTransformEntry {
    private static final String[] BINDING_PRIORITY = {
            JointTransform.PARENT,
            JointTransform.JOINT_LOCAL_TRANSFORM,
            JointTransform.ANIMATION_TRANSFORM,
            JointTransform.RESULT1,
            JointTransform.RESULT2
    };
    private final Map<String, StoredMatrix> matrices = new HashMap<>();

    public void put(String entryPosition, OpenMatrix4f matrix) {
        this.put(entryPosition, matrix, OpenMatrix4f::mul);
    }

    public void put(String entryPosition, OpenMatrix4f matrix, MatrixOperation operation) {
        if (entryPosition == null || matrix == null || operation == null) {
            throw new IllegalArgumentException("animation matrix entry is missing");
        }
        if (!matrix.hasFiniteElements()) {
            throw new IllegalArgumentException("animation matrix entry must be finite");
        }
        StoredMatrix applied = this.matrices.get(entryPosition);
        if (applied != null) {
            OpenMatrix4f result = applied.operation().mul(applied.matrix(), matrix, null);
            this.matrices.put(entryPosition, new StoredMatrix(result, operation));
        } else {
            this.matrices.put(entryPosition, new StoredMatrix(new OpenMatrix4f(matrix), operation));
        }
    }

    public OpenMatrix4f getResult() {
        OpenMatrix4f result = new OpenMatrix4f();
        for (String entryName : BINDING_PRIORITY) {
            StoredMatrix stored = this.matrices.get(entryName);
            if (stored != null) {
                stored.operation().mul(result, stored.matrix(), result);
            }
        }
        if (!result.hasFiniteElements()) {
            throw new IllegalArgumentException("bound animation matrix must be finite");
        }
        return result;
    }

    private record StoredMatrix(OpenMatrix4f matrix, MatrixOperation operation) {
    }
}

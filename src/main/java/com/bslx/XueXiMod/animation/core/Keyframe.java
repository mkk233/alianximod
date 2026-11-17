/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/animation/Keyframe.java
 * Modified: 2026-10-24
 *
 * 修改说明：时间在构造时固定，不再提供 setTime。构造会复制变换，transform() 再返回副本。
 * 合法改时间要新建 Keyframe。非有限时间或变换仍直接失败。严格递增由 TransformSheet 检查。
 */
package com.bslx.XueXiMod.animation.core;

import com.bslx.XueXiMod.animation.core.math.FiniteChecks;

public class Keyframe {
    private final float timeStamp;
    private final JointTransform transform;

    public Keyframe(float timeStamp, JointTransform transform) {
        if (transform == null) {
            throw new IllegalArgumentException("keyframe transform is missing");
        }
        FiniteChecks.requireFinite("keyframe time", timeStamp);
        transform.requireFinite();
        this.timeStamp = timeStamp;
        this.transform = transform.copy();
    }

    public Keyframe(Keyframe original) {
        if (original == null) {
            throw new IllegalArgumentException("keyframe is missing");
        }
        this.timeStamp = original.timeStamp;
        this.transform = original.transform.copy();
    }

    public void copyFrom(Keyframe target) {
        if (target == null) {
            throw new IllegalArgumentException("keyframe is missing");
        }
        if (Float.compare(this.timeStamp, target.timeStamp) != 0) {
            throw new IllegalArgumentException("keyframe time is immutable");
        }
        target.transform.requireFinite();
        this.transform.copyFrom(target.transform);
    }

    public float time() {
        return this.timeStamp;
    }

    public JointTransform transform() {
        return this.transform.copy();
    }

    JointTransform ownedTransform() {
        return this.transform;
    }

    @Override
    public String toString() {
        return "Keyframe[Time: " + this.timeStamp + ", " + this.transform.toString() + "]";
    }

    public static Keyframe empty() {
        return new Keyframe(0.0F, JointTransform.empty());
    }
}

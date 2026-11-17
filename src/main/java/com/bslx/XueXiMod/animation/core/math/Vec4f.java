/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/utils/math/Vec4f.java
 * Modified: 2026-10-24
 *
 * 修改说明：OpenMatrix4f.transform / transform3v 的闭包依赖。Vec2f 父类已折进 Vec3f，不再单列。
 * 坐标字段改为 coordinateX/Y/Z/W，以通过项目命名门禁。拒绝非有限分量。
 */
package com.bslx.XueXiMod.animation.core.math;

public class Vec4f extends Vec3f {
    public float coordinateW;

    public Vec4f() {
        super();
        this.coordinateW = 0.0F;
    }

    public Vec4f(float coordinateX, float coordinateY, float coordinateZ, float coordinateW) {
        super(coordinateX, coordinateY, coordinateZ);
        FiniteChecks.requireFinite("vector", coordinateW);
        this.coordinateW = coordinateW;
    }

    public Vec4f(Vec3f source) {
        super(source.coordinateX, source.coordinateY, source.coordinateZ);
        this.coordinateW = 1.0F;
    }

    public void set(float coordinateX, float coordinateY, float coordinateZ, float coordinateW) {
        FiniteChecks.requireFiniteTriple("vector", coordinateX, coordinateY, coordinateZ);
        FiniteChecks.requireFinite("vector", coordinateW);
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
        this.coordinateZ = coordinateZ;
        this.coordinateW = coordinateW;
    }

    public void set(Vec4f source) {
        super.set(source);
        FiniteChecks.requireFinite("vector", source.coordinateW);
        this.coordinateW = source.coordinateW;
    }

    public Vec4f add(float coordinateX, float coordinateY, float coordinateZ, float coordinateW) {
        FiniteChecks.requireAllFinite("vector add", coordinateX, coordinateY, coordinateZ, coordinateW);
        this.coordinateX += coordinateX;
        this.coordinateY += coordinateY;
        this.coordinateZ += coordinateZ;
        this.coordinateW += coordinateW;
        if (!this.validateValues() || !Float.isFinite(this.coordinateW)) {
            throw new IllegalArgumentException("vector add result must be finite");
        }
        return this;
    }

    public static Vec4f add(Vec4f left, Vec4f right, Vec4f destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("vector add input is missing");
        }
        if (destination == null) {
            destination = new Vec4f();
        }
        destination.set(
                left.coordinateX + right.coordinateX,
                left.coordinateY + right.coordinateY,
                left.coordinateZ + right.coordinateZ,
                left.coordinateW + right.coordinateW);
        return destination;
    }

    @Override
    public Vec4f scale(float factor) {
        super.scale(factor);
        this.coordinateW *= factor;
        if (!Float.isFinite(this.coordinateW)) {
            throw new IllegalArgumentException("vector scale result must be finite");
        }
        return this;
    }

    public Vec4f transform(OpenMatrix4f matrix) {
        return OpenMatrix4f.transform(matrix, this, this);
    }

    @Override
    public String toString() {
        return "Vec4f[" + this.coordinateX + ", " + this.coordinateY + ", " + this.coordinateZ + ", " + this.coordinateW + "]";
    }
}

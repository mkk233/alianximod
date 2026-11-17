/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/utils/math/Vec3f.java
 * Parent folded in: src/main/java/yesman/epicfight/api/utils/math/Vec2f.java
 * Modified: 2026-10-24
 *
 * 修改说明：Vec2f 的平面分量并入本类。去掉 EpicFightMod 日志、Minecraft Vec3 互转和共享静态缓冲。
 * 零长度求旋转改为抛出非法参数，不再打印栈后继续。坐标字段为 coordinateX/Y/Z。
 * 变长参数 average 仍按上游写成乘以数量而不是除以数量。getMostSimilar 的点积括号也保持上游写法。
 */
package com.bslx.XueXiMod.animation.core.math;

import java.util.Collection;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;

public class Vec3f {
    public static final Vec3f X_AXIS = new Vec3f(1.0F, 0.0F, 0.0F);
    public static final Vec3f Y_AXIS = new Vec3f(0.0F, 1.0F, 0.0F);
    public static final Vec3f Z_AXIS = new Vec3f(0.0F, 0.0F, 1.0F);
    public static final Vec3f M_X_AXIS = new Vec3f(-1.0F, 0.0F, 0.0F);
    public static final Vec3f M_Y_AXIS = new Vec3f(0.0F, -1.0F, 0.0F);
    public static final Vec3f M_Z_AXIS = new Vec3f(0.0F, 0.0F, -1.0F);
    public static final Vec3f ZERO = new Vec3f(0.0F, 0.0F, 0.0F);

    public float coordinateX;
    public float coordinateY;
    public float coordinateZ;

    public Vec3f() {
        this.coordinateX = 0.0F;
        this.coordinateY = 0.0F;
        this.coordinateZ = 0.0F;
    }

    public Vec3f(float coordinateX, float coordinateY, float coordinateZ) {
        FiniteChecks.requireFiniteTriple("vector", coordinateX, coordinateY, coordinateZ);
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
        this.coordinateZ = coordinateZ;
    }

    public Vec3f(double coordinateX, double coordinateY, double coordinateZ) {
        this((float) coordinateX, (float) coordinateY, (float) coordinateZ);
    }

    public Vec3f set(float coordinateX, float coordinateY, float coordinateZ) {
        FiniteChecks.requireFiniteTriple("vector", coordinateX, coordinateY, coordinateZ);
        this.coordinateX = coordinateX;
        this.coordinateY = coordinateY;
        this.coordinateZ = coordinateZ;
        return this;
    }

    public Vec3f set(Vec3f source) {
        if (source == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        return this.set(source.coordinateX, source.coordinateY, source.coordinateZ);
    }

    public Vec3f add(float coordinateX, float coordinateY, float coordinateZ) {
        FiniteChecks.requireFiniteTriple("vector add", coordinateX, coordinateY, coordinateZ);
        float nextX = this.coordinateX + coordinateX;
        float nextY = this.coordinateY + coordinateY;
        float nextZ = this.coordinateZ + coordinateZ;
        return this.set(nextX, nextY, nextZ);
    }

    public Vec3f add(Vec3f vector) {
        if (vector == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        return this.add(vector.coordinateX, vector.coordinateY, vector.coordinateZ);
    }

    public Vec3f sub(float coordinateX, float coordinateY, float coordinateZ) {
        FiniteChecks.requireFiniteTriple("vector subtract", coordinateX, coordinateY, coordinateZ);
        return this.set(this.coordinateX - coordinateX, this.coordinateY - coordinateY, this.coordinateZ - coordinateZ);
    }

    public Vec3f sub(Vec3f vector) {
        if (vector == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        return this.sub(vector.coordinateX, vector.coordinateY, vector.coordinateZ);
    }

    public static Vec3f add(Vec3f left, Vec3f right, Vec3f destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("vector add input is missing");
        }
        if (destination == null) {
            return new Vec3f(left.coordinateX + right.coordinateX, left.coordinateY + right.coordinateY, left.coordinateZ + right.coordinateZ);
        }
        destination.set(left.coordinateX + right.coordinateX, left.coordinateY + right.coordinateY, left.coordinateZ + right.coordinateZ);
        return destination;
    }

    public static Vec3f sub(Vec3f left, Vec3f right, Vec3f destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("vector subtract input is missing");
        }
        if (destination == null) {
            return new Vec3f(left.coordinateX - right.coordinateX, left.coordinateY - right.coordinateY, left.coordinateZ - right.coordinateZ);
        }
        destination.set(left.coordinateX - right.coordinateX, left.coordinateY - right.coordinateY, left.coordinateZ - right.coordinateZ);
        return destination;
    }

    public Vec3f multiply(Vec3f vector) {
        if (vector == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        return multiply(this, this, vector.coordinateX, vector.coordinateY, vector.coordinateZ);
    }

    public Vec3f multiply(float factorX, float factorY, float factorZ) {
        return multiply(this, this, factorX, factorY, factorZ);
    }

    public static Vec3f multiply(Vec3f source, Vec3f destination, float factorX, float factorY, float factorZ) {
        if (source == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        FiniteChecks.requireFiniteTriple("vector multiply", factorX, factorY, factorZ);
        if (destination == null) {
            destination = new Vec3f();
        }
        destination.set(source.coordinateX * factorX, source.coordinateY * factorY, source.coordinateZ * factorZ);
        return destination;
    }

    public Vec3f scale(float factor) {
        return scale(this, this, factor);
    }

    public static Vec3f scale(Vec3f source, Vec3f destination, float factor) {
        if (source == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        FiniteChecks.requireFinite("vector scale", factor);
        if (destination == null) {
            destination = new Vec3f();
        }
        destination.set(source.coordinateX * factor, source.coordinateY * factor, source.coordinateZ * factor);
        return destination;
    }

    public Vec3f copy() {
        return new Vec3f(this.coordinateX, this.coordinateY, this.coordinateZ);
    }

    public float length() {
        return (float) Math.sqrt(this.lengthSqr());
    }

    public float lengthSqr() {
        return this.coordinateX * this.coordinateX + this.coordinateY * this.coordinateY + this.coordinateZ * this.coordinateZ;
    }

    public float distance(Vec3f opponent) {
        return (float) Math.sqrt(this.distanceSqr(opponent));
    }

    public float distanceSqr(Vec3f opponent) {
        if (opponent == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        float deltaX = this.coordinateX - opponent.coordinateX;
        float deltaY = this.coordinateY - opponent.coordinateY;
        float deltaZ = this.coordinateZ - opponent.coordinateZ;
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }

    public float horizontalDistance() {
        return (float) Math.sqrt(this.horizontalDistanceSqr());
    }

    public float horizontalDistanceSqr() {
        return this.coordinateX * this.coordinateX + this.coordinateZ * this.coordinateZ;
    }

    public void rotate(float degree, Vec3f axis) {
        rotate(degree, axis, this, this);
    }

    public void invalidate() {
        this.coordinateX = Float.NaN;
        this.coordinateY = Float.NaN;
        this.coordinateZ = Float.NaN;
    }

    public boolean validateValues() {
        return Float.isFinite(this.coordinateX) && Float.isFinite(this.coordinateY) && Float.isFinite(this.coordinateZ);
    }

    public static Vec3f rotate(float degree, Vec3f axis, Vec3f source, Vec3f destination) {
        if (destination == null) {
            destination = new Vec3f();
        }
        return OpenMatrix4f.transform3v(OpenMatrix4f.createRotatorDeg(degree, axis), source, destination);
    }

    public static Vec3f rotate(Quaternionf rotation, Vec3f source, Vec3f destination) {
        if (rotation == null || source == null) {
            throw new IllegalArgumentException("quaternion rotation input is missing");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        Vector3f sourceStorage = new Vector3f(source.coordinateX, source.coordinateY, source.coordinateZ);
        Vector3f transformed = new Vector3f();
        rotation.transform(sourceStorage, transformed);
        destination.set(transformed.x(), transformed.y(), transformed.z());
        return destination;
    }

    public static float dot(Vec3f left, Vec3f right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("vector dot input is missing");
        }
        return left.coordinateX * right.coordinateX + left.coordinateY * right.coordinateY + left.coordinateZ * right.coordinateZ;
    }

    public static Vec3f cross(Vec3f left, Vec3f right, Vec3f destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("vector cross input is missing");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        destination.set(
                left.coordinateY * right.coordinateZ - left.coordinateZ * right.coordinateY,
                right.coordinateX * left.coordinateZ - right.coordinateZ * left.coordinateX,
                left.coordinateX * right.coordinateY - left.coordinateY * right.coordinateX);
        return destination;
    }

    public static float getAngleBetween(Vec3f left, Vec3f right) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("angle input is missing");
        }
        float leftLength = left.length();
        float rightLength = right.length();
        if (leftLength <= 1.0E-5F || rightLength <= 1.0E-5F) {
            return 0.0F;
        }
        float cosine = Math.min(1.0F, Vec3f.dot(left, right) / (leftLength * rightLength));
        if (cosine < -1.0F) {
            cosine = -1.0F;
        }
        return (float) Math.acos(cosine);
    }

    public static Quaternionf getRotatorBetween(Vec3f left, Vec3f right, Quaternionf destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("rotator input is missing");
        }
        if (destination == null) {
            destination = new Quaternionf();
        }
        Vec3f axis = Vec3f.cross(left, right, null).normalize();
        float leftLength = left.length();
        float rightLength = right.length();
        if (!(leftLength > 0.0F) || !(rightLength > 0.0F)) {
            throw new IllegalArgumentException("vector length is zero");
        }
        float dotDivLength = Vec3f.dot(left, right) / (leftLength * rightLength);
        if (!Float.isFinite(dotDivLength)) {
            throw new IllegalArgumentException("vector length is zero");
        }
        if (dotDivLength > 1.0F) {
            dotDivLength = 1.0F;
        } else if (dotDivLength < -1.0F) {
            dotDivLength = -1.0F;
        }
        float radian = (float) Math.acos(dotDivLength);
        destination.setAngleAxis(radian, axis.coordinateX, axis.coordinateY, axis.coordinateZ);
        FiniteChecks.requireFiniteQuaternion("vector rotator", destination.x(), destination.y(), destination.z(), destination.w());
        return destination;
    }

    public static Vec3f interpolate(Vec3f from, Vec3f toward, float interpolation, Vec3f destination) {
        return MathUtils.lerpVector(from, toward, interpolation, destination);
    }

    public Vec3f normalize() {
        return normalize(this, this);
    }

    public static Vec3f normalize(Vec3f source, Vec3f destination) {
        if (source == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        float norm = (float) Math.sqrt(source.coordinateX * source.coordinateX + source.coordinateY * source.coordinateY + source.coordinateZ * source.coordinateZ);
        if (norm > 1.0E-5F) {
            destination.set(source.coordinateX / norm, source.coordinateY / norm, source.coordinateZ / norm);
        } else {
            destination.set(0.0F, 0.0F, 0.0F);
        }
        return destination;
    }

    @Override
    public String toString() {
        return "[" + this.coordinateX + ", " + this.coordinateY + ", " + this.coordinateZ + "]";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        } else if (other instanceof Vec3f vector) {
            return Float.compare(this.coordinateX, vector.coordinateX) == 0
                    && Float.compare(this.coordinateY, vector.coordinateY) == 0
                    && Float.compare(this.coordinateZ, vector.coordinateZ) == 0;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int mixed = Float.floatToIntBits(this.coordinateX);
        int accumulated = mixed;
        mixed = Float.floatToIntBits(this.coordinateY);
        accumulated = 31 * accumulated + mixed;
        mixed = Float.floatToIntBits(this.coordinateZ);
        return 31 * accumulated + mixed;
    }

    public static Vec3f average(Collection<Vec3f> vectors, Vec3f destination) {
        if (vectors == null || vectors.isEmpty()) {
            throw new IllegalArgumentException("cannot average an empty vector collection");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        destination.set(0.0F, 0.0F, 0.0F);
        for (Vec3f vector : vectors) {
            destination.add(vector);
        }
        destination.scale(1.0F / vectors.size());
        return destination;
    }

    public static Vec3f average(Vec3f destination, Vec3f... vectors) {
        if (vectors == null || vectors.length == 0) {
            throw new IllegalArgumentException("cannot average an empty vector collection");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        destination.set(0.0F, 0.0F, 0.0F);
        for (Vec3f vector : vectors) {
            destination.add(vector);
        }
        destination.scale(vectors.length);
        return destination;
    }

    public static int getNearest(Vec3f from, List<Vec3f> vectors) {
        if (from == null || vectors == null) {
            throw new IllegalArgumentException("nearest vector input is missing");
        }
        float nearestDistance = Float.MAX_VALUE;
        int nearest = -1;
        for (int vectorIndex = 0; vectorIndex < vectors.size(); vectorIndex++) {
            Vec3f candidate = vectors.get(vectorIndex);
            if (candidate == null || !candidate.validateValues()) {
                continue;
            }
            float distanceSquared = from.distanceSqr(candidate);
            if (distanceSquared < nearestDistance) {
                nearestDistance = distanceSquared;
                nearest = vectorIndex;
            }
        }
        return nearest;
    }

    public static int getNearest(Vec3f from, Vec3f... vectors) {
        if (from == null || vectors == null) {
            throw new IllegalArgumentException("nearest vector input is missing");
        }
        float nearestDistance = Float.MAX_VALUE;
        int nearest = -1;
        for (int vectorIndex = 0; vectorIndex < vectors.length; vectorIndex++) {
            Vec3f candidate = vectors[vectorIndex];
            if (candidate == null || !candidate.validateValues()) {
                continue;
            }
            float distanceSquared = from.distanceSqr(candidate);
            if (distanceSquared < nearestDistance) {
                nearestDistance = distanceSquared;
                nearest = vectorIndex;
            }
        }
        return nearest;
    }

    public static int getMostSimilar(Vec3f start, Vec3f end, Vec3f... vectors) {
        if (start == null || end == null || vectors == null) {
            throw new IllegalArgumentException("similar vector input is missing");
        }
        Vec3f basisDirection = Vec3f.sub(end, start, null);
        if (!(basisDirection.length() > 0.0F)) {
            throw new IllegalArgumentException("similar vector baseline length is zero");
        }
        float highestDot = Float.NEGATIVE_INFINITY;
        int nearest = -1;
        for (int vectorIndex = 0; vectorIndex < vectors.length; vectorIndex++) {
            Vec3f candidate = vectors[vectorIndex];
            if (candidate == null || !candidate.validateValues()) {
                continue;
            }
            Vec3f comparison = Vec3f.sub(candidate, start, null);
            float dot = Vec3f.dot(basisDirection, comparison) / basisDirection.length() * comparison.length();
            if (dot > highestDot) {
                highestDot = dot;
                nearest = vectorIndex;
            }
        }
        return nearest;
    }

    public static int getMostSimilar(Vec3f start, Vec3f end, List<Vec3f> vectors) {
        if (start == null || end == null || vectors == null) {
            throw new IllegalArgumentException("similar vector input is missing");
        }
        Vec3f basisDirection = Vec3f.sub(end, start, null);
        if (!(basisDirection.length() > 0.0F)) {
            throw new IllegalArgumentException("similar vector baseline length is zero");
        }
        float highestDot = Float.NEGATIVE_INFINITY;
        int nearest = -1;
        for (int vectorIndex = 0; vectorIndex < vectors.size(); vectorIndex++) {
            Vec3f candidate = vectors.get(vectorIndex);
            if (candidate == null || !candidate.validateValues()) {
                continue;
            }
            Vec3f comparison = Vec3f.sub(candidate, start, null);
            float dot = Vec3f.dot(basisDirection, comparison) / basisDirection.length() * comparison.length();
            if (dot > highestDot) {
                highestDot = dot;
                nearest = vectorIndex;
            }
        }
        return nearest;
    }

    public Vector3f toMojangVector() {
        return new Vector3f(this.coordinateX, this.coordinateY, this.coordinateZ);
    }

    public static Vec3f fromMojangVector(Vector3f source) {
        if (source == null) {
            throw new IllegalArgumentException("vector is missing");
        }
        return new Vec3f(source.x(), source.y(), source.z());
    }

    public Vec3f rotateDegree(Vec3f axis, float degree) {
        OpenMatrix4f destination = new OpenMatrix4f();
        OpenMatrix4f.ofRotationDegree(degree, axis, destination);
        OpenMatrix4f.transform3v(destination, this, this);
        return this;
    }
}

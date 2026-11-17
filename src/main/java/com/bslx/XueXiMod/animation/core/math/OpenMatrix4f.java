/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/utils/math/OpenMatrix4f.java
 * Modified: 2026-10-24
 *
 * 修改说明：去掉 Minecraft Vec3 变换和共享静态缓冲。不可逆或结果非有限时抛出非法参数，
 * 不再把行列式为 0 的矩阵改写成单位矩阵，也不把 NaN 留给后续渲染。
 * 乘法、求逆、平移、旋转、缩放、四元数互转的公式保持上游。
 * 由 JointTransform 构造时仍是先旋转、再平移、再缩放；JointTransform.toMatrix 的顺序另见该类。
 */
package com.bslx.XueXiMod.animation.core.math;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.joml.Math;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import com.bslx.XueXiMod.animation.core.JointTransform;

public class OpenMatrix4f {
    public static final OpenMatrix4f IDENTITY = new OpenMatrix4f().unmodifiable();

    /*
     * m00 m01 m02 m03
     * m10 m11 m12 m13
     * m20 m21 m22 m23
     * m30 m31 m32 m33
     */
    public float m00, m01, m02, m03, m10, m11, m12, m13, m20, m21, m22, m23, m30, m31, m32, m33;

    private final boolean immutable;

    public OpenMatrix4f() {
        this.setIdentity();
        this.immutable = false;
    }

    public OpenMatrix4f(final OpenMatrix4f source) {
        this(source, false);
    }

    public OpenMatrix4f(final OpenMatrix4f source, boolean immutable) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        this.m00 = source.m00;
        this.m01 = source.m01;
        this.m02 = source.m02;
        this.m03 = source.m03;
        this.m10 = source.m10;
        this.m11 = source.m11;
        this.m12 = source.m12;
        this.m13 = source.m13;
        this.m20 = source.m20;
        this.m21 = source.m21;
        this.m22 = source.m22;
        this.m23 = source.m23;
        this.m30 = source.m30;
        this.m31 = source.m31;
        this.m32 = source.m32;
        this.m33 = source.m33;
        this.immutable = immutable;
    }

    public OpenMatrix4f(final JointTransform jointTransform) {
        if (jointTransform == null) {
            throw new IllegalArgumentException("joint transform is missing");
        }
        this.load(OpenMatrix4f.fromQuaternion(jointTransform.rotation()).translate(jointTransform.translation()).scale(jointTransform.scale()));
        this.immutable = false;
    }

    public OpenMatrix4f(
            float m00, float m01, float m02, float m03,
            float m10, float m11, float m12, float m13,
            float m20, float m21, float m22, float m23,
            float m30, float m31, float m32, float m33) {
        FiniteChecks.requireAllFinite(
                "matrix",
                m00, m01, m02, m03,
                m10, m11, m12, m13,
                m20, m21, m22, m23,
                m30, m31, m32, m33);
        this.m00 = m00;
        this.m01 = m01;
        this.m02 = m02;
        this.m03 = m03;
        this.m10 = m10;
        this.m11 = m11;
        this.m12 = m12;
        this.m13 = m13;
        this.m20 = m20;
        this.m21 = m21;
        this.m22 = m22;
        this.m23 = m23;
        this.m30 = m30;
        this.m31 = m31;
        this.m32 = m32;
        this.m33 = m33;
        this.immutable = false;
    }

    public boolean hasFiniteElements() {
        return Float.isFinite(this.m00) && Float.isFinite(this.m01) && Float.isFinite(this.m02) && Float.isFinite(this.m03)
                && Float.isFinite(this.m10) && Float.isFinite(this.m11) && Float.isFinite(this.m12) && Float.isFinite(this.m13)
                && Float.isFinite(this.m20) && Float.isFinite(this.m21) && Float.isFinite(this.m22) && Float.isFinite(this.m23)
                && Float.isFinite(this.m30) && Float.isFinite(this.m31) && Float.isFinite(this.m32) && Float.isFinite(this.m33);
    }

    public OpenMatrix4f setIdentity() {
        return setIdentity(this);
    }

    public static OpenMatrix4f setIdentity(OpenMatrix4f matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (matrix.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        matrix.m00 = 1.0F;
        matrix.m01 = 0.0F;
        matrix.m02 = 0.0F;
        matrix.m03 = 0.0F;
        matrix.m10 = 0.0F;
        matrix.m11 = 1.0F;
        matrix.m12 = 0.0F;
        matrix.m13 = 0.0F;
        matrix.m20 = 0.0F;
        matrix.m21 = 0.0F;
        matrix.m22 = 1.0F;
        matrix.m23 = 0.0F;
        matrix.m30 = 0.0F;
        matrix.m31 = 0.0F;
        matrix.m32 = 0.0F;
        matrix.m33 = 1.0F;
        return matrix;
    }

    public OpenMatrix4f load(OpenMatrix4f source) {
        return load(source, this);
    }

    public static OpenMatrix4f load(OpenMatrix4f source, OpenMatrix4f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        destination.m00 = source.m00;
        destination.m01 = source.m01;
        destination.m02 = source.m02;
        destination.m03 = source.m03;
        destination.m10 = source.m10;
        destination.m11 = source.m11;
        destination.m12 = source.m12;
        destination.m13 = source.m13;
        destination.m20 = source.m20;
        destination.m21 = source.m21;
        destination.m22 = source.m22;
        destination.m23 = source.m23;
        destination.m30 = source.m30;
        destination.m31 = source.m31;
        destination.m32 = source.m32;
        destination.m33 = source.m33;
        return destination;
    }

    public static OpenMatrix4f load(OpenMatrix4f destination, float[] elements) {
        if (elements == null || elements.length < 16) {
            throw new IllegalArgumentException("matrix elements must contain 16 values");
        }
        FiniteChecks.requireAllFinite(
                "matrix",
                elements[0], elements[1], elements[2], elements[3],
                elements[4], elements[5], elements[6], elements[7],
                elements[8], elements[9], elements[10], elements[11],
                elements[12], elements[13], elements[14], elements[15]);
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        destination.m00 = elements[0];
        destination.m01 = elements[1];
        destination.m02 = elements[2];
        destination.m03 = elements[3];
        destination.m10 = elements[4];
        destination.m11 = elements[5];
        destination.m12 = elements[6];
        destination.m13 = elements[7];
        destination.m20 = elements[8];
        destination.m21 = elements[9];
        destination.m22 = elements[10];
        destination.m23 = elements[11];
        destination.m30 = elements[12];
        destination.m31 = elements[13];
        destination.m32 = elements[14];
        destination.m33 = elements[15];
        return destination;
    }

    public static OpenMatrix4f load(OpenMatrix4f destination, FloatBuffer buffer) {
        if (buffer == null || buffer.capacity() < 16) {
            throw new IllegalArgumentException("matrix buffer must hold 16 floats");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        buffer.position(0);
        destination.m00 = buffer.get();
        destination.m01 = buffer.get();
        destination.m02 = buffer.get();
        destination.m03 = buffer.get();
        destination.m10 = buffer.get();
        destination.m11 = buffer.get();
        destination.m12 = buffer.get();
        destination.m13 = buffer.get();
        destination.m20 = buffer.get();
        destination.m21 = buffer.get();
        destination.m22 = buffer.get();
        destination.m23 = buffer.get();
        destination.m30 = buffer.get();
        destination.m31 = buffer.get();
        destination.m32 = buffer.get();
        destination.m33 = buffer.get();
        if (!destination.hasFiniteElements()) {
            throw new IllegalArgumentException("matrix buffer must be finite");
        }
        return destination;
    }

    public OpenMatrix4f load(FloatBuffer buffer) {
        return OpenMatrix4f.load(this, buffer);
    }

    public OpenMatrix4f store(FloatBuffer buffer) {
        if (buffer == null || buffer.remaining() < 16) {
            throw new IllegalArgumentException("matrix buffer must hold 16 floats");
        }
        buffer.put(this.m00);
        buffer.put(this.m01);
        buffer.put(this.m02);
        buffer.put(this.m03);
        buffer.put(this.m10);
        buffer.put(this.m11);
        buffer.put(this.m12);
        buffer.put(this.m13);
        buffer.put(this.m20);
        buffer.put(this.m21);
        buffer.put(this.m22);
        buffer.put(this.m23);
        buffer.put(this.m30);
        buffer.put(this.m31);
        buffer.put(this.m32);
        buffer.put(this.m33);
        return this;
    }

    public List<Float> toList() {
        List<Float> elements = new ArrayList<>(16);
        elements.add(this.m00);
        elements.add(this.m01);
        elements.add(this.m02);
        elements.add(this.m03);
        elements.add(this.m10);
        elements.add(this.m11);
        elements.add(this.m12);
        elements.add(this.m13);
        elements.add(this.m20);
        elements.add(this.m21);
        elements.add(this.m22);
        elements.add(this.m23);
        elements.add(this.m30);
        elements.add(this.m31);
        elements.add(this.m32);
        elements.add(this.m33);
        return elements;
    }

    public OpenMatrix4f unmodifiable() {
        return new OpenMatrix4f(this, true);
    }

    public static OpenMatrix4f add(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("matrix add input is missing");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float out00 = left.m00 + right.m00;
        float out01 = left.m01 + right.m01;
        float out02 = left.m02 + right.m02;
        float out03 = left.m03 + right.m03;
        float out10 = left.m10 + right.m10;
        float out11 = left.m11 + right.m11;
        float out12 = left.m12 + right.m12;
        float out13 = left.m13 + right.m13;
        float out20 = left.m20 + right.m20;
        float out21 = left.m21 + right.m21;
        float out22 = left.m22 + right.m22;
        float out23 = left.m23 + right.m23;
        float out30 = left.m30 + right.m30;
        float out31 = left.m31 + right.m31;
        float out32 = left.m32 + right.m32;
        float out33 = left.m33 + right.m33;
        FiniteChecks.requireAllFinite(
                "matrix add",
                out00, out01, out02, out03,
                out10, out11, out12, out13,
                out20, out21, out22, out23,
                out30, out31, out32, out33);
        destination.m00 = out00;
        destination.m01 = out01;
        destination.m02 = out02;
        destination.m03 = out03;
        destination.m10 = out10;
        destination.m11 = out11;
        destination.m12 = out12;
        destination.m13 = out13;
        destination.m20 = out20;
        destination.m21 = out21;
        destination.m22 = out22;
        destination.m23 = out23;
        destination.m30 = out30;
        destination.m31 = out31;
        destination.m32 = out32;
        destination.m33 = out33;
        return destination;
    }

    public OpenMatrix4f mulFront(OpenMatrix4f mulTransform) {
        return OpenMatrix4f.mul(mulTransform, this, this);
    }

    public OpenMatrix4f mulBack(OpenMatrix4f mulTransform) {
        return OpenMatrix4f.mul(this, mulTransform, this);
    }

    public static OpenMatrix4f mul(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f destination) {
        if (left == null || right == null) {
            throw new IllegalArgumentException("matrix product input is missing");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float out00 = left.m00 * right.m00 + left.m10 * right.m01 + left.m20 * right.m02 + left.m30 * right.m03;
        float out01 = left.m01 * right.m00 + left.m11 * right.m01 + left.m21 * right.m02 + left.m31 * right.m03;
        float out02 = left.m02 * right.m00 + left.m12 * right.m01 + left.m22 * right.m02 + left.m32 * right.m03;
        float out03 = left.m03 * right.m00 + left.m13 * right.m01 + left.m23 * right.m02 + left.m33 * right.m03;
        float out10 = left.m00 * right.m10 + left.m10 * right.m11 + left.m20 * right.m12 + left.m30 * right.m13;
        float out11 = left.m01 * right.m10 + left.m11 * right.m11 + left.m21 * right.m12 + left.m31 * right.m13;
        float out12 = left.m02 * right.m10 + left.m12 * right.m11 + left.m22 * right.m12 + left.m32 * right.m13;
        float out13 = left.m03 * right.m10 + left.m13 * right.m11 + left.m23 * right.m12 + left.m33 * right.m13;
        float out20 = left.m00 * right.m20 + left.m10 * right.m21 + left.m20 * right.m22 + left.m30 * right.m23;
        float out21 = left.m01 * right.m20 + left.m11 * right.m21 + left.m21 * right.m22 + left.m31 * right.m23;
        float out22 = left.m02 * right.m20 + left.m12 * right.m21 + left.m22 * right.m22 + left.m32 * right.m23;
        float out23 = left.m03 * right.m20 + left.m13 * right.m21 + left.m23 * right.m22 + left.m33 * right.m23;
        float out30 = left.m00 * right.m30 + left.m10 * right.m31 + left.m20 * right.m32 + left.m30 * right.m33;
        float out31 = left.m01 * right.m30 + left.m11 * right.m31 + left.m21 * right.m32 + left.m31 * right.m33;
        float out32 = left.m02 * right.m30 + left.m12 * right.m31 + left.m22 * right.m32 + left.m32 * right.m33;
        float out33 = left.m03 * right.m30 + left.m13 * right.m31 + left.m23 * right.m32 + left.m33 * right.m33;
        FiniteChecks.requireAllFinite(
                "matrix product",
                out00, out01, out02, out03,
                out10, out11, out12, out13,
                out20, out21, out22, out23,
                out30, out31, out32, out33);
        destination.m00 = out00;
        destination.m01 = out01;
        destination.m02 = out02;
        destination.m03 = out03;
        destination.m10 = out10;
        destination.m11 = out11;
        destination.m12 = out12;
        destination.m13 = out13;
        destination.m20 = out20;
        destination.m21 = out21;
        destination.m22 = out22;
        destination.m23 = out23;
        destination.m30 = out30;
        destination.m31 = out31;
        destination.m32 = out32;
        destination.m33 = out33;
        return destination;
    }

    public static OpenMatrix4f mulMatrices(OpenMatrix4f... sources) {
        OpenMatrix4f result = new OpenMatrix4f();
        if (sources == null) {
            throw new IllegalArgumentException("matrix product input is missing");
        }
        for (OpenMatrix4f source : sources) {
            result.mulBack(source);
        }
        return result;
    }

    public static OpenMatrix4f mulAsOrigin(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f destination) {
        if (right == null) {
            throw new IllegalArgumentException("matrix product input is missing");
        }
        float preservedX = right.m30;
        float preservedY = right.m31;
        float preservedZ = right.m32;
        OpenMatrix4f result = mul(left, right, destination);
        result.m30 = preservedX;
        result.m31 = preservedY;
        result.m32 = preservedZ;
        if (!result.hasFiniteElements()) {
            throw new IllegalArgumentException("origin matrix product must be finite");
        }
        return result;
    }

    public static OpenMatrix4f mulAsOriginInverse(OpenMatrix4f left, OpenMatrix4f right, OpenMatrix4f destination) {
        return mulAsOrigin(right, left, destination);
    }

    public static Vec4f transform(OpenMatrix4f matrix, Vec4f source, Vec4f destination) {
        if (matrix == null || source == null) {
            throw new IllegalArgumentException("matrix vector input is missing");
        }
        if (destination == null) {
            destination = new Vec4f();
        }
        float transformedX = matrix.m00 * source.coordinateX + matrix.m10 * source.coordinateY + matrix.m20 * source.coordinateZ + matrix.m30 * source.coordinateW;
        float transformedY = matrix.m01 * source.coordinateX + matrix.m11 * source.coordinateY + matrix.m21 * source.coordinateZ + matrix.m31 * source.coordinateW;
        float transformedZ = matrix.m02 * source.coordinateX + matrix.m12 * source.coordinateY + matrix.m22 * source.coordinateZ + matrix.m32 * source.coordinateW;
        float transformedW = matrix.m03 * source.coordinateX + matrix.m13 * source.coordinateY + matrix.m23 * source.coordinateZ + matrix.m33 * source.coordinateW;
        destination.set(transformedX, transformedY, transformedZ, transformedW);
        return destination;
    }

    public static Vec3f transform3v(OpenMatrix4f matrix, Vec3f source, Vec3f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix vector input is missing");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        Vec4f weighted = new Vec4f(source.coordinateX, source.coordinateY, source.coordinateZ, 1.0F);
        Vec4f result = transform(matrix, weighted, null);
        destination.set(result.coordinateX, result.coordinateY, result.coordinateZ);
        return destination;
    }

    public OpenMatrix4f transpose() {
        return transpose(this);
    }

    public OpenMatrix4f transpose(OpenMatrix4f destination) {
        return transpose(this, destination);
    }

    public static OpenMatrix4f transpose(OpenMatrix4f source, OpenMatrix4f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float out00 = source.m00;
        float out01 = source.m10;
        float out02 = source.m20;
        float out03 = source.m30;
        float out10 = source.m01;
        float out11 = source.m11;
        float out12 = source.m21;
        float out13 = source.m31;
        float out20 = source.m02;
        float out21 = source.m12;
        float out22 = source.m22;
        float out23 = source.m32;
        float out30 = source.m03;
        float out31 = source.m13;
        float out32 = source.m23;
        float out33 = source.m33;
        FiniteChecks.requireAllFinite(
                "matrix transpose",
                out00, out01, out02, out03,
                out10, out11, out12, out13,
                out20, out21, out22, out23,
                out30, out31, out32, out33);
        destination.m00 = out00;
        destination.m01 = out01;
        destination.m02 = out02;
        destination.m03 = out03;
        destination.m10 = out10;
        destination.m11 = out11;
        destination.m12 = out12;
        destination.m13 = out13;
        destination.m20 = out20;
        destination.m21 = out21;
        destination.m22 = out22;
        destination.m23 = out23;
        destination.m30 = out30;
        destination.m31 = out31;
        destination.m32 = out32;
        destination.m33 = out33;
        return destination;
    }

    public float determinant() {
        float term = this.m00 * ((this.m11 * this.m22 * this.m33 + this.m12 * this.m23 * this.m31 + this.m13 * this.m21 * this.m32)
                - this.m13 * this.m22 * this.m31 - this.m11 * this.m23 * this.m32 - this.m12 * this.m21 * this.m33);
        term -= this.m01 * ((this.m10 * this.m22 * this.m33 + this.m12 * this.m23 * this.m30 + this.m13 * this.m20 * this.m32)
                - this.m13 * this.m22 * this.m30 - this.m10 * this.m23 * this.m32 - this.m12 * this.m20 * this.m33);
        term += this.m02 * ((this.m10 * this.m21 * this.m33 + this.m11 * this.m23 * this.m30 + this.m13 * this.m20 * this.m31)
                - this.m13 * this.m21 * this.m30 - this.m10 * this.m23 * this.m31 - this.m11 * this.m20 * this.m33);
        term -= this.m03 * ((this.m10 * this.m21 * this.m32 + this.m11 * this.m22 * this.m30 + this.m12 * this.m20 * this.m31)
                - this.m12 * this.m21 * this.m30 - this.m10 * this.m22 * this.m31 - this.m11 * this.m20 * this.m32);
        return term;
    }

    private static float determinant3x3(
            float t00, float t01, float t02,
            float t10, float t11, float t12,
            float t20, float t21, float t22) {
        return t00 * (t11 * t22 - t12 * t21) + t01 * (t12 * t20 - t10 * t22) + t02 * (t10 * t21 - t11 * t20);
    }

    public OpenMatrix4f invert() {
        return OpenMatrix4f.invert(this, this);
    }

    public static OpenMatrix4f invert(OpenMatrix4f source, OpenMatrix4f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        float determinant = source.determinant();
        if (!Float.isFinite(determinant) || determinant == 0.0F) {
            throw new IllegalArgumentException("matrix is not invertible");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float determinantInverse = 1.0F / determinant;
        float t00 = determinant3x3(source.m11, source.m12, source.m13, source.m21, source.m22, source.m23, source.m31, source.m32, source.m33);
        float t01 = -determinant3x3(source.m10, source.m12, source.m13, source.m20, source.m22, source.m23, source.m30, source.m32, source.m33);
        float t02 = determinant3x3(source.m10, source.m11, source.m13, source.m20, source.m21, source.m23, source.m30, source.m31, source.m33);
        float t03 = -determinant3x3(source.m10, source.m11, source.m12, source.m20, source.m21, source.m22, source.m30, source.m31, source.m32);
        float t10 = -determinant3x3(source.m01, source.m02, source.m03, source.m21, source.m22, source.m23, source.m31, source.m32, source.m33);
        float t11 = determinant3x3(source.m00, source.m02, source.m03, source.m20, source.m22, source.m23, source.m30, source.m32, source.m33);
        float t12 = -determinant3x3(source.m00, source.m01, source.m03, source.m20, source.m21, source.m23, source.m30, source.m31, source.m33);
        float t13 = determinant3x3(source.m00, source.m01, source.m02, source.m20, source.m21, source.m22, source.m30, source.m31, source.m32);
        float t20 = determinant3x3(source.m01, source.m02, source.m03, source.m11, source.m12, source.m13, source.m31, source.m32, source.m33);
        float t21 = -determinant3x3(source.m00, source.m02, source.m03, source.m10, source.m12, source.m13, source.m30, source.m32, source.m33);
        float t22 = determinant3x3(source.m00, source.m01, source.m03, source.m10, source.m11, source.m13, source.m30, source.m31, source.m33);
        float t23 = -determinant3x3(source.m00, source.m01, source.m02, source.m10, source.m11, source.m12, source.m30, source.m31, source.m32);
        float t30 = -determinant3x3(source.m01, source.m02, source.m03, source.m11, source.m12, source.m13, source.m21, source.m22, source.m23);
        float t31 = determinant3x3(source.m00, source.m02, source.m03, source.m10, source.m12, source.m13, source.m20, source.m22, source.m23);
        float t32 = -determinant3x3(source.m00, source.m01, source.m03, source.m10, source.m11, source.m13, source.m20, source.m21, source.m23);
        float t33 = determinant3x3(source.m00, source.m01, source.m02, source.m10, source.m11, source.m12, source.m20, source.m21, source.m22);
        float out00 = t00 * determinantInverse;
        float out11 = t11 * determinantInverse;
        float out22 = t22 * determinantInverse;
        float out33 = t33 * determinantInverse;
        float out01 = t10 * determinantInverse;
        float out10 = t01 * determinantInverse;
        float out20 = t02 * determinantInverse;
        float out02 = t20 * determinantInverse;
        float out12 = t21 * determinantInverse;
        float out21 = t12 * determinantInverse;
        float out03 = t30 * determinantInverse;
        float out30 = t03 * determinantInverse;
        float out13 = t31 * determinantInverse;
        float out31 = t13 * determinantInverse;
        float out32 = t23 * determinantInverse;
        float out23 = t32 * determinantInverse;
        FiniteChecks.requireAllFinite(
                "matrix inverse",
                out00, out01, out02, out03,
                out10, out11, out12, out13,
                out20, out21, out22, out23,
                out30, out31, out32, out33);
        destination.m00 = out00;
        destination.m11 = out11;
        destination.m22 = out22;
        destination.m33 = out33;
        destination.m01 = out01;
        destination.m10 = out10;
        destination.m20 = out20;
        destination.m02 = out02;
        destination.m12 = out12;
        destination.m21 = out21;
        destination.m03 = out03;
        destination.m30 = out30;
        destination.m13 = out13;
        destination.m31 = out31;
        destination.m32 = out32;
        destination.m23 = out23;
        return destination;
    }

    public OpenMatrix4f translate(float coordinateX, float coordinateY, float coordinateZ) {
        return translate(new Vec3f(coordinateX, coordinateY, coordinateZ), this);
    }

    public OpenMatrix4f translate(Vec3f vector) {
        return translate(vector, this);
    }

    public OpenMatrix4f translate(Vec3f vector, OpenMatrix4f destination) {
        return translate(vector, this, destination);
    }

    public static OpenMatrix4f translate(Vec3f vector, OpenMatrix4f source, OpenMatrix4f destination) {
        if (vector == null || !vector.validateValues() || source == null) {
            throw new IllegalArgumentException("translation input must be finite");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float next30 = destination.m30 + source.m00 * vector.coordinateX + source.m10 * vector.coordinateY + source.m20 * vector.coordinateZ;
        float next31 = destination.m31 + source.m01 * vector.coordinateX + source.m11 * vector.coordinateY + source.m21 * vector.coordinateZ;
        float next32 = destination.m32 + source.m02 * vector.coordinateX + source.m12 * vector.coordinateY + source.m22 * vector.coordinateZ;
        float next33 = destination.m33 + source.m03 * vector.coordinateX + source.m13 * vector.coordinateY + source.m23 * vector.coordinateZ;
        FiniteChecks.requireAllFinite("translation", next30, next31, next32, next33);
        destination.m30 = next30;
        destination.m31 = next31;
        destination.m32 = next32;
        destination.m33 = next33;
        return destination;
    }

    public static OpenMatrix4f createTranslation(float coordinateX, float coordinateY, float coordinateZ) {
        return ofTranslation(coordinateX, coordinateY, coordinateZ, null);
    }

    public static OpenMatrix4f ofTranslation(float coordinateX, float coordinateY, float coordinateZ, OpenMatrix4f destination) {
        FiniteChecks.requireFiniteTriple("translation", coordinateX, coordinateY, coordinateZ);
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        destination.setIdentity();
        destination.m30 = coordinateX;
        destination.m31 = coordinateY;
        destination.m32 = coordinateZ;
        return destination;
    }

    public static OpenMatrix4f createScale(float coordinateX, float coordinateY, float coordinateZ) {
        return ofScale(coordinateX, coordinateY, coordinateZ, null);
    }

    public static OpenMatrix4f ofScale(float coordinateX, float coordinateY, float coordinateZ, OpenMatrix4f destination) {
        FiniteChecks.requireFiniteTriple("scale", coordinateX, coordinateY, coordinateZ);
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        destination.setIdentity();
        destination.m00 = coordinateX;
        destination.m11 = coordinateY;
        destination.m22 = coordinateZ;
        return destination;
    }

    public OpenMatrix4f rotateDeg(float angle, Vec3f axis) {
        return rotate((float) Math.toRadians(angle), axis);
    }

    public OpenMatrix4f rotate(float angle, Vec3f axis) {
        return rotate(angle, axis, this);
    }

    public OpenMatrix4f rotate(float angle, Vec3f axis, OpenMatrix4f destination) {
        return rotate(angle, axis, this, destination);
    }

    public static OpenMatrix4f createRotatorDeg(float degree, Vec3f axis) {
        return rotate((float) Math.toRadians(degree), axis, new OpenMatrix4f(), null);
    }

    public static OpenMatrix4f ofRotationDegree(float degree, Vec3f axis, OpenMatrix4f destination) {
        if (destination == null) {
            throw new IllegalArgumentException("rotation destination is missing");
        }
        destination.setIdentity();
        return rotate((float) Math.toRadians(degree), axis, destination, destination);
    }

    public static OpenMatrix4f rotate(float angle, Vec3f axis, OpenMatrix4f source, OpenMatrix4f destination) {
        if (!Float.isFinite(angle) || axis == null || !axis.validateValues() || source == null) {
            throw new IllegalArgumentException("rotation input must be finite");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float cosine = (float) Math.cos(angle);
        float sine = (float) Math.sin(angle);
        float oneMinusCosine = 1.0F - cosine;
        float axisXY = axis.coordinateX * axis.coordinateY;
        float axisYZ = axis.coordinateY * axis.coordinateZ;
        float axisXZ = axis.coordinateX * axis.coordinateZ;
        float axisXSine = axis.coordinateX * sine;
        float axisYSine = axis.coordinateY * sine;
        float axisZSine = axis.coordinateZ * sine;
        float f00 = axis.coordinateX * axis.coordinateX * oneMinusCosine + cosine;
        float f01 = axisXY * oneMinusCosine + axisZSine;
        float f02 = axisXZ * oneMinusCosine - axisYSine;
        float f10 = axisXY * oneMinusCosine - axisZSine;
        float f11 = axis.coordinateY * axis.coordinateY * oneMinusCosine + cosine;
        float f12 = axisYZ * oneMinusCosine + axisXSine;
        float f20 = axisXZ * oneMinusCosine + axisYSine;
        float f21 = axisYZ * oneMinusCosine - axisXSine;
        float f22 = axis.coordinateZ * axis.coordinateZ * oneMinusCosine + cosine;
        float t00 = source.m00 * f00 + source.m10 * f01 + source.m20 * f02;
        float t01 = source.m01 * f00 + source.m11 * f01 + source.m21 * f02;
        float t02 = source.m02 * f00 + source.m12 * f01 + source.m22 * f02;
        float t03 = source.m03 * f00 + source.m13 * f01 + source.m23 * f02;
        float t10 = source.m00 * f10 + source.m10 * f11 + source.m20 * f12;
        float t11 = source.m01 * f10 + source.m11 * f11 + source.m21 * f12;
        float t12 = source.m02 * f10 + source.m12 * f11 + source.m22 * f12;
        float t13 = source.m03 * f10 + source.m13 * f11 + source.m23 * f12;
        float out20 = source.m00 * f20 + source.m10 * f21 + source.m20 * f22;
        float out21 = source.m01 * f20 + source.m11 * f21 + source.m21 * f22;
        float out22 = source.m02 * f20 + source.m12 * f21 + source.m22 * f22;
        float out23 = source.m03 * f20 + source.m13 * f21 + source.m23 * f22;
        FiniteChecks.requireAllFinite("rotation", t00, t01, t02, t03, t10, t11, t12, t13, out20, out21, out22, out23);
        destination.m20 = out20;
        destination.m21 = out21;
        destination.m22 = out22;
        destination.m23 = out23;
        destination.m00 = t00;
        destination.m01 = t01;
        destination.m02 = t02;
        destination.m03 = t03;
        destination.m10 = t10;
        destination.m11 = t11;
        destination.m12 = t12;
        destination.m13 = t13;
        return destination;
    }

    public Vec3f toTranslationVector() {
        return toTranslationVector(this);
    }

    public Vec3f toTranslationVector(Vec3f destination) {
        return toTranslationVector(this, destination);
    }

    public static Vec3f toTranslationVector(OpenMatrix4f matrix) {
        return toTranslationVector(matrix, null);
    }

    public static Vec3f toTranslationVector(OpenMatrix4f matrix, Vec3f destination) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new Vec3f();
        }
        destination.set(matrix.m30, matrix.m31, matrix.m32);
        return destination;
    }

    public Quaternionf toQuaternion() {
        return OpenMatrix4f.toQuaternion(this);
    }

    public Quaternionf toQuaternion(Quaternionf destination) {
        return OpenMatrix4f.toQuaternion(this, destination);
    }

    public static Quaternionf toQuaternion(OpenMatrix4f matrix) {
        return toQuaternion(matrix, new Quaternionf());
    }

    public static Quaternionf toQuaternion(OpenMatrix4f matrix, Quaternionf destination) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new Quaternionf();
        }
        OpenMatrix4f storage = new OpenMatrix4f();
        OpenMatrix4f.load(matrix, storage);
        storage.transpose();
        float columnX = storage.m00 * storage.m00 + storage.m01 * storage.m01 + storage.m02 * storage.m02;
        float columnY = storage.m10 * storage.m10 + storage.m11 * storage.m11 + storage.m12 * storage.m12;
        float columnZ = storage.m20 * storage.m20 + storage.m21 * storage.m21 + storage.m22 * storage.m22;
        if (columnX == 0.0F || columnY == 0.0F || columnZ == 0.0F) {
            return new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F);
        }
        columnX = Math.invsqrt(columnX);
        columnY = Math.invsqrt(columnY);
        columnZ = Math.invsqrt(columnZ);
        if (!Float.isFinite(columnX) || !Float.isFinite(columnY) || !Float.isFinite(columnZ)) {
            throw new IllegalArgumentException("quaternion extraction is not finite");
        }
        storage.m00 *= columnX;
        storage.m01 *= columnX;
        storage.m02 *= columnX;
        storage.m10 *= columnY;
        storage.m11 *= columnY;
        storage.m12 *= columnY;
        storage.m20 *= columnZ;
        storage.m21 *= columnZ;
        storage.m22 *= columnZ;
        float trace = storage.m00 + storage.m11 + storage.m22;
        float quatX;
        float quatY;
        float quatZ;
        float quatW;
        if (trace >= 0.0F) {
            float root = (float) Math.sqrt(trace + 1.0F);
            if (!(root > 0.0F) || !Float.isFinite(root)) {
                throw new IllegalArgumentException("quaternion extraction is not finite");
            }
            quatW = root * 0.5F;
            root = 0.5F / root;
            quatX = (storage.m12 - storage.m21) * root;
            quatY = (storage.m20 - storage.m02) * root;
            quatZ = (storage.m01 - storage.m10) * root;
        } else if (storage.m00 >= storage.m11 && storage.m00 >= storage.m22) {
            float root = (float) Math.sqrt(storage.m00 - (storage.m11 + storage.m22) + 1.0F);
            if (!(root > 0.0F) || !Float.isFinite(root)) {
                throw new IllegalArgumentException("quaternion extraction is not finite");
            }
            quatX = root * 0.5F;
            root = 0.5F / root;
            quatY = (storage.m10 + storage.m01) * root;
            quatZ = (storage.m02 + storage.m20) * root;
            quatW = (storage.m12 - storage.m21) * root;
        } else if (storage.m11 > storage.m22) {
            float root = (float) Math.sqrt(storage.m11 - (storage.m22 + storage.m00) + 1.0F);
            if (!(root > 0.0F) || !Float.isFinite(root)) {
                throw new IllegalArgumentException("quaternion extraction is not finite");
            }
            quatY = root * 0.5F;
            root = 0.5F / root;
            quatZ = (storage.m21 + storage.m12) * root;
            quatX = (storage.m10 + storage.m01) * root;
            quatW = (storage.m20 - storage.m02) * root;
        } else {
            float root = (float) Math.sqrt(storage.m22 - (storage.m00 + storage.m11) + 1.0F);
            if (!(root > 0.0F) || !Float.isFinite(root)) {
                throw new IllegalArgumentException("quaternion extraction is not finite");
            }
            quatZ = root * 0.5F;
            root = 0.5F / root;
            quatX = (storage.m02 + storage.m20) * root;
            quatY = (storage.m21 + storage.m12) * root;
            quatW = (storage.m01 - storage.m10) * root;
        }
        FiniteChecks.requireFiniteQuaternion("quaternion extraction", quatX, quatY, quatZ, quatW);
        destination.set(quatX, quatY, quatZ, quatW);
        return destination;
    }

    public static OpenMatrix4f fromQuaternion(Quaternionf quaternion) {
        return fromQuaternion(quaternion, null);
    }

    public static OpenMatrix4f fromQuaternion(Quaternionf quaternion, OpenMatrix4f destination) {
        if (quaternion == null) {
            throw new IllegalArgumentException("quaternion is missing");
        }
        FiniteChecks.requireFiniteQuaternion("quaternion", quaternion.x(), quaternion.y(), quaternion.z(), quaternion.w());
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float quatX = quaternion.x();
        float quatY = quaternion.y();
        float quatZ = quaternion.z();
        float quatW = quaternion.w();
        float productXY = quatX * quatY;
        float productXZ = quatX * quatZ;
        float productXW = quatX * quatW;
        float productYZ = quatY * quatZ;
        float productYW = quatY * quatW;
        float productZW = quatZ * quatW;
        float doubledX = 2.0F * quatX * quatX;
        float doubledY = 2.0F * quatY * quatY;
        float doubledZ = 2.0F * quatZ * quatZ;
        float out00 = 1.0F - doubledY - doubledZ;
        float out01 = 2.0F * (productXY - productZW);
        float out02 = 2.0F * (productXZ + productYW);
        float out10 = 2.0F * (productXY + productZW);
        float out11 = 1.0F - doubledX - doubledZ;
        float out12 = 2.0F * (productYZ - productXW);
        float out20 = 2.0F * (productXZ - productYW);
        float out21 = 2.0F * (productYZ + productXW);
        float out22 = 1.0F - doubledX - doubledY;
        FiniteChecks.requireAllFinite("quaternion matrix", out00, out01, out02, out10, out11, out12, out20, out21, out22);
        destination.m00 = out00;
        destination.m01 = out01;
        destination.m02 = out02;
        destination.m10 = out10;
        destination.m11 = out11;
        destination.m12 = out12;
        destination.m20 = out20;
        destination.m21 = out21;
        destination.m22 = out22;
        return destination;
    }

    public OpenMatrix4f scale(float coordinateX, float coordinateY, float coordinateZ) {
        return this.scale(new Vec3f(coordinateX, coordinateY, coordinateZ));
    }

    public OpenMatrix4f scale(Vec3f vector) {
        return scale(vector, this, this);
    }

    public static OpenMatrix4f scale(Vec3f vector, OpenMatrix4f source, OpenMatrix4f destination) {
        if (vector == null || !vector.validateValues() || source == null) {
            throw new IllegalArgumentException("scale input must be finite");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        float out00 = source.m00 * vector.coordinateX;
        float out01 = source.m01 * vector.coordinateX;
        float out02 = source.m02 * vector.coordinateX;
        float out03 = source.m03 * vector.coordinateX;
        float out10 = source.m10 * vector.coordinateY;
        float out11 = source.m11 * vector.coordinateY;
        float out12 = source.m12 * vector.coordinateY;
        float out13 = source.m13 * vector.coordinateY;
        float out20 = source.m20 * vector.coordinateZ;
        float out21 = source.m21 * vector.coordinateZ;
        float out22 = source.m22 * vector.coordinateZ;
        float out23 = source.m23 * vector.coordinateZ;
        FiniteChecks.requireAllFinite("scale", out00, out01, out02, out03, out10, out11, out12, out13, out20, out21, out22, out23);
        destination.m00 = out00;
        destination.m01 = out01;
        destination.m02 = out02;
        destination.m03 = out03;
        destination.m10 = out10;
        destination.m11 = out11;
        destination.m12 = out12;
        destination.m13 = out13;
        destination.m20 = out20;
        destination.m21 = out21;
        destination.m22 = out22;
        destination.m23 = out23;
        return destination;
    }

    public Vec3f toScaleVector() {
        return toScaleVector(null);
    }

    public Vec3f toScaleVector(Vec3f destination) {
        if (destination == null) {
            destination = new Vec3f();
        }
        Vec3f column = new Vec3f(this.m00, this.m01, this.m02);
        float scaleX = column.length();
        column.set(this.m10, this.m11, this.m12);
        float scaleY = column.length();
        column.set(this.m20, this.m21, this.m22);
        float scaleZ = column.length();
        destination.set(scaleX, scaleY, scaleZ);
        return destination;
    }

    public OpenMatrix4f removeTranslation() {
        return removeTranslation(this, null);
    }

    public static OpenMatrix4f removeTranslation(OpenMatrix4f source, OpenMatrix4f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        destination.load(source);
        destination.m30 = 0.0F;
        destination.m31 = 0.0F;
        destination.m32 = 0.0F;
        return destination;
    }

    public OpenMatrix4f removeScale() {
        return removeScale(this, null);
    }

    public static OpenMatrix4f removeScale(OpenMatrix4f source, OpenMatrix4f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new OpenMatrix4f();
        } else if (destination.immutable) {
            throw new UnsupportedOperationException("Can't modify immutable matrix");
        }
        Vec3f column = new Vec3f(source.m00, source.m01, source.m02);
        float scaleX = column.length();
        column.set(source.m10, source.m11, source.m12);
        float scaleY = column.length();
        column.set(source.m20, source.m21, source.m22);
        float scaleZ = column.length();
        if (!Float.isFinite(scaleX) || !Float.isFinite(scaleY) || !Float.isFinite(scaleZ) || scaleX == 0.0F || scaleY == 0.0F || scaleZ == 0.0F) {
            throw new IllegalArgumentException("cannot remove a zero or non-finite scale");
        }
        destination.load(source);
        destination.scale(1.0F / scaleX, 1.0F / scaleY, 1.0F / scaleZ);
        return destination;
    }

    @Override
    public String toString() {
        return "\n"
                + String.format("%.4f", this.m00) + " " + String.format("%.4f", this.m01) + " " + String.format("%.4f", this.m02) + " " + String.format("%.4f", this.m03) + "\n"
                + String.format("%.4f", this.m10) + " " + String.format("%.4f", this.m11) + " " + String.format("%.4f", this.m12) + " " + String.format("%.4f", this.m13) + "\n"
                + String.format("%.4f", this.m20) + " " + String.format("%.4f", this.m21) + " " + String.format("%.4f", this.m22) + " " + String.format("%.4f", this.m23) + "\n"
                + String.format("%.4f", this.m30) + " " + String.format("%.4f", this.m31) + " " + String.format("%.4f", this.m32) + " " + String.format("%.4f", this.m33) + "\n";
    }

    public static Matrix4f exportToMojangMatrix(OpenMatrix4f source) {
        return exportToMojangMatrix(source, null);
    }

    public static Matrix4f exportToMojangMatrix(OpenMatrix4f source, Matrix4f destination) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        if (destination == null) {
            destination = new Matrix4f();
        }
        FloatBuffer transformer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        source.store(transformer);
        transformer.position(0);
        return destination.set(transformer);
    }

    public static OpenMatrix4f importFromMojangMatrix(Matrix4f source) {
        if (source == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        FloatBuffer transformer = ByteBuffer.allocateDirect(16 * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
        source.get(transformer);
        return OpenMatrix4f.load(null, transformer);
    }

    public static OpenMatrix4f[] allocateMatrixArray(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("matrix array size must be non-negative");
        }
        OpenMatrix4f[] matrixArray = new OpenMatrix4f[size];
        for (int slot = 0; slot < size; slot++) {
            matrixArray[slot] = new OpenMatrix4f();
        }
        return matrixArray;
    }
}

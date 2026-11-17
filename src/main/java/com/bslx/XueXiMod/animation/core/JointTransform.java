/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/animation/JointTransform.java
 * Modified: 2026-10-24
 *
 * 修改说明：平移、旋转、缩放必须有限，旋转长度不能接近 0。插值进度先拒绝 NaN 再夹到 [0, 1]。
 * toMatrix 仍是平移后右乘旋转再缩放。OpenMatrix4f(JointTransform) 的构造顺序保持上游，两者不同。
 * copyFrom 深复制每个条目和嵌套变换。副本图内可以共享同一子节点，不与源图共享。
 * 发现环时抛出 IllegalArgumentException，目标保持原样。copyFrom(this) 不改内容。
 */
package com.bslx.XueXiMod.animation.core;

import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

import org.joml.Quaternionf;

import com.bslx.XueXiMod.animation.core.math.FiniteChecks;
import com.bslx.XueXiMod.animation.core.math.MathUtils;
import com.bslx.XueXiMod.animation.core.math.MatrixOperation;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.math.AnimationTransformEntry;

public class JointTransform {
    public static final String ANIMATION_TRANSFORM = "animation_transform";
    public static final String JOINT_LOCAL_TRANSFORM = "joint_local_transform";
    public static final String PARENT = "parent";
    public static final String RESULT1 = "front_result";
    public static final String RESULT2 = "overwrite_rotation";

    public static class TransformEntry {
        public final MatrixOperation multiplyFunction;
        public final JointTransform transform;

        public TransformEntry(MatrixOperation multiplyFunction, JointTransform transform) {
            if (multiplyFunction == null || transform == null) {
                throw new IllegalArgumentException("transform entry is missing");
            }
            this.multiplyFunction = multiplyFunction;
            this.transform = transform;
        }
    }

    private final Map<String, TransformEntry> entries = new HashMap<>();
    private final Vec3f translation;
    private final Vec3f scale;
    private final Quaternionf rotation;

    public JointTransform(Vec3f translation, Quaternionf rotation, Vec3f scale) {
        if (translation == null || rotation == null || scale == null) {
            throw new IllegalArgumentException("joint transform is missing");
        }
        this.translation = translation;
        this.rotation = rotation;
        this.scale = scale;
        this.requireFinite();
    }

    public void requireFinite() {
        if (this.translation == null || !this.translation.validateValues() || this.scale == null || !this.scale.validateValues()) {
            throw new IllegalArgumentException("joint transform vectors must be finite");
        }
        if (this.rotation == null) {
            throw new IllegalArgumentException("joint rotation is missing");
        }
        FiniteChecks.requireFiniteQuaternion("joint rotation", this.rotation.x(), this.rotation.y(), this.rotation.z(), this.rotation.w());
    }

    public Vec3f translation() {
        return this.translation;
    }

    public Quaternionf rotation() {
        return this.rotation;
    }

    public Vec3f scale() {
        return this.scale;
    }

    public void clearTransform() {
        this.translation.set(0.0F, 0.0F, 0.0F);
        this.rotation.set(0.0F, 0.0F, 0.0F, 1.0F);
        this.scale.set(1.0F, 1.0F, 1.0F);
    }

    public JointTransform copy() {
        return JointTransform.empty().copyFrom(this);
    }

    public JointTransform copyFrom(JointTransform source) {
        if (source == null) {
            throw new IllegalArgumentException("joint transform is missing");
        }
        if (source == this) {
            return this;
        }
        JointTransform temporary = duplicateGraph(source, new IdentityHashMap<>(), Collections.newSetFromMap(new IdentityHashMap<>()));
        this.translation.set(temporary.translation);
        this.rotation.set(temporary.rotation);
        this.scale.set(temporary.scale);
        this.entries.clear();
        this.entries.putAll(temporary.entries);
        this.requireFinite();
        return this;
    }

    private static JointTransform duplicateGraph(
            JointTransform source,
            Map<JointTransform, JointTransform> completedCopies,
            Set<JointTransform> activePath) {
        JointTransform finished = completedCopies.get(source);
        if (finished != null) {
            return finished;
        }
        if (!activePath.add(source)) {
            throw new IllegalArgumentException("joint transform entry cycle");
        }
        source.requireFinite();
        JointTransform temporary = JointTransform.empty();
        temporary.translation.set(source.translation);
        temporary.rotation.set(source.rotation);
        temporary.scale.set(source.scale);
        Map<String, TransformEntry> copiedEntries = new HashMap<>();
        for (Map.Entry<String, TransformEntry> entry : source.entries.entrySet()) {
            TransformEntry sourceEntry = entry.getValue();
            if (entry.getKey() == null || sourceEntry == null || sourceEntry.multiplyFunction == null || sourceEntry.transform == null) {
                throw new IllegalArgumentException("transform entry is missing");
            }
            JointTransform nestedCopy = duplicateGraph(sourceEntry.transform, completedCopies, activePath);
            copiedEntries.put(entry.getKey(), new TransformEntry(sourceEntry.multiplyFunction, nestedCopy));
        }
        temporary.entries.putAll(copiedEntries);
        temporary.requireFinite();
        activePath.remove(source);
        completedCopies.put(source, temporary);
        return temporary;
    }

    public void jointLocal(JointTransform transform, MatrixOperation multiplyFunction) {
        this.entries.put(JOINT_LOCAL_TRANSFORM, new TransformEntry(multiplyFunction, this.mergeIfExist(JOINT_LOCAL_TRANSFORM, transform)));
    }

    public void parent(JointTransform transform, MatrixOperation multiplyFunction) {
        this.entries.put(PARENT, new TransformEntry(multiplyFunction, this.mergeIfExist(PARENT, transform)));
    }

    public void animationTransform(JointTransform transform, MatrixOperation multiplyFunction) {
        this.entries.put(ANIMATION_TRANSFORM, new TransformEntry(multiplyFunction, this.mergeIfExist(ANIMATION_TRANSFORM, transform)));
    }

    public void frontResult(JointTransform transform, MatrixOperation multiplyFunction) {
        this.entries.put(RESULT1, new TransformEntry(multiplyFunction, this.mergeIfExist(RESULT1, transform)));
    }

    public void overwriteRotation(JointTransform transform) {
        this.entries.put(RESULT2, new TransformEntry(OpenMatrix4f::mul, this.mergeIfExist(RESULT2, transform)));
    }

    public JointTransform mergeIfExist(String entryName, JointTransform transform) {
        if (entryName == null || transform == null) {
            throw new IllegalArgumentException("transform entry is missing");
        }
        TransformEntry existing = this.entries.get(entryName);
        if (existing != null) {
            return JointTransform.mul(transform, existing.transform, existing.multiplyFunction);
        }
        return transform;
    }

    public OpenMatrix4f getAnimationBoundMatrix(Joint joint, OpenMatrix4f parentTransform) {
        if (joint == null || parentTransform == null) {
            throw new IllegalArgumentException("bound matrix input is missing");
        }
        this.requireFinite();
        AnimationTransformEntry animationTransformEntry = new AnimationTransformEntry();
        for (Map.Entry<String, TransformEntry> entry : this.entries.entrySet()) {
            animationTransformEntry.put(entry.getKey(), entry.getValue().transform.toMatrix(), entry.getValue().multiplyFunction);
        }
        animationTransformEntry.put(ANIMATION_TRANSFORM, this.toMatrix(), OpenMatrix4f::mul);
        animationTransformEntry.put(JOINT_LOCAL_TRANSFORM, joint.getLocalTransform());
        animationTransformEntry.put(PARENT, parentTransform);
        return animationTransformEntry.getResult();
    }

    public OpenMatrix4f toMatrix() {
        this.requireFinite();
        return new OpenMatrix4f().translate(this.translation).mulBack(OpenMatrix4f.fromQuaternion(this.rotation)).scale(this.scale);
    }

    @Override
    public String toString() {
        return String.format("translation:%s, rotation:%s, scale:%s %d entries ", this.translation, this.rotation, this.scale, this.entries.size());
    }

    public static JointTransform interpolateTransform(JointTransform previous, JointTransform next, float progression, JointTransform destination) {
        if (previous == null || next == null) {
            throw new IllegalArgumentException("joint transform is missing");
        }
        if (destination == null) {
            destination = JointTransform.empty();
        }
        previous.requireFinite();
        next.requireFinite();
        float clamped = clampProgression(progression);
        MathUtils.lerpVector(previous.translation, next.translation, clamped, destination.translation);
        MathUtils.lerpQuaternion(previous.rotation, next.rotation, clamped, destination.rotation);
        MathUtils.lerpVector(previous.scale, next.scale, clamped, destination.scale);
        destination.requireFinite();
        return destination;
    }

    public static JointTransform interpolate(JointTransform previous, JointTransform next, float progression) {
        return interpolate(previous, next, progression, null);
    }

    public static JointTransform interpolate(JointTransform previous, JointTransform next, float progression, JointTransform destination) {
        if (destination == null) {
            destination = JointTransform.empty();
        }
        if (previous == null || next == null) {
            destination.clearTransform();
            return destination;
        }
        float clamped = clampProgression(progression);
        interpolateTransform(previous, next, clamped, destination);
        destination.entries.clear();
        for (Map.Entry<String, TransformEntry> entry : previous.entries.entrySet()) {
            JointTransform counterpart = next.entries.containsKey(entry.getKey()) ? next.entries.get(entry.getKey()).transform : JointTransform.empty();
            destination.entries.put(entry.getKey(), new TransformEntry(entry.getValue().multiplyFunction, interpolateTransform(entry.getValue().transform, counterpart, clamped, null)));
        }
        for (Map.Entry<String, TransformEntry> entry : next.entries.entrySet()) {
            if (!destination.entries.containsKey(entry.getKey())) {
                destination.entries.put(entry.getKey(), new TransformEntry(entry.getValue().multiplyFunction, interpolateTransform(JointTransform.empty(), entry.getValue().transform, clamped, null)));
            }
        }
        return destination;
    }

    private static float clampProgression(float progression) {
        if (!Float.isFinite(progression)) {
            throw new IllegalArgumentException("progression must be finite");
        }
        if (progression < 0.0F) {
            return 0.0F;
        }
        if (progression > 1.0F) {
            return 1.0F;
        }
        return progression;
    }

    public static JointTransform fromMatrixWithoutScale(OpenMatrix4f matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        return new JointTransform(matrix.toTranslationVector(), matrix.toQuaternion(), new Vec3f(1.0F, 1.0F, 1.0F));
    }

    public static JointTransform translation(Vec3f vector) {
        return JointTransform.translationRotation(vector, new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F));
    }

    public static JointTransform rotation(Quaternionf quaternion) {
        return JointTransform.translationRotation(new Vec3f(0.0F, 0.0F, 0.0F), quaternion);
    }

    public static JointTransform scale(Vec3f vector) {
        return new JointTransform(new Vec3f(0.0F, 0.0F, 0.0F), new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F), vector);
    }

    public static JointTransform fromMatrix(OpenMatrix4f matrix) {
        if (matrix == null) {
            throw new IllegalArgumentException("matrix is missing");
        }
        return new JointTransform(matrix.toTranslationVector(), matrix.toQuaternion(), matrix.toScaleVector());
    }

    public static JointTransform translationRotation(Vec3f vector, Quaternionf quaternion) {
        return new JointTransform(vector, quaternion, new Vec3f(1.0F, 1.0F, 1.0F));
    }

    public static JointTransform mul(JointTransform left, JointTransform right, MatrixOperation operation) {
        if (left == null || right == null || operation == null) {
            throw new IllegalArgumentException("joint transform product is missing");
        }
        return JointTransform.fromMatrix(operation.mul(left.toMatrix(), right.toMatrix(), null));
    }

    public static JointTransform fromPrimitives(
            float translationX, float translationY, float translationZ,
            float rotationX, float rotationY, float rotationZ, float rotationW,
            float scaleX, float scaleY, float scaleZ) {
        return new JointTransform(
                new Vec3f(translationX, translationY, translationZ),
                new Quaternionf(rotationX, rotationY, rotationZ, rotationW),
                new Vec3f(scaleX, scaleY, scaleZ));
    }

    public static JointTransform empty() {
        return new JointTransform(new Vec3f(0.0F, 0.0F, 0.0F), new Quaternionf(0.0F, 0.0F, 0.0F, 1.0F), new Vec3f(1.0F, 1.0F, 1.0F));
    }
}

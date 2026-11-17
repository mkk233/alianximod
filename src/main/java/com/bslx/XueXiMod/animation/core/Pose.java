/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/animation/Pose.java
 * Modified: 2026-10-24
 *
 * 修改说明：插值仍按两侧关节名并集逐关节混合。缺少的关节用空变换。集合改为 JDK。
 */
package com.bslx.XueXiMod.animation.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

public class Pose {
    public static final Pose EMPTY_POSE = new Pose();

    public static Pose interpolatePose(Pose pose1, Pose pose2, float progression) {
        if (pose1 == null || pose2 == null) {
            throw new IllegalArgumentException("pose is missing");
        }
        Pose pose = new Pose();
        Set<String> mergedNames = new HashSet<>(pose1.jointTransformData.keySet());
        mergedNames.addAll(pose2.jointTransformData.keySet());
        for (String jointName : mergedNames) {
            pose.putJointData(jointName, JointTransform.interpolate(pose1.orElseEmpty(jointName), pose2.orElseEmpty(jointName), progression));
        }
        return pose;
    }

    protected final Map<String, JointTransform> jointTransformData;

    public Pose() {
        this(new HashMap<>());
    }

    public Pose(Map<String, JointTransform> jointTransforms) {
        if (jointTransforms == null) {
            throw new IllegalArgumentException("pose is missing");
        }
        this.jointTransformData = jointTransforms;
    }

    public void putJointData(String name, JointTransform transform) {
        if (name == null || transform == null) {
            throw new IllegalArgumentException("pose joint is missing");
        }
        transform.requireFinite();
        this.jointTransformData.put(name, transform);
    }

    public Map<String, JointTransform> getJointTransformData() {
        return this.jointTransformData;
    }

    public void disableJoint(Predicate<? super Map.Entry<String, JointTransform>> predicate) {
        if (predicate == null) {
            throw new IllegalArgumentException("pose predicate is missing");
        }
        this.jointTransformData.entrySet().removeIf(predicate);
    }

    public void disableAllJoints() {
        this.jointTransformData.clear();
    }

    public boolean hasTransform(String jointName) {
        return this.jointTransformData.containsKey(jointName);
    }

    public JointTransform get(String jointName) {
        return this.jointTransformData.get(jointName);
    }

    public JointTransform orElseEmpty(String jointName) {
        return this.jointTransformData.getOrDefault(jointName, JointTransform.empty());
    }

    public JointTransform orElse(String jointName, JointTransform fallback) {
        return this.jointTransformData.getOrDefault(jointName, fallback);
    }

    public void forEachEnabledTransforms(BiConsumer<String, JointTransform> task) {
        if (task == null) {
            throw new IllegalArgumentException("pose task is missing");
        }
        this.jointTransformData.forEach(task);
    }

    public void load(Pose pose, LoadOperation operation) {
        if (pose == null || operation == null) {
            throw new IllegalArgumentException("pose load input is missing");
        }
        switch (operation) {
            case SET -> {
                this.disableAllJoints();
                pose.forEachEnabledTransforms(this::putJointData);
            }
            case OVERWRITE -> pose.forEachEnabledTransforms(this::putJointData);
            case APPEND_ABSENT -> pose.forEachEnabledTransforms((name, transform) -> {
                if (!this.hasTransform(name)) {
                    this.putJointData(name, transform);
                }
            });
        }
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append("Pose: ");
        for (Map.Entry<String, JointTransform> entry : this.jointTransformData.entrySet()) {
            text.append(String.format("%s{%s, %s}, ", entry.getKey(), entry.getValue().translation().toString(), entry.getValue().rotation().toString()));
            text.append("\n");
        }
        return text.toString();
    }

    public enum LoadOperation {
        SET, OVERWRITE, APPEND_ABSENT
    }
}

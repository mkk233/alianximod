/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/model/Armature.java
 * Modified: 2026-10-24
 *
 * 修改说明：去掉开发日志和 JsonAssetLoader 导出。关节编号必须落在 [0, jointCount)，彼此唯一，
 * 层级无环，数量与 jointCount 一致。父子姿势矩阵仍先写入再递归子关节，最后才乘回去原点逆矩阵。
 * 整数映射改为 HashMap。getOrLogException 找不到关节时返回 Joint.EMPTY，不再写日志。
 */
package com.bslx.XueXiMod.animation.core.model;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

public class Armature {
    private final String name;
    private final Map<Integer, Joint> jointById;
    private final Map<String, Joint> jointByName;
    private final Map<String, Joint.HierarchicalJointAccessor> pathIndexMap;
    private final int jointCount;
    private final OpenMatrix4f[] poseMatrices;
    public final Joint rootJoint;

    public Armature(String name, int jointCount, Joint rootJoint, Map<String, Joint> jointMap) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("armature name is missing");
        }
        if (jointCount < 0 || jointCount > Joint.HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalArgumentException("joint count is outside the supported range");
        }
        if (rootJoint == null || jointMap == null) {
            throw new IllegalArgumentException("armature hierarchy is missing");
        }
        this.name = name;
        this.jointCount = jointCount;
        this.rootJoint = rootJoint;
        this.jointByName = jointMap;
        this.jointById = new HashMap<>();
        this.pathIndexMap = new HashMap<>();
        List<Joint> hierarchy = this.validatedHierarchy();
        if (hierarchy.size() != this.jointCount) {
            throw new IllegalArgumentException("joint count does not match the hierarchy");
        }
        for (Joint joint : this.jointByName.values()) {
            if (joint == null) {
                throw new IllegalArgumentException("joint map contains a missing joint");
            }
            Joint previous = this.jointById.put(joint.getId(), joint);
            if (previous != null && previous != joint) {
                throw new IllegalArgumentException("joint id is duplicated");
            }
        }
        this.poseMatrices = OpenMatrix4f.allocateMatrixArray(this.jointCount);
    }

    private List<Joint> validatedHierarchy() {
        List<Joint> ordered = new ArrayList<>();
        Set<Joint> active = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Joint> visited = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<String> names = new java.util.HashSet<>();
        Set<Integer> identifiers = new java.util.HashSet<>();
        this.walk(this.rootJoint, active, visited, ordered, names, identifiers);
        for (Map.Entry<String, Joint> entry : this.jointByName.entrySet()) {
            Joint mapped = entry.getValue();
            if (mapped == null || entry.getKey() == null) {
                throw new IllegalArgumentException("joint map entry is missing");
            }
            if (mapped == Joint.EMPTY) {
                continue;
            }
            if (!visited.contains(mapped)) {
                throw new IllegalArgumentException("joint map contains a joint outside the hierarchy");
            }
            if (!entry.getKey().equals(mapped.getName())) {
                throw new IllegalArgumentException("joint map key does not match the joint name");
            }
        }
        for (Joint joint : ordered) {
            Joint mapped = this.jointByName.get(joint.getName());
            if (mapped != joint) {
                throw new IllegalArgumentException("hierarchy joint is missing from the joint map");
            }
        }
        return ordered;
    }

    private void walk(Joint joint, Set<Joint> active, Set<Joint> visited, List<Joint> ordered, Set<String> names, Set<Integer> identifiers) {
        if (joint == null || joint == Joint.EMPTY) {
            throw new IllegalArgumentException("hierarchy joint is missing");
        }
        if (!active.add(joint)) {
            throw new IllegalArgumentException("joint hierarchy contains a cycle");
        }
        if (!visited.add(joint)) {
            throw new IllegalArgumentException("joint appears more than once");
        }
        if (ordered.size() >= Joint.HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalStateException("joint hierarchy is too deep");
        }
        int jointId = joint.getId();
        if (jointId < 0 || jointId >= this.jointCount) {
            throw new IllegalArgumentException("joint id is outside the pose matrix range");
        }
        if (!identifiers.add(jointId)) {
            throw new IllegalArgumentException("joint id is duplicated");
        }
        if (!names.add(joint.getName())) {
            throw new IllegalArgumentException("joint name is duplicated");
        }
        OpenMatrix4f localTransform = joint.getLocalTransform();
        if (localTransform == null || !localTransform.hasFiniteElements()) {
            throw new IllegalArgumentException("joint bind matrix must be finite");
        }
        float determinant = localTransform.determinant();
        if (!Float.isFinite(determinant) || determinant == 0.0F) {
            throw new IllegalArgumentException("joint bind matrix must be invertible");
        }
        ordered.add(joint);
        for (Joint child : joint.getSubJoints()) {
            this.walk(child, active, visited, ordered, names, identifiers);
        }
        active.remove(joint);
    }

    protected Joint getOrLogException(Map<String, Joint> jointMap, String jointName) {
        if (jointMap == null || jointName == null || !jointMap.containsKey(jointName)) {
            return Joint.EMPTY;
        }
        return jointMap.get(jointName);
    }

    public void setPose(Pose pose) {
        this.getPoseTransform(this.rootJoint, new OpenMatrix4f(), pose, this.poseMatrices, false, 0);
    }

    public void bakeOriginMatrices() {
        this.rootJoint.initOriginTransform(new OpenMatrix4f());
    }

    public OpenMatrix4f[] getPoseMatrices() {
        return this.poseMatrices;
    }

    public OpenMatrix4f[] getPoseAsTransformMatrix(Pose pose, boolean applyOriginTransform) {
        OpenMatrix4f[] jointMatrices = new OpenMatrix4f[this.jointCount];
        this.getPoseTransform(this.rootJoint, new OpenMatrix4f(), pose, jointMatrices, applyOriginTransform, 0);
        return jointMatrices;
    }

    private void getPoseTransform(Joint joint, OpenMatrix4f parentTransform, Pose pose, OpenMatrix4f[] jointMatrices, boolean applyOriginTransform, int depth) {
        if (depth > this.jointCount) {
            throw new IllegalStateException("joint hierarchy depth exceeded the joint count");
        }
        if (joint == null || parentTransform == null || pose == null || jointMatrices == null) {
            throw new IllegalArgumentException("pose transform input is missing");
        }
        int jointId = joint.getId();
        if (jointId < 0 || jointId >= jointMatrices.length) {
            throw new IllegalArgumentException("joint id is outside the pose matrix range");
        }
        OpenMatrix4f result = pose.orElseEmpty(joint.getName()).getAnimationBoundMatrix(joint, parentTransform);
        if (result == null || !result.hasFiniteElements()) {
            throw new IllegalArgumentException("pose matrix must be finite");
        }
        jointMatrices[jointId] = result;
        for (Joint child : joint.getSubJoints()) {
            this.getPoseTransform(child, result, pose, jointMatrices, applyOriginTransform, depth + 1);
        }
        if (applyOriginTransform) {
            result.mulBack(joint.getToOrigin());
            if (!result.hasFiniteElements()) {
                throw new IllegalArgumentException("origin-adjusted pose matrix must be finite");
            }
        }
    }

    @Deprecated(forRemoval = true, since = "1.21.1")
    public OpenMatrix4f getBindedTransformFor(Pose pose, Joint joint) {
        return this.getBoundTransformFor(pose, joint);
    }

    public OpenMatrix4f getBoundTransformFor(Pose pose, Joint joint) {
        if (joint == null) {
            throw new IllegalArgumentException("joint is missing");
        }
        return this.getBoundTransformByJointIndex(pose, this.searchPathIndex(joint.getName()).createAccessTicket(this.rootJoint));
    }

    public OpenMatrix4f getBoundTransformByJointIndex(Pose pose, Joint.AccessTicket pathIndices) {
        return this.getBoundJointTransformRecursively(pose, this.rootJoint, new OpenMatrix4f(), pathIndices, 0);
    }

    private OpenMatrix4f getBoundJointTransformRecursively(Pose pose, Joint joint, OpenMatrix4f parentTransform, Joint.AccessTicket pathIndices, int depth) {
        if (depth > this.jointCount) {
            throw new IllegalStateException("joint hierarchy depth exceeded the joint count");
        }
        if (pose == null || joint == null || parentTransform == null || pathIndices == null) {
            throw new IllegalArgumentException("bound transform input is missing");
        }
        JointTransform jointTransform = pose.orElseEmpty(joint.getName());
        OpenMatrix4f result = jointTransform.getAnimationBoundMatrix(joint, parentTransform);
        return pathIndices.hasNext()
                ? this.getBoundJointTransformRecursively(pose, pathIndices.next(), result, pathIndices, depth + 1)
                : result;
    }

    public boolean hasJoint(String jointName) {
        return this.jointByName.containsKey(jointName);
    }

    public Joint searchJointById(int jointId) {
        return this.jointById.get(jointId);
    }

    public Joint searchJointByName(String jointName) {
        return this.jointByName.get(jointName);
    }

    public Joint.HierarchicalJointAccessor searchPathIndex(String terminalJointName) {
        return this.searchPathIndex(this.rootJoint, terminalJointName);
    }

    public Joint.HierarchicalJointAccessor searchPathIndex(Joint start, String terminalJointName) {
        if (start == null || terminalJointName == null) {
            throw new IllegalArgumentException("joint path input is missing");
        }
        String signature = start.getName() + "-" + terminalJointName;
        Joint.HierarchicalJointAccessor cached = this.pathIndexMap.get(signature);
        if (cached != null) {
            return cached;
        }
        Joint.HierarchicalJointAccessor.Builder pathBuilder = start.searchPath(Joint.HierarchicalJointAccessor.builder(), terminalJointName);
        if (pathBuilder == null) {
            throw new IllegalArgumentException("Failed to get joint path index for " + terminalJointName);
        }
        Joint.HierarchicalJointAccessor accessor = pathBuilder.build();
        this.pathIndexMap.put(signature, accessor);
        return accessor;
    }

    public void gatherAllJointsInPathToTerminal(String terminalJointName, Collection<String> jointsInPath) {
        if (jointsInPath == null) {
            throw new IllegalArgumentException("joint path collection is missing");
        }
        if (!this.jointByName.containsKey(terminalJointName)) {
            throw new NoSuchElementException("No " + terminalJointName + " joint in this armature!");
        }
        Joint.HierarchicalJointAccessor pathIndices = this.searchPathIndex(terminalJointName);
        Joint.AccessTicket accessTicket = pathIndices.createAccessTicket(this.rootJoint);
        jointsInPath.add(this.rootJoint.getName());
        while (accessTicket.hasNext()) {
            jointsInPath.add(accessTicket.next().getName());
        }
    }

    public int getJointNumber() {
        return this.jointCount;
    }

    @Override
    public String toString() {
        return this.name;
    }

    public Armature deepCopy() {
        Map<String, Joint> oldToNewJoint = new HashMap<>();
        oldToNewJoint.put("empty", Joint.EMPTY);
        Joint newRoot = this.copyHierarchy(this.rootJoint, oldToNewJoint);
        newRoot.initOriginTransform(new OpenMatrix4f());
        try {
            Constructor<? extends Armature> constructor = this.getClass().getConstructor(String.class, int.class, Joint.class, Map.class);
            return constructor.newInstance(this.name, this.jointCount, newRoot, oldToNewJoint);
        } catch (NoSuchMethodException | SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException failure) {
            throw new IllegalStateException("Armature copy failed! " + failure);
        }
    }

    private Joint copyHierarchy(Joint joint, Map<String, Joint> oldToNewJoint) {
        if (joint == Joint.EMPTY) {
            return Joint.EMPTY;
        }
        Joint newJoint = new Joint(joint.getName(), joint.getId(), joint.getLocalTransform());
        oldToNewJoint.put(joint.getName(), newJoint);
        for (Joint subJoint : joint.getSubJoints()) {
            newJoint.addSubJoints(this.copyHierarchy(subJoint, oldToNewJoint));
        }
        return newJoint;
    }
}

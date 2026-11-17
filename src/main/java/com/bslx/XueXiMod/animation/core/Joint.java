/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/animation/Joint.java
 * Modified: 2026-10-24
 *
 * 修改说明：绑定矩阵必须有限且可逆。遍历深度超过 HIERARCHY_DEPTH_LIMIT 时失败，避免环导致栈溢出。
 * 去掉指向 Armature 的文档型导入。路径相等原先丢掉比较结果，现按签名比较。
 * 子关节索引越界时抛出 NoSuchElementException。
 */
package com.bslx.XueXiMod.animation.core;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.function.Consumer;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

public class Joint {
    public static final int HIERARCHY_DEPTH_LIMIT = 1024;
    public static final Joint EMPTY = new Joint("empty", -1, new OpenMatrix4f());

    private final List<Joint> subJoints = new ArrayList<>();
    private final int jointId;
    private final String jointName;
    private final OpenMatrix4f localTransform;
    private final OpenMatrix4f toOrigin = new OpenMatrix4f();

    public Joint(String name, int jointId, OpenMatrix4f localTransform) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("joint name is missing");
        }
        if (localTransform == null || !localTransform.hasFiniteElements()) {
            throw new IllegalArgumentException("joint bind matrix must be finite");
        }
        float determinant = localTransform.determinant();
        if (!Float.isFinite(determinant) || determinant == 0.0F) {
            throw new IllegalArgumentException("joint bind matrix must be invertible");
        }
        this.jointId = jointId;
        this.jointName = name;
        this.localTransform = localTransform.unmodifiable();
    }

    public void addSubJoints(Joint... joints) {
        if (joints == null) {
            throw new IllegalArgumentException("joint child is missing");
        }
        for (Joint joint : joints) {
            if (joint == null || joint == this) {
                throw new IllegalArgumentException("joint child is missing or recursive");
            }
            if (!this.subJoints.contains(joint)) {
                this.subJoints.add(joint);
            }
        }
    }

    public void removeSubJoints(Joint... joints) {
        if (joints == null) {
            throw new IllegalArgumentException("joint child is missing");
        }
        for (Joint joint : joints) {
            this.subJoints.remove(joint);
        }
    }

    public List<Joint> getAllJoints() {
        List<Joint> list = new ArrayList<>();
        this.collectJoints(list, 0);
        return list;
    }

    public void iterSubJoints(Consumer<Joint> iterTask) {
        if (iterTask == null) {
            throw new IllegalArgumentException("joint task is missing");
        }
        this.iterSubJoints(iterTask, 0);
    }

    private void iterSubJoints(Consumer<Joint> iterTask, int depth) {
        if (depth > HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalStateException("joint traversal is too deep");
        }
        iterTask.accept(this);
        for (Joint joint : this.subJoints) {
            joint.iterSubJoints(iterTask, depth + 1);
        }
    }

    private void collectJoints(List<Joint> list, int depth) {
        if (depth > HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalStateException("joint traversal is too deep");
        }
        list.add(this);
        for (Joint joint : this.subJoints) {
            joint.collectJoints(list, depth + 1);
        }
    }

    public void initOriginTransform(OpenMatrix4f parentTransform) {
        this.initOriginTransform(parentTransform, 0);
    }

    private void initOriginTransform(OpenMatrix4f parentTransform, int depth) {
        if (depth > HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalStateException("joint hierarchy is too deep");
        }
        if (parentTransform == null || !parentTransform.hasFiniteElements()) {
            throw new IllegalArgumentException("parent bind matrix must be finite");
        }
        OpenMatrix4f modelTransform = OpenMatrix4f.mul(parentTransform, this.localTransform, null);
        OpenMatrix4f.invert(modelTransform, this.toOrigin);
        for (Joint joint : this.subJoints) {
            joint.initOriginTransform(modelTransform, depth + 1);
        }
    }

    public OpenMatrix4f getLocalTransform() {
        return this.localTransform;
    }

    public OpenMatrix4f getToOrigin() {
        return this.toOrigin;
    }

    public List<Joint> getSubJoints() {
        return this.subJoints;
    }

    public Joint getSubJoint(int childIndex) {
        if (childIndex < 0 || this.subJoints.size() <= childIndex) {
            return null;
        }
        return this.subJoints.get(childIndex);
    }

    public String getName() {
        return this.jointName;
    }

    public int getId() {
        return this.jointId;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof Joint joint) {
            return this.jointName.equals(joint.jointName) && this.jointId == joint.jointId;
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.jointName.hashCode() ^ this.jointId;
    }

    public HierarchicalJointAccessor.Builder searchPath(HierarchicalJointAccessor.Builder builder, String targetName) {
        return this.searchPath(builder, targetName, 0);
    }

    private HierarchicalJointAccessor.Builder searchPath(HierarchicalJointAccessor.Builder builder, String targetName, int depth) {
        if (depth > HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalStateException("joint path search is too deep");
        }
        if (builder == null || targetName == null) {
            throw new IllegalArgumentException("joint path input is missing");
        }
        if (targetName.equals(this.getName())) {
            return builder;
        }
        int childIndex = 0;
        for (Joint subJoint : this.subJoints) {
            HierarchicalJointAccessor.Builder nextBuilder = subJoint.searchPath(builder.append(childIndex), targetName, depth + 1);
            childIndex++;
            if (nextBuilder != null) {
                return nextBuilder;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        StringBuilder text = new StringBuilder();
        text.append("\nid: ").append(this.jointId);
        text.append("\nname: ").append(this.jointName);
        text.append("\nlocal transform: ").append(this.localTransform);
        text.append("\nto origin: ").append(this.toOrigin);
        text.append("\nchildren: [");
        int shown = 0;
        for (Joint joint : this.subJoints) {
            shown++;
            text.append(joint.jointName);
            if (shown != this.subJoints.size()) {
                text.append(", ");
            }
        }
        text.append("]\n");
        return text.toString();
    }

    public String printIncludingChildren() {
        return this.printIncludingChildren(0);
    }

    private String printIncludingChildren(int depth) {
        if (depth > HIERARCHY_DEPTH_LIMIT) {
            throw new IllegalStateException("joint traversal is too deep");
        }
        StringBuilder text = new StringBuilder();
        text.append(this.toString());
        for (Joint joint : this.subJoints) {
            text.append(joint.printIncludingChildren(depth + 1));
        }
        return text.toString();
    }

    public static class HierarchicalJointAccessor {
        private final Queue<Integer> indicesToTerminal;
        private final String signature;

        private HierarchicalJointAccessor(Builder builder) {
            this.indicesToTerminal = builder.indicesToTerminal;
            this.signature = builder.signature;
        }

        public AccessTicket createAccessTicket(Joint rootJoint) {
            return new AccessTicket(this.indicesToTerminal, rootJoint);
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof HierarchicalJointAccessor accessor) {
                return this.signature.equals(accessor.signature);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return this.signature.hashCode();
        }

        public static Builder builder() {
            return new Builder(new LinkedList<>(), "");
        }

        public static class Builder {
            private final Queue<Integer> indicesToTerminal;
            private final String signature;

            private Builder(Queue<Integer> indicesToTerminal, String signature) {
                this.indicesToTerminal = indicesToTerminal;
                this.signature = signature;
            }

            public Builder append(int childIndex) {
                String signatureNext;
                if (this.indicesToTerminal.isEmpty()) {
                    signatureNext = this.signature + childIndex;
                } else {
                    signatureNext = this.signature + "-" + childIndex;
                }
                Queue<Integer> nextQueue = new LinkedList<>(this.indicesToTerminal);
                nextQueue.add(childIndex);
                return new Builder(nextQueue, signatureNext);
            }

            public HierarchicalJointAccessor build() {
                return new HierarchicalJointAccessor(this);
            }
        }
    }

    public static class AccessTicket implements Iterator<Joint> {
        private final Queue<Integer> accessStack;
        private Joint joint;

        private AccessTicket(Queue<Integer> indicesToTerminal, Joint rootJoint) {
            if (indicesToTerminal == null || rootJoint == null) {
                throw new IllegalArgumentException("joint path input is missing");
            }
            this.accessStack = new LinkedList<>(indicesToTerminal);
            this.joint = rootJoint;
        }

        @Override
        public boolean hasNext() {
            return !this.accessStack.isEmpty();
        }

        @Override
        public Joint next() {
            if (!this.hasNext()) {
                throw new NoSuchElementException();
            }
            int childIndex = this.accessStack.poll();
            List<Joint> children = this.joint.subJoints;
            if (childIndex < 0 || childIndex >= children.size()) {
                throw new NoSuchElementException("joint path index is out of range");
            }
            this.joint = children.get(childIndex);
            return this.joint;
        }
    }
}

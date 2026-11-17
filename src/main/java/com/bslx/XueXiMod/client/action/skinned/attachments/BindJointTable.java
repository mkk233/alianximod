package com.bslx.XueXiMod.client.action.skinned.attachments;

import java.util.HashMap;
import java.util.Map;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

/**
 * 从骨架副本的 toOrigin 反推 Gbind。不读取授权文件里的数字，也不改正在播放的那份骨架。
 * 同一骨架身份和资源代次只建一次。
 */
final class BindJointTable {
    private static BindJointTable cached;
    private static int cachedIdentity;
    private static long cachedGeneration = Long.MIN_VALUE;

    private final Map<String, OpenMatrix4f> globals;
    private final Map<String, Integer> jointIds;
    private final int jointLimit;

    private BindJointTable(Map<String, OpenMatrix4f> globals, Map<String, Integer> jointIds, int jointLimit) {
        this.globals = globals;
        this.jointIds = jointIds;
        this.jointLimit = jointLimit;
    }

    static BindJointTable published(long resourceGeneration) {
        Armature armature = ActionRegistry.sourceArmature();
        if (armature == null) {
            return null;
        }
        return capture(armature, resourceGeneration);
    }

    static BindJointTable capture(Armature armature, long resourceGeneration) {
        if (armature == null) {
            return null;
        }
        int identity = System.identityHashCode(armature);
        synchronized (BindJointTable.class) {
            if (cached != null && cachedIdentity == identity && cachedGeneration == resourceGeneration) {
                return cached;
            }
            BindJointTable table = build(armature);
            cached = table;
            cachedIdentity = identity;
            cachedGeneration = resourceGeneration;
            return table;
        }
    }

    static void clearCache() {
        synchronized (BindJointTable.class) {
            cached = null;
            cachedIdentity = 0;
            cachedGeneration = Long.MIN_VALUE;
        }
    }

    int jointLimit() {
        return this.jointLimit;
    }

    boolean has(String jointName) {
        return jointName != null && this.jointIds.containsKey(jointName);
    }

    int jointId(String jointName) {
        Integer jointId = this.jointIds.get(jointName);
        if (jointId == null) {
            throw new SkinDiagnostic("bind-joint-missing", "骨架缺少关节 " + jointName);
        }
        return jointId;
    }

    OpenMatrix4f global(String jointName) {
        OpenMatrix4f global = this.globals.get(jointName);
        if (global == null) {
            throw new SkinDiagnostic("bind-joint-missing", "骨架缺少关节 " + jointName);
        }
        return new OpenMatrix4f(global);
    }

    Vec3f origin(String jointName) {
        OpenMatrix4f global = this.global(jointName);
        return new Vec3f(global.m30, global.m31, global.m32);
    }

    private static BindJointTable build(Armature armature) {
        Armature copy = armature.deepCopy();
        Map<String, OpenMatrix4f> globals = new HashMap<>();
        Map<String, Integer> jointIds = new HashMap<>();
        int highestId = -1;
        for (Joint joint : copy.rootJoint.getAllJoints()) {
            if (joint == null || joint == Joint.EMPTY || joint.getId() < 0) {
                continue;
            }
            OpenMatrix4f bindGlobal = OpenMatrix4f.invert(joint.getToOrigin(), null);
            if (bindGlobal == null || !bindGlobal.hasFiniteElements()) {
                throw new SkinDiagnostic("bind-missing", "关节 " + joint.getName() + " 的绑定全局不可用");
            }
            globals.put(joint.getName(), bindGlobal);
            jointIds.put(joint.getName(), joint.getId());
            if (joint.getId() > highestId) {
                highestId = joint.getId();
            }
        }
        int jointLimit = copy.getJointNumber();
        if (highestId + 1 > jointLimit) {
            jointLimit = highestId + 1;
        }
        if (jointLimit < 1) {
            throw new SkinDiagnostic("bind-missing", "骨架没有可用关节");
        }
        return new BindJointTable(globals, jointIds, jointLimit);
    }
}

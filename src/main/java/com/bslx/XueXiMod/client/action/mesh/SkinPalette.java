/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/client/model/SkinnedMesh.java
 * Modified: 2026-10-24
 *
 * 修改说明：S = G * inverseBind 只乘一次。prepare 只用 getPoseAsTransformMatrix(pose, false)。
 * 已经乘过 inverseBind 的矩阵必须走 fromAlreadySkinned，不能再乘。
 * 法线用 3x3 逆转置，不用 removeTranslation 冒充非均匀缩放。
 * 附件读取的是未乘 inverseBind 的 G。
 */
package com.bslx.XueXiMod.client.action.mesh;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;

public final class SkinPalette implements PaletteFrames {
    private final String[] jointNames;
    private final float[] globalElements;
    private final float[] skinElements;
    private final float[] normalElements;

    private SkinPalette(String[] jointNames, float[] globalElements, float[] skinElements, float[] normalElements) {
        this.jointNames = jointNames;
        this.globalElements = globalElements;
        this.skinElements = skinElements;
        this.normalElements = normalElements;
    }

    public static SkinPalette prepare(Armature armature, Pose pose) {
        if (armature == null || pose == null) {
            throw new SkinDiagnostic("palette", "骨架或姿态缺失");
        }
        OpenMatrix4f[] globalFrames = armature.getPoseAsTransformMatrix(pose, false);
        return fromGlobalFrames(armature, globalFrames);
    }

    public static SkinPalette fromGlobalFrames(Armature armature, OpenMatrix4f[] globalFrames) {
        int jointCount = checkArmature(armature, globalFrames, null);
        float[] globalElements = new float[jointCount * 16];
        float[] skinElements = new float[jointCount * 16];
        float[] normalElements = new float[jointCount * 9];
        String[] jointNames = new String[jointCount];
        for (int jointId = 0; jointId < jointCount; jointId++) {
            Joint joint = armature.searchJointById(jointId);
            if (joint == null) {
                throw new SkinDiagnostic("palette", "骨架缺少关节 " + jointId);
            }
            jointNames[jointId] = joint.getName();
            OpenMatrix4f globalFrame = globalFrames[jointId];
            if (globalFrame == null || !globalFrame.hasFiniteElements()) {
                throw new SkinDiagnostic("palette", "全局姿态矩阵缺失或不是有限数");
            }
            OpenMatrix4f skin = OpenMatrix4f.mul(globalFrame, joint.getToOrigin(), null);
            if (!skin.hasFiniteElements()) {
                throw new SkinDiagnostic("palette", "蒙皮矩阵不是有限数");
            }
            storeMatrix(globalFrame, globalElements, jointId * 16);
            storeMatrix(skin, skinElements, jointId * 16);
            writeInverseTranspose(skin, normalElements, jointId * 9);
        }
        rejectDuplicateNames(jointNames);
        return new SkinPalette(jointNames, globalElements, skinElements, normalElements);
    }

    public static SkinPalette fromAlreadySkinned(Armature armature, OpenMatrix4f[] globalFrames, OpenMatrix4f[] skinMatrices) {
        int jointCount = checkArmature(armature, globalFrames, skinMatrices);
        float[] globalElements = new float[jointCount * 16];
        float[] skinElements = new float[jointCount * 16];
        float[] normalElements = new float[jointCount * 9];
        String[] jointNames = new String[jointCount];
        for (int jointId = 0; jointId < jointCount; jointId++) {
            Joint joint = armature.searchJointById(jointId);
            if (joint == null) {
                throw new SkinDiagnostic("palette", "骨架缺少关节 " + jointId);
            }
            jointNames[jointId] = joint.getName();
            OpenMatrix4f globalFrame = globalFrames[jointId];
            OpenMatrix4f skin = skinMatrices[jointId];
            if (globalFrame == null || skin == null || !globalFrame.hasFiniteElements() || !skin.hasFiniteElements()) {
                throw new SkinDiagnostic("palette", "已经蒙皮的矩阵缺失或不是有限数");
            }
            storeMatrix(globalFrame, globalElements, jointId * 16);
            storeMatrix(skin, skinElements, jointId * 16);
            writeInverseTranspose(skin, normalElements, jointId * 9);
        }
        rejectDuplicateNames(jointNames);
        return new SkinPalette(jointNames, globalElements, skinElements, normalElements);
    }

    @Override
    public int jointCount() {
        return this.jointNames.length;
    }

    @Override
    public OpenMatrix4f copyGlobalFrame(int jointId) {
        return copyMatrix(this.globalElements, jointId);
    }

    @Override
    public OpenMatrix4f copyGlobalFrame(String jointName) {
        return this.copyGlobalFrame(this.jointId(jointName));
    }

    @Override
    public OpenMatrix4f copySkinMatrix(int jointId) {
        return copyMatrix(this.skinElements, jointId);
    }

    @Override
    public OpenMatrix4f copyToolRightFrame() {
        return this.copyGlobalFrame("Tool_R");
    }

    @Override
    public OpenMatrix4f copyToolLeftFrame() {
        return this.copyGlobalFrame("Tool_L");
    }

    @Override
    public OpenMatrix4f copyHandRightFrame() {
        return this.copyGlobalFrame("Hand_R");
    }

    @Override
    public OpenMatrix4f copyHandLeftFrame() {
        return this.copyGlobalFrame("Hand_L");
    }

    @Override
    public OpenMatrix4f copyHeadFrame() {
        return this.copyGlobalFrame("Head");
    }

    @Override
    public OpenMatrix4f copyChestFrame() {
        return this.copyGlobalFrame("Chest");
    }

    float skin(int jointId, int element) {
        return this.skinElements[jointId * 16 + element];
    }

    float normalLinear(int jointId, int element) {
        return this.normalElements[jointId * 9 + element];
    }

    static void writeInverseTranspose(OpenMatrix4f skin, float[] destination, int offset) {
        float a00 = skin.m00;
        float a01 = skin.m10;
        float a02 = skin.m20;
        float a10 = skin.m01;
        float a11 = skin.m11;
        float a12 = skin.m21;
        float a20 = skin.m02;
        float a21 = skin.m12;
        float a22 = skin.m22;
        float determinant = a00 * (a11 * a22 - a12 * a21) - a01 * (a10 * a22 - a12 * a20) + a02 * (a10 * a21 - a11 * a20);
        if (!Float.isFinite(determinant) || Math.abs(determinant) < 1.0E-8F) {
            throw new SkinDiagnostic("normal-matrix", "蒙皮矩阵的法线部分不可逆");
        }
        float inverseDeterminant = 1.0F / determinant;
        destination[offset] = (a11 * a22 - a12 * a21) * inverseDeterminant;
        destination[offset + 1] = (a12 * a20 - a10 * a22) * inverseDeterminant;
        destination[offset + 2] = (a10 * a21 - a11 * a20) * inverseDeterminant;
        destination[offset + 3] = (a02 * a21 - a01 * a22) * inverseDeterminant;
        destination[offset + 4] = (a00 * a22 - a02 * a20) * inverseDeterminant;
        destination[offset + 5] = (a01 * a20 - a00 * a21) * inverseDeterminant;
        destination[offset + 6] = (a01 * a12 - a02 * a11) * inverseDeterminant;
        destination[offset + 7] = (a02 * a10 - a00 * a12) * inverseDeterminant;
        destination[offset + 8] = (a00 * a11 - a01 * a10) * inverseDeterminant;
        for (int elementIndex = 0; elementIndex < 9; elementIndex++) {
            if (!Float.isFinite(destination[offset + elementIndex])) {
                throw new SkinDiagnostic("normal-matrix", "法线矩阵不是有限数");
            }
        }
    }

    private int jointId(String jointName) {
        if (jointName == null) {
            throw new SkinDiagnostic("joint-name", "关节名缺失");
        }
        for (int jointId = 0; jointId < this.jointNames.length; jointId++) {
            if (jointName.equals(this.jointNames[jointId])) {
                return jointId;
            }
        }
        throw new SkinDiagnostic("joint-name", "骨架里没有关节 " + jointName);
    }

    private static OpenMatrix4f copyMatrix(float[] elements, int jointId) {
        if (jointId < 0 || jointId >= elements.length / 16) {
            throw new SkinDiagnostic("joint-id", "关节编号越界");
        }
        int offset = jointId * 16;
        return new OpenMatrix4f(
                elements[offset], elements[offset + 1], elements[offset + 2], elements[offset + 3],
                elements[offset + 4], elements[offset + 5], elements[offset + 6], elements[offset + 7],
                elements[offset + 8], elements[offset + 9], elements[offset + 10], elements[offset + 11],
                elements[offset + 12], elements[offset + 13], elements[offset + 14], elements[offset + 15]);
    }

    private static void storeMatrix(OpenMatrix4f matrix, float[] destination, int offset) {
        destination[offset] = matrix.m00;
        destination[offset + 1] = matrix.m01;
        destination[offset + 2] = matrix.m02;
        destination[offset + 3] = matrix.m03;
        destination[offset + 4] = matrix.m10;
        destination[offset + 5] = matrix.m11;
        destination[offset + 6] = matrix.m12;
        destination[offset + 7] = matrix.m13;
        destination[offset + 8] = matrix.m20;
        destination[offset + 9] = matrix.m21;
        destination[offset + 10] = matrix.m22;
        destination[offset + 11] = matrix.m23;
        destination[offset + 12] = matrix.m30;
        destination[offset + 13] = matrix.m31;
        destination[offset + 14] = matrix.m32;
        destination[offset + 15] = matrix.m33;
    }

    private static int checkArmature(Armature armature, OpenMatrix4f[] globalFrames, OpenMatrix4f[] skinMatrices) {
        if (armature == null || globalFrames == null) {
            throw new SkinDiagnostic("palette", "骨架或全局矩阵缺失");
        }
        int jointCount = armature.getJointNumber();
        if (jointCount < 1 || jointCount > MeshLimits.JOINT_LIMIT || globalFrames.length != jointCount) {
            throw new SkinDiagnostic("palette", "全局矩阵数量和骨架关节数不一致");
        }
        if (skinMatrices != null && skinMatrices.length != jointCount) {
            throw new SkinDiagnostic("palette", "已蒙皮矩阵数量和骨架关节数不一致");
        }
        return jointCount;
    }

    private static void rejectDuplicateNames(String[] jointNames) {
        for (int leftIndex = 0; leftIndex < jointNames.length; leftIndex++) {
            if (jointNames[leftIndex] == null || jointNames[leftIndex].isBlank()) {
                throw new SkinDiagnostic("joint-name", "关节名缺失");
            }
            for (int rightIndex = leftIndex + 1; rightIndex < jointNames.length; rightIndex++) {
                if (jointNames[leftIndex].equals(jointNames[rightIndex])) {
                    throw new SkinDiagnostic("joint-name", "关节名重复");
                }
            }
        }
    }
}

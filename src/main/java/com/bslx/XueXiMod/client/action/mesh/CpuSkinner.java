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
 * 修改说明：位置按权重累加 S*p。法线按权重累加逆转置后的方向再归一化，不吃平移。
 * 每个位置每帧写一次。法线按位置和法线下标一起缓存。循环里不分配对象。
 */
package com.bslx.XueXiMod.client.action.mesh;

import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.model.Armature;

public final class CpuSkinner {
    private CpuSkinner() {
    }

    public static SkinPalette preparePalette(Armature armature, Pose pose) {
        return SkinPalette.prepare(armature, pose);
    }

    public static void deform(PlayerSkinMesh mesh, SkinPalette palette, SkinLease lease) {
        if (mesh == null || palette == null || lease == null) {
            throw new SkinDiagnostic("deform", "蒙皮输入缺失");
        }
        if (Thread.currentThread() != lease.ownerThread()) {
            throw new SkinDiagnostic("thread", "蒙皮缓冲不能跨线程使用");
        }
        SkinWorkspace workspace = lease.workspace();
        if (!workspace.leased || workspace.positionCount != mesh.positionCount() || workspace.normalCount != mesh.normalCount()) {
            throw new SkinDiagnostic("workspace", "蒙皮缓冲和网格不匹配");
        }
        if (palette.jointCount() < mesh.jointLimit()) {
            throw new SkinDiagnostic("palette", "调色板关节数盖不住网格");
        }
        if (workspace.reuseDeform(mesh, palette)) {
            return;
        }
        workspace.invalidateDeform();
        workspace.normalEvaluations = 0;
        int normalPairs = mesh.positionCount() * mesh.normalCount();
        for (int pairIndex = 0; pairIndex < normalPairs; pairIndex++) {
            workspace.normalReady[pairIndex] = 0;
        }
        for (int positionIndex = 0; positionIndex < mesh.positionCount(); positionIndex++) {
            float sourceX = mesh.positionX(positionIndex);
            float sourceY = mesh.positionY(positionIndex);
            float sourceZ = mesh.positionZ(positionIndex);
            float accumulatedX = 0.0F;
            float accumulatedY = 0.0F;
            float accumulatedZ = 0.0F;
            int start = mesh.influenceStart(positionIndex);
            int influenceCount = mesh.influenceCountAt(positionIndex);
            for (int influenceSlot = 0; influenceSlot < influenceCount; influenceSlot++) {
                int packed = start + influenceSlot;
                int jointId = mesh.jointAtPacked(packed);
                float weight = mesh.weightAtPacked(packed);
                float transformedX = palette.skin(jointId, 0) * sourceX + palette.skin(jointId, 4) * sourceY
                        + palette.skin(jointId, 8) * sourceZ + palette.skin(jointId, 12);
                float transformedY = palette.skin(jointId, 1) * sourceX + palette.skin(jointId, 5) * sourceY
                        + palette.skin(jointId, 9) * sourceZ + palette.skin(jointId, 13);
                float transformedZ = palette.skin(jointId, 2) * sourceX + palette.skin(jointId, 6) * sourceY
                        + palette.skin(jointId, 10) * sourceZ + palette.skin(jointId, 14);
                accumulatedX += weight * transformedX;
                accumulatedY += weight * transformedY;
                accumulatedZ += weight * transformedZ;
            }
            if (!Float.isFinite(accumulatedX) || !Float.isFinite(accumulatedY) || !Float.isFinite(accumulatedZ)) {
                throw new SkinDiagnostic("deform", "蒙皮位置不是有限数");
            }
            int offset = positionIndex * 3;
            workspace.deformedPositions[offset] = accumulatedX;
            workspace.deformedPositions[offset + 1] = accumulatedY;
            workspace.deformedPositions[offset + 2] = accumulatedZ;
        }
        workspace.publishDeform(mesh, palette);
    }

    public static void copyDeformed(SkinLease lease, int positionIndex, float[] destination) {
        if (lease == null || destination == null || destination.length < 3) {
            throw new SkinDiagnostic("deform", "读取蒙皮位置的目标缺失");
        }
        if (Thread.currentThread() != lease.ownerThread()) {
            throw new SkinDiagnostic("thread", "蒙皮缓冲不能跨线程使用");
        }
        SkinWorkspace workspace = lease.workspace();
        if (positionIndex < 0 || positionIndex >= workspace.positionCount) {
            throw new SkinDiagnostic("position-index", "位置下标越界");
        }
        int offset = positionIndex * 3;
        destination[0] = workspace.deformedPositions[offset];
        destination[1] = workspace.deformedPositions[offset + 1];
        destination[2] = workspace.deformedPositions[offset + 2];
    }

    static void ensureNormal(PlayerSkinMesh mesh, SkinPalette palette, SkinWorkspace workspace, int positionIndex, int normalIndex) {
        int pairIndex = positionIndex * workspace.normalCount + normalIndex;
        if (workspace.normalReady[pairIndex] == 1) {
            return;
        }
        float sourceX = mesh.normalX(normalIndex);
        float sourceY = mesh.normalY(normalIndex);
        float sourceZ = mesh.normalZ(normalIndex);
        float accumulatedX = 0.0F;
        float accumulatedY = 0.0F;
        float accumulatedZ = 0.0F;
        int start = mesh.influenceStart(positionIndex);
        int influenceCount = mesh.influenceCountAt(positionIndex);
        for (int influenceSlot = 0; influenceSlot < influenceCount; influenceSlot++) {
            int packed = start + influenceSlot;
            int jointId = mesh.jointAtPacked(packed);
            float weight = mesh.weightAtPacked(packed);
            float transformedX = palette.normalLinear(jointId, 0) * sourceX + palette.normalLinear(jointId, 1) * sourceY
                    + palette.normalLinear(jointId, 2) * sourceZ;
            float transformedY = palette.normalLinear(jointId, 3) * sourceX + palette.normalLinear(jointId, 4) * sourceY
                    + palette.normalLinear(jointId, 5) * sourceZ;
            float transformedZ = palette.normalLinear(jointId, 6) * sourceX + palette.normalLinear(jointId, 7) * sourceY
                    + palette.normalLinear(jointId, 8) * sourceZ;
            accumulatedX += weight * transformedX;
            accumulatedY += weight * transformedY;
            accumulatedZ += weight * transformedZ;
        }
        float lengthSquared = accumulatedX * accumulatedX + accumulatedY * accumulatedY + accumulatedZ * accumulatedZ;
        if (!Float.isFinite(lengthSquared) || lengthSquared < 1.0E-12F) {
            throw new SkinDiagnostic("normal-degenerate", "蒙皮法线长度退化");
        }
        float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);
        float normalX = accumulatedX * inverseLength;
        float normalY = accumulatedY * inverseLength;
        float normalZ = accumulatedZ * inverseLength;
        if (!Float.isFinite(normalX) || !Float.isFinite(normalY) || !Float.isFinite(normalZ)) {
            throw new SkinDiagnostic("normal-degenerate", "蒙皮法线不是有限数");
        }
        int offset = pairIndex * 3;
        workspace.deformedNormals[offset] = normalX;
        workspace.deformedNormals[offset + 1] = normalY;
        workspace.deformedNormals[offset + 2] = normalZ;
        workspace.normalReady[pairIndex] = 1;
        workspace.normalEvaluations++;
    }
}

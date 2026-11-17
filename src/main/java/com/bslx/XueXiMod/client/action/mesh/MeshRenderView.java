package com.bslx.XueXiMod.client.action.mesh;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

/**
 * 把蒙皮后的三角形按 a,b,c,c 退化成原版四边形角点。
 * 外部根变换或世界对齐只乘这一次，不按分区绕各自枢轴再转。
 * 这里不烘焙站立平移，也不乘玩家缩放。
 */
public final class MeshRenderView {
    private MeshRenderView() {
    }

    public static int emit(
            PlayerSkinMesh mesh,
            SkinPalette palette,
            SkinLease lease,
            PartVisibility visibility,
            OpenMatrix4f wholeMeshTransform,
            int packedLight,
            int overlay,
            CornerSink sink) {
        if (mesh == null || palette == null || lease == null || visibility == null || sink == null) {
            throw new SkinDiagnostic("emit", "绘制输入缺失");
        }
        CpuSkinner.deform(mesh, palette, lease);
        SkinWorkspace workspace = lease.workspace();
        workspace.transformWhole = wholeMeshTransform != null;
        if (workspace.transformWhole) {
            if (!wholeMeshTransform.hasFiniteElements()) {
                throw new SkinDiagnostic("mesh-transform", "整网变换不是有限数");
            }
            storeWhole(wholeMeshTransform, workspace.wholeElements);
            SkinPalette.writeInverseTranspose(wholeMeshTransform, workspace.wholeNormal, 0);
        }
        for (PlayerMeshPart part : PlayerMeshPart.values()) {
            if (!visible(mesh, visibility, part)) {
                continue;
            }
            int start = mesh.partStart(part);
            int cornerCount = mesh.partCornerCount(part);
            for (int cornerIndex = 0; cornerIndex < cornerCount; cornerIndex++) {
                placeCorner(mesh, palette, workspace, start + cornerIndex);
            }
        }
        int emitted = 0;
        for (PlayerMeshPart part : PlayerMeshPart.values()) {
            if (!visible(mesh, visibility, part)) {
                continue;
            }
            int start = mesh.partStart(part);
            int cornerCount = mesh.partCornerCount(part);
            for (int cornerIndex = 0; cornerIndex < cornerCount; cornerIndex += 3) {
                emitCorner(mesh, palette, workspace, sink, start + cornerIndex, packedLight, overlay);
                emitCorner(mesh, palette, workspace, sink, start + cornerIndex + 1, packedLight, overlay);
                emitCorner(mesh, palette, workspace, sink, start + cornerIndex + 2, packedLight, overlay);
                emitCorner(mesh, palette, workspace, sink, start + cornerIndex + 2, packedLight, overlay);
                emitted += 4;
            }
        }
        return emitted;
    }

    private static boolean visible(PlayerSkinMesh mesh, PartVisibility visibility, PlayerMeshPart part) {
        return mesh.hasPart(part) && visibility.shows(part);
    }

    private static void emitCorner(
            PlayerSkinMesh mesh,
            SkinPalette palette,
            SkinWorkspace workspace,
            CornerSink sink,
            int cornerIndex,
            int packedLight,
            int overlay) {
        placeCorner(mesh, palette, workspace, cornerIndex);
        int textureIndex = mesh.cornerTexture(cornerIndex);
        sink.corner(
                workspace.scratchPoint[0],
                workspace.scratchPoint[1],
                workspace.scratchPoint[2],
                workspace.placedNormal[0],
                workspace.placedNormal[1],
                workspace.placedNormal[2],
                mesh.textureU(textureIndex),
                mesh.textureV(textureIndex),
                packedLight,
                overlay);
    }

    private static void placeCorner(PlayerSkinMesh mesh, SkinPalette palette, SkinWorkspace workspace, int cornerIndex) {
        int positionIndex = mesh.cornerPosition(cornerIndex);
        int normalIndex = mesh.cornerNormal(cornerIndex);
        CpuSkinner.ensureNormal(mesh, palette, workspace, positionIndex, normalIndex);
        int positionOffset = positionIndex * 3;
        float positionX = workspace.deformedPositions[positionOffset];
        float positionY = workspace.deformedPositions[positionOffset + 1];
        float positionZ = workspace.deformedPositions[positionOffset + 2];
        int normalOffset = (positionIndex * workspace.normalCount + normalIndex) * 3;
        float normalX = workspace.deformedNormals[normalOffset];
        float normalY = workspace.deformedNormals[normalOffset + 1];
        float normalZ = workspace.deformedNormals[normalOffset + 2];
        if (workspace.transformWhole) {
            float movedX = workspace.wholeElements[0] * positionX + workspace.wholeElements[4] * positionY
                    + workspace.wholeElements[8] * positionZ + workspace.wholeElements[12];
            float movedY = workspace.wholeElements[1] * positionX + workspace.wholeElements[5] * positionY
                    + workspace.wholeElements[9] * positionZ + workspace.wholeElements[13];
            float movedZ = workspace.wholeElements[2] * positionX + workspace.wholeElements[6] * positionY
                    + workspace.wholeElements[10] * positionZ + workspace.wholeElements[14];
            if (!Float.isFinite(movedX) || !Float.isFinite(movedY) || !Float.isFinite(movedZ)) {
                throw new SkinDiagnostic("mesh-transform", "整网位置不是有限数");
            }
            positionX = movedX;
            positionY = movedY;
            positionZ = movedZ;
            float bentX = workspace.wholeNormal[0] * normalX + workspace.wholeNormal[1] * normalY + workspace.wholeNormal[2] * normalZ;
            float bentY = workspace.wholeNormal[3] * normalX + workspace.wholeNormal[4] * normalY + workspace.wholeNormal[5] * normalZ;
            float bentZ = workspace.wholeNormal[6] * normalX + workspace.wholeNormal[7] * normalY + workspace.wholeNormal[8] * normalZ;
            float lengthSquared = bentX * bentX + bentY * bentY + bentZ * bentZ;
            if (!Float.isFinite(lengthSquared) || lengthSquared < 1.0E-12F) {
                throw new SkinDiagnostic("normal-degenerate", "整网法线长度退化");
            }
            float inverseLength = 1.0F / (float) Math.sqrt(lengthSquared);
            normalX = bentX * inverseLength;
            normalY = bentY * inverseLength;
            normalZ = bentZ * inverseLength;
        }
        workspace.scratchPoint[0] = positionX;
        workspace.scratchPoint[1] = positionY;
        workspace.scratchPoint[2] = positionZ;
        workspace.placedNormal[0] = normalX;
        workspace.placedNormal[1] = normalY;
        workspace.placedNormal[2] = normalZ;
    }

    private static void storeWhole(OpenMatrix4f matrix, float[] destination) {
        destination[0] = matrix.m00;
        destination[1] = matrix.m01;
        destination[2] = matrix.m02;
        destination[3] = matrix.m03;
        destination[4] = matrix.m10;
        destination[5] = matrix.m11;
        destination[6] = matrix.m12;
        destination[7] = matrix.m13;
        destination[8] = matrix.m20;
        destination[9] = matrix.m21;
        destination[10] = matrix.m22;
        destination[11] = matrix.m23;
        destination[12] = matrix.m30;
        destination[13] = matrix.m31;
        destination[14] = matrix.m32;
        destination[15] = matrix.m33;
    }
}

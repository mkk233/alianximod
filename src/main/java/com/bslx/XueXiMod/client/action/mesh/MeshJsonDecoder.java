/*
 * SPDX-License-Identifier: GPL-3.0-only
 *
 * Copyright (C) Epic Fight Team / Yesman
 * Local modifications Copyright (C) 2026 XueXi Mod contributors.
 *
 * Upstream: https://github.com/Antikythera-Studios/epicfight
 * Commit: 9d25f19c121c5e51b1db9efe01833a563706eeee
 * Source-Path: src/main/java/yesman/epicfight/api/asset/JsonAssetLoader.java
 * Modified: 2026-10-24
 *
 * 修改说明：只保留 skinned vertices 的索引和载入时坐标转换。
 * 位置、法线乘 rotX(-90) 一次；法线用 w=0。UV 和权重不改，不除以 16。
 * 不读取纹理、shader、armor，也不接受未知格式悄悄兼容。
 */
package com.bslx.XueXiMod.client.action.mesh;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.math.Vec4f;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

public final class MeshJsonDecoder {
    private MeshJsonDecoder() {
    }

    public static PlayerSkinMesh decode(String text, PlayerMeshKind expectedKind) {
        if (text == null || expectedKind == null) {
            throw new SkinDiagnostic("mesh-json", "网格文本或体型缺失");
        }
        if (text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > MeshLimits.FILE_BYTE_LIMIT) {
            throw new SkinDiagnostic("file-size", "网格文件超过上限");
        }
        try (JsonReader reader = new JsonReader(new StringReader(text))) {
            reader.setLenient(false);
            ParsedMesh parsed = readDocument(reader);
            if (reader.peek() != JsonToken.END_DOCUMENT) {
                throw new SkinDiagnostic("mesh-json", "网格文件末尾还有多余内容");
            }
            if (parsed.schemaVersion != 1) {
                throw new SkinDiagnostic("schema", "不认识的网格 schema，不会按旧格式兼容");
            }
            if (parsed.kind != expectedKind) {
                throw new SkinDiagnostic("model", "文件体型和请求不一致");
            }
            if (!MeshLimits.UPSTREAM_COMMIT.equals(parsed.upstreamCommit)) {
                throw new SkinDiagnostic("commit", "网格提交不是固定的 9d25f19c");
            }
            if (!expectedKind.sourceGitPath().equals(parsed.sourceGitPath)) {
                throw new SkinDiagnostic("source-path", "网格来源路径不是授权的玩家网格");
            }
            if (!parsed.localOnly || parsed.playbackReady) {
                throw new SkinDiagnostic("local-only", "网格必须标明仅本地，且不能写成已经播放验收");
            }
            if (!MeshLimits.AUTHORIZATION.equals(parsed.authorization)) {
                throw new SkinDiagnostic("authorization", "网格授权声明不匹配");
            }
            if (!"source-meters".equals(parsed.coordinateSpace)) {
                throw new SkinDiagnostic("coordinates", "网格坐标不是源米制");
            }
            if (!MeshLimits.RIG_RELATIVE_PATH.equals(parsed.rigRelativePath)) {
                throw new SkinDiagnostic("rig", "网格没有复用已有骨架元数据路径");
            }
            if (!parsed.coordinateConversionApplied) {
                applyAxisConversion(parsed.positions, false);
                applyAxisConversion(parsed.normals, true);
            }
            checkCounts(parsed);
            return PlayerSkinMesh.compose(parsed.kind, MeshLimits.JOINT_LIMIT, parsed.positions, parsed.normals,
                    parsed.textureCoordinates, parsed.weights, parsed.influenceCounts, parsed.influenceJoints,
                    parsed.influenceWeightSlots, parsed.partCorners, true, parsed.sourceGitPath, parsed.sourceSha256,
                    true);
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (RuntimeException | java.io.IOException failure) {
            throw new SkinDiagnostic("mesh-json", "网格文件无法解析：" + failure.getMessage());
        }
    }

    private static void applyAxisConversion(float[] coordinates, boolean direction) {
        OpenMatrix4f conversion = OpenMatrix4f.createRotatorDeg(-90.0F, Vec3f.X_AXIS);
        Vec4f source = new Vec4f();
        Vec4f converted = new Vec4f();
        float homogeneous = direction ? 0.0F : 1.0F;
        for (int elementIndex = 0; elementIndex < coordinates.length; elementIndex += 3) {
            source.set(coordinates[elementIndex], coordinates[elementIndex + 1], coordinates[elementIndex + 2], homogeneous);
            OpenMatrix4f.transform(conversion, source, converted);
            if (!Float.isFinite(converted.coordinateX) || !Float.isFinite(converted.coordinateY) || !Float.isFinite(converted.coordinateZ)) {
                throw new SkinDiagnostic("coordinates", "坐标转换结果不是有限数");
            }
            coordinates[elementIndex] = converted.coordinateX;
            coordinates[elementIndex + 1] = converted.coordinateY;
            coordinates[elementIndex + 2] = converted.coordinateZ;
        }
    }

    private static void checkCounts(ParsedMesh parsed) {
        int positionCount = parsed.positions.length / 3;
        int normalCount = parsed.normals.length / 3;
        int textureCount = parsed.textureCoordinates.length / 2;
        int influenceTotal = 0;
        for (int count : parsed.influenceCounts) {
            influenceTotal += count;
        }
        int cornerTotal = 0;
        int presentParts = 0;
        for (int[] corners : parsed.partCorners) {
            if (corners != null) {
                presentParts++;
                cornerTotal += corners.length / 3;
            }
        }
        if (parsed.countPositions != positionCount || parsed.countNormals != normalCount || parsed.countTextures != textureCount
                || parsed.countWeights != parsed.weights.length || parsed.countInfluences != influenceTotal
                || parsed.countCorners != cornerTotal || parsed.countTriangles != cornerTotal / 3
                || parsed.countParts != presentParts || parsed.highestInfluence != highest(parsed.influenceCounts)) {
            throw new SkinDiagnostic("counts", "文件声明的计数和数组不一致");
        }
    }

    private static int highest(int[] counts) {
        int highest = 0;
        for (int count : counts) {
            if (count > highest) {
                highest = count;
            }
        }
        return highest;
    }

    private static ParsedMesh readDocument(JsonReader reader) throws java.io.IOException {
        ParsedMesh parsed = new ParsedMesh();
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            switch (fieldName) {
                case "schemaVersion" -> parsed.schemaVersion = reader.nextInt();
                case "model" -> parsed.kind = PlayerMeshKind.fromWireName(reader.nextString());
                case "upstreamCommit" -> parsed.upstreamCommit = reader.nextString();
                case "sourceGitPath" -> parsed.sourceGitPath = reader.nextString();
                case "sourceSha256" -> parsed.sourceSha256 = reader.nextString();
                case "localOnly" -> parsed.localOnly = reader.nextBoolean();
                case "authorization" -> parsed.authorization = reader.nextString();
                case "rigRelativePath" -> parsed.rigRelativePath = reader.nextString();
                case "coordinateSpace" -> parsed.coordinateSpace = reader.nextString();
                case "coordinateConversionApplied" -> parsed.coordinateConversionApplied = reader.nextBoolean();
                case "playbackReady" -> parsed.playbackReady = reader.nextBoolean();
                case "positions" -> parsed.positions = readFloatChannel(reader, 3);
                case "normals" -> parsed.normals = readFloatChannel(reader, 3);
                case "uvs" -> parsed.textureCoordinates = readFloatChannel(reader, 2);
                case "weights" -> parsed.weights = readFloatChannel(reader, 1);
                case "vcounts" -> parsed.influenceCounts = readIntChannel(reader, 1);
                case "vindices" -> readInfluences(reader, parsed);
                case "parts" -> parsed.partCorners = readParts(reader);
                case "counts" -> readCounts(reader, parsed);
                default -> throw new SkinDiagnostic("unknown-field", "网格含未知字段 " + fieldName);
            }
        }
        reader.endObject();
        if (parsed.kind == null || parsed.positions == null || parsed.normals == null || parsed.textureCoordinates == null
                || parsed.weights == null || parsed.influenceCounts == null || parsed.influenceJoints == null
                || parsed.partCorners == null || parsed.authorization == null || parsed.authorization.isBlank()
                || parsed.sourceSha256 == null || parsed.sourceSha256.length() != 64) {
            throw new SkinDiagnostic("mesh-json", "网格缺少必需字段");
        }
        return parsed;
    }

    private static float[] readFloatChannel(JsonReader reader, int expectedStride) throws java.io.IOException {
        int stride = -1;
        float[] values = null;
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            switch (fieldName) {
                case "stride" -> stride = reader.nextInt();
                case "array" -> values = readFloatArray(reader);
                default -> throw new SkinDiagnostic("unknown-field", "数值通道含未知字段 " + fieldName);
            }
        }
        reader.endObject();
        if (stride != expectedStride || values == null || values.length % expectedStride != 0) {
            throw new SkinDiagnostic("stride", "数值通道步长不是 " + expectedStride);
        }
        return values;
    }

    private static int[] readIntChannel(JsonReader reader, int expectedStride) throws java.io.IOException {
        int stride = -1;
        int[] values = null;
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            switch (fieldName) {
                case "stride" -> stride = reader.nextInt();
                case "array" -> values = readIntArray(reader);
                default -> throw new SkinDiagnostic("unknown-field", "整数通道含未知字段 " + fieldName);
            }
        }
        reader.endObject();
        if (stride != expectedStride || values == null || values.length % expectedStride != 0) {
            throw new SkinDiagnostic("stride", "整数通道步长不是 " + expectedStride);
        }
        return values;
    }

    private static void readInfluences(JsonReader reader, ParsedMesh parsed) throws java.io.IOException {
        int[] flat = readIntChannel(reader, 2);
        if (flat.length % 2 != 0) {
            throw new SkinDiagnostic("vindices", "影响索引不是成对的");
        }
        int pairs = flat.length / 2;
        parsed.influenceJoints = new int[pairs];
        parsed.influenceWeightSlots = new int[pairs];
        for (int pairIndex = 0; pairIndex < pairs; pairIndex++) {
            parsed.influenceJoints[pairIndex] = flat[pairIndex * 2];
            parsed.influenceWeightSlots[pairIndex] = flat[pairIndex * 2 + 1];
        }
    }

    private static int[][] readParts(JsonReader reader) throws java.io.IOException {
        int[][] partCorners = new int[PlayerMeshPart.values().length][];
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            PlayerMeshPart part = PlayerMeshPart.fromWireName(fieldName);
            if (part == null) {
                throw new SkinDiagnostic("parts", "未知分区 " + fieldName);
            }
            partCorners[part.ordinal()] = readIntChannel(reader, 3);
        }
        reader.endObject();
        if (seen.size() != PlayerMeshPart.values().length) {
            throw new SkinDiagnostic("parts", "玩家网格必须正好有 12 个分区");
        }
        return partCorners;
    }

    private static void readCounts(JsonReader reader, ParsedMesh parsed) throws java.io.IOException {
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            int value = reader.nextInt();
            switch (fieldName) {
                case "parts" -> parsed.countParts = value;
                case "positions" -> parsed.countPositions = value;
                case "normals" -> parsed.countNormals = value;
                case "uvs" -> parsed.countTextures = value;
                case "weights" -> parsed.countWeights = value;
                case "influences" -> parsed.countInfluences = value;
                case "corners" -> parsed.countCorners = value;
                case "triangles" -> parsed.countTriangles = value;
                case "highestInfluence" -> parsed.highestInfluence = value;
                default -> throw new SkinDiagnostic("unknown-field", "计数含未知字段 " + fieldName);
            }
        }
        reader.endObject();
        if (seen.size() != 9) {
            throw new SkinDiagnostic("counts", "计数对象不完整");
        }
    }

    private static float[] readFloatArray(JsonReader reader) throws java.io.IOException {
        List<Float> values = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            double number = reader.nextDouble();
            if (!Double.isFinite(number)) {
                throw new SkinDiagnostic("finite", "网格含有非有限数");
            }
            values.add((float) number);
        }
        reader.endArray();
        float[] copy = new float[values.size()];
        for (int elementIndex = 0; elementIndex < values.size(); elementIndex++) {
            copy[elementIndex] = values.get(elementIndex);
        }
        return copy;
    }

    private static int[] readIntArray(JsonReader reader) throws java.io.IOException {
        List<Integer> values = new ArrayList<>();
        reader.beginArray();
        while (reader.hasNext()) {
            values.add(reader.nextInt());
        }
        reader.endArray();
        int[] copy = new int[values.size()];
        for (int elementIndex = 0; elementIndex < values.size(); elementIndex++) {
            copy[elementIndex] = values.get(elementIndex);
        }
        return copy;
    }

    private static String takeName(JsonReader reader, Set<String> seen) throws java.io.IOException {
        String fieldName = reader.nextName();
        if (!seen.add(fieldName)) {
            throw new SkinDiagnostic("duplicate-field", "字段重复：" + fieldName);
        }
        return fieldName;
    }

    private static final class ParsedMesh {
        private int schemaVersion = -1;
        private PlayerMeshKind kind;
        private String upstreamCommit = "";
        private String sourceGitPath = "";
        private String sourceSha256 = "";
        private boolean localOnly;
        private String authorization;
        private String rigRelativePath = "";
        private String coordinateSpace = "";
        private boolean coordinateConversionApplied;
        private boolean playbackReady = true;
        private float[] positions;
        private float[] normals;
        private float[] textureCoordinates;
        private float[] weights;
        private int[] influenceCounts;
        private int[] influenceJoints;
        private int[] influenceWeightSlots;
        private int[][] partCorners;
        private int countParts = -1;
        private int countPositions = -1;
        private int countNormals = -1;
        private int countTextures = -1;
        private int countWeights = -1;
        private int countInfluences = -1;
        private int countCorners = -1;
        private int countTriangles = -1;
        private int highestInfluence = -1;
    }
}

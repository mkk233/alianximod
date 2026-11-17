package com.bslx.XueXiMod.client.action.mesh;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;

/** 独立的 mesh 清单。不改动作清单。 */
final class MeshCatalog {
    final String rigSha256;
    final MeshEntry normalEntry;
    final MeshEntry slimEntry;

    private MeshCatalog(String rigSha256, MeshEntry normalEntry, MeshEntry slimEntry) {
        this.rigSha256 = rigSha256;
        this.normalEntry = normalEntry;
        this.slimEntry = slimEntry;
    }

    static MeshCatalog read(Path manifestFile) {
        try {
            byte[] bytes = MeshRepository.readBounded(manifestFile);
            String text = new String(bytes, StandardCharsets.UTF_8);
            try (JsonReader reader = new JsonReader(new StringReader(text))) {
                reader.setLenient(false);
                MeshCatalog catalog = parse(reader);
                if (reader.peek() != JsonToken.END_DOCUMENT) {
                    throw new SkinDiagnostic("mesh-manifest", "网格清单末尾还有多余内容");
                }
                return catalog;
            }
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (RuntimeException | IOException failure) {
            throw new SkinDiagnostic("mesh-manifest", "网格清单无法解析：" + failure.getMessage());
        }
    }

    private static MeshCatalog parse(JsonReader reader) throws IOException {
        int schemaVersion = -1;
        String artifact = "";
        String upstreamCommit = "";
        String verifiedHead = "";
        boolean localOnly = false;
        String distribution = "";
        boolean playbackReady = true;
        String runtimeStatus = "";
        String coordinateSpace = "";
        boolean coordinateConversionApplied = true;
        String rigRelativePath = "";
        String rigSha256 = "";
        int skeletonBoneCount = -1;
        int actionRegistrationCount = -1;
        int actionFileCount = -1;
        MeshEntry normalEntry = null;
        MeshEntry slimEntry = null;
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            switch (fieldName) {
                case "schemaVersion" -> schemaVersion = reader.nextInt();
                case "artifact" -> artifact = reader.nextString();
                case "upstreamCommit" -> upstreamCommit = reader.nextString();
                case "verifiedHead" -> verifiedHead = reader.nextString();
                case "localOnly" -> localOnly = reader.nextBoolean();
                case "distribution" -> distribution = reader.nextString();
                case "playbackReady" -> playbackReady = reader.nextBoolean();
                case "runtimeStatus" -> runtimeStatus = reader.nextString();
                case "coordinateSpace" -> coordinateSpace = reader.nextString();
                case "coordinateConversionApplied" -> coordinateConversionApplied = reader.nextBoolean();
                case "rigRelativePath" -> rigRelativePath = reader.nextString();
                case "rigSha256" -> rigSha256 = reader.nextString();
                case "skeletonBoneCount" -> skeletonBoneCount = reader.nextInt();
                case "actionRegistrationCount" -> actionRegistrationCount = reader.nextInt();
                case "actionFileCount" -> actionFileCount = reader.nextInt();
                case "meshes" -> {
                    reader.beginArray();
                    while (reader.hasNext()) {
                        MeshEntry entry = readEntry(reader);
                        if (entry.kind == PlayerMeshKind.NORMAL) {
                            if (normalEntry != null) {
                                throw new SkinDiagnostic("mesh-manifest", "普通网格重复");
                            }
                            normalEntry = entry;
                        } else if (slimEntry != null) {
                            throw new SkinDiagnostic("mesh-manifest", "瘦手臂网格重复");
                        } else {
                            slimEntry = entry;
                        }
                    }
                    reader.endArray();
                }
                default -> throw new SkinDiagnostic("unknown-field", "网格清单含未知字段 " + fieldName);
            }
        }
        reader.endObject();
        if (schemaVersion != 1 || !"player-mesh".equals(artifact) || !MeshLimits.UPSTREAM_COMMIT.equals(upstreamCommit)
                || verifiedHead == null || verifiedHead.length() != 40 || !localOnly || !"local-dev-only".equals(distribution)
                || playbackReady || !"pending-runtime".equals(runtimeStatus) || !"source-meters".equals(coordinateSpace)
                || coordinateConversionApplied || !MeshLimits.RIG_RELATIVE_PATH.equals(rigRelativePath)
                || rigSha256 == null || rigSha256.length() != 64 || skeletonBoneCount != MeshLimits.JOINT_LIMIT
                || actionRegistrationCount != 118 || actionFileCount != 117 || normalEntry == null || slimEntry == null) {
            throw new SkinDiagnostic("mesh-manifest", "网格清单不符合本地数据契约，或被写成了已播放验收");
        }
        return new MeshCatalog(rigSha256, normalEntry, slimEntry);
    }

    private static MeshEntry readEntry(JsonReader reader) throws IOException {
        String model = "";
        String sourceGitPath = "";
        String sourceSha256 = "";
        String extractedRelativePath = "";
        String extractedSha256 = "";
        int parts = -1;
        int positions = -1;
        int normals = -1;
        int textures = -1;
        int weights = -1;
        int influences = -1;
        int corners = -1;
        int triangles = -1;
        int highestInfluence = -1;
        Set<String> seen = new HashSet<>();
        reader.beginObject();
        while (reader.hasNext()) {
            String fieldName = takeName(reader, seen);
            switch (fieldName) {
                case "model" -> model = reader.nextString();
                case "sourceGitPath" -> sourceGitPath = reader.nextString();
                case "sourceSha256" -> sourceSha256 = reader.nextString();
                case "extractedRelativePath" -> extractedRelativePath = reader.nextString();
                case "extractedSha256" -> extractedSha256 = reader.nextString();
                case "parts" -> parts = reader.nextInt();
                case "positions" -> positions = reader.nextInt();
                case "normals" -> normals = reader.nextInt();
                case "uvs" -> textures = reader.nextInt();
                case "weights" -> weights = reader.nextInt();
                case "influences" -> influences = reader.nextInt();
                case "corners" -> corners = reader.nextInt();
                case "triangles" -> triangles = reader.nextInt();
                case "highestInfluence" -> highestInfluence = reader.nextInt();
                default -> throw new SkinDiagnostic("unknown-field", "网格条目含未知字段 " + fieldName);
            }
        }
        reader.endObject();
        PlayerMeshKind kind = PlayerMeshKind.fromWireName(model);
        if (kind == null || seen.size() != 14 || sourceSha256.length() != 64 || extractedSha256.length() != 64) {
            throw new SkinDiagnostic("mesh-manifest", "网格条目不完整或体型未知");
        }
        String expectedRelative = kind == PlayerMeshKind.NORMAL ? "mesh/biped-normal.json" : "mesh/biped-slim.json";
        if (!expectedRelative.equals(extractedRelativePath) || !kind.sourceGitPath().equals(sourceGitPath)) {
            throw new SkinDiagnostic("mesh-manifest", "网格条目路径不在授权白名单");
        }
        if (parts != PlayerMeshPart.values().length || positions < 1 || normals < 1 || textures < 1 || weights < 1
                || influences < 1 || corners < 3 || triangles != corners / 3 || highestInfluence < 1
                || highestInfluence > MeshLimits.INFLUENCES_PER_POSITION_LIMIT) {
            throw new SkinDiagnostic("mesh-manifest", "网格条目计数不可接受");
        }
        return new MeshEntry(kind, sourceGitPath, sourceSha256, extractedRelativePath, extractedSha256,
                parts, positions, normals, textures, weights, influences, corners, triangles, highestInfluence);
    }

    private static String takeName(JsonReader reader, Set<String> seen) throws IOException {
        String fieldName = reader.nextName();
        if (!seen.add(fieldName)) {
            throw new SkinDiagnostic("duplicate-field", "清单字段重复：" + fieldName);
        }
        return fieldName;
    }

    static final class MeshEntry {
        final PlayerMeshKind kind;
        final String sourceGitPath;
        final String sourceSha256;
        final String relativePath;
        final String extractedSha256;
        final int parts;
        final int positions;
        final int normals;
        final int textures;
        final int weights;
        final int influences;
        final int corners;
        final int triangles;
        final int highestInfluence;

        private MeshEntry(
                PlayerMeshKind kind,
                String sourceGitPath,
                String sourceSha256,
                String relativePath,
                String extractedSha256,
                int parts,
                int positions,
                int normals,
                int textures,
                int weights,
                int influences,
                int corners,
                int triangles,
                int highestInfluence) {
            this.kind = kind;
            this.sourceGitPath = sourceGitPath;
            this.sourceSha256 = sourceSha256;
            this.relativePath = relativePath;
            this.extractedSha256 = extractedSha256;
            this.parts = parts;
            this.positions = positions;
            this.normals = normals;
            this.textures = textures;
            this.weights = weights;
            this.influences = influences;
            this.corners = corners;
            this.triangles = triangles;
            this.highestInfluence = highestInfluence;
        }
    }
}

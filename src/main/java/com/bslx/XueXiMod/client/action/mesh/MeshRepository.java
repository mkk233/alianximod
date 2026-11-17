package com.bslx.XueXiMod.client.action.mesh;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import com.bslx.XueXiMod.animation.runtime.ActionBootstrap;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * 显式开发目录上的 normal/slim 缓存。不挂进 common 初始化。
 * 单档失败只写入该档诊断；动作 runtime 不读这里。
 */
public final class MeshRepository {
    private static final Object GUARD = new Object();
    private static volatile MeshLoadResult cached = MeshLoadResult.notLoaded();

    private MeshRepository() {
    }

    public static MeshLoadResult current() {
        return cached;
    }

    public static void clear() {
        synchronized (GUARD) {
            cached = MeshLoadResult.notLoaded();
        }
    }

    public static MeshLoadResult reload(Path devRoot, boolean enabled) {
        synchronized (GUARD) {
            MeshLoadResult result = read(devRoot, enabled);
            cached = result;
            return result;
        }
    }

    public static MeshLoadResult reloadFromDevProperty() {
        String configured = System.getProperty(ActionBootstrap.DEV_PROPERTY);
        if (configured == null || configured.isBlank()) {
            return reloadDisabled("没有设置 " + ActionBootstrap.DEV_PROPERTY + "，skin 后端关闭");
        }
        Path directory;
        try {
            directory = Path.of(configured);
        } catch (RuntimeException failure) {
            return reloadDisabled("开发目录无法解析，skin 后端关闭");
        }
        return reload(directory, true);
    }

    private static MeshLoadResult reloadDisabled(String diagnostic) {
        synchronized (GUARD) {
            MeshLoadResult disabled = MeshLoadResult.disabled(diagnostic);
            cached = disabled;
            return disabled;
        }
    }

    private static MeshLoadResult read(Path devRoot, boolean enabled) {
        if (!enabled) {
            return MeshLoadResult.disabled("显式开关关闭了 skin 后端");
        }
        try {
            if (devRoot == null || !devRoot.isAbsolute()) {
                return MeshLoadResult.disabled("开发目录必须是绝对路径");
            }
            Path rootReal = devRoot.toRealPath();
            verifyActionLicense(rootReal);
            MeshCatalog catalog = MeshCatalog.read(resolveInside(rootReal, "mesh-manifest.json"));
            verifyRig(rootReal, catalog.rigSha256);
            String normalDiagnostic = "";
            String slimDiagnostic = "";
            PlayerSkinMesh normalMesh = null;
            PlayerSkinMesh slimMesh = null;
            try {
                normalMesh = loadEntry(rootReal, catalog.normalEntry);
            } catch (SkinDiagnostic diagnostic) {
                normalDiagnostic = diagnostic.getMessage();
            }
            try {
                slimMesh = loadEntry(rootReal, catalog.slimEntry);
            } catch (SkinDiagnostic diagnostic) {
                slimDiagnostic = diagnostic.getMessage();
            }
            return MeshLoadResult.loaded(normalMesh, slimMesh, normalDiagnostic, slimDiagnostic);
        } catch (SkinDiagnostic diagnostic) {
            return MeshLoadResult.disabled(diagnostic.getMessage());
        } catch (RuntimeException | IOException failure) {
            return MeshLoadResult.disabled("skin 目录不可用：" + safeReason(failure));
        }
    }

    private static PlayerSkinMesh loadEntry(Path rootReal, MeshCatalog.MeshEntry entry) {
        Path file = resolveInside(rootReal, entry.relativePath);
        byte[] bytes = readBounded(file);
        String actualHash = sha256(bytes);
        if (!actualHash.equalsIgnoreCase(entry.extractedSha256)) {
            throw new SkinDiagnostic("hash", entry.kind.wireName() + " 网格哈希和清单不一致");
        }
        PlayerSkinMesh mesh = MeshJsonDecoder.decode(new String(bytes, StandardCharsets.UTF_8), entry.kind);
        if (!entry.sourceSha256.equalsIgnoreCase(mesh.sourceSha256()) || !entry.sourceGitPath.equals(mesh.sourceGitPath())) {
            throw new SkinDiagnostic("source", entry.kind.wireName() + " 来源声明和清单不一致");
        }
        int highest = 0;
        for (int positionIndex = 0; positionIndex < mesh.positionCount(); positionIndex++) {
            int influenceCount = mesh.influenceCountAt(positionIndex);
            if (influenceCount > highest) {
                highest = influenceCount;
            }
        }
        if (mesh.partCount() != entry.parts || mesh.positionCount() != entry.positions || mesh.normalCount() != entry.normals
                || mesh.textureCount() != entry.textures || mesh.weightCount() != entry.weights
                || mesh.influenceCount() != entry.influences || mesh.cornerCount() != entry.corners
                || mesh.triangleCount() != entry.triangles || highest != entry.highestInfluence) {
            throw new SkinDiagnostic("counts", entry.kind.wireName() + " 解码计数和清单不一致");
        }
        return mesh;
    }

    private static void verifyActionLicense(Path rootReal) {
        Path manifest = resolveInside(rootReal, "import-manifest.json");
        JsonObject root;
        try {
            root = JsonParser.parseString(Files.readString(manifest, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException | IOException failure) {
            throw new SkinDiagnostic("action-manifest", "动作清单不可读，skin 后端关闭");
        }
        if (!MeshLimits.UPSTREAM_COMMIT.equals(text(root, "upstreamCommit"))) {
            throw new SkinDiagnostic("action-manifest", "动作清单提交不是固定的 9d25f19c");
        }
        if (root.has("playbackReady") && root.get("playbackReady").getAsBoolean()) {
            throw new SkinDiagnostic("action-manifest", "动作清单已被写成播放验收，skin 不依附它");
        }
        if (integer(root, "registrationCount") != 118 || integer(root, "fileCount") != 117
                || integer(root, "skeletonBoneCount") != 20) {
            throw new SkinDiagnostic("action-manifest", "动作清单不再是 118 个 ID、117 个文件、20 骨");
        }
        if (!MeshLimits.RIG_RELATIVE_PATH.equals(text(root, "skeletonExtractedRelativePath"))) {
            throw new SkinDiagnostic("action-manifest", "动作清单骨架路径不是已有 rig");
        }
    }

    private static void verifyRig(Path rootReal, String expectedHash) {
        try {
            Path rig = resolveInside(rootReal, MeshLimits.RIG_RELATIVE_PATH);
            byte[] bytes = readBounded(rig);
            if (!sha256(bytes).equalsIgnoreCase(expectedHash)) {
                throw new SkinDiagnostic("rig", "骨架哈希和网格清单不一致");
            }
            String rigText = new String(bytes, StandardCharsets.UTF_8);
            for (String token : List.of("\"vertices\"", "\"mesh\"", "\"weights\"", "\"textures\"", "\"uv\"")) {
                if (rigText.contains(token)) {
                    throw new SkinDiagnostic("rig", "骨架元数据含未授权字段");
                }
            }
            JsonObject root = JsonParser.parseString(rigText).getAsJsonObject();
            if (root.get("boneCount").getAsInt() != MeshLimits.JOINT_LIMIT) {
                throw new SkinDiagnostic("rig", "骨架骨数不是 20");
            }
            JsonArray joints = root.getAsJsonArray("joints");
            if (joints == null || joints.size() != MeshLimits.JOINT_NAMES.length) {
                throw new SkinDiagnostic("rig", "骨架关节名数量不是 20");
            }
            for (int jointIndex = 0; jointIndex < MeshLimits.JOINT_NAMES.length; jointIndex++) {
                if (!MeshLimits.JOINT_NAMES[jointIndex].equals(joints.get(jointIndex).getAsString())) {
                    throw new SkinDiagnostic("rig", "骨架关节顺序和已授权名单不一致");
                }
            }
            Path actionManifest = resolveInside(rootReal, "import-manifest.json");
            JsonObject actionRoot = JsonParser.parseString(Files.readString(actionManifest, StandardCharsets.UTF_8)).getAsJsonObject();
            String actionRigHash = text(actionRoot, "skeletonExtractedSha256");
            if (actionRigHash.isBlank() || !sha256(bytes).equalsIgnoreCase(actionRigHash)) {
                throw new SkinDiagnostic("rig", "骨架哈希和动作清单不一致");
            }
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (RuntimeException | IOException failure) {
            throw new SkinDiagnostic("rig", "骨架元数据不可用");
        }
    }

    static Path resolveInside(Path rootReal, String relativePath) {
        if (rootReal == null || relativePath == null || relativePath.isBlank() || relativePath.indexOf('\\') >= 0
                || relativePath.indexOf(':') >= 0 || relativePath.startsWith("/") || relativePath.contains("..")) {
            throw new SkinDiagnostic("path", "开发路径越界");
        }
        Path absolute = rootReal.resolve(relativePath).normalize();
        if (!contained(rootReal, absolute)) {
            throw new SkinDiagnostic("path", "开发路径越界");
        }
        try {
            if (!Files.isRegularFile(absolute)) {
                throw new SkinDiagnostic("path", "开发文件不存在：" + relativePath);
            }
            Path real = absolute.toRealPath();
            if (!contained(rootReal, real)) {
                throw new SkinDiagnostic("path", "开发文件真实路径越界");
            }
            return real;
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (IOException failure) {
            throw new SkinDiagnostic("path", "开发文件无法解析：" + relativePath);
        }
    }

    static byte[] readBounded(Path file) {
        try {
            long size = Files.size(file);
            if (size < 0 || size > MeshLimits.FILE_BYTE_LIMIT) {
                throw new SkinDiagnostic("file-size", "开发文件超过上限");
            }
            return Files.readAllBytes(file);
        } catch (SkinDiagnostic diagnostic) {
            throw diagnostic;
        } catch (IOException failure) {
            throw new SkinDiagnostic("io", "开发文件读不了");
        }
    }

    private static boolean contained(Path rootReal, Path candidate) {
        Path absolute = candidate.toAbsolutePath().normalize();
        if (!absolute.startsWith(rootReal)) {
            return false;
        }
        Path relative = rootReal.relativize(absolute);
        return !relative.startsWith("..");
    }

    private static int integer(JsonObject root, String fieldName) {
        if (root == null || !root.has(fieldName) || !root.get(fieldName).isJsonPrimitive()) {
            throw new SkinDiagnostic("action-manifest", "动作清单缺少 " + fieldName);
        }
        return root.get(fieldName).getAsInt();
    }

    private static String text(JsonObject root, String fieldName) {
        if (root == null || !root.has(fieldName) || !root.get(fieldName).isJsonPrimitive()) {
            return "";
        }
        return root.get(fieldName).getAsString();
    }

    private static String sha256(byte[] bytes) {
        try {
            byte[] hashed = MessageDigest.getInstance("SHA-256").digest(bytes);
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (int byteIndex = 0; byteIndex < hashed.length; byteIndex++) {
                builder.append(String.format(Locale.ROOT, "%02x", hashed[byteIndex] & 0xff));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException failure) {
            throw new SkinDiagnostic("hash", "SHA-256 不可用");
        }
    }

    private static String safeReason(Exception failure) {
        String message = failure.getMessage();
        if (message == null || message.isBlank()) {
            return failure.getClass().getSimpleName();
        }
        return message;
    }
}

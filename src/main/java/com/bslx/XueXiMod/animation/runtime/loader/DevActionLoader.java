package com.bslx.XueXiMod.animation.runtime.loader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.TransformSheet;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionDefinition;
import com.bslx.XueXiMod.animation.runtime.ActionStatus;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.resources.ResourceLocation;

/**
 * 只读开发目录里的清单、骨架元数据和白名单动画。
 * 单条失败留在该条的状态里，不让其余登记一起失败。
 */
public final class DevActionLoader {
    public static final String ARMATURE_NAME = "epicfight:entity/biped";
    public static final String UPSTREAM_COMMIT = "9d25f19c121c5e51b1db9efe01833a563706eeee";
    private static final long FILE_BYTE_LIMIT = 5_000_000L;
    private static final float DURATION_TOLERANCE = 0.002F;
    private static final String[] JOINT_NAMES = {
            "Root", "Thigh_R", "Leg_R", "Knee_R", "Thigh_L", "Leg_L", "Knee_L", "Torso", "Chest", "Head",
            "Shoulder_R", "Arm_R", "Hand_R", "Tool_R", "Elbow_R", "Shoulder_L", "Arm_L", "Hand_L", "Tool_L", "Elbow_L"
    };
    private static final Map<String, String[]> CHILD_NAMES = childNames();

    private DevActionLoader() {
    }

    public static DevLoadResult load(Path devRoot) {
        if (devRoot == null) {
            throw new IllegalArgumentException("dev directory is missing");
        }
        if (!devRoot.isAbsolute()) {
            return emptyFailure("", "开发目录必须是绝对路径");
        }
        Path rootReal;
        try {
            rootReal = devRoot.toRealPath();
        } catch (IOException failure) {
            return emptyFailure("", "开发目录无法解析：" + safeReason(failure));
        }
        Path manifestPath = rootReal.resolve("import-manifest.json");
        if (!contained(rootReal, manifestPath) || !Files.isRegularFile(manifestPath)) {
            return emptyFailure("", "开发目录里没有 import-manifest.json");
        }
        JsonObject manifest;
        try {
            manifest = JsonParser.parseString(Files.readString(manifestPath, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (RuntimeException | IOException failure) {
            return emptyFailure("", "清单无法解析：" + safeReason(failure));
        }
        String upstreamCommit = textOrEmpty(manifest, "upstreamCommit");
        if (!UPSTREAM_COMMIT.equals(upstreamCommit)) {
            return emptyFailure(upstreamCommit, "清单提交不是固定的 9d25f19c");
        }
        if (!manifest.has("files") || !manifest.get("files").isJsonArray()) {
            return emptyFailure(upstreamCommit, "清单缺少 files 数组");
        }
        List<FileRecord> records = new ArrayList<>();
        try {
            JsonArray fileArray = manifest.getAsJsonArray("files");
            for (int fileIndex = 0; fileIndex < fileArray.size(); fileIndex++) {
                records.add(FileRecord.read(fileArray.get(fileIndex).getAsJsonObject()));
            }
        } catch (RuntimeException failure) {
            return emptyFailure(upstreamCommit, "清单文件记录不完整：" + safeReason(failure));
        }
        int declaredFiles = manifest.has("fileCount") ? manifest.get("fileCount").getAsInt() : -1;
        int declaredRegistrations = manifest.has("registrationCount") ? manifest.get("registrationCount").getAsInt() : -1;
        if (declaredFiles != records.size()) {
            return blockedRecords(upstreamCommit, records, "清单 fileCount 与文件记录数不一致");
        }
        int registrationTotal = 0;
        for (FileRecord record : records) {
            registrationTotal += record.registrations.size();
        }
        if (declaredRegistrations != registrationTotal) {
            return blockedRecords(upstreamCommit, records, "清单 registrationCount 与登记数不一致");
        }
        PreparedRig preparedRig;
        try {
            preparedRig = readRig(rootReal, manifest);
        } catch (RuntimeException | IOException failure) {
            return blockedRecords(upstreamCommit, records, "骨架元数据不可用：" + safeReason(failure));
        }
        Map<String, AnimationClip> clipByPath = new LinkedHashMap<>();
        Map<String, BuiltClip> builtByPath = new LinkedHashMap<>();
        List<ActionStatus> statuses = new ArrayList<>();
        Map<ResourceLocation, AnimationClip> clips = new LinkedHashMap<>();
        Map<ResourceLocation, ActionDefinition> definitions = new LinkedHashMap<>();
        Set<ResourceLocation> seenIds = new HashSet<>();
        int aliasCount = 0;
        for (FileRecord record : records) {
            BuiltClip built = builtByPath.get(record.devRelativePath);
            if (built == null) {
                built = decodeFile(rootReal, record, preparedRig);
                builtByPath.put(record.devRelativePath, built);
                if (built.clip != null) {
                    clipByPath.put(record.devRelativePath, built.clip);
                }
            }
            for (RegistrationRecord registration : record.registrations) {
                if (registration.aliasOf != null && !registration.aliasOf.isBlank()) {
                    aliasCount++;
                }
                ResourceLocation actionId;
                try {
                    actionId = parseActionId(registration.actionId);
                } catch (RuntimeException failure) {
                    statuses.add(rejectedPlaceholder(statuses.size(), registration.actionId, record, "动作 ID 不合法：" + safeReason(failure)));
                    continue;
                }
                if (!seenIds.add(actionId)) {
                    statuses.add(statusFor(actionId, record, registration, ActionStatus.Availability.REJECTED,
                            "动作 ID 重复", "", "", false, upstreamCommit));
                    continue;
                }
                if (!ARMATURE_NAME.equals(record.armatureName) || !ARMATURE_NAME.equals(registration.expectedArmature)) {
                    statuses.add(statusFor(actionId, record, registration, ActionStatus.Availability.REJECTED,
                            "骨架不是 epicfight:entity/biped", built.entryShape, built.auxiliaryText, false, upstreamCommit));
                    continue;
                }
                if (built.failure != null) {
                    ActionStatus.Availability availability = built.quarantine
                            ? ActionStatus.Availability.QUARANTINED
                            : ActionStatus.Availability.REJECTED;
                    statuses.add(statusFor(actionId, record, registration, availability,
                            built.failure, built.entryShape, built.auxiliaryText, false, upstreamCommit));
                    continue;
                }
                ActionStatus.Availability availability = built.degraded
                        ? ActionStatus.Availability.POSE_DEGRADED
                        : ActionStatus.Availability.POSE_UNVERIFIED;
                String reason = built.degraded
                        ? "主入口 20 骨姿态可采样，但是降阶。" + built.auxiliaryText
                        + " 辅助轨不驱动实体移动。数值尚未与外部上游对照。刚体适配未做。游戏观感未验证。不是完整上游还原。"
                        : "主入口 20 骨姿态可采样，没有辅助轨。数值尚未与外部上游对照。刚体适配未做。游戏观感未验证。不能标成已完成还原。";
                if (actionId.getPath().contains("roll")) {
                    reason = reason + " 翻滚只播放姿态，不移动、不开无敌、不改碰撞箱。";
                }
                if (registration.aliasOf != null && !registration.aliasOf.isBlank()) {
                    reason = reason + " 别名共享 " + registration.aliasOf + " 的剪辑，不另计一份文件。";
                }
                statuses.add(statusFor(actionId, record, registration, availability, reason,
                        built.entryShape, built.auxiliaryText, true, upstreamCommit));
                clips.put(actionId, built.clip);
                definitions.put(actionId, new ActionDefinition(
                        actionId,
                        ARMATURE_NAME,
                        built.duration,
                        false,
                        0.08F,
                        Set.of(),
                        Set.of(ActionDefinition.Channel.TRANSLATION, ActionDefinition.Channel.ROTATION, ActionDefinition.Channel.SCALE),
                        List.of(),
                        "授权开发目录，本地姿态，不是发行资源"));
            }
        }
        int distinctFiles = builtByPath.size();
        String summary = "commit=" + upstreamCommit
                + " files=" + distinctFiles
                + " ids=" + statuses.size()
                + " posePlayable=" + countPlayable(statuses)
                + " degraded=" + countDegraded(statuses)
                + " quarantined=" + countAvailability(statuses, ActionStatus.Availability.QUARANTINED)
                + " rejected=" + countAvailability(statuses, ActionStatus.Availability.REJECTED)
                + " oracleCompared=0 rigidAdapted=0 lookVerified=0"
                + " alias=" + aliasCount;
        return new DevLoadResult(upstreamCommit, summary, preparedRig.armature, statuses, clips, definitions, distinctFiles, aliasCount);
    }

    private static BuiltClip decodeFile(Path rootReal, FileRecord record, PreparedRig preparedRig) {
        try {
            if (record.devRelativePath.contains("..") || record.devRelativePath.startsWith("/")
                    || record.devRelativePath.contains("\\") || !record.devRelativePath.startsWith("sources/")) {
                return BuiltClip.rejected("相对路径越出开发目录或不是 sources 下的清单路径");
            }
            Path clipPath = rootReal.resolve(record.devRelativePath).normalize();
            if (!contained(rootReal, clipPath) || !Files.isRegularFile(clipPath)) {
                return BuiltClip.rejected("动画文件不在开发目录内");
            }
            Path clipReal = clipPath.toRealPath();
            if (!contained(rootReal, clipReal)) {
                return BuiltClip.rejected("动画真实路径越出开发目录");
            }
            if (record.byteLength < 0 || record.byteLength > FILE_BYTE_LIMIT) {
                return BuiltClip.rejected("清单记录的文件长度不可接受");
            }
            byte[] bytes = Files.readAllBytes(clipReal);
            if (bytes.length != record.byteLength) {
                return BuiltClip.rejected("文件长度与清单 byteLength 不一致");
            }
            String actualHash = sha256(bytes);
            if (!actualHash.equalsIgnoreCase(record.importedSha256) || !actualHash.equalsIgnoreCase(record.sourceSha256)) {
                return BuiltClip.rejected("文件哈希与清单 source/imported 不一致");
            }
            JsonObject root = JsonParser.parseString(new String(bytes, StandardCharsets.UTF_8)).getAsJsonObject();
            if (root.has("format")) {
                String formatName = root.get("format").getAsString();
                if (!"MATRIX".equals(formatName)) {
                    return BuiltClip.rejected("变换格式不是 MATRIX：" + formatName);
                }
            }
            for (String keyName : root.keySet()) {
                if (!"animation".equals(keyName) && !"format".equals(keyName)) {
                    return BuiltClip.rejected("动画文件含未允许的顶层字段 " + keyName);
                }
            }
            if (!root.has("animation") || !root.get("animation").isJsonArray()) {
                return BuiltClip.rejected("缺少 animation 数组");
            }
            return decodeTracks(root.getAsJsonArray("animation"), record, preparedRig);
        } catch (RuntimeException | IOException failure) {
            return BuiltClip.rejected(safeReason(failure));
        }
    }

    private static BuiltClip decodeTracks(JsonArray tracks, FileRecord record, PreparedRig preparedRig) {
        if (tracks.size() != record.trackCount) {
            return BuiltClip.rejected("轨道数与清单不一致");
        }
        boolean rootCorrectionPending = true;
        boolean sawCoord = false;
        boolean sawIk = false;
        Set<String> seenBones = new HashSet<>();
        Set<String> seenAuxiliary = new HashSet<>();
        List<String> auxiliaryNotes = new ArrayList<>();
        List<String> trackOrder = new ArrayList<>();
        Map<String, TransformSheet> sheets = new LinkedHashMap<>();
        float duration = 0.0F;
        for (int trackIndex = 0; trackIndex < tracks.size(); trackIndex++) {
            JsonElement trackElement = tracks.get(trackIndex);
            if (!trackElement.isJsonObject()) {
                return BuiltClip.rejected("轨道不是对象");
            }
            JsonObject track = trackElement.getAsJsonObject();
            for (String keyName : track.keySet()) {
                if (!"name".equals(keyName) && !"time".equals(keyName) && !"transform".equals(keyName)) {
                    return BuiltClip.rejected("轨道含未允许字段 " + keyName);
                }
            }
            if (!track.has("name") || !track.get("name").isJsonPrimitive()) {
                return BuiltClip.rejected("轨道缺少名称");
            }
            String trackName = track.get("name").getAsString();
            trackOrder.add(trackName);
            TrackSamples samples;
            try {
                samples = readSamples(track, trackName);
            } catch (RuntimeException failure) {
                return BuiltClip.rejected(trackName + "：" + safeReason(failure));
            }
            if (trackName.startsWith("IK")) {
                if (!seenAuxiliary.add(trackName)) {
                    return BuiltClip.rejected("辅助轨重复：" + trackName);
                }
                sawIk = true;
                auxiliaryNotes.add(trackName + " 格式 MATRIX 帧数 " + samples.times.size() + "，登记后跳过，不消耗根校正，不生成骨");
                continue;
            }
            if ("Coord".equals(trackName)) {
                if (!seenAuxiliary.add(trackName)) {
                    return BuiltClip.rejected("Coord 轨道重复");
                }
                sawCoord = true;
                auxiliaryNotes.add("Coord 格式 MATRIX 帧数 " + samples.times.size() + "，登记后丢弃位移，不作为骨，根校正标志置假");
                rootCorrectionPending = false;
                continue;
            }
            if (!preparedRig.inverseBinds.containsKey(trackName)) {
                return BuiltClip.quarantine("未知辅助轨 " + trackName + " 可能影响坐标，且没有上游主入口证据，本条隔离");
            }
            if (!seenBones.add(trackName)) {
                return BuiltClip.rejected("骨骼轨道重复：" + trackName);
            }
            try {
                OpenMatrix4f inverseBind = preparedRig.inverseBinds.get(trackName);
                List<Keyframe> frames = new ArrayList<>();
                for (int frameIndex = 0; frameIndex < samples.times.size(); frameIndex++) {
                    JointTransform transform = SourceMatrixMath.delta(samples.matrices.get(frameIndex), inverseBind, rootCorrectionPending);
                    frames.add(new Keyframe(samples.times.get(frameIndex), transform));
                }
                TransformSheet sheet = new TransformSheet(frames);
                sheets.put(trackName, sheet);
                duration = Math.max(duration, sheet.maxFrameTime());
            } catch (RuntimeException failure) {
                return BuiltClip.quarantine(trackName + " 姿态分解失败：" + safeReason(failure));
            }
            rootCorrectionPending = false;
        }
        if (sawCoord != record.hasCoord || sawIk != record.hasIk) {
            return BuiltClip.rejected("Coord/IK 标记与清单不一致");
        }
        String shapeFailure = unexpectedShape(trackOrder, sawCoord, sawIk);
        if (shapeFailure != null) {
            return BuiltClip.quarantine(shapeFailure);
        }
        for (String jointName : JOINT_NAMES) {
            if (!seenBones.contains(jointName)) {
                return BuiltClip.rejected("缺少骨骼轨道 " + jointName);
            }
        }
        if (!Float.isFinite(duration) || duration <= 0.0F) {
            return BuiltClip.rejected("时长不是有限正数");
        }
        if (Math.abs(duration - (float) record.durationSeconds) > DURATION_TOLERANCE) {
            return BuiltClip.rejected("解析时长与清单 durationSeconds 不一致");
        }
        AnimationClip clip = new AnimationClip();
        for (Map.Entry<String, TransformSheet> entry : sheets.entrySet()) {
            clip.addJointTransform(entry.getKey(), entry.getValue());
        }
        clip.setClipTime(duration);
        try {
            finiteCheck(preparedRig.armature, clip, duration);
        } catch (RuntimeException failure) {
            return BuiltClip.quarantine("有限姿态自检失败：" + safeReason(failure));
        }
        BuiltClip built = new BuiltClip();
        built.clip = clip;
        built.duration = duration;
        built.entryShape = shapeOf(trackOrder);
        built.auxiliaryText = String.join("；", auxiliaryNotes);
        built.degraded = sawCoord || sawIk;
        return built;
    }

    private static String unexpectedShape(List<String> trackOrder, boolean sawCoord, boolean sawIk) {
        if (trackOrder.isEmpty()) {
            return "没有轨道";
        }
        if (sawIk) {
            if (trackOrder.size() < 4
                    || !"IK_left".equals(trackOrder.get(0))
                    || !"IK_right".equals(trackOrder.get(1))
                    || !"Coord".equals(trackOrder.get(2))
                    || !"Root".equals(trackOrder.get(3))) {
                return "IK 轨道顺序不是 IK_left、IK_right、Coord、Root，本条隔离";
            }
            return null;
        }
        if (sawCoord && !"Coord".equals(trackOrder.get(0))) {
            return "带 Coord 的剪辑不是 Coord 首轨，本条隔离";
        }
        if (!sawCoord && !"Root".equals(trackOrder.get(0))) {
            return "无辅助轨的剪辑不是 Root 首轨，本条隔离";
        }
        return null;
    }

    private static String shapeOf(List<String> trackOrder) {
        if (trackOrder.size() >= 4 && "IK_left".equals(trackOrder.get(0))) {
            return "ik-coord-root";
        }
        if (!trackOrder.isEmpty() && "Coord".equals(trackOrder.get(0))) {
            return "coord-first";
        }
        if (!trackOrder.isEmpty() && "Root".equals(trackOrder.get(0))) {
            return "root-first";
        }
        return "other";
    }

    private static void finiteCheck(Armature armature, AnimationClip clip, float duration) {
        float middle = duration * 0.5F;
        float[] sampleTimes = {0.0F, middle, duration};
        for (int sampleIndex = 0; sampleIndex < sampleTimes.length; sampleIndex++) {
            Pose pose = clip.getPoseInTime(sampleTimes[sampleIndex]);
            OpenMatrix4f[] matrices = armature.getPoseAsTransformMatrix(pose, false);
            if (matrices.length != JOINT_NAMES.length) {
                throw new IllegalArgumentException("pose matrix count is not 20");
            }
            for (int matrixIndex = 0; matrixIndex < matrices.length; matrixIndex++) {
                OpenMatrix4f matrix = matrices[matrixIndex];
                if (matrix == null || !matrix.hasFiniteElements()) {
                    throw new IllegalArgumentException("pose matrix is not finite");
                }
            }
        }
    }

    private static TrackSamples readSamples(JsonObject track, String trackName) {
        if (!track.has("time") || !track.get("time").isJsonArray() || !track.has("transform") || !track.get("transform").isJsonArray()) {
            throw new IllegalArgumentException("时间和变换数组缺失");
        }
        JsonArray times = track.getAsJsonArray("time");
        JsonArray transforms = track.getAsJsonArray("transform");
        if (times.size() != transforms.size()) {
            throw new IllegalArgumentException("时间与变换数量不一致");
        }
        if (times.size() < 2) {
            throw new IllegalArgumentException("单帧或空轨道");
        }
        List<Float> timeValues = new ArrayList<>();
        List<float[]> matrices = new ArrayList<>();
        float previous = Float.NEGATIVE_INFINITY;
        for (int frameIndex = 0; frameIndex < times.size(); frameIndex++) {
            JsonElement timeElement = times.get(frameIndex);
            if (!timeElement.isJsonPrimitive() || !timeElement.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException("时间不是数字");
            }
            float time = timeElement.getAsFloat();
            if (!Float.isFinite(time) || time < 0.0F || !(time > previous)) {
                throw new IllegalArgumentException("时间必须有限、非负且严格递增");
            }
            previous = time;
            timeValues.add(time);
            matrices.add(readMatrix(transforms.get(frameIndex), trackName));
        }
        TrackSamples samples = new TrackSamples();
        samples.times = timeValues;
        samples.matrices = matrices;
        return samples;
    }

    private static float[] readMatrix(JsonElement element, String label) {
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException(label + " 的变换不是 16 个数");
        }
        JsonArray array = element.getAsJsonArray();
        if (array.size() != 16) {
            throw new IllegalArgumentException(label + " 的矩阵长度不是 16");
        }
        float[] rowMajor = new float[16];
        for (int componentIndex = 0; componentIndex < 16; componentIndex++) {
            JsonElement component = array.get(componentIndex);
            if (!component.isJsonPrimitive() || !component.getAsJsonPrimitive().isNumber()) {
                throw new IllegalArgumentException(label + " 的矩阵分量不是数字");
            }
            float number = component.getAsFloat();
            if (!Float.isFinite(number)) {
                throw new IllegalArgumentException(label + " 的矩阵分量不是有限数");
            }
            rowMajor[componentIndex] = number;
        }
        return rowMajor;
    }

    private static PreparedRig readRig(Path rootReal, JsonObject manifest) throws IOException {
        String relative = textOrEmpty(manifest, "skeletonExtractedRelativePath");
        if (!"rig/biped-armature.json".equals(relative)) {
            throw new IllegalArgumentException("骨架相对路径不是 rig/biped-armature.json");
        }
        Path rigPath = rootReal.resolve(relative).normalize();
        if (!contained(rootReal, rigPath)) {
            throw new IllegalArgumentException("骨架路径越界");
        }
        byte[] bytes = Files.readAllBytes(rigPath.toRealPath());
        String actualHash = sha256(bytes);
        String expectedHash = textOrEmpty(manifest, "skeletonExtractedSha256");
        if (expectedHash.isBlank() || !actualHash.equalsIgnoreCase(expectedHash)) {
            throw new IllegalArgumentException("骨架哈希与清单不一致");
        }
        String rigText = new String(bytes, StandardCharsets.UTF_8);
        for (String token : List.of("\"vertices\"", "\"mesh\"", "\"weights\"", "\"textures\"", "\"uv\"")) {
            if (rigText.contains(token)) {
                throw new IllegalArgumentException("骨架元数据含未授权字段");
            }
        }
        JsonObject rig = JsonParser.parseString(rigText).getAsJsonObject();
        if (rig.get("boneCount").getAsInt() != 20) {
            throw new IllegalArgumentException("骨骼数不是 20");
        }
        if (!UPSTREAM_COMMIT.equals(textOrEmpty(rig, "upstreamCommit"))) {
            throw new IllegalArgumentException("骨架提交不匹配");
        }
        JsonArray jointArray = rig.getAsJsonArray("joints");
        if (jointArray.size() != 20) {
            throw new IllegalArgumentException("关节名数量不是 20");
        }
        Map<String, Integer> jointIds = new LinkedHashMap<>();
        for (int jointIndex = 0; jointIndex < 20; jointIndex++) {
            String jointName = jointArray.get(jointIndex).getAsString();
            if (!JOINT_NAMES[jointIndex].equals(jointName)) {
                throw new IllegalArgumentException("关节顺序不一致");
            }
            jointIds.put(jointName, jointIndex);
        }
        JsonObject hierarchy = rig.getAsJsonObject("hierarchy");
        Map<String, Joint> jointMap = new LinkedHashMap<>();
        Joint rootJoint = buildJoint(hierarchy, jointIds, jointMap, true);
        if (!"Root".equals(rootJoint.getName())) {
            throw new IllegalArgumentException("层级根不是 Root");
        }
        Armature armature = new Armature(ARMATURE_NAME, jointMap.size(), rootJoint, jointMap);
        armature.bakeOriginMatrices();
        Map<String, OpenMatrix4f> inverseBinds = new HashMap<>();
        for (Joint joint : rootJoint.getAllJoints()) {
            inverseBinds.put(joint.getName(), OpenMatrix4f.invert(joint.getLocalTransform(), null));
        }
        PreparedRig prepared = new PreparedRig();
        prepared.armature = armature;
        prepared.inverseBinds = inverseBinds;
        return prepared;
    }

    private static Joint buildJoint(JsonObject node, Map<String, Integer> jointIds, Map<String, Joint> jointMap, boolean rootCorrection) {
        String jointName = node.get("name").getAsString();
        Integer jointId = jointIds.get(jointName);
        if (jointId == null) {
            throw new IllegalArgumentException("层级里有未知关节 " + jointName);
        }
        String[] expectedChildren = CHILD_NAMES.get(jointName);
        if (expectedChildren == null) {
            throw new IllegalArgumentException("层级关节不在白名单：" + jointName);
        }
        OpenMatrix4f bind = SourceMatrixMath.convertedAbsolute(readMatrix(node.get("transform"), jointName), rootCorrection);
        Joint joint = new Joint(jointName, jointId, bind);
        if (jointMap.put(jointName, joint) != null) {
            throw new IllegalArgumentException("关节重复：" + jointName);
        }
        JsonArray children = node.has("children") ? node.getAsJsonArray("children") : new JsonArray();
        if (children.size() != expectedChildren.length) {
            throw new IllegalArgumentException("子关节数量不一致：" + jointName);
        }
        for (int childIndex = 0; childIndex < children.size(); childIndex++) {
            JsonObject child = children.get(childIndex).getAsJsonObject();
            if (!expectedChildren[childIndex].equals(child.get("name").getAsString())) {
                throw new IllegalArgumentException("子关节顺序不一致：" + jointName);
            }
            joint.addSubJoints(buildJoint(child, jointIds, jointMap, false));
        }
        return joint;
    }

    private static DevLoadResult emptyFailure(String upstreamCommit, String reason) {
        return new DevLoadResult(upstreamCommit, reason, null, List.of(), Map.of(), Map.of(), 0, 0);
    }

    private static DevLoadResult blockedRecords(String upstreamCommit, List<FileRecord> records, String reason) {
        List<ActionStatus> statuses = new ArrayList<>();
        for (FileRecord record : records) {
            for (RegistrationRecord registration : record.registrations) {
                try {
                    ResourceLocation actionId = parseActionId(registration.actionId);
                    statuses.add(statusFor(actionId, record, registration, ActionStatus.Availability.REJECTED,
                            reason, "", "", false, upstreamCommit));
                } catch (RuntimeException failure) {
                    statuses.add(rejectedPlaceholder(statuses.size(), registration.actionId, record, reason));
                }
            }
        }
        String summary = "commit=" + upstreamCommit + " ids=" + statuses.size() + " posePlayable=0 rejected=" + statuses.size()
                + " reason=" + reason;
        return new DevLoadResult(upstreamCommit, summary, null, statuses, Map.of(), Map.of(), records.size(), 0);
    }

    private static ActionStatus rejectedPlaceholder(int uniqueIndex, String rawId, FileRecord record, String reason) {
        ResourceLocation placeholder = ResourceLocation.fromNamespaceAndPath("xueximod", "rejected/unparsed_" + uniqueIndex);
        return statusFor(placeholder, record, new RegistrationRecord(rawId == null ? "" : rawId, "", "", ""),
                ActionStatus.Availability.REJECTED, reason + " 原始 ID：" + rawId, "", "", false, UPSTREAM_COMMIT);
    }

    private static ActionStatus statusFor(
            ResourceLocation actionId,
            FileRecord record,
            RegistrationRecord registration,
            ActionStatus.Availability availability,
            String reason,
            String entryShape,
            String auxiliaryText,
            boolean finitePoseChecked,
            String upstreamCommit) {
        return new ActionStatus(
                actionId,
                availability,
                trim(reason),
                record.armatureName,
                record.sourceGitPath,
                upstreamCommit,
                registration.category,
                registration.aliasOf,
                (float) record.durationSeconds,
                entryShape,
                auxiliaryText,
                finitePoseChecked);
    }

    private static String trim(String reason) {
        if (reason == null) {
            return "";
        }
        if (reason.length() <= 360) {
            return reason;
        }
        return reason.substring(0, 360);
    }

    public static ResourceLocation parseActionId(String text) {
        if (text == null) {
            throw new IllegalArgumentException("action id is missing");
        }
        int split = text.indexOf(':');
        if (split <= 0 || split >= text.length() - 1 || text.indexOf(':', split + 1) >= 0) {
            throw new IllegalArgumentException("action id needs one namespace");
        }
        String namespace = text.substring(0, split);
        String locationPath = text.substring(split + 1);
        if (!namespace.matches("[a-z0-9_.-]+") || !locationPath.matches("[a-z0-9_./-]+")) {
            throw new IllegalArgumentException("action id has illegal characters");
        }
        return ResourceLocation.fromNamespaceAndPath(namespace, locationPath);
    }

    private static boolean contained(Path rootReal, Path candidate) {
        try {
            Path absolute = candidate.toAbsolutePath().normalize();
            if (!absolute.startsWith(rootReal)) {
                return false;
            }
            Path relative = rootReal.relativize(absolute);
            return !relative.startsWith("..");
        } catch (RuntimeException failure) {
            return false;
        }
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
            throw new IllegalStateException("SHA-256 is unavailable");
        }
    }

    private static String textOrEmpty(JsonObject object, String keyName) {
        if (object == null || !object.has(keyName) || object.get(keyName).isJsonNull()) {
            return "";
        }
        return object.get(keyName).getAsString();
    }

    private static String safeReason(Throwable failure) {
        String text = failure.getMessage();
        if (text == null || text.isBlank()) {
            text = failure.getClass().getSimpleName();
        }
        return trim(text.replace('\n', ' '));
    }

    private static int countPlayable(List<ActionStatus> statuses) {
        int playable = 0;
        for (ActionStatus status : statuses) {
            if (status.posePlayable()) {
                playable++;
            }
        }
        return playable;
    }

    private static int countDegraded(List<ActionStatus> statuses) {
        int degraded = 0;
        for (ActionStatus status : statuses) {
            if (status.degraded()) {
                degraded++;
            }
        }
        return degraded;
    }

    private static int countAvailability(List<ActionStatus> statuses, ActionStatus.Availability availability) {
        int matched = 0;
        for (ActionStatus status : statuses) {
            if (status.availability() == availability) {
                matched++;
            }
        }
        return matched;
    }

    private static Map<String, String[]> childNames() {
        Map<String, String[]> children = new LinkedHashMap<>();
        children.put("Root", new String[] {"Thigh_R", "Thigh_L", "Torso"});
        children.put("Thigh_R", new String[] {"Leg_R", "Knee_R"});
        children.put("Leg_R", new String[] {});
        children.put("Knee_R", new String[] {});
        children.put("Thigh_L", new String[] {"Leg_L", "Knee_L"});
        children.put("Leg_L", new String[] {});
        children.put("Knee_L", new String[] {});
        children.put("Torso", new String[] {"Chest"});
        children.put("Chest", new String[] {"Head", "Shoulder_R", "Shoulder_L"});
        children.put("Head", new String[] {});
        children.put("Shoulder_R", new String[] {"Arm_R"});
        children.put("Arm_R", new String[] {"Hand_R", "Elbow_R"});
        children.put("Hand_R", new String[] {"Tool_R"});
        children.put("Tool_R", new String[] {});
        children.put("Elbow_R", new String[] {});
        children.put("Shoulder_L", new String[] {"Arm_L"});
        children.put("Arm_L", new String[] {"Hand_L", "Elbow_L"});
        children.put("Hand_L", new String[] {"Tool_L"});
        children.put("Tool_L", new String[] {});
        children.put("Elbow_L", new String[] {});
        return children;
    }

    private static final class TrackSamples {
        private List<Float> times;
        private List<float[]> matrices;
    }

    private static final class PreparedRig {
        private Armature armature;
        private Map<String, OpenMatrix4f> inverseBinds;
    }

    private static final class BuiltClip {
        private AnimationClip clip;
        private float duration;
        private String entryShape = "";
        private String auxiliaryText = "";
        private boolean degraded;
        private String failure;
        private boolean quarantine;

        private static BuiltClip rejected(String reason) {
            BuiltClip built = new BuiltClip();
            built.failure = reason;
            built.quarantine = false;
            return built;
        }

        private static BuiltClip quarantine(String reason) {
            BuiltClip built = new BuiltClip();
            built.failure = reason;
            built.quarantine = true;
            return built;
        }
    }

    private static final class RegistrationRecord {
        private final String actionId;
        private final String category;
        private final String aliasOf;
        private final String expectedArmature;

        private RegistrationRecord(String actionId, String category, String aliasOf, String expectedArmature) {
            this.actionId = actionId;
            this.category = category;
            this.aliasOf = aliasOf;
            this.expectedArmature = expectedArmature;
        }

        private static RegistrationRecord read(JsonObject object) {
            String alias = "";
            if (object.has("aliasOf") && !object.get("aliasOf").isJsonNull()) {
                alias = object.get("aliasOf").getAsString();
            }
            return new RegistrationRecord(
                    object.get("id").getAsString(),
                    object.has("category") && !object.get("category").isJsonNull() ? object.get("category").getAsString() : "",
                    alias,
                    object.has("expectedArmature") ? object.get("expectedArmature").getAsString() : "");
        }
    }

    private static final class FileRecord {
        private final String sourceGitPath;
        private final String devRelativePath;
        private final String sourceSha256;
        private final String importedSha256;
        private final long byteLength;
        private final double durationSeconds;
        private final String armatureName;
        private final int trackCount;
        private final boolean hasCoord;
        private final boolean hasIk;
        private final List<RegistrationRecord> registrations;

        private FileRecord(
                String sourceGitPath,
                String devRelativePath,
                String sourceSha256,
                String importedSha256,
                long byteLength,
                double durationSeconds,
                String armatureName,
                int trackCount,
                boolean hasCoord,
                boolean hasIk,
                List<RegistrationRecord> registrations) {
            this.sourceGitPath = sourceGitPath;
            this.devRelativePath = devRelativePath;
            this.sourceSha256 = sourceSha256;
            this.importedSha256 = importedSha256;
            this.byteLength = byteLength;
            this.durationSeconds = durationSeconds;
            this.armatureName = armatureName;
            this.trackCount = trackCount;
            this.hasCoord = hasCoord;
            this.hasIk = hasIk;
            this.registrations = registrations;
        }

        private static FileRecord read(JsonObject object) {
            JsonArray registrationArray = object.getAsJsonArray("registrations");
            List<RegistrationRecord> registrations = new ArrayList<>();
            for (int registrationIndex = 0; registrationIndex < registrationArray.size(); registrationIndex++) {
                registrations.add(RegistrationRecord.read(registrationArray.get(registrationIndex).getAsJsonObject()));
            }
            if (registrations.isEmpty()) {
                throw new IllegalArgumentException("file record has no registrations");
            }
            return new FileRecord(
                    object.get("sourceGitPath").getAsString(),
                    object.get("devRelativePath").getAsString(),
                    object.get("sourceSha256").getAsString(),
                    object.get("importedSha256").getAsString(),
                    object.get("byteLength").getAsLong(),
                    object.get("durationSeconds").getAsDouble(),
                    object.get("armature").getAsString(),
                    object.get("trackCount").getAsInt(),
                    object.get("hasCoord").getAsBoolean(),
                    object.get("hasIk").getAsBoolean(),
                    registrations);
        }
    }
}

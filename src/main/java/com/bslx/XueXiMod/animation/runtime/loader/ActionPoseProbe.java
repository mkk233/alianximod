package com.bslx.XueXiMod.animation.runtime.loader;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.TransformSheet;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.ActionStatus;

import net.minecraft.resources.ResourceLocation;

/**
 * 给外部对照直接调用的采样入口。
 * 这里不写期望数值；对照由调用方拿上游结果来比。
 */
public final class ActionPoseProbe {
    public static final String ROOT_FIRST_ID = "epicfight:biped/combat/sword_auto1";
    public static final String COORD_FIRST_ID = "epicfight:biped/combat/dagger_auto1";
    public static final String IK_COORD_ROOT_ID = "epicfight:biped/skill/blade_rush_combo1";
    public static final List<String> COMPARED_BONES = List.of("Root", "Chest", "Hand_R", "Tool_R");

    private ActionPoseProbe() {
    }

    public static DevLoadResult loadForProbe(Path devRoot) {
        return DevActionLoader.load(devRoot);
    }

    public static List<Float> suggestedSeconds(DevLoadResult result, String actionIdText) {
        AnimationClip clip = clipOf(result, actionIdText);
        if (clip == null) {
            return List.of();
        }
        float duration = clip.getClipTime();
        float nonKey = nonKeySeconds(clip, duration);
        float middle = duration * 0.5F;
        return List.of(0.0F, nonKey, middle, duration);
    }

    public static ProbeFrame sample(DevLoadResult result, String actionIdText, float sampleSeconds) {
        if (result == null) {
            return ProbeFrame.failed(actionIdText, sampleSeconds, "没有加载结果");
        }
        ResourceLocation actionId;
        try {
            actionId = DevActionLoader.parseActionId(actionIdText);
        } catch (RuntimeException failure) {
            return ProbeFrame.failed(actionIdText, sampleSeconds, "动作 ID 不合法");
        }
        ActionStatus status = result.status(actionId);
        if (status == null || !status.posePlayable()) {
            String reason = status == null ? "清单里没有这个 ID" : status.reasonText();
            return ProbeFrame.failed(actionIdText, sampleSeconds, reason);
        }
        return sampleClip(result.sourceArmature(), result.clip(actionId), actionIdText, sampleSeconds);
    }

    public static ProbeFrame sampleLoaded(String actionIdText, float sampleSeconds) {
        ResourceLocation actionId;
        try {
            actionId = DevActionLoader.parseActionId(actionIdText);
        } catch (RuntimeException failure) {
            return ProbeFrame.failed(actionIdText, sampleSeconds, "动作 ID 不合法");
        }
        ActionStatus status = ActionRegistry.status(actionId);
        if (status == null || !status.posePlayable()) {
            String reason = status == null ? "运行时没有这个 ID" : status.reasonText();
            return ProbeFrame.failed(actionIdText, sampleSeconds, reason);
        }
        Armature armature = ActionRegistry.armature(status.armatureName());
        return sampleClip(armature, ActionRegistry.clip(actionId), actionIdText, sampleSeconds);
    }

    public static List<ProbeFrame> sampleRepresentatives(DevLoadResult result) {
        List<ProbeFrame> frames = new ArrayList<>();
        for (String actionIdText : List.of(ROOT_FIRST_ID, COORD_FIRST_ID, IK_COORD_ROOT_ID)) {
            List<Float> times = suggestedSeconds(result, actionIdText);
            if (times.isEmpty()) {
                frames.add(ProbeFrame.failed(actionIdText, 0.0F, "代表剪辑不能采样"));
                continue;
            }
            for (float sampleSeconds : times) {
                frames.add(sample(result, actionIdText, sampleSeconds));
            }
        }
        return frames;
    }

    private static ProbeFrame sampleClip(Armature armature, AnimationClip clip, String actionIdText, float sampleSeconds) {
        if (armature == null || clip == null) {
            return ProbeFrame.failed(actionIdText, sampleSeconds, "骨架或剪辑缺失");
        }
        if (!Float.isFinite(sampleSeconds)) {
            return ProbeFrame.failed(actionIdText, sampleSeconds, "采样时间不是有限数");
        }
        Pose pose = clip.getPoseInTime(sampleSeconds);
        List<ProbeBone> bones = new ArrayList<>();
        for (String boneName : COMPARED_BONES) {
            Joint joint = armature.searchJointByName(boneName);
            if (joint == null || !pose.hasTransform(boneName)) {
                bones.add(ProbeBone.absent(boneName));
                continue;
            }
            JointTransform local = pose.get(boneName).copy();
            OpenMatrix4f localMatrix = local.toMatrix();
            OpenMatrix4f forward = armature.getBoundTransformFor(pose, joint);
            bones.add(new ProbeBone(
                    boneName,
                    true,
                    new float[] {local.translation().coordinateX, local.translation().coordinateY, local.translation().coordinateZ},
                    new float[] {local.rotation().x(), local.rotation().y(), local.rotation().z(), local.rotation().w()},
                    new float[] {local.scale().coordinateX, local.scale().coordinateY, local.scale().coordinateZ},
                    matrixElements(localMatrix),
                    matrixElements(forward)));
        }
        return new ProbeFrame(actionIdText, sampleSeconds, "", bones);
    }

    private static float nonKeySeconds(AnimationClip clip, float duration) {
        TransformSheet rootSheet = clip.getJointTransform("Root");
        if (rootSheet == null) {
            return duration * 0.37F;
        }
        Keyframe[] frames = rootSheet.getKeyframes();
        if (frames.length >= 2) {
            float between = (frames[0].time() + frames[1].time()) * 0.5F;
            if (between > frames[0].time() && between < frames[1].time()) {
                return between;
            }
        }
        float candidate = duration * 0.37F;
        for (Keyframe frame : frames) {
            if (Math.abs(frame.time() - candidate) < 0.0001F) {
                return duration * 0.13F;
            }
        }
        return candidate;
    }

    private static AnimationClip clipOf(DevLoadResult result, String actionIdText) {
        if (result == null) {
            return null;
        }
        try {
            return result.clip(DevActionLoader.parseActionId(actionIdText));
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static float[] matrixElements(OpenMatrix4f matrix) {
        return new float[] {
                matrix.m00, matrix.m01, matrix.m02, matrix.m03,
                matrix.m10, matrix.m11, matrix.m12, matrix.m13,
                matrix.m20, matrix.m21, matrix.m22, matrix.m23,
                matrix.m30, matrix.m31, matrix.m32, matrix.m33
        };
    }

    public static final class ProbeFrame {
        private final String actionIdText;
        private final float sampleSeconds;
        private final String failureText;
        private final List<ProbeBone> bones;

        private ProbeFrame(String actionIdText, float sampleSeconds, String failureText, List<ProbeBone> bones) {
            this.actionIdText = actionIdText == null ? "" : actionIdText;
            this.sampleSeconds = sampleSeconds;
            this.failureText = failureText == null ? "" : failureText;
            this.bones = List.copyOf(bones == null ? List.of() : bones);
        }

        private static ProbeFrame failed(String actionIdText, float sampleSeconds, String failureText) {
            return new ProbeFrame(actionIdText, sampleSeconds, failureText, List.of());
        }

        public String actionIdText() {
            return this.actionIdText;
        }

        public float sampleSeconds() {
            return this.sampleSeconds;
        }

        public String failureText() {
            return this.failureText;
        }

        public boolean sampled() {
            return this.failureText.isEmpty();
        }

        public List<ProbeBone> bones() {
            return this.bones;
        }
    }

    public static final class ProbeBone {
        private final String boneName;
        private final boolean present;
        private final float[] translation;
        private final float[] rotation;
        private final float[] scale;
        private final float[] localMatrix;
        private final float[] forwardKinematics;

        private ProbeBone(
                String boneName,
                boolean present,
                float[] translation,
                float[] rotation,
                float[] scale,
                float[] localMatrix,
                float[] forwardKinematics) {
            this.boneName = boneName;
            this.present = present;
            this.translation = translation;
            this.rotation = rotation;
            this.scale = scale;
            this.localMatrix = localMatrix;
            this.forwardKinematics = forwardKinematics;
        }

        private static ProbeBone absent(String boneName) {
            return new ProbeBone(boneName, false, new float[3], new float[4], new float[3], new float[16], new float[16]);
        }

        public String boneName() {
            return this.boneName;
        }

        public boolean present() {
            return this.present;
        }

        public float[] translation() {
            return this.translation.clone();
        }

        public float[] rotation() {
            return this.rotation.clone();
        }

        public float[] scale() {
            return this.scale.clone();
        }

        public float[] localMatrix() {
            return this.localMatrix.clone();
        }

        public float[] forwardKinematics() {
            return this.forwardKinematics.clone();
        }
    }
}

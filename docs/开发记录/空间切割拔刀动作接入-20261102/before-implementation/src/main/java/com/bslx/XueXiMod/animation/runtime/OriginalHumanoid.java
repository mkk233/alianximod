package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.joml.Quaternionf;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.TransformSheet;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;

import net.minecraft.resources.ResourceLocation;

/**
 * 发行版自己的目标骨架和水刃三拍。
 * 绑定数值是按米手写的，不取授权骨架的 localBind。
 */
public final class OriginalHumanoid {
    public static final String ARMATURE_NAME = "xueximod:target/humanoid";
    public static final ResourceLocation LEFT_ID = ResourceLocation.fromNamespaceAndPath("xueximod", "fallback/water_blade_left");
    public static final ResourceLocation RIGHT_ID = ResourceLocation.fromNamespaceAndPath("xueximod", "fallback/water_blade_right");
    public static final ResourceLocation HORIZONTAL_ID = ResourceLocation.fromNamespaceAndPath("xueximod", "fallback/water_blade_horizontal");
    public static final ResourceLocation EXAMPLE_ID = ResourceLocation.fromNamespaceAndPath("xueximod", "example/acknowledge");
    public static final ResourceLocation SPACE_CUT_ID = ResourceLocation.fromNamespaceAndPath("xueximod", "space_cut");

    /** 蓄力 → 挥出 → 收势。索引与下面六张帧表一一对应。 */
    private static final float[] SWING_TIMES = {0.0F, 0.18F, 0.30F, 0.45F, 0.60F, 0.78F, 1.0F};

    private static final float[] SPACE_CUT_TIMES = {0.0F, 0.15F, 0.30F, 0.50F, 0.65F, 0.85F, 1.0F};

    // 实机验证：经 RetargetProfile 的 diag(-1,-1,1) 后，pitch 正值把直臂甩向 +Z（身后）。
    // 水刃三拍统一用负 pitch，向 -Z（身前）蓄力和劈下。
    private static final float[][] LEFT_ARM = {
            {0, 0, 0}, {-105, 0, 20}, {-120, 0, 15}, {-95, 0, 5},
            {-55, 0, 20}, {-25, 0, 10}, {0, 0, 0}};
    private static final float[][] LEFT_BODY = {
            {0, 0, 0}, {0, -4, 0}, {0, -6, 0}, {0, 2, 0},
            {0, 6, 0}, {0, 3, 0}, {0, 0, 0}};
    private static final float[][] RIGHT_ARM = {
            {0, 0, 0}, {-95, 0, 30}, {-110, 0, 30}, {-90, 0, 10},
            {-50, 0, 25}, {-20, 0, 15}, {0, 0, 0}};
    private static final float[][] RIGHT_BODY = {
            {0, 0, 0}, {0, 4, 0}, {0, 6, 0}, {0, -2, 0},
            {0, -6, 0}, {0, -3, 0}, {0, 0, 0}};
    private static final float[][] HORIZONTAL_ARM = {
            {0, 0, 0}, {-80, 0, 25}, {-90, 0, 35}, {-90, 0, 10},
            {-85, 0, 40}, {-40, 0, 20}, {0, 0, 0}};
    private static final float[][] HORIZONTAL_BODY = {
            {0, 0, 0}, {0, -5, 0}, {0, -8, 0}, {0, 0, 0},
            {0, 8, 0}, {0, 4, 0}, {0, 0, 0}};

    // 拔刀：Blockbench 帧表，已按 rotation(x, -y, z) 换算成游戏源角度。
    private static final float[][] SPACE_CUT_BODY = {
            {15, 0, 0}, {20, -5, 0}, {20, 0, 0}, {10, 10, 0},
            {5, 20, 0}, {0, 10, 0}, {0, 10, 0}};
    private static final float[][] SPACE_CUT_HEAD = {
            {15, -10, 0}, {18, -10, 0}, {10, 0, 0}, {0, 10, 0},
            {-5, 15, 0}, {0, 8, 0}, {0, 8, 0}};
    private static final float[][] SPACE_CUT_ARM_RIGHT = {
            {-25, 0, 35}, {-30, 0, 40}, {-45, 0, 20}, {-90, 0, 0},
            {-110, 0, -30}, {-70, 0, -20}, {-70, 0, -20}};
    private static final float[][] SPACE_CUT_ARM_LEFT = {
            {0, 0, -5}, {5, 0, -10}, {-5, 0, -20}, {-20, 0, -30},
            {-30, 0, -35}, {-10, 0, -15}, {-10, 0, -15}};

    private OriginalHumanoid() {
    }

    public static void register() {
        Armature armature = createArmature();
        ActionRegistry.registerArmature(armature);
        registerSwing(LEFT_ID, "原创水刃左斜兜底，不是上游动作", 0.42F);
        registerSwing(RIGHT_ID, "原创水刃右斜兜底，不是上游动作", 0.42F);
        registerSwing(HORIZONTAL_ID, "原创水刃水平兜底，不是上游动作", 0.48F);
        registerSpaceCut();
        registerExample();
    }

    private static void registerSpaceCut() {
        float duration = 1.0F;
        AnimationClip clip = new AnimationClip();
        clip.addJointTransform("Root", hold(duration));
        clip.addJointTransform("Body", sampledSheet(duration, SPACE_CUT_TIMES, SPACE_CUT_BODY));
        clip.addJointTransform("Head", sampledSheet(duration, SPACE_CUT_TIMES, SPACE_CUT_HEAD));
        clip.addJointTransform("ArmRight", sampledSheet(duration, SPACE_CUT_TIMES, SPACE_CUT_ARM_RIGHT));
        clip.addJointTransform("ArmLeft", sampledSheet(duration, SPACE_CUT_TIMES, SPACE_CUT_ARM_LEFT));
        clip.setClipTime(duration);
        ActionDefinition definition = new ActionDefinition(
                SPACE_CUT_ID,
                ARMATURE_NAME,
                duration,
                false,
                0.06F,
                Set.of("Body", "Head", "ArmRight", "ArmLeft"),
                Set.of(ActionDefinition.Channel.ROTATION),
                List.of(new ActionDefinition.TimeNotice(0.62F, "draw-release")),
                "原创拔刀动作");
        ActionRegistry.registerPlayable(definition, clip, new ActionStatus(
                SPACE_CUT_ID,
                ActionStatus.Availability.ORIGINAL,
                "原创拔刀动作。五骨适配，不含独立刀身翻转；游戏观感待验证。",
                ARMATURE_NAME,
                "",
                "",
                "authored",
                "",
                duration,
                "authored",
                "",
                true));
    }

    private static void registerSwing(ResourceLocation actionId, String reason, float duration) {
        float[][] armFrames;
        float[][] bodyFrames;
        if (LEFT_ID.equals(actionId)) {
            armFrames = LEFT_ARM;
            bodyFrames = LEFT_BODY;
        } else if (RIGHT_ID.equals(actionId)) {
            armFrames = RIGHT_ARM;
            bodyFrames = RIGHT_BODY;
        } else {
            armFrames = HORIZONTAL_ARM;
            bodyFrames = HORIZONTAL_BODY;
        }
        AnimationClip clip = new AnimationClip();
        clip.addJointTransform("Root", hold(duration));
        clip.addJointTransform("Body", sheet(duration, bodyFrames));
        clip.addJointTransform("ArmRight", sheet(duration, armFrames));
        clip.setClipTime(duration);
        ActionDefinition definition = new ActionDefinition(
                actionId,
                ARMATURE_NAME,
                duration,
                false,
                0.06F,
                Set.of("Body", "ArmRight"),
                Set.of(ActionDefinition.Channel.ROTATION),
                List.of(new ActionDefinition.TimeNotice(duration * 0.45F, "slash-mid")),
                "原创兜底");
        ActionRegistry.registerPlayable(definition, clip, new ActionStatus(
                actionId,
                ActionStatus.Availability.ORIGINAL,
                reason + " 刚体适配和游戏观感仍待渲染批验证。",
                ARMATURE_NAME,
                "",
                "",
                "fallback",
                "",
                duration,
                "authored",
                "",
                true));
    }

    private static TransformSheet sheet(float duration, float[][] frames) {
        List<Keyframe> keyframes = new ArrayList<>(frames.length);
        for (int i = 0; i < frames.length; i++) {
            float[] frame = frames[i];
            JointTransform transform = i == 0 || i == frames.length - 1
                    ? JointTransform.empty()
                    : rotation(frame[0], frame[1], frame[2]);
            keyframes.add(new Keyframe(duration * SWING_TIMES[i], transform));
        }
        return new TransformSheet(List.copyOf(keyframes));
    }

    /** 按 0.01 秒均匀采样，首尾姿态保留帧表原值。 */
    private static TransformSheet sampledSheet(float duration, float[] times, float[][] frames) {
        int steps = Math.max(1, Math.round(duration / 0.01F));
        List<Keyframe> keyframes = new ArrayList<>(steps + 1);
        for (int step = 0; step <= steps; step++) {
            float time = duration * step / steps;
            float[] euler = sampleEuler(times, frames, time);
            keyframes.add(new Keyframe(time, rotation(euler[0], euler[1], euler[2])));
        }
        return new TransformSheet(List.copyOf(keyframes));
    }

    private static float[] sampleEuler(float[] times, float[][] frames, float time) {
        int last = times.length - 1;
        if (time <= times[0]) {
            return new float[] {frames[0][0], frames[0][1], frames[0][2]};
        }
        if (time >= times[last]) {
            return new float[] {frames[last][0], frames[last][1], frames[last][2]};
        }
        int index = 0;
        while (index < last && time > times[index + 1]) {
            index++;
        }
        float span = times[index + 1] - times[index];
        float weight = span > 0.0F ? (time - times[index]) / span : 0.0F;
        float[] from = frames[index];
        float[] to = frames[index + 1];
        return new float[] {
                from[0] + (to[0] - from[0]) * weight,
                from[1] + (to[1] - from[1]) * weight,
                from[2] + (to[2] - from[2]) * weight};
    }

    private static void registerExample() {
        float duration = 0.36F;
        AnimationClip clip = new AnimationClip();
        clip.addJointTransform("Root", hold(duration));
        clip.addJointTransform("Head", new TransformSheet(List.of(
                new Keyframe(0.0F, JointTransform.empty()),
                new Keyframe(0.16F, rotation(18.0F, 0.0F, 0.0F)),
                new Keyframe(duration, JointTransform.empty()))));
        clip.setClipTime(duration);
        ActionDefinition definition = new ActionDefinition(
                EXAMPLE_ID,
                ARMATURE_NAME,
                duration,
                false,
                0.05F,
                Set.of("Head"),
                Set.of(ActionDefinition.Channel.ROTATION),
                List.of(new ActionDefinition.TimeNotice(0.16F, "nod")),
                "原创扩展示例");
        ActionRegistry.registerPlayable(definition, clip, new ActionStatus(
                EXAMPLE_ID,
                ActionStatus.Availability.EXAMPLE,
                "原创点头示例，用来证明注册新动作不必改播放器。不进入正式术式列表。",
                ARMATURE_NAME,
                "",
                "",
                "example",
                "",
                duration,
                "authored",
                "",
                true));
    }

    private static Armature createArmature() {
        Joint root = new Joint("Root", 0, OpenMatrix4f.createTranslation(0.0F, 0.0F, 0.0F));
        Joint body = new Joint("Body", 1, OpenMatrix4f.createTranslation(0.0F, 0.72F, 0.0F));
        Joint head = new Joint("Head", 2, OpenMatrix4f.createTranslation(0.0F, 0.48F, 0.0F));
        Joint armRight = new Joint("ArmRight", 3, OpenMatrix4f.createTranslation(0.22F, 0.32F, 0.0F));
        Joint armLeft = new Joint("ArmLeft", 4, OpenMatrix4f.createTranslation(-0.22F, 0.32F, 0.0F));
        body.addSubJoints(head, armRight, armLeft);
        root.addSubJoints(body);
        Map<String, Joint> joints = new LinkedHashMap<>();
        joints.put("Root", root);
        joints.put("Body", body);
        joints.put("Head", head);
        joints.put("ArmRight", armRight);
        joints.put("ArmLeft", armLeft);
        return new Armature(ARMATURE_NAME, 5, root, joints);
    }

    private static TransformSheet hold(float duration) {
        return new TransformSheet(List.of(
                new Keyframe(0.0F, JointTransform.empty()),
                new Keyframe(duration, JointTransform.empty())));
    }

    private static JointTransform rotation(float pitchDegrees, float yawDegrees, float rollDegrees) {
        Quaternionf quaternion = new Quaternionf()
                .rotateX((float) Math.toRadians(pitchDegrees))
                .rotateY((float) Math.toRadians(yawDegrees))
                .rotateZ((float) Math.toRadians(rollDegrees));
        return JointTransform.rotation(quaternion);
    }
}

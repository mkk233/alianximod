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
        float duration = 1.6F;
        AnimationClip clip = new AnimationClip();
        clip.addJointTransform("Root", hold(duration));
        clip.addJointTransform("Body", hold(duration));
        clip.addJointTransform("Head", hold(duration));
        clip.addJointTransform("ArmRight", hold(duration));
        clip.addJointTransform("ArmLeft", hold(duration));
        clip.setClipTime(duration);
        ActionDefinition definition = new ActionDefinition(
                SPACE_CUT_ID,
                ARMATURE_NAME,
                duration,
                false,
                0.06F,
                Set.of("Body", "Head", "ArmRight", "ArmLeft"),
                Set.of(ActionDefinition.Channel.ROTATION),
                List.of(new ActionDefinition.TimeNotice(0.48F, "draw-release")),
                "左腰右拔 1.6 秒，只驱动姿态时钟");
        ActionRegistry.registerPlayable(definition, clip, new ActionStatus(
                SPACE_CUT_ID,
                ActionStatus.Availability.ORIGINAL,
                "左腰右拔 v5，1.6 秒一次。双节臂和刀挂点在客户端按确认帧绘制，不推迟伤害。",
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
        for (int step = 0; step < frames.length; step++) {
            float[] frame = frames[step];
            JointTransform transform = step == 0 || step == frames.length - 1
                    ? JointTransform.empty()
                    : rotation(frame[0], frame[1], frame[2]);
            keyframes.add(new Keyframe(duration * SWING_TIMES[step], transform));
        }
        return new TransformSheet(List.copyOf(keyframes));
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

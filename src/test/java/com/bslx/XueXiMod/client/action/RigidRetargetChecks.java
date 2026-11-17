package com.bslx.XueXiMod.client.action;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.bslx.XueXiMod.animation.core.AnimationClip;
import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.JointTransform;
import com.bslx.XueXiMod.animation.core.Keyframe;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.TransformSheet;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.math.Vec3f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.ActionRegistry;
import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;
import com.bslx.XueXiMod.animation.runtime.loader.DevActionLoader;
import com.bslx.XueXiMod.animation.runtime.loader.DevLoadResult;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

import com.mojang.blaze3d.vertex.PoseStack;

/** 刚性重定向的方向探针。期望值按轴关系手写，不调用实现公式回推。 */
public final class RigidRetargetChecks {
    private static final float TOLERANCE = 1.0E-3F;
    private static final float BASE_BODY_PITCH = 0.2F;
    private static final float BASE_BODY_YAW = -0.1F;
    private static final float BASE_BODY_ROLL = 0.05F;
    private static final float BASE_HEAD_PITCH = 0.15F;
    private static final float BASE_HEAD_YAW = 0.05F;
    private static final float BASE_HEAD_ROLL = -0.05F;
    private static final float BASE_ARM_PITCH = 0.25F;
    private static final float BASE_ARM_YAW = 0.1F;
    private static final float BASE_ARM_ROLL = -0.15F;
    private static final float BASE_LEFT_ARM_PITCH = -0.2F;
    private static final float BASE_LEFT_ARM_YAW = 0.12F;
    private static final float BASE_LEFT_ARM_ROLL = 0.1F;
    private static final float BASE_LEG_PITCH = -0.27F;
    private static final float BASE_LEG_YAW = 0.11F;
    private static final float BASE_LEG_ROLL = 0.07F;
    // 原版 y 向下，3.2 是蹲伏时的 body 枢轴偏移。三个分量都非零，且 z 拉到 -1.5，
    // 让漏加枢轴的实现把 body 落点带偏 0.5 以上。
    private static final float SLASH_BODY_PIVOT_X = 1.5F;
    private static final float SLASH_BODY_PIVOT_Y = 3.2F;
    private static final float SLASH_BODY_PIVOT_Z = -1.5F;
    private static int checks;

    private RigidRetargetChecks() {
    }

    public static void main(String[] programArguments) {
        checkSourceRollProbe();
        checkSourcePitchProbe();
        checkMirroredPitch();
        checkRestIgnoresBladeDirection();
        checkParentIncludedOnce();
        checkOriginalRest();
        checkAimLeavesUndrivenParts();
        checkFallbackContinuity();
        checkSwingOwnership();
        checkPoseScopeWiring();
        checkUpperBodySlash();
        checkSlashPlaneYawZero();
        checkSlashPlaneYawNinety();
        checkContractRight();
        checkFirstPersonNeutralAndSlash();
        checkFirstPersonCameraTurn();
        checkRegisteredSlashPlanes();
        checkCaptureIncludesExtraPitch();
        checkItemDisplayAxes();
        checkRecipeCache();
        checkWhitelistCoverage();
        System.out.println("RigidRetargetChecks: " + checks + " assertions");
    }

    private static void checkSourceRollProbe() {
        OpenMatrix4f source = sourceRollPositive();
        Vec3f sourceDown = MatrixFrames.apply(source, 0.0F, -1.0F, 0.0F);
        near(sourceDown, 1.0F, 0.0F, 0.0F, "source roll fixture");
        OpenMatrix4f target = RigidPoseRetarget.targetRotation(RetargetProfile.sourceBiped().axisFlip(), source, new OpenMatrix4f());
        Vec3f landed = MatrixFrames.apply(target, 0.0F, 1.0F, 0.0F);
        near(landed, -1.0F, 0.0F, 0.0F, "source roll target");
    }

    private static void checkSourcePitchProbe() {
        OpenMatrix4f source = sourcePitchPositive();
        Vec3f sourceDown = MatrixFrames.apply(source, 0.0F, -1.0F, 0.0F);
        near(sourceDown, 0.0F, 0.0F, -1.0F, "source pitch fixture");
        OpenMatrix4f target = RigidPoseRetarget.targetRotation(RetargetProfile.sourceBiped().axisFlip(), source, new OpenMatrix4f());
        Vec3f landed = MatrixFrames.apply(target, 0.0F, 1.0F, 0.0F);
        near(landed, 0.0F, 0.0F, -1.0F, "source pitch target");
        VanillaEuler euler = VanillaEuler.from(target);
        near(euler.pitchRadians(), -(float) Math.PI / 2.0F, "vanilla pitch");
        near(euler.yawRadians(), 0.0F, "vanilla yaw");
        near(euler.rollRadians(), 0.0F, "vanilla roll");
        Vec3f rebuilt = independentZyx(euler.pitchRadians(), euler.yawRadians(), euler.rollRadians(), 0.0F, 1.0F, 0.0F);
        near(rebuilt, 0.0F, 0.0F, -1.0F, "independent euler");
    }

    private static void checkMirroredPitch() {
        OpenMatrix4f rightTarget = RigidPoseRetarget.targetRotation(RetargetProfile.sourceBiped().axisFlip(), sourcePitchPositive(), new OpenMatrix4f());
        OpenMatrix4f leftTarget = RigidPoseRetarget.targetRotation(RetargetProfile.sourceBiped().axisFlip(), sourcePitchNegative(), new OpenMatrix4f());
        near(VanillaEuler.from(rightTarget).pitchRadians(), -(float) Math.PI / 2.0F, "right thigh pitch");
        near(VanillaEuler.from(leftTarget).pitchRadians(), (float) Math.PI / 2.0F, "left thigh pitch");
    }

    private static void checkRestIgnoresBladeDirection() {
        Armature armature = sourceArmature();
        Pose pose = identityPose(armature);
        RetargetedPose adapted = RigidPoseRetarget.adapt(armature, pose, RetargetProfile.sourceBiped());
        nearEuler(adapted.body(), "rest body");
        nearEuler(adapted.head(), "rest head");
        nearEuler(adapted.rightArm(), "rest right arm");
        nearEuler(adapted.leftArm(), "rest left arm");
        nearEuler(adapted.rightLeg(), "rest right leg");
        nearEuler(adapted.leftLeg(), "rest left leg");
    }

    private static void checkParentIncludedOnce() {
        Armature armature = sourceArmature();
        Pose pose = new Pose();
        pose.putJointData("Torso", JointTransform.rotation(new Quaternionf().rotateX(1.0F)));
        pose.putJointData("Chest", JointTransform.empty());
        pose.putJointData("Head", JointTransform.empty());
        RetargetProfile profile = RetargetProfile.sourceBiped();
        RetargetedPose adapted = RigidPoseRetarget.adapt(armature, pose, profile);
        OpenMatrix4f[] current = armature.getPoseAsTransformMatrix(pose, false);
        OpenMatrix4f[] bind = armature.getPoseAsTransformMatrix(new Pose(), false);
        OpenMatrix4f headChain = RigidPoseRetarget.targetRotation(profile.axisFlip(), current[3], bind[3]);
        OpenMatrix4f chestChain = RigidPoseRetarget.targetRotation(profile.axisFlip(), current[2], bind[2]);
        Vec3f headLanded = MatrixFrames.apply(adapted.head(), 0.0F, 1.0F, 0.0F);
        Vec3f chainLanded = MatrixFrames.apply(headChain, 0.0F, 1.0F, 0.0F);
        Vec3f chestLanded = MatrixFrames.apply(chestChain, 0.0F, 1.0F, 0.0F);
        near(headLanded, chainLanded.coordinateX, chainLanded.coordinateY, chainLanded.coordinateZ, "head uses full chain");
        near(headLanded, chestLanded.coordinateX, chestLanded.coordinateY, chestLanded.coordinateZ, "head matches chest chain");
        Vec3f doubled = MatrixFrames.apply(MatrixFrames.premultiply(chestChain, chestChain), 0.0F, 1.0F, 0.0F);
        yes(distance(headLanded, doubled) > 0.2F, "head is not body applied twice");
        yes(distance(headLanded, new Vec3f(0.0F, 1.0F, 0.0F)) > 0.2F, "parent rotation is visible");
        yes(adapted.rightArm() == null && adapted.leftLeg() == null, "undriven limbs stay empty");
    }

    private static void checkOriginalRest() {
        Armature armature = originalArmature();
        RetargetedPose adapted = RigidPoseRetarget.adapt(armature, identityPose(armature), RetargetProfile.forArmature(armature));
        nearEuler(adapted.body(), "original body");
        nearEuler(adapted.head(), "original head");
        nearEuler(adapted.rightArm(), "original right arm");
        nearEuler(adapted.leftArm(), "original left arm");
        yes(adapted.rightLeg() == null && adapted.leftLeg() == null, "original rig has no thighs");
    }

    private static void checkAimLeavesUndrivenParts() {
        OpenMatrix4f alignment = SlashPlaneAlign.modelAlignment(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 90.0F);
        RetargetedPose local = new RetargetedPose(null, null, new OpenMatrix4f(), null, null, null, null);
        RetargetedPose aimed = RigidPoseRetarget.aim(local, alignment);
        yes(aimed.rightArm() == local.rightArm(), "aim does not bake alignment into the arm");
        yes(aimed.worldBasis() == alignment, "alignment stays on the side");
        yes(aimed.leftArm() == null && aimed.body() == null && aimed.head() == null, "undriven parts stay empty");
    }

    private static void checkSlashPlaneYawZero() {
        OpenMatrix4f alignment = SlashPlaneAlign.modelAlignment(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
        Vec3f modelForward = MatrixFrames.apply(alignment, 0.0F, 0.0F, -1.0F);
        near(modelForward, 0.0F, 0.0F, -1.0F, "aligned model forward at yaw zero");
        Vec3f worldForward = SlashPlaneAlign.modelToWorld(modelForward.coordinateX, modelForward.coordinateY, modelForward.coordinateZ, 0.0F);
        near(worldForward, 0.0F, 0.0F, 1.0F, "yaw zero returns world forward");
    }

    private static void checkSlashPlaneYawNinety() {
        OpenMatrix4f alignment = SlashPlaneAlign.modelAlignment(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 90.0F);
        Vec3f modelForward = MatrixFrames.apply(alignment, 0.0F, 0.0F, -1.0F);
        near(modelForward, 1.0F, 0.0F, 0.0F, "yaw ninety model forward");
        Vec3f worldForward = SlashPlaneAlign.modelToWorld(modelForward.coordinateX, modelForward.coordinateY, modelForward.coordinateZ, 90.0F);
        near(worldForward, 0.0F, 0.0F, 1.0F, "yaw ninety returns the same world forward");
    }

    private static void checkContractRight() {
        Vec3f right = SlashPlaneAlign.contractRight(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F);
        near(right, 1.0F, 0.0F, 0.0F, "contract right");
        Vec3f restored = MatrixFrames.cross(new Vec3f(0.0F, 0.0F, 1.0F), right);
        near(restored, 0.0F, 1.0F, 0.0F, "forward cross right restores normal");
    }

    private static void checkFirstPersonNeutralAndSlash() {
        Vec3f rest = new Vec3f(0.0F, -1.0F, 0.0F);
        Vec3f neutral = FirstPersonHandBasis.itemAxis(new OpenMatrix4f(), rest);
        near(neutral, 0.0F, -1.0F, 0.0F, "neutral item axis");
        OpenMatrix4f slash = modelPitchNegative();
        Vec3f rightAxis = FirstPersonHandBasis.itemAxis(slash, rest);
        Vec3f leftAxis = FirstPersonHandBasis.itemAxis(slash, rest);
        near(rightAxis, 0.0F, 0.0F, -1.0F, "right slash item axis");
        near(leftAxis, 0.0F, 0.0F, -1.0F, "left slash item axis");
        Vec3f rightPoint = FirstPersonHandBasis.pointOnAxis(true, 0.0F, FirstPersonHandBasis.cameraRotation(slash), rest);
        Vec3f leftPoint = FirstPersonHandBasis.pointOnAxis(false, 0.0F, FirstPersonHandBasis.cameraRotation(slash), rest);
        near(rightPoint, 0.56F, -0.52F, -1.72F, "right hand point");
        near(leftPoint, -0.56F, -0.52F, -1.72F, "left hand point");
        Quaternionf quaternion = FirstPersonHandBasis.quaternion(FirstPersonHandBasis.cameraRotation(slash));
        Vector3f turned = quaternion.transform(new Vector3f(0.0F, -1.0F, 0.0F));
        near(turned.x(), 0.0F, "quaternion across");
        near(turned.y(), 0.0F, "quaternion upward");
        near(turned.z(), -1.0F, "quaternion ahead");
    }

    private static void checkFirstPersonCameraTurn() {
        OpenMatrix4f aligned = FirstPersonHandBasis.cameraAlignment(
                0.0F, 0.0F, 1.0F,
                0.0F, 1.0F, 0.0F,
                0.0F, 0.0F, 1.0F,
                0.0F, 1.0F, 0.0F,
                1.0F, 0.0F, 0.0F);
        Vec3f ahead = MatrixFrames.apply(aligned, 0.0F, 0.0F, -1.0F);
        near(ahead, 0.0F, 0.0F, -1.0F, "camera still facing the frame");
        OpenMatrix4f turned = FirstPersonHandBasis.cameraAlignment(
                0.0F, 0.0F, 1.0F,
                0.0F, 1.0F, 0.0F,
                1.0F, 0.0F, 0.0F,
                0.0F, 1.0F, 0.0F,
                0.0F, 0.0F, -1.0F);
        Vec3f turnedAhead = MatrixFrames.apply(turned, 0.0F, 0.0F, -1.0F);
        near(turnedAhead, 1.0F, 0.0F, 0.0F, "camera turned away from the frame");
    }

    private static OpenMatrix4f sourceRollPositive() {
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m00 = 0.0F;
        matrix.m10 = -1.0F;
        matrix.m01 = 1.0F;
        matrix.m11 = 0.0F;
        return matrix;
    }

    private static OpenMatrix4f sourcePitchPositive() {
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m11 = 0.0F;
        matrix.m22 = 0.0F;
        matrix.m21 = -1.0F;
        matrix.m12 = 1.0F;
        return matrix;
    }

    private static OpenMatrix4f sourcePitchNegative() {
        OpenMatrix4f matrix = new OpenMatrix4f();
        matrix.m11 = 0.0F;
        matrix.m22 = 0.0F;
        matrix.m21 = 1.0F;
        matrix.m12 = -1.0F;
        return matrix;
    }

    private static OpenMatrix4f modelPitchNegative() {
        return sourcePitchNegative();
    }

    private static Vec3f independentZyx(float pitch, float yaw, float roll, float across, float upward, float ahead) {
        float pitchCosine = (float) Math.cos(pitch);
        float pitchSine = (float) Math.sin(pitch);
        float yawCosine = (float) Math.cos(yaw);
        float yawSine = (float) Math.sin(yaw);
        float rollCosine = (float) Math.cos(roll);
        float rollSine = (float) Math.sin(roll);
        float pitchedUpward = pitchCosine * upward - pitchSine * ahead;
        float pitchedAhead = pitchSine * upward + pitchCosine * ahead;
        float yawedAcross = yawCosine * across + yawSine * pitchedAhead;
        float yawedAhead = -yawSine * across + yawCosine * pitchedAhead;
        float rolledAcross = rollCosine * yawedAcross - rollSine * pitchedUpward;
        float rolledUpward = rollSine * yawedAcross + rollCosine * pitchedUpward;
        return new Vec3f(rolledAcross, rolledUpward, yawedAhead);
    }

    private static Armature sourceArmature() {
        Joint root = joint("Root", 0, 0.0F, 0.0F, 0.0F);
        Joint torso = joint("Torso", 1, 0.0F, 0.6F, 0.0F);
        Joint chest = joint("Chest", 2, 0.0F, 0.25F, 0.0F);
        Joint head = joint("Head", 3, 0.0F, 0.25F, 0.0F);
        Joint thighRight = joint("Thigh_R", 4, 0.12F, -0.05F, 0.0F);
        Joint thighLeft = joint("Thigh_L", 5, -0.12F, -0.05F, 0.0F);
        Joint armRight = joint("Arm_R", 6, 0.2F, 0.1F, 0.0F);
        Joint handRight = joint("Hand_R", 7, 0.0F, -0.25F, 0.0F);
        Joint toolRight = joint("Tool_R", 8, 0.0F, -0.35F, 0.0F);
        Joint armLeft = joint("Arm_L", 9, -0.2F, 0.1F, 0.0F);
        Joint handLeft = joint("Hand_L", 10, 0.0F, -0.25F, 0.0F);
        Joint toolLeft = joint("Tool_L", 11, 0.0F, -0.35F, 0.0F);
        chest.addSubJoints(head, armRight, armLeft);
        armRight.addSubJoints(handRight);
        handRight.addSubJoints(toolRight);
        armLeft.addSubJoints(handLeft);
        handLeft.addSubJoints(toolLeft);
        torso.addSubJoints(chest);
        root.addSubJoints(torso, thighRight, thighLeft);
        Map<String, Joint> joints = new LinkedHashMap<>();
        joints.put("Root", root);
        joints.put("Torso", torso);
        joints.put("Chest", chest);
        joints.put("Head", head);
        joints.put("Thigh_R", thighRight);
        joints.put("Thigh_L", thighLeft);
        joints.put("Arm_R", armRight);
        joints.put("Hand_R", handRight);
        joints.put("Tool_R", toolRight);
        joints.put("Arm_L", armLeft);
        joints.put("Hand_L", handLeft);
        joints.put("Tool_L", toolLeft);
        return new Armature("probe-biped", joints.size(), root, joints);
    }

    private static Armature originalArmature() {
        Joint root = joint("Root", 0, 0.0F, 0.0F, 0.0F);
        Joint body = joint("Body", 1, 0.0F, 0.72F, 0.0F);
        Joint head = joint("Head", 2, 0.0F, 0.48F, 0.0F);
        Joint armRight = joint("ArmRight", 3, 0.22F, 0.32F, 0.0F);
        Joint armLeft = joint("ArmLeft", 4, -0.22F, 0.32F, 0.0F);
        body.addSubJoints(head, armRight, armLeft);
        root.addSubJoints(body);
        Map<String, Joint> joints = new LinkedHashMap<>();
        joints.put("Root", root);
        joints.put("Body", body);
        joints.put("Head", head);
        joints.put("ArmRight", armRight);
        joints.put("ArmLeft", armLeft);
        return new Armature("probe-original", joints.size(), root, joints);
    }

    private static Joint joint(String boneName, int jointId, float across, float upward, float ahead) {
        return new Joint(boneName, jointId, OpenMatrix4f.createTranslation(across, upward, ahead));
    }

    private static Pose identityPose(Armature armature) {
        Pose pose = new Pose();
        armature.rootJoint.iterSubJoints(joint -> pose.putJointData(joint.getName(), JointTransform.empty()));
        return pose;
    }

    private static void nearEuler(OpenMatrix4f rotation, String label) {
        yes(rotation != null, label + " missing");
        VanillaEuler euler = VanillaEuler.from(rotation);
        near(euler.pitchRadians(), 0.0F, label + " pitch");
        near(euler.yawRadians(), 0.0F, label + " yaw");
        near(euler.rollRadians(), 0.0F, label + " roll");
    }

    private static void checkRegisteredSlashPlanes() {
        OriginalHumanoid.register();
        DevLoadResult loaded = DevActionLoader.load(Path.of("D:/alianximod/dev-authorized/epicfight"));
        yes(loaded.sourceArmature() != null, "dev rig missing: " + loaded.summaryText());
        ActionRegistry.publishDev(loaded);
        ActionVisualCatalog.invalidate();
        ResourceLocation[] visualIds = {
                OriginalHumanoid.LEFT_ID,
                OriginalHumanoid.RIGHT_ID,
                OriginalHumanoid.HORIZONTAL_ID,
                ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto1"),
                ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto2"),
                ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/sword_auto3")
        };
        Vec3f[] forwards = new Vec3f[visualIds.length];
        Vec3f[] normals = new Vec3f[visualIds.length];
        String[] notes = new String[visualIds.length];
        for (int visualIndex = 0; visualIndex < visualIds.length; visualIndex++) {
            ResourceLocation visualId = visualIds[visualIndex];
            yes(ActionVisualCatalog.blade(visualId), "registered blade");
            yes(ActionVisualCatalog.usesHand(visualId, true), "right hand applies");
            yes(!ActionVisualCatalog.usesHand(visualId, false), "left hand stays out of the recipe");
            forwards[visualIndex] = ActionVisualCatalog.referenceForward(visualId);
            normals[visualIndex] = ActionVisualCatalog.referenceNormal(visualId);
            yes(forwards[visualIndex] != null && normals[visualIndex] != null, "calibrated " + visualId);
            near(lengthOf(forwards[visualIndex]), 1.0F, "forward unit");
            near(lengthOf(normals[visualIndex]), 1.0F, "normal unit");
            yes(Math.abs(dot(forwards[visualIndex], normals[visualIndex])) < TOLERANCE, "reference axes orthogonal");
            notes[visualIndex] = ActionVisualCatalog.basis(visualId);
            yes(notes[visualIndex].length() > 8, "basis note");
            OpenMatrix4f alignment = SlashPlaneAlign.modelAlignment(
                    0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F, null, forwards[visualIndex], normals[visualIndex]);
            Vec3f landedForward = MatrixFrames.apply(alignment, forwards[visualIndex].coordinateX, forwards[visualIndex].coordinateY, forwards[visualIndex].coordinateZ);
            near(landedForward, 0.0F, 0.0F, -1.0F, "reference forward meets the frozen forward");
            Vec3f landedNormal = MatrixFrames.apply(alignment, normals[visualIndex].coordinateX, normals[visualIndex].coordinateY, normals[visualIndex].coordinateZ);
            near(landedNormal, 0.0F, -1.0F, 0.0F, "reference normal meets the frozen normal");
            Vec3f cameraForward = new Vec3f(-forwards[visualIndex].coordinateX, -forwards[visualIndex].coordinateY, forwards[visualIndex].coordinateZ);
            OpenMatrix4f cameraAim = FirstPersonHandBasis.cameraAlignment(
                    0.0F, 0.0F, 1.0F,
                    0.0F, 1.0F, 0.0F,
                    0.0F, 0.0F, 1.0F,
                    0.0F, 1.0F, 0.0F,
                    1.0F, 0.0F, 0.0F,
                    forwards[visualIndex],
                    normals[visualIndex]);
            Vec3f cameraLanded = MatrixFrames.apply(cameraAim, cameraForward.coordinateX, cameraForward.coordinateY, cameraForward.coordinateZ);
            near(cameraLanded, 0.0F, 0.0F, -1.0F, "first person uses the same reference");
        }
        yes(forwards[0].coordinateZ < 0.0F && forwards[1].coordinateZ < 0.0F, "water slashes face forward");
        for (int outerIndex = 0; outerIndex < visualIds.length; outerIndex++) {
            for (int innerIndex = outerIndex + 1; innerIndex < visualIds.length; innerIndex++) {
                float forwardDot = dot(forwards[outerIndex], forwards[innerIndex]);
                float normalDot = dot(normals[outerIndex], normals[innerIndex]);
                // 上身水刃不乘 L，和授权剪辑共用接近的平面不算回归；水刃彼此仍要区分。
                boolean upperOuter = ActionVisualCatalog.alignmentScope(visualIds[outerIndex]) == SlashAlignmentScope.UPPER_BODY_SLASH;
                boolean upperInner = ActionVisualCatalog.alignmentScope(visualIds[innerIndex]) == SlashAlignmentScope.UPPER_BODY_SLASH;
                if (upperOuter == upperInner) {
                    yes(!(forwardDot > 0.98F && normalDot > 0.98F), "slash frames are not copies");
                }
                yes(!notes[outerIndex].equals(notes[innerIndex]), "basis notes are not copies");
            }
        }
        yes(!ActionVisualCatalog.lateralAxis(OriginalHumanoid.HORIZONTAL_ID), "horizontal slash takes the blade axis, not the lateral one");
        yes(!ActionVisualCatalog.lateralAxis(OriginalHumanoid.LEFT_ID), "left slash uses the blade axis");
        float[] firstAuto = ActionVisualCatalog.fractions(visualIds[3]);
        float[] secondAuto = ActionVisualCatalog.fractions(visualIds[4]);
        float[] thirdAuto = ActionVisualCatalog.fractions(visualIds[5]);
        near(firstAuto[1], 0.1F / 0.6F, "first sword tool key");
        near(secondAuto[0], 0.0833F / 0.6F, "second sword open key");
        near(secondAuto[1], 0.2167F / 0.6F, "second sword hit key");
        near(thirdAuto[1], 0.2F / 0.6F, "third sword tool key");
        yes(!ActionVisualCatalog.blade(ResourceLocation.fromNamespaceAndPath("xueximod", "probe/unknown")), "unknown action is not a blade");
        ResourceLocation rollId = ResourceLocation.fromNamespaceAndPath("xueximod", "probe/roll");
        ActionVisualCatalog.registerRoll(rollId, "探针翻滚，不校准挥斩平面");
        yes(ActionVisualCatalog.roll(rollId) && !ActionVisualCatalog.blade(rollId), "roll stays out of first person");
        int fitted = ActionVisualCatalog.calibrationCount(OriginalHumanoid.LEFT_ID);
        ActionVisualCatalog.referenceForward(OriginalHumanoid.LEFT_ID);
        yes(ActionVisualCatalog.calibrationCount(OriginalHumanoid.LEFT_ID) == fitted, "cached clip is not refit");
        ActionVisualCatalog.invalidate();
        ActionVisualCatalog.referenceForward(OriginalHumanoid.LEFT_ID);
        yes(ActionVisualCatalog.calibrationCount(OriginalHumanoid.LEFT_ID) == fitted + 1, "reload refits from the clip");
    }

    private static void checkCaptureIncludesExtraPitch() {
        PoseStack poseStack = new PoseStack();
        RenderSpaceCapture.push(poseStack);
        poseStack.pushPose();
        poseStack.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180.0F)));
        poseStack.scale(-1.0F, -1.0F, 1.0F);
        OpenMatrix4f plain = RenderSpaceCapture.worldToModel();
        RenderSpaceCapture.pop();
        yes(plain != null && RenderSpaceCapture.worldToModel() == null, "capture follows the render scope");
        OpenMatrix4f fromCapture = SlashPlaneAlign.modelAlignment(
                0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 90.0F, plain, new Vec3f(0.0F, 0.0F, -1.0F), new Vec3f(0.0F, -1.0F, 0.0F));
        OpenMatrix4f fromYaw = SlashPlaneAlign.modelAlignment(0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 0.0F);
        Vec3f capturedAhead = MatrixFrames.apply(fromCapture, 0.0F, 0.0F, -1.0F);
        Vec3f yawAhead = MatrixFrames.apply(fromYaw, 0.0F, 0.0F, -1.0F);
        near(capturedAhead, yawAhead.coordinateX, yawAhead.coordinateY, yawAhead.coordinateZ, "plain stack matches body yaw");
        PoseStack pitched = new PoseStack();
        RenderSpaceCapture.push(pitched);
        pitched.pushPose();
        pitched.mulPose(new Quaternionf().rotationY((float) Math.toRadians(155.0F)));
        pitched.mulPose(new Quaternionf().rotationX((float) Math.toRadians(40.0F)));
        pitched.scale(-1.0F, -1.0F, 1.0F);
        OpenMatrix4f captured = RenderSpaceCapture.worldToModel();
        RenderSpaceCapture.pop();
        Vec3f referenceForward = new Vec3f(0.0F, 0.0F, -1.0F);
        Vec3f referenceNormal = new Vec3f(0.0F, -1.0F, 0.0F);
        OpenMatrix4f aimed = SlashPlaneAlign.modelAlignment(
                0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 180.0F, captured, referenceForward, referenceNormal);
        Vec3f modelForward = MatrixFrames.apply(aimed, referenceForward.coordinateX, referenceForward.coordinateY, referenceForward.coordinateZ);
        OpenMatrix4f inverseCaptured = new OpenMatrix4f(captured);
        inverseCaptured.invert();
        Vec3f restoredForward = MatrixFrames.apply(inverseCaptured, modelForward.coordinateX, modelForward.coordinateY, modelForward.coordinateZ);
        near(restoredForward, 0.0F, 0.0F, 1.0F, "captured matrix restores world forward");
        Vec3f modelNormal = MatrixFrames.apply(aimed, referenceNormal.coordinateX, referenceNormal.coordinateY, referenceNormal.coordinateZ);
        Vec3f restoredNormal = MatrixFrames.apply(inverseCaptured, modelNormal.coordinateX, modelNormal.coordinateY, modelNormal.coordinateZ);
        near(restoredNormal, 0.0F, 1.0F, 0.0F, "captured matrix restores world normal");
        OpenMatrix4f yawOnly = SlashPlaneAlign.modelAlignment(
                0.0F, 0.0F, 1.0F, 0.0F, 1.0F, 0.0F, 25.0F, null, referenceForward, referenceNormal);
        Vec3f yawOnlyForward = MatrixFrames.apply(yawOnly, referenceForward.coordinateX, referenceForward.coordinateY, referenceForward.coordinateZ);
        yes(distance(modelForward, yawOnlyForward) > 0.05F, "extra pitch remains in the captured matrix");
    }

    private static void checkItemDisplayAxes() {
        Vec3f expected = displayRotation(0.0F, (float) Math.toRadians(-90.0F), (float) Math.toRadians(25.0F), 0.0F, 1.0F, 0.0F);
        Vec3f rightNeutral = FirstPersonHandBasis.displayedBlade(new OpenMatrix4f(), true);
        Vec3f leftNeutral = FirstPersonHandBasis.displayedBlade(new OpenMatrix4f(), false);
        near(rightNeutral, expected.coordinateX, expected.coordinateY, expected.coordinateZ, "neutral right display axis");
        near(leftNeutral, expected.coordinateX, expected.coordinateY, expected.coordinateZ, "neutral left display axis");
        Vec3f swung = independentZyx(0.0F, (float) Math.toRadians(45.0F), 0.0F, expected.coordinateX, expected.coordinateY, expected.coordinateZ);
        Vec3f cancelled = independentZyx(0.0F, (float) Math.toRadians(-45.0F), 0.0F, swung.coordinateX, swung.coordinateY, swung.coordinateZ);
        near(cancelled, expected.coordinateX, expected.coordinateY, expected.coordinateZ, "attack pair at swing zero cancels");
        OpenMatrix4f slash = modelPitchNegative();
        Vec3f rightActed = FirstPersonHandBasis.displayedBlade(slash, true);
        Vec3f leftActed = FirstPersonHandBasis.displayedBlade(slash, false);
        near(rightActed, expected.coordinateX, expected.coordinateZ, -expected.coordinateY, "acted right display axis");
        near(leftActed, expected.coordinateX, expected.coordinateZ, -expected.coordinateY, "acted left display axis");
    }

    private static Vec3f displayRotation(float angleX, float angleY, float angleZ, float across, float upward, float ahead) {
        Vec3f rolled = independentZyx(0.0F, 0.0F, angleZ, across, upward, ahead);
        Vec3f yawed = independentZyx(0.0F, angleY, 0.0F, rolled.coordinateX, rolled.coordinateY, rolled.coordinateZ);
        return independentZyx(angleX, 0.0F, 0.0F, yawed.coordinateX, yawed.coordinateY, yawed.coordinateZ);
    }

    private static void checkRecipeCache() {
        ResourceLocation stillId = ResourceLocation.fromNamespaceAndPath("xueximod", "probe/still_arm");
        ActionVisualCatalog.registerBlade(stillId, true, false, new float[] {0.0F, 0.5F, 1.0F}, false, "静止探针，平面应退化");
        Armature armature = originalArmature();
        AnimationClip still = holdClip();
        int before = ActionVisualCatalog.calibrationCount(stillId);
        yes(!ActionVisualCatalog.calibrated(stillId, still, armature), "still clip stays uncalibrated");
        yes(ActionVisualCatalog.calibrationCount(stillId) == before + 1, "still clip was fitted once");
        yes(!ActionVisualCatalog.calibrated(stillId, still, armature), "uncalibrated result stays cached");
        yes(ActionVisualCatalog.calibrationCount(stillId) == before + 1, "same clip does not refit");
        yes(!ActionVisualCatalog.calibrated(stillId, holdClip(), armature), "replaced clip is fitted again");
        yes(ActionVisualCatalog.calibrationCount(stillId) == before + 2, "new clip identity invalidates the cache");
        ResourceLocation movingId = ResourceLocation.fromNamespaceAndPath("xueximod", "probe/moving_arm");
        ActionVisualCatalog.registerBlade(movingId, true, false, new float[] {0.0F, 0.5F, 1.0F}, false, "摆动探针，应当得到自己的参考架");
        yes(ActionVisualCatalog.calibrated(movingId, movingClip(), armature), "moving clip calibrates");
    }

    private static AnimationClip holdClip() {
        AnimationClip clip = new AnimationClip();
        clip.addJointTransform("ArmRight", new TransformSheet(List.of(
                new Keyframe(0.0F, JointTransform.empty()),
                new Keyframe(1.0F, JointTransform.empty()))));
        clip.setClipTime(1.0F);
        return clip;
    }

    private static AnimationClip movingClip() {
        AnimationClip clip = new AnimationClip();
        Quaternionf middle = new Quaternionf().rotateX((float) Math.toRadians(70.0F));
        Quaternionf ending = new Quaternionf().rotateZ((float) Math.toRadians(55.0F));
        clip.addJointTransform("ArmRight", new TransformSheet(List.of(
                new Keyframe(0.0F, JointTransform.empty()),
                new Keyframe(0.5F, JointTransform.rotation(middle)),
                new Keyframe(1.0F, JointTransform.rotation(ending)))));
        clip.setClipTime(1.0F);
        return clip;
    }

    private static void checkWhitelistCoverage() {
        String payload;
        try {
            payload = Files.readString(Path.of("D:/alianximod/dev-authorized/epicfight/audited-whitelist.json"));
        } catch (IOException failure) {
            throw new IllegalStateException(failure);
        }
        JsonObject document = JsonParser.parseString(payload).getAsJsonObject();
        int bladeCount = 0;
        int rollCount = 0;
        for (JsonElement element : document.getAsJsonArray("entries")) {
            JsonObject entry = element.getAsJsonObject();
            String categoryName = entry.get("category").getAsString();
            String rawId = entry.get("id").getAsString();
            int colon = rawId.indexOf(':');
            ResourceLocation visualId = ResourceLocation.fromNamespaceAndPath(
                    rawId.substring(0, colon), rawId.substring(colon + 1));
            if ("roll".equals(categoryName)) {
                yes(ActionVisualCatalog.roll(visualId) && !ActionVisualCatalog.blade(visualId), rawId);
                rollCount++;
            } else if (bladeCategory(categoryName)) {
                yes(ActionVisualCatalog.blade(visualId) && !ActionVisualCatalog.roll(visualId), rawId);
                boolean recipeKept = "biped/combat/sword_auto1".equals(visualId.getPath())
                        || "biped/combat/sword_auto2".equals(visualId.getPath())
                        || "biped/combat/sword_auto3".equals(visualId.getPath());
                yes(ActionVisualCatalog.hasRecipe(visualId) == recipeKept, rawId + " recipe");
                bladeCount++;
            } else {
                throw new IllegalStateException("unexpected category " + categoryName);
            }
        }
        yes(bladeCount == 116, "authorized blade count");
        yes(rollCount == 2, "authorized roll count");
        assertPlainPose(epic("biped/combat/dagger_auto1"), true, false, "dagger_auto1");
        assertPlainPose(epic("biped/combat/sword_dual_auto3"), true, true, "sword_dual_auto3");
        assertPlainPose(epic("biped/combat/longsword_auto1"), true, false, "longsword_auto1");
        assertPlainPose(epic("biped/combat/tachi_auto1"), true, false, "tachi_auto1");
        yes(ActionVisualCatalog.roll(epic("biped/skill/roll_forward"))
                && !ActionVisualCatalog.blade(epic("biped/skill/roll_forward")), "roll forward stays out of blade");
        yes(ActionVisualCatalog.roll(epic("biped/skill/roll_backward"))
                && !ActionVisualCatalog.blade(epic("biped/skill/roll_backward")), "roll backward stays out of blade");
        ResourceLocation unknown = ResourceLocation.fromNamespaceAndPath("epicfight", "biped/combat/not_authorized");
        yes(!ActionVisualCatalog.blade(unknown) && !ActionVisualCatalog.roll(unknown), "unknown id is not a blade");
        yes(ActionVisualCatalog.hasRecipe(OriginalHumanoid.LEFT_ID), "original left recipe kept");
        yes(ActionVisualCatalog.hasRecipe(OriginalHumanoid.RIGHT_ID), "original right recipe kept");
        yes(ActionVisualCatalog.hasRecipe(OriginalHumanoid.HORIZONTAL_ID), "original horizontal recipe kept");
    }

    private static void assertPlainPose(ResourceLocation visualId, boolean rightHand, boolean leftHand, String label) {
        yes(ActionVisualCatalog.blade(visualId), label + " blade");
        yes(ActionVisualCatalog.usesHand(visualId, true) == rightHand, label + " right hand");
        yes(ActionVisualCatalog.usesHand(visualId, false) == leftHand, label + " left hand");
        yes(!ActionVisualCatalog.hasRecipe(visualId), label + " has no recipe");
        yes(ActionVisualCatalog.fractions(visualId).length == 0, label + " has no sample times");
        yes(ActionVisualCatalog.referenceForward(visualId) == null, label + " has no slash frame");
        yes(ActionVisualCatalog.calibrationCount(visualId) == 0, label + " was not fitted");
    }

    private static boolean bladeCategory(String categoryName) {
        return "sword".equals(categoryName)
                || "dagger".equals(categoryName)
                || "greatsword".equals(categoryName)
                || "uchigatana".equals(categoryName)
                || "tachi".equals(categoryName)
                || "longsword".equals(categoryName)
                || "dual_sword_shared".equals(categoryName);
    }

    private static ResourceLocation epic(String path) {
        return ResourceLocation.fromNamespaceAndPath("epicfight", path);
    }

    private static float dot(Vec3f left, Vec3f right) {
        return left.coordinateX * right.coordinateX + left.coordinateY * right.coordinateY + left.coordinateZ * right.coordinateZ;
    }

    private static float lengthOf(Vec3f source) {
        return MatrixFrames.length(source.coordinateX, source.coordinateY, source.coordinateZ);
    }

    private static float distance(Vec3f left, Vec3f right) {
        float across = left.coordinateX - right.coordinateX;
        float upward = left.coordinateY - right.coordinateY;
        float ahead = left.coordinateZ - right.coordinateZ;
        return MatrixFrames.length(across, upward, ahead);
    }

    private static void checkFallbackContinuity() {
        float quarter = (float) (Math.PI / 2.0);
        OpenMatrix4f roll = VanillaEuler.zyx(0.0F, 0.0F, quarter);
        OpenMatrix4f pitch = VanillaEuler.zyx(quarter, 0.0F, 0.0F);
        OpenMatrix4f yaw = VanillaEuler.zyx(0.0F, quarter, 0.0F);
        OpenMatrix4f identity = new OpenMatrix4f();
        RigidFallback.Sample leftHip = part(-1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        RigidFallback.Placed rolledHip = RigidFallback.place(leftHip, 0.0F, 0.0F, 0.0F, null, null, roll, false);
        near(rolledHip.across(), -12.0F, "left hip roll x");
        near(rolledHip.upward(), -1.9F, "left hip roll y");
        near(rolledHip.ahead(), 0.0F, "left hip roll z");
        yes(Math.abs(rolledHip.across() - leftHip.across()) > 1.0F, "left hip leaves its vanilla pivot");
        float leftTravel = MatrixFrames.length(rolledHip.across() + 1.9F, rolledHip.upward() - 12.0F, rolledHip.ahead());
        near(leftTravel, 17.18197F, "left hip travel");
        RigidFallback.Placed rightHip = RigidFallback.place(part(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F), 0.0F, 0.0F, 0.0F, null, null, roll, false);
        near(rightHip.across(), -12.0F, "right hip roll x");
        near(rightHip.upward(), 1.9F, "right hip roll y");
        near(rightHip.ahead(), 0.0F, "right hip roll z");
        float rightTravel = MatrixFrames.length(rightHip.across() - 1.9F, rightHip.upward() - 12.0F, rightHip.ahead());
        near(rightTravel, 17.18197F, "hip gap");
        RigidFallback.Placed shoulder = RigidFallback.place(part(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F), 0.0F, 0.0F, 0.0F, null, null, roll, false);
        near(shoulder.across(), -2.0F, "shoulder roll x");
        near(shoulder.upward(), -5.0F, "shoulder roll y");
        near(shoulder.ahead(), 0.0F, "shoulder roll z");
        float shoulderTravel = MatrixFrames.length(shoulder.across() + 5.0F, shoulder.upward() - 2.0F, shoulder.ahead());
        near(shoulderTravel, 7.61577F, "shoulder gap");
        Vec3f expectedHip = MatrixFrames.apply(roll, 1.9F, 12.0F, 0.0F);
        near(rightHip.across(), expectedHip.coordinateX, "production hip x");
        near(rightHip.upward(), expectedHip.coordinateY, "production hip y");
        near(rightHip.ahead(), expectedHip.coordinateZ, "production hip z");
        RigidFallback.Placed yawHip = RigidFallback.place(part(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F), 0.0F, 0.0F, 0.0F, null, null, yaw, false);
        near(yawHip.across(), 0.0F, "yaw hip x");
        near(yawHip.upward(), 12.0F, "yaw hip y");
        near(yawHip.ahead(), -1.9F, "yaw hip z");
        RigidFallback.Placed pitchHip = RigidFallback.place(part(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F), 0.0F, 0.0F, 0.0F, null, null, pitch, false);
        near(pitchHip.across(), 1.9F, "pitch hip x");
        near(pitchHip.upward(), 0.0F, "pitch hip y");
        near(pitchHip.ahead(), 12.0F, "pitch hip z");
        RigidFallback.Sample crouchBody = part(0.0F, 3.2F, 0.0F, 0.5F, 0.0F, 0.0F);
        RigidFallback.Placed neutralBody = RigidFallback.place(crouchBody, 0.0F, 3.2F, 0.0F, identity, identity, identity, true);
        sameSample(neutralBody, crouchBody, "neutral crouch body");
        RigidFallback.Sample crouchLeg = part(-1.9F, 12.2F, 4.0F, 0.25F, -0.1F, 0.05F);
        RigidFallback.Placed neutralLeg = RigidFallback.place(crouchLeg, 0.0F, 3.2F, 0.0F, identity, identity, identity, false);
        sameSample(neutralLeg, crouchLeg, "neutral crouch leg");
        RigidFallback.Placed rolledBody = RigidFallback.place(crouchBody, 0.0F, 3.2F, 0.0F, null, roll, roll, true);
        yes(rolledBody.across() == 0.0F && rolledBody.upward() == 3.2F && rolledBody.ahead() == 0.0F, "body pivot stays");
        RigidFallback.Sample freeArm = part(-5.0F, 2.0F, 0.0F, 0.4F, 0.0F, 0.0F);
        RigidFallback.Placed inherited = RigidFallback.place(freeArm, 0.0F, 0.0F, 0.0F, null, null, roll, false);
        near(inherited.across(), -2.0F, "free arm x");
        near(inherited.upward(), -5.0F, "free arm y");
        OpenMatrix4f inheritedSpin = VanillaEuler.zyx(inherited.pitchRadians(), inherited.yawRadians(), inherited.rollRadians());
        OpenMatrix4f once = MatrixFrames.product(roll, VanillaEuler.zyx(0.4F, 0.0F, 0.0F));
        sameSpin(inheritedSpin, once, "free arm inherits trunk");
        Vec3f stayed = MatrixFrames.apply(VanillaEuler.zyx(0.4F, 0.0F, 0.0F), 0.0F, 1.0F, 0.0F);
        Vec3f moved = MatrixFrames.apply(inheritedSpin, 0.0F, 1.0F, 0.0F);
        yes(distance(stayed, moved) > 0.2F, "free arm does not keep the vanilla angles");
        RigidFallback.Placed drivenArm = RigidFallback.place(part(-5.0F, 2.0F, 0.0F, 0.0F, 0.0F, 0.0F), 0.0F, 0.0F, 0.0F, null, pitch, roll, false);
        near(drivenArm.across(), -2.0F, "driven arm still orbits with the trunk");
        OpenMatrix4f drivenSpin = VanillaEuler.zyx(drivenArm.pitchRadians(), drivenArm.yawRadians(), drivenArm.rollRadians());
        sameSpin(drivenSpin, pitch, "driven delta is not multiplied by the body again");
        OpenMatrix4f doubled = MatrixFrames.product(roll, pitch);
        yes(distance(MatrixFrames.apply(drivenSpin, 1.0F, 0.0F, 0.0F), MatrixFrames.apply(doubled, 1.0F, 0.0F, 0.0F)) > 0.2F,
                "body delta is not applied twice");
        OpenMatrix4f modelArm = VanillaEuler.zyx(0.4F, 0.0F, 0.0F);
        RetargetedPose local = new RetargetedPose(null, null, modelArm, null, null, null, null);
        RetargetedPose aimed = RigidPoseRetarget.aim(local, yaw);
        yes(aimed.rightArm() == modelArm, "first person arm matrix is still the local delta");
        OpenMatrix4f heldLocal = FirstPersonHandBasis.heldRotation(modelArm, yaw);
        OpenMatrix4f heldAimed = FirstPersonHandBasis.heldRotation(aimed.rightArm(), yaw);
        sameSpin(heldLocal, heldAimed, "first person ignores the third person pivot path");
        OpenMatrix4f mildYaw = VanillaEuler.zyx(0.0F, 0.7F, 0.0F);
        OpenMatrix4f mildPitch = VanillaEuler.zyx(0.5F, 0.0F, 0.0F);
        RigidFallback.Sample alignedArm = part(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        OpenMatrix4f trunk = MatrixFrames.product(mildYaw, identity);
        RigidFallback.Placed aligned = RigidFallback.place(alignedArm, 0.0F, 0.0F, 0.0F, mildYaw, mildPitch, trunk, false);
        OpenMatrix4f alignedSpin = VanillaEuler.zyx(aligned.pitchRadians(), aligned.yawRadians(), aligned.rollRadians());
        OpenMatrix4f onceAlignment = MatrixFrames.product(mildYaw, mildPitch);
        OpenMatrix4f twiceAlignment = MatrixFrames.product(mildYaw, onceAlignment);
        sameSpin(alignedSpin, onceAlignment, "alignment is applied once");
        yes(distance(MatrixFrames.apply(alignedSpin, 0.0F, 1.0F, 0.0F), MatrixFrames.apply(twiceAlignment, 0.0F, 1.0F, 0.0F)) > 0.2F,
                "alignment is not applied twice");
        yes(!new RetargetedPose(null, null, null, null, null, null, yaw).anyDriven(), "alignment alone does not drive");
        yes(!ThirdPersonPoseWriter.drives(RetargetedPose.empty()), "inactive pose does not write");
        RigidFallback.Placed firstCrouch = RigidFallback.place(crouchLeg, 0.0F, 3.2F, 0.0F, identity, identity, identity, false);
        RigidFallback.Placed otherHip = RigidFallback.place(part(1.9F, 12.0F, 0.0F, 0.0F, 0.0F, 0.0F), 0.0F, 0.0F, 0.0F, null, null, roll, false);
        RigidFallback.Placed secondCrouch = RigidFallback.place(crouchLeg, 0.0F, 3.2F, 0.0F, identity, identity, identity, false);
        sameSample(firstCrouch, crouchLeg, "first player crouch");
        sameSample(secondCrouch, crouchLeg, "second player crouch");
        near(otherHip.across(), -12.0F, "other player hip x");
        yes(firstCrouch.upward() == secondCrouch.upward() && firstCrouch.ahead() == 4.0F, "players do not share pivots");
    }

    private static void checkSwingOwnership() {
        OpenMatrix4f driven = new OpenMatrix4f();
        float stored = 0.37F;
        RetargetedPose rightOwned = new RetargetedPose(driven, null, driven, null, null, null, null);
        RetargetedPose leftOwned = new RetargetedPose(driven, null, null, driven, null, null, null);
        RetargetedPose bothOwned = new RetargetedPose(driven, null, driven, driven, null, null, null);
        RetargetedPose usingRight = bothOwned.withoutRightArm();
        yes(kept(true, true, rightOwned, InteractionHand.MAIN_HAND, HumanoidArm.RIGHT, stored) == 0.0F, "active right main suppresses");
        yes(stored == 0.37F, "right suppress leaves the stored field");
        yes(kept(true, true, leftOwned, InteractionHand.OFF_HAND, HumanoidArm.RIGHT, stored) == 0.0F, "active left offhand suppresses");
        yes(kept(true, true, leftOwned, InteractionHand.MAIN_HAND, HumanoidArm.LEFT, stored) == 0.0F, "left main arm suppresses");
        yes(kept(true, true, rightOwned, InteractionHand.MAIN_HAND, HumanoidArm.LEFT, stored) == stored, "opposite main is not the owned arm");
        yes(kept(true, true, bothOwned, InteractionHand.MAIN_HAND, HumanoidArm.RIGHT, stored) == 0.0F, "both hands suppress main");
        yes(kept(true, true, bothOwned, InteractionHand.OFF_HAND, HumanoidArm.RIGHT, stored) == 0.0F, "both hands suppress offhand");
        yes(kept(true, true, rightOwned, InteractionHand.OFF_HAND, HumanoidArm.RIGHT, stored) == stored, "unowned offhand keeps the swing");
        yes(kept(true, true, usingRight, InteractionHand.MAIN_HAND, HumanoidArm.RIGHT, stored) == stored, "use item clears the owned arm");
        yes(kept(true, true, usingRight, InteractionHand.OFF_HAND, HumanoidArm.RIGHT, stored) == 0.0F, "the other hand stays owned");
        yes(kept(true, true, RetargetedPose.empty(), InteractionHand.MAIN_HAND, HumanoidArm.RIGHT, stored) == stored, "inactive keeps the swing");
        yes(kept(false, true, bothOwned, InteractionHand.MAIN_HAND, HumanoidArm.RIGHT, stored) == stored, "non player model keeps the swing");
        yes(kept(true, false, bothOwned, InteractionHand.MAIN_HAND, HumanoidArm.RIGHT, stored) == stored, "non player entity keeps the swing");
        yes(kept(true, true, bothOwned, null, HumanoidArm.RIGHT, stored) == stored, "missing swing hand keeps the swing");
        yes(stored == 0.37F, "unsuppressed calls keep the original bits");
    }

    private static void checkPoseScopeWiring() {
        yes(RenderPoseScope.depth() == 0, "scope starts empty");
        PlayerModel<?> selfModel = playerModel();
        PlayerModel<?> otherModel = playerModel();
        Object self = new Object();
        Object other = new Object();
        OpenMatrix4f roll = VanillaEuler.zyx(0.0F, 0.0F, (float) (Math.PI / 2.0));
        RetargetedPose rolled = new RetargetedPose(roll, null, null, null, null, null, null);
        parkLeg(selfModel);
        yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "no scope keeps the vanilla pivot");
        sameLeg(selfModel, "unscoped");
        RenderPoseScope.runThirdPerson(selfModel, self, () -> {
            yes(ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "third person setup writes the pivot");
        });
        near(selfModel.leftLeg.x, -12.0F, "scoped hip x");
        near(selfModel.leftLeg.y, 1.9F, "scoped hip y");
        near(selfModel.leftLeg.z, 0.0F, "scoped hip z");
        yes(selfModel.body.x == 0.0F && selfModel.body.y == 0.0F && selfModel.body.z == 0.0F, "body pivot stays");
        yes(RenderPoseScope.depth() == 0, "third person scope closed");
        parkLeg(selfModel);
        RenderPoseScope.runFirstPerson(selfModel, self, () -> {
            yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "hand setup does not write third person pivots");
        });
        sameLeg(selfModel, "hand");
        parkLeg(otherModel);
        RenderPoseScope.runThirdPerson(otherModel, other, () -> {
            yes(ThirdPersonPoseWriter.applyScoped(otherModel, other, rolled), "another player still uses third person");
            yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "the viewer is not the rendered player");
        });
        near(otherModel.leftLeg.x, -12.0F, "other player hip x");
        near(otherModel.leftLeg.y, 1.9F, "other player hip y");
        sameLeg(selfModel, "viewer");
        parkLeg(selfModel);
        parkLeg(otherModel);
        RenderPoseScope.runThirdPerson(selfModel, self, () -> {
            RenderPoseScope.runFirstPerson(selfModel, self, () -> {
                yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "hand nested in third person does not write");
            });
            yes(ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "third person resumes after the hand");
        });
        near(selfModel.leftLeg.x, -12.0F, "resumed hip x");
        parkLeg(selfModel);
        parkLeg(otherModel);
        RenderPoseScope.runFirstPerson(selfModel, self, () -> {
            yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "outer hand does not write");
            RenderPoseScope.runThirdPerson(otherModel, other, () -> {
                yes(ThirdPersonPoseWriter.applyScoped(otherModel, other, rolled), "nested third person writes the other player");
                yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "nested third person leaves the hand model");
            });
            yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "hand scope remains after the nested player");
        });
        near(otherModel.leftLeg.x, -12.0F, "nested other hip x");
        sameLeg(selfModel, "nested hand");
        yes(RenderPoseScope.depth() == 0, "nested scopes closed");
        boolean escaped = false;
        try {
            RenderPoseScope.runThirdPerson(selfModel, self, () -> {
                throw new IllegalStateException("render failed");
            });
        } catch (IllegalStateException failure) {
            escaped = "render failed".equals(failure.getMessage());
        }
        yes(escaped, "render failure escapes the scope");
        yes(RenderPoseScope.depth() == 0, "failed render leaves no scope");
        yes(!ThirdPersonPoseWriter.applyScoped(selfModel, self, rolled), "the next setup without a scope does not write");
        parkLeg(otherModel);
        RenderPoseScope.runThirdPerson(otherModel, other, () -> {
            yes(ThirdPersonPoseWriter.applyScoped(otherModel, other, rolled), "the next player writes after the failure");
        });
        yes(RenderPoseScope.depth() == 0, "next player scope closed");
        parkLeg(selfModel);
        parkLeg(otherModel);
        RenderPoseScope.runThirdPerson(selfModel, self, () -> {
            yes(!ThirdPersonPoseWriter.applyScoped(otherModel, self, rolled), "a different model is not this call");
            yes(!ThirdPersonPoseWriter.applyScoped(selfModel, other, rolled), "a different player is not this call");
        });
        sameLeg(selfModel, "mismatched");
        sameLeg(otherModel, "mismatched other");
    }

    /**
     * 上身挥斩：旋转中心是腰点，双腿完全不写，躯干和头只走 T，持剑臂走 D。
     * 原创五骨骨架没有武器骨，L 会把直臂拧向躯干，所以这条路径忽略对齐。
     * 期望值按腰点 = 当帧 body 枢轴（测试设成非零蹲伏值） + B_body*(0,12,0) 手算，不调用写入器回推。
     */
    private static void checkUpperBodySlash() {
        PlayerModel<?> model = playerModel();
        Object player = new Object();
        OpenMatrix4f trunk = VanillaEuler.zyx(0.3F, 0.4F, 0.0F);
        OpenMatrix4f armDelta = VanillaEuler.zyx(0.15F, -0.25F, 0.35F);
        OpenMatrix4f firstAlignment = VanillaEuler.zyx(0.0F, 0.5F, 0.0F);
        OpenMatrix4f secondAlignment = VanillaEuler.zyx(0.0F, 0.0F, 0.5F);
        OpenMatrix4f bodyBasis = VanillaEuler.zyx(BASE_BODY_PITCH, BASE_BODY_YAW, BASE_BODY_ROLL);
        OpenMatrix4f headBasis = VanillaEuler.zyx(BASE_HEAD_PITCH, BASE_HEAD_YAW, BASE_HEAD_ROLL);
        OpenMatrix4f armBasis = VanillaEuler.zyx(BASE_ARM_PITCH, BASE_ARM_YAW, BASE_ARM_ROLL);
        OpenMatrix4f leftArmBasis = VanillaEuler.zyx(BASE_LEFT_ARM_PITCH, BASE_LEFT_ARM_YAW, BASE_LEFT_ARM_ROLL);
        // 腰点 = 原版 body 枢轴 + B_body*(0,12,0)，枢轴用 baseSlashModel 设的蹲伏值。
        Vec3f waistOffset = MatrixFrames.apply(bodyBasis, 0.0F, 12.0F, 0.0F);
        Vec3f waist = new Vec3f(
                SLASH_BODY_PIVOT_X + waistOffset.coordinateX,
                SLASH_BODY_PIVOT_Y + waistOffset.coordinateY,
                SLASH_BODY_PIVOT_Z + waistOffset.coordinateZ);

        baseSlashModel(model);
        float[] leftLegBefore = snapshotPart(model.leftLeg);
        float[] rightLegBefore = snapshotPart(model.rightLeg);
        float[] bodyBefore = snapshotPart(model.body);
        applyUpperBodySlash(model, player, trunk, armDelta, firstAlignment);
        sameSnapshot(snapshotPart(model.leftLeg), leftLegBefore, "upper body leaves the left leg");
        sameSnapshot(snapshotPart(model.rightLeg), rightLegBefore, "upper body leaves the right leg");

        float[] bodyFirst = snapshotPart(model.body);
        float[] headFirst = snapshotPart(model.head);
        float[] rightArmFirst = snapshotPart(model.rightArm);
        float[] leftArmFirst = snapshotPart(model.leftArm);
        OpenMatrix4f bodySpin = VanillaEuler.zyx(bodyFirst[3], bodyFirst[4], bodyFirst[5]);
        OpenMatrix4f headSpin = VanillaEuler.zyx(headFirst[3], headFirst[4], headFirst[5]);
        OpenMatrix4f rightArmSpin = VanillaEuler.zyx(rightArmFirst[3], rightArmFirst[4], rightArmFirst[5]);
        OpenMatrix4f leftArmSpin = VanillaEuler.zyx(leftArmFirst[3], leftArmFirst[4], leftArmFirst[5]);

        sameSpin(bodySpin, MatrixFrames.product(trunk, bodyBasis), "upper body trunk spin");
        sameSpin(headSpin, MatrixFrames.product(trunk, headBasis), "upper body head spin");
        sameSpin(rightArmSpin, MatrixFrames.product(armDelta, armBasis), "upper body sword arm spin");
        sameSpin(leftArmSpin, MatrixFrames.product(trunk, leftArmBasis), "upper body free arm spin");

        near(positionOf(bodyFirst), orbitPoint(waist, trunk,
                SLASH_BODY_PIVOT_X, SLASH_BODY_PIVOT_Y, SLASH_BODY_PIVOT_Z), "upper body trunk pivot");
        Vec3f waistWithoutPivot = new Vec3f(
                waistOffset.coordinateX, waistOffset.coordinateY, waistOffset.coordinateZ);
        Vec3f bodyWhenPivotDropped = orbitPoint(waistWithoutPivot, trunk,
                SLASH_BODY_PIVOT_X, SLASH_BODY_PIVOT_Y, SLASH_BODY_PIVOT_Z);
        yes(distance(positionOf(bodyFirst), bodyWhenPivotDropped) > 0.5F,
                "upper body trunk needs the body pivot in the waist point");
        near(positionOf(headFirst), orbitPoint(waist, trunk, 0.0F, 0.0F, 0.0F), "upper body head orbits the waist");
        near(positionOf(rightArmFirst), orbitPoint(waist, trunk, -5.0F, 2.0F, 0.0F),
                "upper body sword arm orbits the waist");
        near(positionOf(leftArmFirst), orbitPoint(waist, trunk, 5.0F, 2.0F, 0.0F),
                "upper body free arm orbits the waist");
        yes(distance(positionOf(bodyBefore), positionOf(bodyFirst)) > 1.0F, "upper body trunk leaves the neck point");

        OpenMatrix4f alignedBody = MatrixFrames.product(firstAlignment, MatrixFrames.product(trunk, bodyBasis));
        OpenMatrix4f alignedHead = MatrixFrames.product(firstAlignment, MatrixFrames.product(trunk, headBasis));
        OpenMatrix4f alignedFreeArm = MatrixFrames.product(firstAlignment, MatrixFrames.product(trunk, leftArmBasis));
        OpenMatrix4f alignedSwordArm = MatrixFrames.product(firstAlignment,
                MatrixFrames.product(armDelta, armBasis));
        yes(axisDistance(bodySpin, alignedBody) > 0.2F, "upper body trunk leaves the alignment out");
        yes(axisDistance(headSpin, alignedHead) > 0.2F, "upper body head leaves the alignment out");
        yes(axisDistance(leftArmSpin, alignedFreeArm) > 0.2F, "upper body free arm leaves the alignment out");
        yes(axisDistance(rightArmSpin, alignedSwordArm) > 0.1F, "upper body sword arm ignores the alignment");

        baseSlashModel(model);
        float[] leftLegSecondBefore = snapshotPart(model.leftLeg);
        float[] rightLegSecondBefore = snapshotPart(model.rightLeg);
        applyUpperBodySlash(model, player, trunk, armDelta, secondAlignment);
        sameSnapshot(snapshotPart(model.leftLeg), leftLegSecondBefore, "second alignment leaves the left leg");
        sameSnapshot(snapshotPart(model.rightLeg), rightLegSecondBefore, "second alignment leaves the right leg");
        sameSnapshot(snapshotPart(model.body), bodyFirst, "second alignment does not move the trunk");
        sameSnapshot(snapshotPart(model.head), headFirst, "second alignment does not move the head");
        sameSnapshot(snapshotPart(model.leftArm), leftArmFirst, "second alignment does not move the free arm");
        sameSnapshot(snapshotPart(model.rightArm), rightArmFirst, "second alignment does not move the sword arm");

        baseSlashModel(model);
        float[] legacyLeftLeg = snapshotPart(model.leftLeg);
        float[] legacyRightLeg = snapshotPart(model.rightLeg);
        RetargetedPose legacy = new RetargetedPose(trunk, trunk, armDelta, null, null, null,
                firstAlignment, SlashAlignmentScope.LEGACY);
        RenderPoseScope.runThirdPerson(model, player, () -> {
            yes(ThirdPersonPoseWriter.applyScoped(model, player, legacy), "legacy scope still writes");
        });
        differentSnapshot(snapshotPart(model.leftLeg), legacyLeftLeg, "legacy scope still moves the left leg");
        differentSnapshot(snapshotPart(model.rightLeg), legacyRightLeg, "legacy scope still moves the right leg");
    }

    /** 上身挥斩的基准站姿。腿给非零角度，好让「双腿完全不变」不是空断言。 */
    private static void baseSlashModel(PlayerModel<?> model) {
        model.body.x = SLASH_BODY_PIVOT_X;
        model.body.y = SLASH_BODY_PIVOT_Y;
        model.body.z = SLASH_BODY_PIVOT_Z;
        model.body.xRot = BASE_BODY_PITCH;
        model.body.yRot = BASE_BODY_YAW;
        model.body.zRot = BASE_BODY_ROLL;
        model.head.x = 0.0F;
        model.head.y = 0.0F;
        model.head.z = 0.0F;
        model.head.xRot = BASE_HEAD_PITCH;
        model.head.yRot = BASE_HEAD_YAW;
        model.head.zRot = BASE_HEAD_ROLL;
        model.rightArm.x = -5.0F;
        model.rightArm.y = 2.0F;
        model.rightArm.z = 0.0F;
        model.rightArm.xRot = BASE_ARM_PITCH;
        model.rightArm.yRot = BASE_ARM_YAW;
        model.rightArm.zRot = BASE_ARM_ROLL;
        model.leftArm.x = 5.0F;
        model.leftArm.y = 2.0F;
        model.leftArm.z = 0.0F;
        model.leftArm.xRot = BASE_LEFT_ARM_PITCH;
        model.leftArm.yRot = BASE_LEFT_ARM_YAW;
        model.leftArm.zRot = BASE_LEFT_ARM_ROLL;
        model.rightLeg.x = -1.9F;
        model.rightLeg.y = 12.0F;
        model.rightLeg.z = 0.0F;
        model.rightLeg.xRot = BASE_LEG_PITCH;
        model.rightLeg.yRot = BASE_LEG_YAW;
        model.rightLeg.zRot = BASE_LEG_ROLL;
        model.leftLeg.x = 1.9F;
        model.leftLeg.y = 12.0F;
        model.leftLeg.z = 0.0F;
        model.leftLeg.xRot = -BASE_LEG_PITCH;
        model.leftLeg.yRot = -BASE_LEG_YAW;
        model.leftLeg.zRot = -BASE_LEG_ROLL;
    }

    private static void applyUpperBodySlash(
            PlayerModel<?> model,
            Object player,
            OpenMatrix4f trunk,
            OpenMatrix4f armDelta,
            OpenMatrix4f alignment) {
        RetargetedPose pose = new RetargetedPose(trunk, trunk, armDelta, null, null, null,
                alignment, SlashAlignmentScope.UPPER_BODY_SLASH);
        RenderPoseScope.runThirdPerson(model, player, () -> {
            yes(ThirdPersonPoseWriter.applyScoped(model, player, pose), "upper body slash writes");
        });
    }

    private static float[] snapshotPart(ModelPart part) {
        return new float[] {part.x, part.y, part.z, part.xRot, part.yRot, part.zRot};
    }

    private static Vec3f positionOf(float[] snapshot) {
        return new Vec3f(snapshot[0], snapshot[1], snapshot[2]);
    }

    private static Vec3f orbitPoint(Vec3f center, OpenMatrix4f spin, float across, float upward, float ahead) {
        Vec3f turned = MatrixFrames.apply(spin,
                across - center.coordinateX,
                upward - center.coordinateY,
                ahead - center.coordinateZ);
        return new Vec3f(
                center.coordinateX + turned.coordinateX,
                center.coordinateY + turned.coordinateY,
                center.coordinateZ + turned.coordinateZ);
    }

    private static float axisDistance(OpenMatrix4f actual, OpenMatrix4f expected) {
        return distance(MatrixFrames.apply(actual, 1.0F, 0.0F, 0.0F),
                MatrixFrames.apply(expected, 1.0F, 0.0F, 0.0F));
    }

    private static void sameSnapshot(float[] actual, float[] expected, String label) {
        near(actual[0], expected[0], label + " x");
        near(actual[1], expected[1], label + " y");
        near(actual[2], expected[2], label + " z");
        near(actual[3], expected[3], label + " pitch");
        near(actual[4], expected[4], label + " yaw");
        near(actual[5], expected[5], label + " roll");
    }

    private static void differentSnapshot(float[] actual, float[] expected, String label) {
        float extent = 0.0F;
        for (int index = 0; index < expected.length; index++) {
            extent += Math.abs(actual[index] - expected[index]);
        }
        yes(extent > 0.05F, label);
    }

    private static PlayerModel<?> playerModel() {
        ModelPart root = PlayerModel.createMesh(CubeDeformation.NONE, false).getRoot().bake(64, 64);
        return new PlayerModel<>(root, false);
    }

    private static void parkLeg(PlayerModel<?> model) {
        model.body.x = 0.0F;
        model.body.y = 0.0F;
        model.body.z = 0.0F;
        model.body.xRot = 0.0F;
        model.body.yRot = 0.0F;
        model.body.zRot = 0.0F;
        model.leftLeg.x = 1.9F;
        model.leftLeg.y = 12.0F;
        model.leftLeg.z = 0.0F;
        model.leftLeg.xRot = 0.0F;
        model.leftLeg.yRot = 0.0F;
        model.leftLeg.zRot = 0.0F;
    }

    private static void sameLeg(PlayerModel<?> model, String label) {
        yes(model.leftLeg.x == 1.9F, label + " leg x");
        yes(model.leftLeg.y == 12.0F, label + " leg y");
        yes(model.leftLeg.z == 0.0F, label + " leg z");
    }

    private static float kept(
            boolean playerModel,
            boolean playerEntity,
            RetargetedPose pose,
            InteractionHand swingingHand,
            HumanoidArm mainArm,
            float actual) {
        return ActionRenderQueries.keepAttackTime(playerModel, playerEntity, pose, swingingHand, mainArm, actual);
    }

    private static RigidFallback.Sample part(
            float across,
            float upward,
            float ahead,
            float pitchRadians,
            float yawRadians,
            float rollRadians) {
        return new RigidFallback.Sample(across, upward, ahead, pitchRadians, yawRadians, rollRadians);
    }

    private static void sameSample(RigidFallback.Placed placed, RigidFallback.Sample vanilla, String label) {
        yes(placed.across() == vanilla.across(), label + " x");
        yes(placed.upward() == vanilla.upward(), label + " y");
        yes(placed.ahead() == vanilla.ahead(), label + " z");
        yes(placed.pitchRadians() == vanilla.pitchRadians(), label + " pitch");
        yes(placed.yawRadians() == vanilla.yawRadians(), label + " yaw");
        yes(placed.rollRadians() == vanilla.rollRadians(), label + " roll");
    }

    private static void sameSpin(OpenMatrix4f actual, OpenMatrix4f expected, String label) {
        near(MatrixFrames.apply(actual, 1.0F, 0.0F, 0.0F), MatrixFrames.apply(expected, 1.0F, 0.0F, 0.0F), label + " x");
        near(MatrixFrames.apply(actual, 0.0F, 1.0F, 0.0F), MatrixFrames.apply(expected, 0.0F, 1.0F, 0.0F), label + " y");
        near(MatrixFrames.apply(actual, 0.0F, 0.0F, 1.0F), MatrixFrames.apply(expected, 0.0F, 0.0F, 1.0F), label + " z");
    }

    private static void near(Vec3f actual, Vec3f expected, String label) {
        near(actual, expected.coordinateX, expected.coordinateY, expected.coordinateZ, label);
    }

    private static void near(Vec3f actual, float across, float upward, float ahead, String label) {
        near(actual.coordinateX, across, label + " x");
        near(actual.coordinateY, upward, label + " y");
        near(actual.coordinateZ, ahead, label + " z");
    }

    private static void near(float actual, float expected, String label) {
        checks++;
        if (!Float.isFinite(actual) || Math.abs(actual - expected) > TOLERANCE) {
            throw new IllegalStateException(label + " expected " + expected + " but was " + actual);
        }
    }

    private static void yes(boolean condition, String label) {
        checks++;
        if (!condition) {
            throw new IllegalStateException(label);
        }
    }
}

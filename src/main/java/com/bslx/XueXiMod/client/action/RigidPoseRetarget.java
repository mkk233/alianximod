package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.Joint;
import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;

/**
 * 把一次已采样的模型空间姿态收成六个全局旋转增量 D。
 * 只读 applyOriginTransform=false 的骨骼矩阵，不把蒙皮逆绑定当骨位置。
 * D 已含父骨，写入时不再乘躯干。挥斩对齐 L 只放在 worldBasis。
 * 肩髋是否跟着躯干移动由写入端按当帧枢轴处理。
 */
public final class RigidPoseRetarget {
    private RigidPoseRetarget() {
    }

    public static RetargetedPose adapt(Armature armature, Pose pose, RetargetProfile profile) {
        if (armature == null || pose == null || profile == null) {
            return RetargetedPose.empty();
        }
        OpenMatrix4f[] current = armature.getPoseAsTransformMatrix(pose, false);
        OpenMatrix4f[] bind = armature.getPoseAsTransformMatrix(new Pose(), false);
        OpenMatrix4f body = bodyRotation(armature, current, bind, pose, profile);
        OpenMatrix4f head = boneRotation(armature, current, bind, pose, profile.axisFlip(), profile.headBone());
        OpenMatrix4f rightLeg = boneRotation(armature, current, bind, pose, profile.axisFlip(), profile.rightLegBone());
        OpenMatrix4f leftLeg = boneRotation(armature, current, bind, pose, profile.axisFlip(), profile.leftLegBone());
        OpenMatrix4f rightArm = armRotation(armature, current, bind, pose, profile.axisFlip(),
                profile.rightArmBone(), profile.rightHandBone(), profile.rightToolBone());
        OpenMatrix4f leftArm = armRotation(armature, current, bind, pose, profile.axisFlip(),
                profile.leftArmBone(), profile.leftHandBone(), profile.leftToolBone());
        return new RetargetedPose(body, head, rightArm, leftArm, rightLeg, leftLeg, null);
    }

    /**
     * 只记下挥斩对齐 L，部件里的 D 保持原样。
     * L 只供第一人称和 LEGACY 路径使用；UPPER_BODY_SLASH 写入时会忽略 worldBasis。
     */
    public static RetargetedPose aim(RetargetedPose pose, OpenMatrix4f alignment) {
        if (pose == null) {
            return RetargetedPose.empty();
        }
        if (alignment == null) {
            return pose;
        }
        return new RetargetedPose(
                pose.body(),
                pose.head(),
                pose.rightArm(),
                pose.leftArm(),
                pose.rightLeg(),
                pose.leftLeg(),
                alignment,
                pose.alignmentScope());
    }

    public static OpenMatrix4f targetRotation(OpenMatrix4f axisFlip, OpenMatrix4f current, OpenMatrix4f bind) {
        return MatrixFrames.conjugate(axisFlip, MatrixFrames.relative(current, bind));
    }

    private static OpenMatrix4f bodyRotation(
            Armature armature,
            OpenMatrix4f[] current,
            OpenMatrix4f[] bind,
            Pose pose,
            RetargetProfile profile) {
        if (!present(pose, profile.bodyBone()) && !present(pose, profile.bodyParentBone())) {
            return null;
        }
        String readBone = armature.hasJoint(profile.bodyBone()) ? profile.bodyBone() : profile.bodyParentBone();
        OpenMatrix4f currentMatrix = jointMatrix(armature, current, readBone);
        OpenMatrix4f bindMatrix = jointMatrix(armature, bind, readBone);
        if (currentMatrix == null || bindMatrix == null) {
            return null;
        }
        return targetRotation(profile.axisFlip(), currentMatrix, bindMatrix);
    }

    private static OpenMatrix4f boneRotation(
            Armature armature,
            OpenMatrix4f[] current,
            OpenMatrix4f[] bind,
            Pose pose,
            OpenMatrix4f axisFlip,
            String boneName) {
        if (!present(pose, boneName) || !armature.hasJoint(boneName)) {
            return null;
        }
        OpenMatrix4f currentMatrix = jointMatrix(armature, current, boneName);
        OpenMatrix4f bindMatrix = jointMatrix(armature, bind, boneName);
        if (currentMatrix == null || bindMatrix == null) {
            return null;
        }
        return targetRotation(axisFlip, currentMatrix, bindMatrix);
    }

    private static OpenMatrix4f armRotation(
            Armature armature,
            OpenMatrix4f[] current,
            OpenMatrix4f[] bind,
            Pose pose,
            OpenMatrix4f axisFlip,
            String armBone,
            String handBone,
            String toolBone) {
        if (!present(pose, armBone) && !present(pose, handBone) && !present(pose, toolBone)) {
            return null;
        }
        OpenMatrix4f currentFrame = weaponFrame(armature, current, handBone, toolBone);
        OpenMatrix4f bindFrame = weaponFrame(armature, bind, handBone, toolBone);
        if (currentFrame != null && bindFrame != null) {
            return targetRotation(axisFlip, currentFrame, bindFrame);
        }
        OpenMatrix4f currentMatrix = jointMatrix(armature, current, armBone);
        OpenMatrix4f bindMatrix = jointMatrix(armature, bind, armBone);
        if (currentMatrix == null || bindMatrix == null) {
            return null;
        }
        return targetRotation(axisFlip, currentMatrix, bindMatrix);
    }

    private static OpenMatrix4f weaponFrame(Armature armature, OpenMatrix4f[] matrices, String handBone, String toolBone) {
        OpenMatrix4f hand = jointMatrix(armature, matrices, handBone);
        OpenMatrix4f tool = jointMatrix(armature, matrices, toolBone);
        if (hand == null || tool == null) {
            return null;
        }
        float bladeAcross = tool.m30 - hand.m30;
        float bladeUpward = tool.m31 - hand.m31;
        float bladeAhead = tool.m32 - hand.m32;
        float bladeLength = MatrixFrames.length(bladeAcross, bladeUpward, bladeAhead);
        if (!(bladeLength > 1.0E-5F)) {
            return null;
        }
        bladeAcross /= bladeLength;
        bladeUpward /= bladeLength;
        bladeAhead /= bladeLength;
        OpenMatrix4f handRotation = MatrixFrames.pureRotation(hand);
        float twistAcross = handRotation.m00;
        float twistUpward = handRotation.m01;
        float twistAhead = handRotation.m02;
        float alongBlade = twistAcross * bladeAcross + twistUpward * bladeUpward + twistAhead * bladeAhead;
        twistAcross -= alongBlade * bladeAcross;
        twistUpward -= alongBlade * bladeUpward;
        twistAhead -= alongBlade * bladeAhead;
        float twistLength = MatrixFrames.length(twistAcross, twistUpward, twistAhead);
        if (!(twistLength > 1.0E-5F)) {
            twistAcross = handRotation.m20;
            twistUpward = handRotation.m21;
            twistAhead = handRotation.m22;
            alongBlade = twistAcross * bladeAcross + twistUpward * bladeUpward + twistAhead * bladeAhead;
            twistAcross -= alongBlade * bladeAcross;
            twistUpward -= alongBlade * bladeUpward;
            twistAhead -= alongBlade * bladeAhead;
            twistLength = MatrixFrames.length(twistAcross, twistUpward, twistAhead);
        }
        if (!(twistLength > 1.0E-5F)) {
            return null;
        }
        twistAcross /= twistLength;
        twistUpward /= twistLength;
        twistAhead /= twistLength;
        float thirdAcross = twistUpward * bladeAhead - twistAhead * bladeUpward;
        float thirdUpward = twistAhead * bladeAcross - twistAcross * bladeAhead;
        float thirdAhead = twistAcross * bladeUpward - twistUpward * bladeAcross;
        OpenMatrix4f frame = new OpenMatrix4f();
        frame.m00 = twistAcross;
        frame.m01 = twistUpward;
        frame.m02 = twistAhead;
        frame.m10 = bladeAcross;
        frame.m11 = bladeUpward;
        frame.m12 = bladeAhead;
        frame.m20 = thirdAcross;
        frame.m21 = thirdUpward;
        frame.m22 = thirdAhead;
        return frame;
    }

    private static OpenMatrix4f jointMatrix(Armature armature, OpenMatrix4f[] matrices, String boneName) {
        if (boneName == null || matrices == null || !armature.hasJoint(boneName)) {
            return null;
        }
        Joint joint = armature.searchJointByName(boneName);
        if (joint == null) {
            return null;
        }
        int jointId = joint.getId();
        if (jointId < 0 || jointId >= matrices.length) {
            return null;
        }
        return matrices[jointId];
    }

    private static boolean present(Pose pose, String boneName) {
        return boneName != null && pose.hasTransform(boneName);
    }
}

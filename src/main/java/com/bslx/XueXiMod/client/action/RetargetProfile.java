package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;
import com.bslx.XueXiMod.animation.core.model.Armature;
import com.bslx.XueXiMod.animation.runtime.OriginalHumanoid;

/**
 * 源模型和原创骨架各自的轴约定。
 * 源空间已经过 loader，这里只用 Q=diag(-1,-1,1)，不再乘一次 C。
 * 原创骨架按自己的右手=+X、上=+Y 单独声明，不抄授权 bind。
 */
public final class RetargetProfile {
    private final OpenMatrix4f axisFlip;
    private final String bodyBone;
    private final String bodyParentBone;
    private final String headBone;
    private final String rightLegBone;
    private final String leftLegBone;
    private final String rightArmBone;
    private final String rightHandBone;
    private final String rightToolBone;
    private final String leftArmBone;
    private final String leftHandBone;
    private final String leftToolBone;

    private RetargetProfile(
            OpenMatrix4f axisFlip,
            String bodyBone,
            String bodyParentBone,
            String headBone,
            String rightLegBone,
            String leftLegBone,
            String rightArmBone,
            String rightHandBone,
            String rightToolBone,
            String leftArmBone,
            String leftHandBone,
            String leftToolBone) {
        this.axisFlip = axisFlip;
        this.bodyBone = bodyBone;
        this.bodyParentBone = bodyParentBone;
        this.headBone = headBone;
        this.rightLegBone = rightLegBone;
        this.leftLegBone = leftLegBone;
        this.rightArmBone = rightArmBone;
        this.rightHandBone = rightHandBone;
        this.rightToolBone = rightToolBone;
        this.leftArmBone = leftArmBone;
        this.leftHandBone = leftHandBone;
        this.leftToolBone = leftToolBone;
    }

    public static RetargetProfile sourceBiped() {
        return new RetargetProfile(
                MatrixFrames.axisFlip(-1.0F, -1.0F, 1.0F),
                "Chest",
                "Torso",
                "Head",
                "Thigh_R",
                "Thigh_L",
                "Arm_R",
                "Hand_R",
                "Tool_R",
                "Arm_L",
                "Hand_L",
                "Tool_L");
    }

    public static RetargetProfile originalHumanoid() {
        return new RetargetProfile(
                MatrixFrames.axisFlip(-1.0F, -1.0F, 1.0F),
                "Body",
                null,
                "Head",
                null,
                null,
                "ArmRight",
                null,
                null,
                "ArmLeft",
                null,
                null);
    }

    public static RetargetProfile forArmature(Armature armature) {
        if (armature == null) {
            return null;
        }
        if (armature.hasJoint("ArmRight") && !armature.hasJoint("Thigh_R")) {
            return originalHumanoid();
        }
        if (OriginalHumanoid.ARMATURE_NAME.equals(armature.toString())) {
            return originalHumanoid();
        }
        if (armature.hasJoint("Thigh_R") && (armature.hasJoint("Chest") || armature.hasJoint("Torso"))) {
            return sourceBiped();
        }
        return null;
    }

    public OpenMatrix4f axisFlip() {
        return this.axisFlip;
    }

    public String bodyBone() {
        return this.bodyBone;
    }

    public String bodyParentBone() {
        return this.bodyParentBone;
    }

    public String headBone() {
        return this.headBone;
    }

    public String rightLegBone() {
        return this.rightLegBone;
    }

    public String leftLegBone() {
        return this.leftLegBone;
    }

    public String rightArmBone() {
        return this.rightArmBone;
    }

    public String rightHandBone() {
        return this.rightHandBone;
    }

    public String rightToolBone() {
        return this.rightToolBone;
    }

    public String leftArmBone() {
        return this.leftArmBone;
    }

    public String leftHandBone() {
        return this.leftHandBone;
    }

    public String leftToolBone() {
        return this.leftToolBone;
    }
}

package com.bslx.XueXiMod.client.action.spacecut;

import java.util.UUID;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

/**
 * 一帧已经合成并换到玩家模型空间的左腰右拔。
 * 旋转是合成之后的 S*R*S，平移是 (x, 24-y, z)，单位是模型像素。
 */
public final class SpaceCutFrame {
    private final UUID ownerId;
    private final float elapsedSeconds;
    private final SpaceCutBone body;
    private final SpaceCutBone head;
    private final SpaceCutBone armRight;
    private final SpaceCutBone forearmRight;
    private final SpaceCutBone swordHand;
    private final SpaceCutBone armLeft;
    private final SpaceCutBone forearmLeft;
    private final SpaceCutBone legRight;
    private final SpaceCutBone legLeft;
    private final float bladeX;
    private final float bladeY;
    private final float bladeZ;
    private boolean slimArms;
    private boolean rightSleeveShown;
    private boolean leftSleeveShown;
    private Object renderedModel;

    SpaceCutFrame(
            UUID ownerId,
            float elapsedSeconds,
            SpaceCutBone body,
            SpaceCutBone head,
            SpaceCutBone armRight,
            SpaceCutBone forearmRight,
            SpaceCutBone swordHand,
            SpaceCutBone armLeft,
            SpaceCutBone forearmLeft,
            SpaceCutBone legRight,
            SpaceCutBone legLeft,
            float bladeX,
            float bladeY,
            float bladeZ) {
        this.ownerId = ownerId;
        this.elapsedSeconds = elapsedSeconds;
        this.body = body;
        this.head = head;
        this.armRight = armRight;
        this.forearmRight = forearmRight;
        this.swordHand = swordHand;
        this.armLeft = armLeft;
        this.forearmLeft = forearmLeft;
        this.legRight = legRight;
        this.legLeft = legLeft;
        this.bladeX = bladeX;
        this.bladeY = bladeY;
        this.bladeZ = bladeZ;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    public float elapsedSeconds() {
        return this.elapsedSeconds;
    }

    public SpaceCutBone body() {
        return this.body;
    }

    public SpaceCutBone head() {
        return this.head;
    }

    public SpaceCutBone armRight() {
        return this.armRight;
    }

    public SpaceCutBone forearmRight() {
        return this.forearmRight;
    }

    public SpaceCutBone swordHand() {
        return this.swordHand;
    }

    public SpaceCutBone armLeft() {
        return this.armLeft;
    }

    public SpaceCutBone forearmLeft() {
        return this.forearmLeft;
    }

    public SpaceCutBone legRight() {
        return this.legRight;
    }

    public SpaceCutBone legLeft() {
        return this.legLeft;
    }

    public float bladeX() {
        return this.bladeX;
    }

    public float bladeY() {
        return this.bladeY;
    }

    public float bladeZ() {
        return this.bladeZ;
    }

    public boolean slimArms() {
        return this.slimArms;
    }

    public boolean rightSleeveShown() {
        return this.rightSleeveShown;
    }

    public boolean leftSleeveShown() {
        return this.leftSleeveShown;
    }

    public Object renderedModel() {
        return this.renderedModel;
    }

    void rememberBody(Object model, boolean slimArms, boolean rightSleeveShown, boolean leftSleeveShown) {
        this.renderedModel = model;
        this.slimArms = slimArms;
        this.rightSleeveShown = rightSleeveShown;
        this.leftSleeveShown = leftSleeveShown;
    }

    /** 模型空间的骨。局部 +Y 沿该段指向子关节，刀身轴是剑骨局部 -Y。 */
    public static final class SpaceCutBone {
        private final float pivotX;
        private final float pivotY;
        private final float pivotZ;
        private final OpenMatrix4f rotation;

        SpaceCutBone(float pivotX, float pivotY, float pivotZ, OpenMatrix4f rotation) {
            this.pivotX = pivotX;
            this.pivotY = pivotY;
            this.pivotZ = pivotZ;
            this.rotation = rotation;
        }

        public float pivotX() {
            return this.pivotX;
        }

        public float pivotY() {
            return this.pivotY;
        }

        public float pivotZ() {
            return this.pivotZ;
        }

        public OpenMatrix4f rotation() {
            return this.rotation;
        }
    }
}

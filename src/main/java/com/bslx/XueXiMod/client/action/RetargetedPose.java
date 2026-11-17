package com.bslx.XueXiMod.client.action;

import com.bslx.XueXiMod.animation.core.math.OpenMatrix4f;

/**
 * 模型空间的旋转增量 D，以及可选的挥斩对齐 L。
 * 空的部件槽表示不接管该部件旋转。LEGACY 路径下它仍要继承 T，不能停在原枢轴；
 * UPPER_BODY_SLASH 路径下空腿槽保持原版，不继承 T。
 * L 存在 worldBasis，没有预先乘进 D。六个槽都空时 anyDriven 为假，单独的 L 不改全身。
 * alignmentScope 只说走哪条写入路径，不改 anyDriven。
 */
public final class RetargetedPose {
    private final OpenMatrix4f body;
    private final OpenMatrix4f head;
    private final OpenMatrix4f rightArm;
    private final OpenMatrix4f leftArm;
    private final OpenMatrix4f rightLeg;
    private final OpenMatrix4f leftLeg;
    private final OpenMatrix4f worldBasis;
    private final SlashAlignmentScope alignmentScope;

    public RetargetedPose(
            OpenMatrix4f body,
            OpenMatrix4f head,
            OpenMatrix4f rightArm,
            OpenMatrix4f leftArm,
            OpenMatrix4f rightLeg,
            OpenMatrix4f leftLeg,
            OpenMatrix4f worldBasis) {
        this(body, head, rightArm, leftArm, rightLeg, leftLeg, worldBasis, null);
    }

    public RetargetedPose(
            OpenMatrix4f body,
            OpenMatrix4f head,
            OpenMatrix4f rightArm,
            OpenMatrix4f leftArm,
            OpenMatrix4f rightLeg,
            OpenMatrix4f leftLeg,
            OpenMatrix4f worldBasis,
            SlashAlignmentScope alignmentScope) {
        this.body = body;
        this.head = head;
        this.rightArm = rightArm;
        this.leftArm = leftArm;
        this.rightLeg = rightLeg;
        this.leftLeg = leftLeg;
        this.worldBasis = worldBasis;
        this.alignmentScope = alignmentScope == null ? SlashAlignmentScope.LEGACY : alignmentScope;
    }

    public OpenMatrix4f body() {
        return this.body;
    }

    public OpenMatrix4f head() {
        return this.head;
    }

    public OpenMatrix4f rightArm() {
        return this.rightArm;
    }

    public OpenMatrix4f leftArm() {
        return this.leftArm;
    }

    public OpenMatrix4f rightLeg() {
        return this.rightLeg;
    }

    public OpenMatrix4f leftLeg() {
        return this.leftLeg;
    }

    public OpenMatrix4f worldBasis() {
        return this.worldBasis;
    }

    public SlashAlignmentScope alignmentScope() {
        return this.alignmentScope;
    }

    public RetargetedPose withAlignmentScope(SlashAlignmentScope scope) {
        return new RetargetedPose(this.body, this.head, this.rightArm, this.leftArm, this.rightLeg, this.leftLeg,
                this.worldBasis, scope);
    }

    /** 不算 worldBasis。没有部件增量时不能靠对齐去转全身。 */
    public boolean anyDriven() {
        return this.body != null || this.head != null || this.rightArm != null || this.leftArm != null
                || this.rightLeg != null || this.leftLeg != null;
    }

    public static RetargetedPose empty() {
        return new RetargetedPose(null, null, null, null, null, null, null);
    }

    public RetargetedPose withoutRightArm() {
        return new RetargetedPose(this.body, this.head, null, this.leftArm, this.rightLeg, this.leftLeg, this.worldBasis,
                this.alignmentScope);
    }

    public RetargetedPose withoutLeftArm() {
        return new RetargetedPose(this.body, this.head, this.rightArm, null, this.rightLeg, this.leftLeg, this.worldBasis,
                this.alignmentScope);
    }
}

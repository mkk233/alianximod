package com.bslx.XueXiMod.client.action.cooperative;

/**
 * 某一帧的骨骼欧拉角，仍是 Blockbench 的角度。根偏航单独给出，不平移脚点。
 */
public final class CooperativePose {
    private final float bodyX;
    private final float bodyY;
    private final float bodyZ;
    private final float headX;
    private final float headY;
    private final float headZ;
    private final float armRightX;
    private final float armRightY;
    private final float armRightZ;
    private final float armLeftX;
    private final float armLeftY;
    private final float armLeftZ;
    private final float legRightX;
    private final float legRightY;
    private final float legRightZ;
    private final float legLeftX;
    private final float legLeftY;
    private final float legLeftZ;
    private final float rootYawDegrees;
    private final float facingYawDegrees;

    public CooperativePose(
            float bodyX, float bodyY, float bodyZ,
            float headX, float headY, float headZ,
            float armRightX, float armRightY, float armRightZ,
            float armLeftX, float armLeftY, float armLeftZ,
            float legRightX, float legRightY, float legRightZ,
            float legLeftX, float legLeftY, float legLeftZ,
            float rootYawDegrees, float facingYawDegrees) {
        this.bodyX = bodyX;
        this.bodyY = bodyY;
        this.bodyZ = bodyZ;
        this.headX = headX;
        this.headY = headY;
        this.headZ = headZ;
        this.armRightX = armRightX;
        this.armRightY = armRightY;
        this.armRightZ = armRightZ;
        this.armLeftX = armLeftX;
        this.armLeftY = armLeftY;
        this.armLeftZ = armLeftZ;
        this.legRightX = legRightX;
        this.legRightY = legRightY;
        this.legRightZ = legRightZ;
        this.legLeftX = legLeftX;
        this.legLeftY = legLeftY;
        this.legLeftZ = legLeftZ;
        this.rootYawDegrees = rootYawDegrees;
        this.facingYawDegrees = facingYawDegrees;
    }

    public float bodyX() {
        return this.bodyX;
    }

    public float bodyY() {
        return this.bodyY;
    }

    public float bodyZ() {
        return this.bodyZ;
    }

    public float headX() {
        return this.headX;
    }

    public float headY() {
        return this.headY;
    }

    public float headZ() {
        return this.headZ;
    }

    public float armRightX() {
        return this.armRightX;
    }

    public float armRightY() {
        return this.armRightY;
    }

    public float armRightZ() {
        return this.armRightZ;
    }

    public float armLeftX() {
        return this.armLeftX;
    }

    public float armLeftY() {
        return this.armLeftY;
    }

    public float armLeftZ() {
        return this.armLeftZ;
    }

    public float legRightX() {
        return this.legRightX;
    }

    public float legRightY() {
        return this.legRightY;
    }

    public float legRightZ() {
        return this.legRightZ;
    }

    public float legLeftX() {
        return this.legLeftX;
    }

    public float legLeftY() {
        return this.legLeftY;
    }

    public float legLeftZ() {
        return this.legLeftZ;
    }

    public float rootYawDegrees() {
        return this.rootYawDegrees;
    }

    public float facingYawDegrees() {
        return this.facingYawDegrees;
    }
}

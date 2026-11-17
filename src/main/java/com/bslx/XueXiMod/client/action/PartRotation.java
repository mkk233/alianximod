package com.bslx.XueXiMod.client.action;

/** 原版 ModelPart 的 ZYX 欧拉角，单位是弧度。 */
public final class PartRotation {
    private final float xRotation;
    private final float yRotation;
    private final float zRotation;

    public PartRotation(float xRotation, float yRotation, float zRotation) {
        this.xRotation = xRotation;
        this.yRotation = yRotation;
        this.zRotation = zRotation;
    }

    public float xRotation() {
        return this.xRotation;
    }

    public float yRotation() {
        return this.yRotation;
    }

    public float zRotation() {
        return this.zRotation;
    }
}

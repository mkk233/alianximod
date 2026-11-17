package com.bslx.XueXiMod.shu.technique.release.entity;

import java.util.UUID;

/** 一份命中实体冻结后的绘制记录。创建之后不再改，刷新同步也不能重播。 */
public final class StillSheetSnapshot {
    private final UUID entityId;
    private final String worldIdentity;
    private final double worldX;
    private final double worldY;
    private final double worldZ;
    private final float faceYaw;
    private final float facePitch;
    private final float faceRoll;
    private final double sheetWidth;
    private final double sheetLength;
    private final double sheetHeight;
    private final CohortVisual cohort;
    private final String texturePath;
    private final int frameCount;
    private final int ticksPerFrame;
    private final long birthGameTime;

    public StillSheetSnapshot(UUID entityId, String worldIdentity, double worldX, double worldY, double worldZ,
                              float faceYaw, float facePitch, float faceRoll, double sheetWidth, double sheetLength,
                              double sheetHeight, CohortVisual cohort, String texturePath, int frameCount,
                              int ticksPerFrame, long birthGameTime) {
        this.entityId = entityId;
        this.worldIdentity = worldIdentity == null ? "" : worldIdentity;
        this.worldX = worldX;
        this.worldY = worldY;
        this.worldZ = worldZ;
        this.faceYaw = faceYaw;
        this.facePitch = facePitch;
        this.faceRoll = faceRoll;
        this.sheetWidth = sheetWidth;
        this.sheetLength = sheetLength;
        this.sheetHeight = sheetHeight;
        this.cohort = cohort == null ? CohortVisual.SINGLE : cohort;
        this.texturePath = texturePath == null ? "" : texturePath;
        this.frameCount = frameCount;
        this.ticksPerFrame = ticksPerFrame;
        this.birthGameTime = birthGameTime;
    }

    public UUID entityId() {
        return this.entityId;
    }

    public String worldIdentity() {
        return this.worldIdentity;
    }

    public double worldX() {
        return this.worldX;
    }

    public double worldY() {
        return this.worldY;
    }

    public double worldZ() {
        return this.worldZ;
    }

    public float faceYaw() {
        return this.faceYaw;
    }

    public float facePitch() {
        return this.facePitch;
    }

    public float faceRoll() {
        return this.faceRoll;
    }

    public double sheetWidth() {
        return this.sheetWidth;
    }

    public double sheetLength() {
        return this.sheetLength;
    }

    public double sheetHeight() {
        return this.sheetHeight;
    }

    public CohortVisual cohort() {
        return this.cohort;
    }

    public String texturePath() {
        return this.texturePath;
    }

    public int frameCount() {
        return this.frameCount;
    }

    public int ticksPerFrame() {
        return this.ticksPerFrame;
    }

    public long birthGameTime() {
        return this.birthGameTime;
    }
}

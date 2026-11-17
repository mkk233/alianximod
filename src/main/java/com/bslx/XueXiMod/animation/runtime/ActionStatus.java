package com.bslx.XueXiMod.animation.runtime;

import net.minecraft.resources.ResourceLocation;

/** 运行时状态。不回写导入清单。数值对照、刚体适配和游戏观感默认都未完成。 */
public final class ActionStatus {
    public enum Availability {
        ORIGINAL,
        EXAMPLE,
        POSE_UNVERIFIED,
        POSE_DEGRADED,
        QUARANTINED,
        REJECTED
    }

    private final ResourceLocation actionId;
    private final Availability availability;
    private final String reasonText;
    private final String armatureName;
    private final String sourcePath;
    private final String upstreamCommit;
    private final String categoryText;
    private final String aliasOf;
    private final float durationSeconds;
    private final String entryShape;
    private final String auxiliaryText;
    private final boolean finitePoseChecked;

    public ActionStatus(
            ResourceLocation actionId,
            Availability availability,
            String reasonText,
            String armatureName,
            String sourcePath,
            String upstreamCommit,
            String categoryText,
            String aliasOf,
            float durationSeconds,
            String entryShape,
            String auxiliaryText,
            boolean finitePoseChecked) {
        if (actionId == null || availability == null) {
            throw new IllegalArgumentException("action status is missing");
        }
        this.actionId = actionId;
        this.availability = availability;
        this.reasonText = reasonText == null ? "" : reasonText;
        this.armatureName = armatureName == null ? "" : armatureName;
        this.sourcePath = sourcePath == null ? "" : sourcePath;
        this.upstreamCommit = upstreamCommit == null ? "" : upstreamCommit;
        this.categoryText = categoryText == null ? "" : categoryText;
        this.aliasOf = aliasOf == null ? "" : aliasOf;
        this.durationSeconds = durationSeconds;
        this.entryShape = entryShape == null ? "" : entryShape;
        this.auxiliaryText = auxiliaryText == null ? "" : auxiliaryText;
        this.finitePoseChecked = finitePoseChecked;
    }

    public ResourceLocation actionId() {
        return this.actionId;
    }

    public Availability availability() {
        return this.availability;
    }

    public boolean posePlayable() {
        return this.availability == Availability.ORIGINAL
                || this.availability == Availability.EXAMPLE
                || this.availability == Availability.POSE_UNVERIFIED
                || this.availability == Availability.POSE_DEGRADED;
    }

    public boolean degraded() {
        return this.availability == Availability.POSE_DEGRADED;
    }

    public boolean oracleCompared() {
        return false;
    }

    public boolean rigidAdapted() {
        return false;
    }

    public boolean lookVerified() {
        return false;
    }

    public String reasonText() {
        return this.reasonText;
    }

    public String armatureName() {
        return this.armatureName;
    }

    public String sourcePath() {
        return this.sourcePath;
    }

    public String upstreamCommit() {
        return this.upstreamCommit;
    }

    public String categoryText() {
        return this.categoryText;
    }

    public String aliasOf() {
        return this.aliasOf;
    }

    public float durationSeconds() {
        return this.durationSeconds;
    }

    public String entryShape() {
        return this.entryShape;
    }

    public String auxiliaryText() {
        return this.auxiliaryText;
    }

    public boolean finitePoseChecked() {
        return this.finitePoseChecked;
    }

    public String reportLine() {
        String playableText = this.posePlayable() ? "可采样" : "不可播放";
        return this.actionId + " [" + this.availability + "/" + playableText + "] "
                + this.reasonText;
    }
}

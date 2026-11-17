package com.bslx.XueXiMod.animation.runtime;

import java.util.UUID;

import net.minecraft.resources.ResourceLocation;

/**
 * 一次攻击组提交的不可变描述。向量是有限浮点，不引用客户端类型。
 * 权威时长由服务端按动作定义写入，不表示 20 tick 的伤害窗。
 */
public final class ActionCommit {
    private final UUID groupId;
    private final ResourceLocation actionId;
    private final ResourceLocation fallbackActionId;
    private final float authoritativeDurationSeconds;
    private final int beatIndex;
    private final boolean hasSlashFrame;
    private final float directionX;
    private final float directionY;
    private final float directionZ;
    private final float planeX;
    private final float planeY;
    private final float planeZ;

    private ActionCommit(
            UUID groupId,
            ResourceLocation actionId,
            ResourceLocation fallbackActionId,
            float authoritativeDurationSeconds,
            int beatIndex,
            boolean hasSlashFrame,
            float directionX,
            float directionY,
            float directionZ,
            float planeX,
            float planeY,
            float planeZ) {
        this.groupId = groupId;
        this.actionId = actionId;
        this.fallbackActionId = fallbackActionId;
        this.authoritativeDurationSeconds = authoritativeDurationSeconds;
        this.beatIndex = beatIndex;
        this.hasSlashFrame = hasSlashFrame;
        this.directionX = directionX;
        this.directionY = directionY;
        this.directionZ = directionZ;
        this.planeX = planeX;
        this.planeY = planeY;
        this.planeZ = planeZ;
    }

    public static ActionCommit group(UUID groupId, ResourceLocation actionId) {
        if (groupId == null || actionId == null) {
            throw new IllegalArgumentException("attack group commit is missing");
        }
        return new ActionCommit(groupId, actionId, null, 0.0F, -1, false, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
    }

    public static ActionCommit framed(
            UUID groupId,
            ResourceLocation actionId,
            int beatIndex,
            float directionX,
            float directionY,
            float directionZ,
            float planeX,
            float planeY,
            float planeZ) {
        if (groupId == null || actionId == null) {
            throw new IllegalArgumentException("attack group commit is missing");
        }
        if (beatIndex < -1) {
            throw new IllegalArgumentException("beat index is missing");
        }
        ActionFrame.Basis basis = requireFrame(directionX, directionY, directionZ, planeX, planeY, planeZ);
        return new ActionCommit(groupId, actionId, null, 0.0F, beatIndex, true,
                basis.forwardX(), basis.forwardY(), basis.forwardZ(), basis.normalX(), basis.normalY(), basis.normalZ());
    }

    public ActionCommit withFallback(ResourceLocation fallbackActionId) {
        return new ActionCommit(this.groupId, this.actionId, fallbackActionId, this.authoritativeDurationSeconds, this.beatIndex,
                this.hasSlashFrame, this.directionX, this.directionY, this.directionZ, this.planeX, this.planeY, this.planeZ);
    }

    public ActionCommit withAuthoritativeDuration(float authoritativeDurationSeconds) {
        if (!Float.isFinite(authoritativeDurationSeconds) || authoritativeDurationSeconds <= 0.0F) {
            throw new IllegalArgumentException("authoritative duration must be finite and positive");
        }
        return new ActionCommit(this.groupId, this.actionId, this.fallbackActionId, authoritativeDurationSeconds, this.beatIndex,
                this.hasSlashFrame, this.directionX, this.directionY, this.directionZ, this.planeX, this.planeY, this.planeZ);
    }

    public UUID groupId() {
        return this.groupId;
    }

    public ResourceLocation actionId() {
        return this.actionId;
    }

    public ResourceLocation fallbackActionId() {
        return this.fallbackActionId;
    }

    public float authoritativeDurationSeconds() {
        return this.authoritativeDurationSeconds;
    }

    public int beatIndex() {
        return this.beatIndex;
    }

    public boolean hasSlashFrame() {
        return this.hasSlashFrame;
    }

    public float directionX() {
        return this.directionX;
    }

    public float directionY() {
        return this.directionY;
    }

    public float directionZ() {
        return this.directionZ;
    }

    public float planeX() {
        return this.planeX;
    }

    public float planeY() {
        return this.planeY;
    }

    public float planeZ() {
        return this.planeZ;
    }

    private static ActionFrame.Basis requireFrame(float directionX, float directionY, float directionZ, float planeX, float planeY, float planeZ) {
        ActionFrame.Basis basis = ActionFrame.canonicalize(directionX, directionY, directionZ, planeX, planeY, planeZ);
        if (basis == null) {
            throw new IllegalArgumentException("slash frame is degenerate");
        }
        return basis;
    }
}

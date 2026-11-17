package com.bslx.XueXiMod.client.action.skinned;

import java.util.UUID;

import com.bslx.XueXiMod.client.action.mesh.PlayerMeshKind;
import com.bslx.XueXiMod.client.action.mesh.SkinDiagnostic;

/**
 * 一次绘制决定。模式、所有者和原因都由调用方写入。
 * 不读取世界、注册表、装备或动作状态，也不表示装备预检已经接线。
 */
public final class RenderPlan {
    public enum Mode {
        VANILLA,
        RIGID,
        SKINNED
    }

    public enum Perspective {
        THIRD_PERSON,
        FIRST_PERSON
    }

    public enum Hand {
        NONE,
        RIGHT,
        LEFT
    }

    private final Mode mode;
    private final UUID ownerId;
    private final int entityId;
    private final Perspective perspective;
    private final Hand hand;
    private final PlayerMeshKind bodyKind;
    private final long resourceGeneration;
    private final String reason;

    private RenderPlan(
            Mode mode,
            UUID ownerId,
            int entityId,
            Perspective perspective,
            Hand hand,
            PlayerMeshKind bodyKind,
            long resourceGeneration,
            String reason) {
        this.mode = mode;
        this.ownerId = ownerId;
        this.entityId = entityId;
        this.perspective = perspective;
        this.hand = hand;
        this.bodyKind = bodyKind;
        this.resourceGeneration = resourceGeneration;
        this.reason = reason;
    }

    public static RenderPlan of(
            Mode mode,
            UUID ownerId,
            int entityId,
            Perspective perspective,
            Hand hand,
            PlayerMeshKind bodyKind,
            long resourceGeneration,
            String reason) {
        if (mode == null) {
            throw new SkinDiagnostic("mode-missing", "绘制模式缺失");
        }
        if (ownerId == null) {
            throw new SkinDiagnostic("owner-missing", "所有者缺失");
        }
        if (perspective == null) {
            throw new SkinDiagnostic("perspective-missing", "视角缺失");
        }
        if (hand == null) {
            throw new SkinDiagnostic("hand-missing", "手缺失");
        }
        if (perspective == Perspective.FIRST_PERSON && hand == Hand.NONE) {
            throw new SkinDiagnostic("hand-missing", "第一人称计划必须写明左右手");
        }
        if (bodyKind == null) {
            throw new SkinDiagnostic("body-kind", "体型缺失");
        }
        if (resourceGeneration < 0L) {
            throw new SkinDiagnostic("generation", "资源代次不能为负");
        }
        if (reason == null || reason.isBlank()) {
            throw new SkinDiagnostic("reason-missing", "模式原因缺失");
        }
        return new RenderPlan(mode, ownerId, entityId, perspective, hand, bodyKind, resourceGeneration, reason);
    }

    public Mode mode() {
        return this.mode;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    /** 调用方传入的实体编号。本类型不查询世界。 */
    public int entityId() {
        return this.entityId;
    }

    public Perspective perspective() {
        return this.perspective;
    }

    public Hand hand() {
        return this.hand;
    }

    public PlayerMeshKind bodyKind() {
        return this.bodyKind;
    }

    public long resourceGeneration() {
        return this.resourceGeneration;
    }

    public String reason() {
        return this.reason;
    }
}

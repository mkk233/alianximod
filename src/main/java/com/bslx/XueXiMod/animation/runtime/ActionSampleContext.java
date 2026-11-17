package com.bslx.XueXiMod.animation.runtime;

import java.util.UUID;

import com.bslx.XueXiMod.animation.core.Pose;
import com.bslx.XueXiMod.animation.core.model.Armature;

import net.minecraft.resources.ResourceLocation;

/**
 * 渲染只读的一次采样。生成它不会推进时间事件。
 * 结束和取消时 active 为假；挥斩方向和平面来自提交快照。
 */
public final class ActionSampleContext {
    private final boolean active;
    private final UUID ownerId;
    private final int entityNetworkId;
    private final long sessionEpoch;
    private final long sequence;
    private final UUID groupId;
    private final ResourceLocation actionId;
    private final ResourceLocation visualActionId;
    private final String armatureName;
    private final boolean hasSlashFrame;
    private final float directionX;
    private final float directionY;
    private final float directionZ;
    private final float planeX;
    private final float planeY;
    private final float planeZ;
    private final float normalizedProgress;
    private final Pose pose;
    private final long startGameTick;
    private final float authoritativeDurationSeconds;
    private final float elapsedSeconds;
    private final int beatIndex;
    private final int revision;
    private final boolean hasFrozenYaw;
    private final float frozenYaw;

    private ActionSampleContext(
            boolean active,
            UUID ownerId,
            int entityNetworkId,
            long sessionEpoch,
            long sequence,
            UUID groupId,
            ResourceLocation actionId,
            ResourceLocation visualActionId,
            String armatureName,
            boolean hasSlashFrame,
            float directionX,
            float directionY,
            float directionZ,
            float planeX,
            float planeY,
            float planeZ,
            float normalizedProgress,
            Pose pose,
            long startGameTick,
            float authoritativeDurationSeconds,
            float elapsedSeconds,
            int beatIndex,
            int revision,
            boolean hasFrozenYaw,
            float frozenYaw) {
        this.active = active;
        this.ownerId = ownerId;
        this.entityNetworkId = entityNetworkId;
        this.sessionEpoch = sessionEpoch;
        this.sequence = sequence;
        this.groupId = groupId;
        this.actionId = actionId;
        this.visualActionId = visualActionId;
        this.armatureName = armatureName == null ? "" : armatureName;
        this.hasSlashFrame = hasSlashFrame;
        this.directionX = directionX;
        this.directionY = directionY;
        this.directionZ = directionZ;
        this.planeX = planeX;
        this.planeY = planeY;
        this.planeZ = planeZ;
        this.normalizedProgress = Float.isFinite(normalizedProgress) ? normalizedProgress : 0.0F;
        this.pose = pose == null ? new Pose() : pose;
        this.startGameTick = startGameTick;
        this.authoritativeDurationSeconds = authoritativeDurationSeconds;
        this.elapsedSeconds = elapsedSeconds;
        this.beatIndex = beatIndex;
        this.revision = revision;
        this.hasFrozenYaw = hasFrozenYaw;
        this.frozenYaw = frozenYaw;
    }

    public static ActionSampleContext inactive(UUID ownerId) {
        return capture(false, ownerId, -1, 0L, 0L, null, null, null, "", false,
                0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, new Pose(), 0L, 0.0F, 0.0F, -1, 0, false, 0.0F);
    }

    public static ActionSampleContext capture(
            boolean active,
            UUID ownerId,
            int entityNetworkId,
            long sessionEpoch,
            long sequence,
            UUID groupId,
            ResourceLocation actionId,
            ResourceLocation visualActionId,
            String armatureName,
            boolean hasSlashFrame,
            float directionX,
            float directionY,
            float directionZ,
            float planeX,
            float planeY,
            float planeZ,
            float normalizedProgress,
            Pose pose,
            long startGameTick,
            float authoritativeDurationSeconds,
            float elapsedSeconds,
            int beatIndex,
            int revision,
            boolean hasFrozenYaw,
            float frozenYaw) {
        ActionFrame.Basis basis = ActionFrame.resolve(hasSlashFrame, directionX, directionY, directionZ, planeX, planeY, planeZ);
        return new ActionSampleContext(active, ownerId, entityNetworkId, sessionEpoch, sequence, groupId, actionId, visualActionId,
                armatureName, basis.present(), basis.forwardX(), basis.forwardY(), basis.forwardZ(), basis.normalX(), basis.normalY(),
                basis.normalZ(), normalizedProgress, pose, startGameTick, authoritativeDurationSeconds, elapsedSeconds, beatIndex,
                revision, hasFrozenYaw, frozenYaw);
    }

    public static float normalized(boolean looping, float elapsedSeconds, float authoritativeDurationSeconds) {
        if (!Float.isFinite(elapsedSeconds) || elapsedSeconds < 0.0F
                || !Float.isFinite(authoritativeDurationSeconds) || authoritativeDurationSeconds <= 0.0F) {
            return 0.0F;
        }
        if (looping) {
            float wrapped = elapsedSeconds % authoritativeDurationSeconds;
            if (wrapped < 0.0F) {
                wrapped += authoritativeDurationSeconds;
            }
            return wrapped / authoritativeDurationSeconds;
        }
        if (elapsedSeconds >= authoritativeDurationSeconds) {
            return 1.0F;
        }
        return elapsedSeconds / authoritativeDurationSeconds;
    }

    public boolean active() {
        return this.active;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    public int entityNetworkId() {
        return this.entityNetworkId;
    }

    public long sessionEpoch() {
        return this.sessionEpoch;
    }

    public long sequence() {
        return this.sequence;
    }

    public UUID groupId() {
        return this.groupId;
    }

    public ResourceLocation actionId() {
        return this.actionId;
    }

    public ResourceLocation visualActionId() {
        return this.visualActionId;
    }

    public String armatureName() {
        return this.armatureName;
    }

    public Armature armature() {
        if (this.armatureName.isBlank()) {
            return null;
        }
        return ActionRegistry.armature(this.armatureName);
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

    public float normalizedProgress() {
        return this.normalizedProgress;
    }

    public Pose pose() {
        return this.pose;
    }

    public long startGameTick() {
        return this.startGameTick;
    }

    public float authoritativeDurationSeconds() {
        return this.authoritativeDurationSeconds;
    }

    public float elapsedSeconds() {
        return this.elapsedSeconds;
    }

    public int beatIndex() {
        return this.beatIndex;
    }

    public int revision() {
        return this.revision;
    }

    public boolean hasFrozenYaw() {
        return this.hasFrozenYaw;
    }

    public float frozenYaw() {
        return this.frozenYaw;
    }
}

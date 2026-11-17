package com.bslx.XueXiMod.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.bslx.XueXiMod.animation.runtime.ActionClientIngress;
import com.bslx.XueXiMod.animation.runtime.ActionFrame;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

/** 动作姿态只从服务端发往客户端。这个包不能驱动伤害。 */
public class ActionSyncS2CPacket {
    public static final int START = 0;
    public static final int PAUSE = 1;
    public static final int FINISH = 2;
    public static final int CANCEL = 3;
    public static final int SNAPSHOT = 4;
    public static final int RESUME = 5;

    public static final int STATE_PLAYING = 0;
    public static final int STATE_PAUSED = 1;
    public static final int STATE_FINISHED = 2;
    public static final int STATE_CANCELLED = 3;

    private final int kind;
    private final int stateCode;
    private final UUID ownerId;
    private final int entityNetworkId;
    private final String dimensionId;
    private final long sessionEpoch;
    private final long sequence;
    private final long startGameTick;
    private final String actionIdText;
    private final float speed;
    private final boolean looping;
    private final float blendSeconds;
    private final float frozenSampleSeconds;
    private final boolean hasFrozenYaw;
    private final float frozenYaw;
    private final boolean hasParentGroup;
    private final UUID parentGroupId;
    private final int revision;
    private final long clockOffsetTicks;
    private final float authoritativeDurationSeconds;
    private final float pausedElapsedSeconds;
    private final float pausedBlendElapsedSeconds;
    private final long pauseBeganTick;
    private final String fallbackActionIdText;
    private final int beatIndex;
    private final boolean hasSlashFrame;
    private final float directionX;
    private final float directionY;
    private final float directionZ;
    private final float planeX;
    private final float planeY;
    private final float planeZ;

    public ActionSyncS2CPacket(
            int kind,
            int stateCode,
            UUID ownerId,
            int entityNetworkId,
            String dimensionId,
            long sessionEpoch,
            long sequence,
            long startGameTick,
            String actionIdText,
            float speed,
            boolean looping,
            float blendSeconds,
            float frozenSampleSeconds,
            boolean hasFrozenYaw,
            float frozenYaw,
            boolean hasParentGroup,
            UUID parentGroupId,
            int revision,
            long clockOffsetTicks,
            float authoritativeDurationSeconds,
            float pausedElapsedSeconds,
            float pausedBlendElapsedSeconds,
            long pauseBeganTick,
            String fallbackActionIdText,
            int beatIndex,
            boolean hasSlashFrame,
            float directionX,
            float directionY,
            float directionZ,
            float planeX,
            float planeY,
            float planeZ) {
        this.kind = kind;
        this.stateCode = stateCode;
        this.ownerId = ownerId;
        this.entityNetworkId = entityNetworkId;
        this.dimensionId = dimensionId == null ? "" : dimensionId;
        this.sessionEpoch = sessionEpoch;
        this.sequence = sequence;
        this.startGameTick = startGameTick;
        this.actionIdText = actionIdText == null ? "" : actionIdText;
        this.speed = speed;
        this.looping = looping;
        this.blendSeconds = blendSeconds;
        this.frozenSampleSeconds = frozenSampleSeconds;
        this.hasFrozenYaw = hasFrozenYaw;
        this.frozenYaw = frozenYaw;
        this.hasParentGroup = hasParentGroup;
        this.parentGroupId = parentGroupId;
        this.revision = revision;
        this.clockOffsetTicks = clockOffsetTicks;
        this.authoritativeDurationSeconds = authoritativeDurationSeconds;
        this.pausedElapsedSeconds = pausedElapsedSeconds;
        this.pausedBlendElapsedSeconds = pausedBlendElapsedSeconds;
        this.pauseBeganTick = pauseBeganTick;
        this.fallbackActionIdText = fallbackActionIdText == null ? "" : fallbackActionIdText;
        this.beatIndex = beatIndex;
        ActionFrame.Basis basis = ActionFrame.resolve(
                hasSlashFrame, directionX, directionY, directionZ, planeX, planeY, planeZ);
        this.hasSlashFrame = basis.present();
        this.directionX = basis.forwardX();
        this.directionY = basis.forwardY();
        this.directionZ = basis.forwardZ();
        this.planeX = basis.normalX();
        this.planeY = basis.normalY();
        this.planeZ = basis.normalZ();
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(this.kind);
        buffer.writeByte(this.stateCode);
        buffer.writeUUID(this.ownerId);
        buffer.writeVarInt(this.entityNetworkId);
        buffer.writeUtf(this.dimensionId);
        buffer.writeLong(this.sessionEpoch);
        buffer.writeLong(this.sequence);
        buffer.writeLong(this.startGameTick);
        buffer.writeUtf(this.actionIdText);
        buffer.writeFloat(this.speed);
        buffer.writeBoolean(this.looping);
        buffer.writeFloat(this.blendSeconds);
        buffer.writeFloat(this.frozenSampleSeconds);
        buffer.writeBoolean(this.hasFrozenYaw);
        buffer.writeFloat(this.frozenYaw);
        buffer.writeBoolean(this.hasParentGroup);
        if (this.hasParentGroup) {
            buffer.writeUUID(this.parentGroupId);
        }
        buffer.writeInt(this.revision);
        buffer.writeLong(this.clockOffsetTicks);
        buffer.writeFloat(this.authoritativeDurationSeconds);
        buffer.writeFloat(this.pausedElapsedSeconds);
        buffer.writeFloat(this.pausedBlendElapsedSeconds);
        buffer.writeLong(this.pauseBeganTick);
        buffer.writeUtf(this.fallbackActionIdText);
        buffer.writeInt(this.beatIndex);
        buffer.writeBoolean(this.hasSlashFrame);
        buffer.writeFloat(this.directionX);
        buffer.writeFloat(this.directionY);
        buffer.writeFloat(this.directionZ);
        buffer.writeFloat(this.planeX);
        buffer.writeFloat(this.planeY);
        buffer.writeFloat(this.planeZ);
    }

    public static ActionSyncS2CPacket decode(FriendlyByteBuf buffer) {
        int kind = buffer.readByte();
        int stateCode = buffer.readByte();
        UUID ownerId = buffer.readUUID();
        int entityNetworkId = buffer.readVarInt();
        String dimensionId = buffer.readUtf();
        long sessionEpoch = buffer.readLong();
        long sequence = buffer.readLong();
        long startGameTick = buffer.readLong();
        String actionIdText = buffer.readUtf();
        float speed = buffer.readFloat();
        boolean looping = buffer.readBoolean();
        float blendSeconds = buffer.readFloat();
        float frozenSampleSeconds = buffer.readFloat();
        boolean hasFrozenYaw = buffer.readBoolean();
        float frozenYaw = buffer.readFloat();
        boolean hasParentGroup = buffer.readBoolean();
        UUID parentGroupId = hasParentGroup ? buffer.readUUID() : null;
        int revision = buffer.readInt();
        long clockOffsetTicks = buffer.readLong();
        float authoritativeDurationSeconds = buffer.readFloat();
        float pausedElapsedSeconds = buffer.readFloat();
        float pausedBlendElapsedSeconds = buffer.readFloat();
        long pauseBeganTick = buffer.readLong();
        String fallbackActionIdText = buffer.readUtf();
        int beatIndex = buffer.readInt();
        boolean hasSlashFrame = buffer.readBoolean();
        float directionX = buffer.readFloat();
        float directionY = buffer.readFloat();
        float directionZ = buffer.readFloat();
        float planeX = buffer.readFloat();
        float planeY = buffer.readFloat();
        float planeZ = buffer.readFloat();
        return new ActionSyncS2CPacket(kind, stateCode, ownerId, entityNetworkId, dimensionId, sessionEpoch, sequence,
                startGameTick, actionIdText, speed, looping, blendSeconds, frozenSampleSeconds, hasFrozenYaw, frozenYaw,
                hasParentGroup, parentGroupId, revision, clockOffsetTicks, authoritativeDurationSeconds, pausedElapsedSeconds,
                pausedBlendElapsedSeconds, pauseBeganTick, fallbackActionIdText, beatIndex, hasSlashFrame,
                directionX, directionY, directionZ, planeX, planeY, planeZ);
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        Object connectionLink = context.getNetworkManager();
        ActionSyncS2CPacket packet = this;
        context.enqueueWork(() -> ActionClientIngress.accept(packet, connectionLink));
        context.setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, ActionSyncS2CPacket.class, ActionSyncS2CPacket::encode, ActionSyncS2CPacket::decode, ActionSyncS2CPacket::handle);
    }

    public int kind() {
        return this.kind;
    }

    public int stateCode() {
        return this.stateCode;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    public int entityNetworkId() {
        return this.entityNetworkId;
    }

    public String dimensionId() {
        return this.dimensionId;
    }

    public long sessionEpoch() {
        return this.sessionEpoch;
    }

    public long sequence() {
        return this.sequence;
    }

    public long startGameTick() {
        return this.startGameTick;
    }

    public String actionIdText() {
        return this.actionIdText;
    }

    public float speed() {
        return this.speed;
    }

    public boolean looping() {
        return this.looping;
    }

    public float blendSeconds() {
        return this.blendSeconds;
    }

    public float frozenSampleSeconds() {
        return this.frozenSampleSeconds;
    }

    public boolean hasFrozenYaw() {
        return this.hasFrozenYaw;
    }

    public float frozenYaw() {
        return this.frozenYaw;
    }

    public boolean hasParentGroup() {
        return this.hasParentGroup;
    }

    public UUID parentGroupId() {
        return this.parentGroupId;
    }

    public int revision() {
        return this.revision;
    }

    public long clockOffsetTicks() {
        return this.clockOffsetTicks;
    }

    public float authoritativeDurationSeconds() {
        return this.authoritativeDurationSeconds;
    }

    public float pausedElapsedSeconds() {
        return this.pausedElapsedSeconds;
    }

    public float pausedBlendElapsedSeconds() {
        return this.pausedBlendElapsedSeconds;
    }

    public long pauseBeganTick() {
        return this.pauseBeganTick;
    }

    public String fallbackActionIdText() {
        return this.fallbackActionIdText;
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
}

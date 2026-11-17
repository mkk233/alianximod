package com.bslx.XueXiMod.network;

import java.util.UUID;
import java.util.function.Supplier;

import com.bslx.XueXiMod.client.action.cooperative.CooperativeActionViews;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ActionPlayback;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeActionIds;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.CooperativeStructureLimits;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.FamilyKey;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.WeaponDisplaySnapshot;

import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 协同动作只发开始、停止和跟踪快照。不逐刻广播骨骼。
 */
public final class CooperativeActionS2CPacket {
    public static final int START = 1;
    public static final int STOP = 2;
    public static final int TRACKING = 3;

    private final int kind;
    private final boolean valid;
    private final UUID serverSession;
    private final ResourceKey<Level> dimension;
    private final UUID familyOwnerId;
    private final UUID familyCastId;
    private final long familyEpoch;
    private final int familyOrdinal;
    private final boolean memberPresent;
    private final int memberOrdinal;
    private final UUID actorId;
    private final int entityId;
    private final long actorGeneration;
    private final long sequence;
    private final ResourceLocation actionId;
    private final long serverStartTick;
    private final int windupTicks;
    private final int recoveryTicks;
    private final float visualYaw;
    private final boolean mirrored;
    private final WeaponDisplaySnapshot weapon;

    private CooperativeActionS2CPacket(
            int kind,
            boolean valid,
            UUID serverSession,
            ResourceKey<Level> dimension,
            UUID familyOwnerId,
            UUID familyCastId,
            long familyEpoch,
            int familyOrdinal,
            boolean memberPresent,
            int memberOrdinal,
            UUID actorId,
            int entityId,
            long actorGeneration,
            long sequence,
            ResourceLocation actionId,
            long serverStartTick,
            int windupTicks,
            int recoveryTicks,
            float visualYaw,
            boolean mirrored,
            WeaponDisplaySnapshot weapon) {
        this.kind = kind;
        this.valid = valid;
        this.serverSession = serverSession;
        this.dimension = dimension;
        this.familyOwnerId = familyOwnerId;
        this.familyCastId = familyCastId;
        this.familyEpoch = familyEpoch;
        this.familyOrdinal = familyOrdinal;
        this.memberPresent = memberPresent;
        this.memberOrdinal = memberOrdinal;
        this.actorId = actorId;
        this.entityId = entityId;
        this.actorGeneration = actorGeneration;
        this.sequence = sequence;
        this.actionId = actionId;
        this.serverStartTick = serverStartTick;
        this.windupTicks = windupTicks;
        this.recoveryTicks = recoveryTicks;
        this.visualYaw = visualYaw;
        this.mirrored = mirrored;
        this.weapon = weapon;
    }

    public static CooperativeActionS2CPacket start(ActionPlayback playback) {
        return fromPlayback(START, playback);
    }

    public static CooperativeActionS2CPacket tracking(ActionPlayback playback) {
        return fromPlayback(TRACKING, playback);
    }

    public static CooperativeActionS2CPacket stop(FamilyKey family, UUID actorId, long sequence) {
        if (family == null || actorId == null || sequence < 1L) {
            return invalid(STOP);
        }
        return new CooperativeActionS2CPacket(
                STOP, true, family.serverSession(), family.dimension(), family.ownerId(), family.castId(),
                family.epoch(), family.familyOrdinal(), false, -1, actorId, 0, 0L, sequence,
                null, 0L, 0, 1, 0.0F, false, null);
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(
                messageId,
                CooperativeActionS2CPacket.class,
                CooperativeActionS2CPacket::encode,
                CooperativeActionS2CPacket::decode,
                CooperativeActionS2CPacket::handle,
                java.util.Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(this.kind);
        buffer.writeBoolean(this.valid);
        if (!this.valid || this.serverSession == null || this.dimension == null || this.actorId == null) {
            return;
        }
        buffer.writeUUID(this.serverSession);
        buffer.writeResourceLocation(this.dimension.location());
        buffer.writeUUID(this.familyOwnerId);
        buffer.writeUUID(this.familyCastId);
        buffer.writeLong(this.familyEpoch);
        buffer.writeVarInt(this.familyOrdinal);
        buffer.writeUUID(this.actorId);
        buffer.writeLong(this.sequence);
        if (this.kind == STOP) {
            return;
        }
        buffer.writeBoolean(this.memberPresent);
        if (this.memberPresent) {
            buffer.writeVarInt(this.memberOrdinal);
        }
        buffer.writeVarInt(this.entityId);
        buffer.writeLong(this.actorGeneration);
        buffer.writeResourceLocation(this.actionId);
        buffer.writeLong(this.serverStartTick);
        buffer.writeVarInt(this.windupTicks);
        buffer.writeVarInt(this.recoveryTicks);
        buffer.writeFloat(this.visualYaw);
        buffer.writeBoolean(this.mirrored);
        boolean weaponPresent = this.weapon != null && this.weapon.count() > 0;
        buffer.writeBoolean(weaponPresent);
        if (weaponPresent) {
            buffer.writeResourceLocation(this.weapon.itemId());
            buffer.writeVarInt(this.weapon.count());
            CompoundTag frozenTag = this.weapon.frozenTag();
            buffer.writeBoolean(frozenTag != null);
            if (frozenTag != null) {
                buffer.writeNbt(frozenTag);
            }
        }
    }

    public static CooperativeActionS2CPacket decode(FriendlyByteBuf buffer) {
        int kind = buffer.readByte();
        boolean marked = buffer.readBoolean();
        if (!marked || (kind != START && kind != STOP && kind != TRACKING)) {
            return invalid(kind);
        }
        UUID serverSession = buffer.readUUID();
        ResourceLocation dimensionId = buffer.readResourceLocation();
        UUID familyOwnerId = buffer.readUUID();
        UUID familyCastId = buffer.readUUID();
        long familyEpoch = buffer.readLong();
        int familyOrdinal = buffer.readVarInt();
        UUID actorId = buffer.readUUID();
        long sequence = buffer.readLong();
        ResourceKey<Level> dimension = ResourceKey.create(Registries.DIMENSION, dimensionId);
        if (kind == STOP) {
            boolean stopValid = serverSession != null && actorId != null && sequence >= 1L && familyOrdinal >= 0;
            return new CooperativeActionS2CPacket(
                    STOP, stopValid, serverSession, dimension, familyOwnerId, familyCastId, familyEpoch, familyOrdinal,
                    false, -1, actorId, 0, 0L, sequence, null, 0L, 0, 1, 0.0F, false, null);
        }
        boolean memberPresent = buffer.readBoolean();
        int memberOrdinal = memberPresent ? buffer.readVarInt() : -1;
        int entityId = buffer.readVarInt();
        long actorGeneration = buffer.readLong();
        ResourceLocation actionId = buffer.readResourceLocation();
        long serverStartTick = buffer.readLong();
        int windupTicks = buffer.readVarInt();
        int recoveryTicks = buffer.readVarInt();
        float visualYaw = buffer.readFloat();
        boolean mirrored = buffer.readBoolean();
        WeaponDisplaySnapshot weapon = null;
        if (buffer.readBoolean()) {
            ResourceLocation itemId = buffer.readResourceLocation();
            int count = buffer.readVarInt();
            CompoundTag frozenTag = buffer.readBoolean() ? buffer.readNbt() : null;
            try {
                weapon = new WeaponDisplaySnapshot(itemId, count, frozenTag);
            } catch (RuntimeException rejected) {
                return invalid(kind);
            }
        }
        boolean playbackValid = sequence >= 1L
                && entityId >= 0
                && familyOrdinal >= 0
                && (!memberPresent || memberOrdinal >= 0)
                && windupTicks >= 0
                && windupTicks <= CooperativeStructureLimits.WINDUP_HIGH
                && recoveryTicks >= CooperativeStructureLimits.RECOVERY_LOW
                && recoveryTicks <= CooperativeStructureLimits.RECOVERY_HIGH
                && Float.isFinite(visualYaw)
                && (CooperativeActionIds.OBLIQUE_SLASH.equals(actionId) || CooperativeActionIds.SPIN.equals(actionId));
        return new CooperativeActionS2CPacket(
                kind, playbackValid, serverSession, dimension, familyOwnerId, familyCastId, familyEpoch, familyOrdinal,
                memberPresent, memberOrdinal, actorId, entityId, actorGeneration, sequence, actionId, serverStartTick,
                windupTicks, recoveryTicks, visualYaw, mirrored, weapon);
    }

    public static void handle(CooperativeActionS2CPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.setPacketHandled(true);
        if (packet == null || !packet.valid || context.getDirection() != NetworkDirection.PLAY_TO_CLIENT) {
            return;
        }
        context.enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> CooperativeActionViews.accept(packet)));
    }

    public int kind() {
        return this.kind;
    }

    public boolean valid() {
        return this.valid;
    }

    public UUID serverSession() {
        return this.serverSession;
    }

    public ResourceKey<Level> dimension() {
        return this.dimension;
    }

    public UUID familyOwnerId() {
        return this.familyOwnerId;
    }

    public UUID familyCastId() {
        return this.familyCastId;
    }

    public long familyEpoch() {
        return this.familyEpoch;
    }

    public int familyOrdinal() {
        return this.familyOrdinal;
    }

    public boolean memberPresent() {
        return this.memberPresent;
    }

    public int memberOrdinal() {
        return this.memberOrdinal;
    }

    public UUID actorId() {
        return this.actorId;
    }

    public int entityId() {
        return this.entityId;
    }

    public long actorGeneration() {
        return this.actorGeneration;
    }

    public long sequence() {
        return this.sequence;
    }

    public ResourceLocation actionId() {
        return this.actionId;
    }

    public long serverStartTick() {
        return this.serverStartTick;
    }

    public int windupTicks() {
        return this.windupTicks;
    }

    public int recoveryTicks() {
        return this.recoveryTicks;
    }

    public float visualYaw() {
        return this.visualYaw;
    }

    public boolean mirrored() {
        return this.mirrored;
    }

    public WeaponDisplaySnapshot weapon() {
        return this.weapon;
    }

    private static CooperativeActionS2CPacket fromPlayback(int kind, ActionPlayback playback) {
        if (playback == null) {
            return invalid(kind);
        }
        FamilyKey family = playback.family();
        return new CooperativeActionS2CPacket(
                kind,
                true,
                playback.serverSession(),
                playback.dimension(),
                family.ownerId(),
                family.castId(),
                family.epoch(),
                family.familyOrdinal(),
                playback.member() != null,
                playback.member() == null ? -1 : playback.member().memberOrdinal(),
                playback.actorId(),
                playback.entityId(),
                playback.actorGeneration(),
                playback.sequence(),
                playback.actionId(),
                playback.serverStartTick(),
                playback.windupTicks(),
                playback.recoveryTicks(),
                playback.visualYaw(),
                playback.mirrored(),
                playback.weapon());
    }

    private static CooperativeActionS2CPacket invalid(int kind) {
        return new CooperativeActionS2CPacket(
                kind, false, null, null, null, null, 0L, 0, false, -1, null, 0, 0L, 0L,
                null, 0L, 0, 1, 0.0F, false, null);
    }
}

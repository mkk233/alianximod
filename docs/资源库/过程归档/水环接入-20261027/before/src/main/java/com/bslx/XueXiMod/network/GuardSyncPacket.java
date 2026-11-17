package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** 服务端把护体快照和姿态发给能看见这位玩家的人。坐标不逐拍发送。 */
public class GuardSyncPacket {
    public static final int FULL = 0;
    public static final int POSE = 1;
    public static final int REMOVE = 2;

    private final int kind;
    private final UUID ownerId;
    private final int revision;
    private final int sequence;
    private final int expiresAt;
    private final int upkeepAt;
    private final int animationStart;
    private final float baseTilt;
    private final float radius;
    private final float band;
    private final float bodyHeight;
    private final float fromX;
    private final float fromY;
    private final float fromZ;
    private final float towardX;
    private final float towardY;
    private final float towardZ;
    private final int poseTicks;

    public GuardSyncPacket(int kind, UUID ownerId, int revision, int sequence,
                           int expiresAt, int upkeepAt, int animationStart,
                           float baseTilt, float radius, float band, float bodyHeight,
                           float fromX, float fromY, float fromZ,
                           float towardX, float towardY, float towardZ, int poseTicks) {
        this.kind = kind;
        this.ownerId = ownerId;
        this.revision = revision;
        this.sequence = sequence;
        this.expiresAt = expiresAt;
        this.upkeepAt = upkeepAt;
        this.animationStart = animationStart;
        this.baseTilt = baseTilt;
        this.radius = radius;
        this.band = band;
        this.bodyHeight = bodyHeight;
        this.fromX = fromX;
        this.fromY = fromY;
        this.fromZ = fromZ;
        this.towardX = towardX;
        this.towardY = towardY;
        this.towardZ = towardZ;
        this.poseTicks = poseTicks;
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeByte(kind);
        buffer.writeUUID(ownerId);
        buffer.writeVarInt(revision);
        buffer.writeVarInt(sequence);
        buffer.writeVarInt(expiresAt);
        buffer.writeVarInt(upkeepAt);
        buffer.writeVarInt(animationStart);
        buffer.writeFloat(baseTilt);
        buffer.writeFloat(radius);
        buffer.writeFloat(band);
        buffer.writeFloat(bodyHeight);
        buffer.writeFloat(fromX);
        buffer.writeFloat(fromY);
        buffer.writeFloat(fromZ);
        buffer.writeFloat(towardX);
        buffer.writeFloat(towardY);
        buffer.writeFloat(towardZ);
        buffer.writeVarInt(poseTicks);
    }

    public static GuardSyncPacket decode(FriendlyByteBuf buffer) {
        return new GuardSyncPacket(buffer.readByte(), buffer.readUUID(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt());
    }

    public void handle(Supplier<NetworkEvent.Context> contextSupplier) {
        GuardSyncPacket packet = this;
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> ClientRings.accept(packet)));
        contextSupplier.get().setPacketHandled(true);
    }

    public static void register(SimpleChannel channel, int messageId) {
        channel.registerMessage(messageId, GuardSyncPacket.class, GuardSyncPacket::encode, GuardSyncPacket::decode, GuardSyncPacket::handle);
    }

    public static void sendAround(ServerPlayer owner, GuardSyncPacket packet) {
        if (owner == null || owner.getServer() == null) {
            return;
        }
        for (ServerPlayer watcher : owner.serverLevel().players()) {
            if (watcher == owner || watcher.distanceTo(owner) <= 128f) {
                ModNetwork.sendToPlayer(watcher, packet);
            }
        }
    }

    public static void sendSnapshot(ServerPlayer watcher, ServerPlayer owner) {
        GuardSyncPacket packet = GuardStates.snapshot(owner.getUUID());
        if (packet != null && watcher != null) {
            ModNetwork.sendToPlayer(watcher, packet);
        }
    }

    public static final class ClientRings {
        private static final ArrayList<View> VIEWS = new ArrayList<>();

        private ClientRings() {
        }

        public static void accept(GuardSyncPacket packet) {
            if (packet.kind == REMOVE) {
                VIEWS.removeIf(view -> view.ownerId.equals(packet.ownerId));
                return;
            }
            View view = find(packet.ownerId);
            if (packet.kind == FULL) {
                if (view != null && packet.revision < view.revision) {
                    return;
                }
                if (view == null) {
                    view = new View();
                    view.ownerId = packet.ownerId;
                    VIEWS.add(view);
                }
                view.revision = packet.revision;
                view.sequence = packet.sequence;
                view.baseTilt = packet.baseTilt;
                view.radius = packet.radius;
                view.band = packet.band;
                view.bodyHeight = packet.bodyHeight;
                view.animationStart = packet.animationStart;
                applyPose(view, packet);
                return;
            }
            if (view == null || packet.revision != view.revision || packet.sequence < view.sequence) {
                return;
            }
            view.sequence = packet.sequence;
            applyPose(view, packet);
        }

        public static void clear() {
            VIEWS.clear();
        }

        public static List<View> all() {
            return VIEWS;
        }

        private static View find(UUID ownerId) {
            for (View view : VIEWS) {
                if (view.ownerId.equals(ownerId)) {
                    return view;
                }
            }
            return null;
        }

        private static void applyPose(View view, GuardSyncPacket packet) {
            view.fromX = packet.fromX;
            view.fromY = packet.fromY;
            view.fromZ = packet.fromZ;
            view.towardX = packet.towardX;
            view.towardY = packet.towardY;
            view.towardZ = packet.towardZ;
            view.poseStartMillis = System.currentTimeMillis();
            view.poseTicks = Math.max(1, packet.poseTicks);
        }

        public static final class View {
            public UUID ownerId;
            public int revision;
            public int sequence;
            public float baseTilt;
            public float radius = 0.9f;
            public float band = 0.12f;
            public float bodyHeight = 0.55f;
            public int animationStart;
            public float fromX;
            public float fromY = 1f;
            public float fromZ;
            public float towardX;
            public float towardY = 1f;
            public float towardZ;
            public long poseStartMillis;
            public int poseTicks = 1;
        }
    }
}

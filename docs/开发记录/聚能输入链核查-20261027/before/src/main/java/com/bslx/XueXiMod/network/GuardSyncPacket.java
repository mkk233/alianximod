package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.client.GuardClientLifecycle;
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

/** 服务端把护体快照和姿态发给能看见这位玩家的人。坐标不逐拍发送。动画相位用年龄，不用墙钟。 */
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
    private final int animationAgeTicks;
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
                           int expiresAt, int upkeepAt, int animationStart, int animationAgeTicks,
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
        this.animationAgeTicks = animationAgeTicks;
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
        buffer.writeVarInt(animationAgeTicks);
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
                buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(), buffer.readVarInt(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readFloat(), buffer.readFloat(), buffer.readFloat(),
                buffer.readVarInt());
    }

    boolean matches(GuardSyncPacket other) {
        return other != null
                && this.kind == other.kind
                && this.ownerId.equals(other.ownerId)
                && this.revision == other.revision
                && this.sequence == other.sequence
                && this.expiresAt == other.expiresAt
                && this.upkeepAt == other.upkeepAt
                && this.animationStart == other.animationStart
                && this.animationAgeTicks == other.animationAgeTicks
                && this.baseTilt == other.baseTilt
                && this.radius == other.radius
                && this.band == other.band
                && this.bodyHeight == other.bodyHeight
                && this.fromX == other.fromX
                && this.fromY == other.fromY
                && this.fromZ == other.fromZ
                && this.towardX == other.towardX
                && this.towardY == other.towardY
                && this.towardZ == other.towardZ
                && this.poseTicks == other.poseTicks;
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
            accept(packet, GuardClientLifecycle.clientGameTick());
        }

        static void accept(GuardSyncPacket packet, long clientGameTick) {
            if (packet.kind == REMOVE) {
                VIEWS.removeIf(view -> removes(view, packet));
                return;
            }
            View view = find(packet.ownerId);
            if (packet.kind == FULL) {
                if (view != null && packet.animationStart < view.animationStart) {
                    return;
                }
                if (view != null && packet.animationStart == view.animationStart && packetLags(view, packet)) {
                    return;
                }
                boolean freshActivation = view == null || packet.animationStart != view.animationStart;
                boolean sameRevision = !freshActivation && packet.revision == view.revision;
                boolean sameSequence = sameRevision && packet.sequence == view.sequence;
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
                anchorAge(view, packet.animationAgeTicks, clientGameTick, freshActivation);
                if (freshActivation || !sameSequence) {
                    applyPose(view, packet);
                }
                return;
            }
            if (packet.kind != POSE || view == null || packet.animationStart != view.animationStart
                    || packet.revision != view.revision || packetLags(view, packet)) {
                return;
            }
            boolean freshActivation = packet.animationStart != view.animationStart;
            boolean sameRevision = packet.revision == view.revision;
            boolean sameSequence = sameRevision && packet.sequence == view.sequence;
            view.sequence = packet.sequence;
            anchorAge(view, packet.animationAgeTicks, clientGameTick, freshActivation);
            if (freshActivation || !sameSequence) {
                applyPose(view, packet);
            }
        }

        public static void clear() {
            VIEWS.clear();
        }

        public static List<View> all() {
            return VIEWS;
        }

        private static boolean removes(View view, GuardSyncPacket packet) {
            if (view.ownerId == null || !view.ownerId.equals(packet.ownerId)) {
                return false;
            }
            if (view.animationStart != packet.animationStart) {
                return false;
            }
            return !packetLags(view, packet);
        }

        private static boolean packetLags(View view, GuardSyncPacket packet) {
            if (packet.revision < view.revision) {
                return true;
            }
            if (packet.revision > view.revision) {
                return false;
            }
            return packet.sequence < view.sequence;
        }

        private static void anchorAge(View view, int packetAge, long clientGameTick, boolean freshActivation) {
            int safeAge = Math.max(0, packetAge);
            if (freshActivation) {
                view.anchoredAgeTicks = safeAge;
                view.anchorClientTick = clientGameTick;
                return;
            }
            double localAge = view.animationAge(clientGameTick, 0f);
            if (safeAge > localAge) {
                view.anchoredAgeTicks = safeAge;
                view.anchorClientTick = clientGameTick;
            }
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
            private int anchoredAgeTicks;
            private long anchorClientTick;

            public double animationAge(long clientGameTick, float partialTick) {
                long elapsedTicks = clientGameTick - this.anchorClientTick;
                if (elapsedTicks < 0L) {
                    elapsedTicks = 0L;
                }
                float partial = partialTick;
                if (!Float.isFinite(partial) || partial < 0f) {
                    partial = 0f;
                }
                double age = this.anchoredAgeTicks + (double) elapsedTicks + partial;
                if (age < 0.0) {
                    return 0.0;
                }
                return age;
            }
        }
    }
}

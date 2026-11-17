package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainBoundary;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.UUID;
import java.util.function.Supplier;

/** 把固定领域盒子和这位观察者能否通过发给客户端。 */
public class DomainSyncPacket {
    private final boolean removed;
    private final UUID instanceId;
    private final int revision;
    private final UUID ownerId;
    private final double lowX;
    private final double lowY;
    private final double lowZ;
    private final double highX;
    private final double highY;
    private final double highZ;
    private final boolean viewerMayPass;
    private final int permitRevision;
    private final int visualKind;
    private final int boundaryKind;
    private final long visualOrigin;
    private final long visualEnd;
    private final int animSeed;

    public DomainSyncPacket(boolean removed, UUID instanceId, int revision, UUID ownerId,
                            double lowX, double lowY, double lowZ, double highX, double highY, double highZ,
                            boolean viewerMayPass, int permitRevision,
                            int visualKind, int boundaryKind, long visualOrigin, long visualEnd, int animSeed) {
        this.removed = removed;
        this.instanceId = instanceId;
        this.revision = revision;
        this.ownerId = ownerId;
        this.lowX = lowX;
        this.lowY = lowY;
        this.lowZ = lowZ;
        this.highX = highX;
        this.highY = highY;
        this.highZ = highZ;
        this.viewerMayPass = viewerMayPass;
        this.permitRevision = permitRevision;
        this.visualKind = visualKind;
        this.boundaryKind = boundaryKind;
        this.visualOrigin = visualOrigin;
        this.visualEnd = visualEnd;
        this.animSeed = animSeed;
    }

    public static void register(SimpleChannel channel, int packetId) {
        channel.registerMessage(packetId, DomainSyncPacket.class, DomainSyncPacket::encode, DomainSyncPacket::decode, DomainSyncPacket::handle);
    }

    public static void broadcast(DomainInstance domain, boolean removed) {
        if (domain == null || domain.hostLevel() == null) {
            return;
        }
        for (ServerPlayer watcher : domain.hostLevel().players()) {
            boolean pass = Relations.permits(domain.ownerId(), watcher) || Relations.ally(domain.ownerId(), watcher);
            ModNetwork.sendToPlayer(watcher, packetFor(domain, removed, pass));
        }
    }

    public static void sendSnapshot(ServerPlayer watcher) {
        if (watcher == null) {
            return;
        }
        for (DomainInstance domain : com.bslx.XueXiMod.shu.technique.release.domain.DomainService.active()) {
            if (domain.active() && domain.hostLevel() == watcher.level()) {
                boolean pass = Relations.permits(domain.ownerId(), watcher) || Relations.ally(domain.ownerId(), watcher);
                ModNetwork.sendToPlayer(watcher, packetFor(domain, false, pass));
            }
        }
    }

    public void encode(FriendlyByteBuf buffer) {
        buffer.writeBoolean(removed);
        buffer.writeUUID(instanceId);
        buffer.writeVarInt(revision);
        buffer.writeUUID(ownerId);
        buffer.writeDouble(lowX);
        buffer.writeDouble(lowY);
        buffer.writeDouble(lowZ);
        buffer.writeDouble(highX);
        buffer.writeDouble(highY);
        buffer.writeDouble(highZ);
        buffer.writeBoolean(viewerMayPass);
        buffer.writeVarInt(permitRevision);
        buffer.writeVarInt(visualKind);
        buffer.writeVarInt(boundaryKind);
        buffer.writeLong(visualOrigin);
        buffer.writeLong(visualEnd);
        buffer.writeVarInt(animSeed);
    }

    public static DomainSyncPacket decode(FriendlyByteBuf buffer) {
        return new DomainSyncPacket(buffer.readBoolean(), buffer.readUUID(), buffer.readVarInt(), buffer.readUUID(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readDouble(), buffer.readDouble(), buffer.readDouble(),
                buffer.readBoolean(), buffer.readVarInt(),
                buffer.readVarInt(), buffer.readVarInt(), buffer.readLong(), buffer.readLong(), buffer.readVarInt());
    }

    private static DomainSyncPacket packetFor(DomainInstance domain, boolean removed, boolean pass) {
        AABB bounds = domain.geometry().bounds();
        return new DomainSyncPacket(removed, domain.instanceId(), domain.revision(), domain.ownerId(),
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ,
                pass, domain.revision(), domain.visual().ordinal(), domain.boundary().ordinal(),
                domain.visualOrigin(), domain.visualEnd(), domain.animSeed());
    }

    public static void handle(DomainSyncPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().enqueueWork(() -> DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> DomainSightStore.accept(packet)));
        contextSupplier.get().setPacketHandled(true);
    }

    public boolean removed() {
        return removed;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public int revision() {
        return revision;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public double lowX() {
        return lowX;
    }

    public double lowY() {
        return lowY;
    }

    public double lowZ() {
        return lowZ;
    }

    public double highX() {
        return highX;
    }

    public double highY() {
        return highY;
    }

    public double highZ() {
        return highZ;
    }

    public boolean viewerMayPass() {
        return viewerMayPass;
    }

    public int permitRevision() {
        return permitRevision;
    }

    public DomainVisual visual() {
        DomainVisual[] kinds = DomainVisual.values();
        if (visualKind < 0 || visualKind >= kinds.length) {
            return DomainVisual.VOLUME;
        }
        return kinds[visualKind];
    }

    public DomainBoundary boundary() {
        DomainBoundary[] kinds = DomainBoundary.values();
        if (boundaryKind < 0 || boundaryKind >= kinds.length) {
            return DomainBoundary.SEAL;
        }
        return kinds[boundaryKind];
    }

    public long visualOrigin() {
        return visualOrigin;
    }

    public long visualEnd() {
        return visualEnd;
    }

    public int animSeed() {
        return animSeed;
    }
}

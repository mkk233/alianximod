package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainBoundary;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSightStore;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainVisual;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.MotionFrame;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.VolumeMotionRuntime;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.List;
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
    private final int motionKind;
    private final boolean liftedColumn;
    private final double attraction;
    private final double pullSpeed;
    private final double orbitSpeed;
    private final double riseSpeed;
    private final double travelSpeed;
    private final double bodyRadius;
    private final UUID mountedHost;
    private final List<UUID> controlledRoots;

    public DomainSyncPacket(boolean removed, UUID instanceId, int revision, UUID ownerId,
                            double lowX, double lowY, double lowZ, double highX, double highY, double highZ,
                            boolean viewerMayPass, int permitRevision,
                            int visualKind, int boundaryKind, long visualOrigin, long visualEnd, int animSeed,
                            int motionKind, boolean liftedColumn, double attraction, double pullSpeed,
                            double orbitSpeed, double riseSpeed, double travelSpeed, double bodyRadius,
                            UUID mountedHost, List<UUID> controlledRoots) {
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
        this.motionKind = motionKind;
        this.liftedColumn = liftedColumn;
        this.attraction = attraction;
        this.pullSpeed = pullSpeed;
        this.orbitSpeed = orbitSpeed;
        this.riseSpeed = riseSpeed;
        this.travelSpeed = travelSpeed;
        this.bodyRadius = bodyRadius;
        this.mountedHost = mountedHost;
        this.controlledRoots = controlledRoots == null ? List.of() : List.copyOf(controlledRoots);
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
        buffer.writeVarInt(motionKind);
        buffer.writeBoolean(liftedColumn);
        buffer.writeDouble(attraction);
        buffer.writeDouble(pullSpeed);
        buffer.writeDouble(orbitSpeed);
        buffer.writeDouble(riseSpeed);
        buffer.writeDouble(travelSpeed);
        buffer.writeDouble(bodyRadius);
        buffer.writeBoolean(mountedHost != null);
        if (mountedHost != null) {
            buffer.writeUUID(mountedHost);
        }
        buffer.writeVarInt(controlledRoots.size());
        for (int cursor = 0; cursor < controlledRoots.size(); cursor++) {
            buffer.writeUUID(controlledRoots.get(cursor));
        }
    }

    public static DomainSyncPacket decode(FriendlyByteBuf buffer) {
        boolean removed = buffer.readBoolean();
        UUID instanceId = buffer.readUUID();
        int revision = buffer.readVarInt();
        UUID ownerId = buffer.readUUID();
        double lowX = buffer.readDouble();
        double lowY = buffer.readDouble();
        double lowZ = buffer.readDouble();
        double highX = buffer.readDouble();
        double highY = buffer.readDouble();
        double highZ = buffer.readDouble();
        boolean viewerMayPass = buffer.readBoolean();
        int permitRevision = buffer.readVarInt();
        int visualKind = buffer.readVarInt();
        int boundaryKind = buffer.readVarInt();
        long visualOrigin = buffer.readLong();
        long visualEnd = buffer.readLong();
        int animSeed = buffer.readVarInt();
        int motionKind = buffer.readVarInt();
        boolean liftedColumn = buffer.readBoolean();
        double attraction = buffer.readDouble();
        double pullSpeed = buffer.readDouble();
        double orbitSpeed = buffer.readDouble();
        double riseSpeed = buffer.readDouble();
        double travelSpeed = buffer.readDouble();
        double bodyRadius = buffer.readDouble();
        UUID mountedHost = buffer.readBoolean() ? buffer.readUUID() : null;
        int rootCount = buffer.readVarInt();
        if (rootCount < 0) {
            rootCount = 0;
        }
        ArrayList<UUID> roots = new ArrayList<>();
        for (int cursor = 0; cursor < rootCount; cursor++) {
            roots.add(buffer.readUUID());
        }
        return new DomainSyncPacket(removed, instanceId, revision, ownerId, lowX, lowY, lowZ, highX, highY, highZ,
                viewerMayPass, permitRevision, visualKind, boundaryKind, visualOrigin, visualEnd, animSeed,
                motionKind, liftedColumn, attraction, pullSpeed, orbitSpeed, riseSpeed, travelSpeed, bodyRadius,
                mountedHost, roots);
    }

    private static DomainSyncPacket packetFor(DomainInstance domain, boolean removed, boolean pass) {
        AABB bounds = domain.geometry().bounds();
        MotionFrame frame = VolumeMotionRuntime.frame(domain);
        return new DomainSyncPacket(removed, domain.instanceId(), domain.revision(), domain.ownerId(),
                bounds.minX, bounds.minY, bounds.minZ, bounds.maxX, bounds.maxY, bounds.maxZ,
                pass, domain.revision(), domain.visual().ordinal(), domain.boundary().ordinal(),
                domain.visualOrigin(), domain.visualEnd(), domain.animSeed(),
                frame.kindOrdinal(), frame.lifted(), frame.attraction(), frame.pull(),
                frame.orbit(), frame.rise(), frame.travelSpeed(), frame.bodyRadius(), domain.mountedHost(),
                domain.publishedRoots());
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

    public MotionFrame motionFrame() {
        return new MotionFrame(motionKind, liftedColumn, attraction, pullSpeed, orbitSpeed, riseSpeed, travelSpeed,
                bodyRadius);
    }

    public List<UUID> controlledRoots() {
        return controlledRoots;
    }

    public UUID mountedHost() {
        return mountedHost;
    }
}

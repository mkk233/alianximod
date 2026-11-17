package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.network.NetherDomainSync;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 客户端收到的幽冥真实范围。外包盒不参与染色。服务端不把这份表当成领域是否存活的依据。 */
public final class NetherDomainSight {
    private static final ConcurrentHashMap<UUID, Sight> SIGHTS = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<UUID, Partial> PARTIALS = new ConcurrentHashMap<>();

    private NetherDomainSight() {
    }

    public static void accept(NetherDomainSync packet) {
        if (packet == null || packet.domainId() == null) {
            return;
        }
        if (packet.removed()) {
            Sight previous = SIGHTS.get(packet.domainId());
            if (previous == null || packet.revision() >= previous.revision) {
                SIGHTS.remove(packet.domainId());
                PARTIALS.remove(packet.domainId());
            }
            return;
        }
        if (packet.volumeCount() < 0 || packet.volumeCount() > NetherDomainSync.VOLUME_CAP
                || packet.fragmentCount() < 1 || packet.fragmentIndex() < 0
                || packet.fragmentIndex() >= packet.fragmentCount()) {
            return;
        }
        Sight published = SIGHTS.get(packet.domainId());
        if (published != null && packet.revision() < published.revision) {
            return;
        }
        Partial partial = PARTIALS.get(packet.domainId());
        if (partial == null || partial.revision != packet.revision() || partial.volumeCount != packet.volumeCount()
                || partial.fragmentCount != packet.fragmentCount()) {
            partial = new Partial(packet);
            PARTIALS.put(packet.domainId(), partial);
        }
        if (!partial.place(packet)) {
            return;
        }
        if (partial.complete()) {
            SIGHTS.put(packet.domainId(), partial.publish());
            PARTIALS.remove(packet.domainId(), partial);
        }
    }

    public static Collection<Sight> all() {
        return SIGHTS.values();
    }

    public static void clear() {
        SIGHTS.clear();
        PARTIALS.clear();
    }

    public static NetherDomainTint.Sample probe(Level level, double worldX, double worldY, double worldZ) {
        if (level == null) {
            return NetherDomainTint.Sample.plain();
        }
        String dimensionId = level.dimension().location().toString();
        long gameTime = level.getGameTime();
        for (Sight sight : SIGHTS.values()) {
            if (sight.covers(dimensionId, gameTime, worldX, worldY, worldZ)) {
                return NetherDomainTint.Sample.gloomTint();
            }
        }
        return NetherDomainTint.Sample.plain();
    }

    public static final class Sight {
        private final UUID domainId;
        private final UUID ownerId;
        private final int revision;
        private final String dimensionId;
        private final AABB bounds;
        private final long visualEnd;
        private final double[] volumes;

        private Sight(Partial partial) {
            this.domainId = partial.domainId;
            this.ownerId = partial.ownerId;
            this.revision = partial.revision;
            this.dimensionId = partial.dimensionId;
            this.bounds = new AABB(partial.lowX, partial.lowY, partial.lowZ, partial.highX, partial.highY, partial.highZ);
            this.visualEnd = partial.visualEnd;
            this.volumes = partial.packed.clone();
        }

        public UUID domainId() {
            return domainId;
        }

        public UUID ownerId() {
            return ownerId;
        }

        public int revision() {
            return revision;
        }

        public String dimensionId() {
            return dimensionId;
        }

        /** 切片的外包盒，只给摆放参考。染色必须走 {@link #copyVolume} 或 {@code sample}。 */
        public AABB bounds() {
            return bounds;
        }

        public long visualEnd() {
            return visualEnd;
        }

        public int volumeCount() {
            return volumes.length / 6;
        }

        public boolean copyVolume(int ordinal, double[] destination) {
            int origin = ordinal * 6;
            if (destination == null || destination.length < 6 || origin < 0 || origin + 5 >= volumes.length) {
                return false;
            }
            int cursor = 0;
            while (cursor < 6) {
                destination[cursor] = volumes[origin + cursor];
                cursor++;
            }
            return true;
        }

        public boolean live(String currentDimension, long gameTime) {
            return dimensionId.equals(currentDimension) && gameTime < visualEnd;
        }

        private boolean covers(String currentDimension, long gameTime, double worldX, double worldY, double worldZ) {
            if (!live(currentDimension, gameTime)) {
                return false;
            }
            int cursor = 0;
            while (cursor + 5 < volumes.length) {
                if (worldX >= volumes[cursor] && worldX < volumes[cursor + 3]
                        && worldY >= volumes[cursor + 1] && worldY < volumes[cursor + 4]
                        && worldZ >= volumes[cursor + 2] && worldZ < volumes[cursor + 5]) {
                    return true;
                }
                cursor += 6;
            }
            return false;
        }
    }

    private static final class Partial {
        private final UUID domainId;
        private final UUID ownerId;
        private final int revision;
        private final String dimensionId;
        private final double lowX;
        private final double lowY;
        private final double lowZ;
        private final double highX;
        private final double highY;
        private final double highZ;
        private final long visualEnd;
        private final int volumeCount;
        private final int fragmentCount;
        private final double[] packed;
        private final boolean[] arrived;
        private int arrivedCount;

        private Partial(NetherDomainSync packet) {
            this.domainId = packet.domainId();
            this.ownerId = packet.ownerId();
            this.revision = packet.revision();
            this.dimensionId = packet.dimensionId();
            this.lowX = packet.lowX();
            this.lowY = packet.lowY();
            this.lowZ = packet.lowZ();
            this.highX = packet.highX();
            this.highY = packet.highY();
            this.highZ = packet.highZ();
            this.visualEnd = packet.visualEnd();
            this.volumeCount = packet.volumeCount();
            this.fragmentCount = packet.fragmentCount();
            this.packed = new double[volumeCount * 6];
            this.arrived = new boolean[fragmentCount];
        }

        private boolean place(NetherDomainSync packet) {
            if (arrived[packet.fragmentIndex()]) {
                return true;
            }
            double[] piece = packet.piece();
            int start = packet.fragmentIndex() * NetherDomainSync.PIECE_LIMIT * 6;
            if (piece == null || piece.length % 6 != 0 || start < 0 || start + piece.length > packed.length) {
                return false;
            }
            if (piece.length > 0) {
                System.arraycopy(piece, 0, packed, start, piece.length);
            }
            arrived[packet.fragmentIndex()] = true;
            arrivedCount++;
            return true;
        }

        private boolean complete() {
            return arrivedCount >= fragmentCount;
        }

        private Sight publish() {
            return new Sight(this);
        }
    }
}

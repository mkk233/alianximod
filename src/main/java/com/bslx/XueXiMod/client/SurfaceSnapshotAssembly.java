package com.bslx.XueXiMod.client;

import com.bslx.XueXiMod.network.FireVisualPacket;
import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceContactField;
import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceSnapshotFrames;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * 客户端把同一宿主、同一几何版本的分片收齐后才替换。没收齐不和旧画面混用。
 * 版本只在同一维度、同一宿主上比较。结束标记挡住迟到分片，直到原来的到期时刻。
 */
public final class SurfaceSnapshotAssembly {
    private static final double EDGE_SLOP = 1.0E-3d;
    private static final Map<Long, Pending> PENDING = new HashMap<>();
    private static final Map<Long, Ended> ENDED = new HashMap<>();

    private SurfaceSnapshotAssembly() {
    }

    /**
     * 收一片。shownVersion 只在来包维度和当前画面维度相同时传入，没有画面时传 0。
     * 更低版本直接丢掉。相同版本只续当前组装，不重置开始时刻。更高版本可以换掉未完成组装。
     */
    public static Completed accept(FireVisualPacket packet, long seenTick, int shownVersion) {
        if (packet == null || packet.kind() != FireVisualPacket.SURFACE) {
            return null;
        }
        dropExpired(seenTick);
        expireEnded(seenTick);
        long hostId = packet.zoneId();
        int version = packet.remainder();
        int fragmentCount = packet.segmentCount();
        int fragmentIndex = packet.interval();
        double[] slices = packet.slices() == null ? new double[0] : packet.slices();
        if (!headerLegal(packet, hostId, version, fragmentCount, fragmentIndex, slices)) {
            return null;
        }
        if (!slicesLegal(packet, slices)) {
            return null;
        }
        if (revivalBlocked(hostId, packet.dimensionId(), seenTick)) {
            return null;
        }
        if (shownVersion > 0 && version <= shownVersion) {
            return null;
        }
        Pending pending = PENDING.get(hostId);
        boolean sameDimension = pending != null && pending.dimensionId.equals(packet.dimensionId());
        if (sameDimension && version < pending.version) {
            return null;
        }
        if (sameDimension && version == pending.version) {
            if (fragmentCount != pending.fragmentCount || !pending.sameFrame(packet)) {
                return null;
            }
            if (pending.filled[fragmentIndex]) {
                return null;
            }
            return storeFragment(pending, fragmentIndex, slices);
        }
        if (pending != null && !sameDimension) {
            return null;
        }
        if (sameDimension && version > pending.version) {
            PENDING.remove(hostId);
        }
        return beginPending(packet, seenTick, hostId, version, fragmentCount, fragmentIndex, slices);
    }

    /** 清掉这个宿主的未完成组装，并保留结束标记到原来的到期时刻。 */
    public static void end(long hostId, String dimensionId, long expireTick) {
        PENDING.remove(hostId);
        if (hostId == 0L) {
            return;
        }
        ENDED.put(hostId, new Ended(dimensionId == null ? "" : dimensionId, expireTick));
    }

    public static long pendingExpire(long hostId) {
        Pending pending = PENDING.get(hostId);
        return pending == null ? 0L : pending.expireTick;
    }

    public static String pendingDimension(long hostId) {
        Pending pending = PENDING.get(hostId);
        return pending == null || pending.dimensionId == null ? "" : pending.dimensionId;
    }

    public static void forget() {
        PENDING.clear();
        ENDED.clear();
    }

    public static void expireEnded(long seenTick) {
        if (seenTick <= 0L) {
            return;
        }
        Iterator<Map.Entry<Long, Ended>> entries = ENDED.entrySet().iterator();
        while (entries.hasNext()) {
            Ended ended = entries.next().getValue();
            if (ended == null || seenTick > ended.expireTick) {
                entries.remove();
            }
        }
    }

    private static boolean headerLegal(FireVisualPacket packet, long hostId, int version, int fragmentCount,
                                       int fragmentIndex, double[] slices) {
        if (hostId == 0L || version <= 0) {
            return false;
        }
        if (fragmentCount < 1 || fragmentCount > SurfaceSnapshotFrames.FRAGMENT_LIMIT) {
            return false;
        }
        if (fragmentIndex < 0 || fragmentIndex >= fragmentCount) {
            return false;
        }
        if (packet.dimensionId() == null || packet.dimensionId().isEmpty()) {
            return false;
        }
        long span = packet.expireTick() - packet.birthTick();
        if (span != SurfaceSnapshotFrames.EXPECTED_LIFE) {
            return false;
        }
        if (!Double.isFinite(packet.originX()) || !Double.isFinite(packet.originY()) || !Double.isFinite(packet.originZ())) {
            return false;
        }
        int sliceCount = slices.length / 6;
        return slices.length % 6 == 0 && sliceCount <= SurfaceSnapshotFrames.PER_PACKET_SLICES;
    }

    private static boolean slicesLegal(FireVisualPacket packet, double[] slices) {
        double limitX = SurfaceContactField.HALF_EXTENT + EDGE_SLOP;
        double limitZ = SurfaceContactField.HALF_EXTENT + EDGE_SLOP;
        double limitY = SurfaceContactField.VERTICAL_SLACK + EDGE_SLOP;
        double rise = SurfaceContactField.CLEARANCE + EDGE_SLOP;
        int slot = 0;
        while (slot + 5 < slices.length) {
            double lowX = slices[slot];
            double lowY = slices[slot + 1];
            double lowZ = slices[slot + 2];
            double highX = slices[slot + 3];
            double highY = slices[slot + 4];
            double highZ = slices[slot + 5];
            if (!Double.isFinite(lowX) || !Double.isFinite(lowY) || !Double.isFinite(lowZ)
                    || !Double.isFinite(highX) || !Double.isFinite(highY) || !Double.isFinite(highZ)) {
                return false;
            }
            if (!(highX > lowX && highY > lowY && highZ > lowZ)) {
                return false;
            }
            if (lowX < packet.originX() - limitX || highX > packet.originX() + limitX) {
                return false;
            }
            if (lowZ < packet.originZ() - limitZ || highZ > packet.originZ() + limitZ) {
                return false;
            }
            if (lowY < packet.originY() - limitY || lowY > packet.originY() + limitY) {
                return false;
            }
            if (highY > lowY + rise) {
                return false;
            }
            slot += 6;
        }
        return true;
    }

    private static Completed beginPending(FireVisualPacket packet, long seenTick, long hostId, int version,
                                          int fragmentCount, int fragmentIndex, double[] slices) {
        if (PENDING.size() >= SurfaceSnapshotFrames.PENDING_HOST_LIMIT) {
            dropOldest();
        }
        if (PENDING.size() >= SurfaceSnapshotFrames.PENDING_HOST_LIMIT) {
            return null;
        }
        Pending created = new Pending(hostId, version, fragmentCount, packet.dimensionId(), packet.originX(), packet.originY(),
                packet.originZ(), packet.birthTick(), packet.expireTick(), packet.furnaceCue(), seenTick);
        PENDING.put(hostId, created);
        return storeFragment(created, fragmentIndex, slices);
    }

    private static Completed storeFragment(Pending pending, int fragmentIndex, double[] slices) {
        pending.fragments[fragmentIndex] = slices.clone();
        pending.filled[fragmentIndex] = true;
        pending.filledCount++;
        if (pending.filledCount < pending.fragmentCount) {
            return null;
        }
        double[] joined = join(pending);
        PENDING.remove(pending.hostId);
        if (joined == null) {
            return null;
        }
        return new Completed(pending.hostId, pending.version, pending.dimensionId, pending.originX, pending.originY,
                pending.originZ, pending.birthTick, pending.expireTick, pending.style, joined);
    }

    private static boolean revivalBlocked(long hostId, String dimensionId, long seenTick) {
        Ended ended = ENDED.get(hostId);
        if (ended == null) {
            return false;
        }
        if (seenTick > ended.expireTick) {
            ENDED.remove(hostId);
            return false;
        }
        boolean sameDimension = dimensionId != null && dimensionId.equals(ended.dimensionId);
        if (sameDimension || ended.dimensionId.isEmpty()) {
            return true;
        }
        return true;
    }

    private static double[] join(Pending pending) {
        int total = 0;
        int fragmentIndex = 0;
        while (fragmentIndex < pending.fragmentCount) {
            double[] piece = pending.fragments[fragmentIndex];
            if (piece == null || piece.length % 6 != 0) {
                return null;
            }
            total += piece.length / 6;
            fragmentIndex++;
        }
        if (total > SurfaceSnapshotFrames.OVERALL_SLICES) {
            return null;
        }
        double[] joined = new double[total * 6];
        int cursor = 0;
        fragmentIndex = 0;
        while (fragmentIndex < pending.fragmentCount) {
            double[] piece = pending.fragments[fragmentIndex];
            System.arraycopy(piece, 0, joined, cursor, piece.length);
            cursor += piece.length;
            fragmentIndex++;
        }
        return joined;
    }

    private static void dropExpired(long seenTick) {
        Iterator<Map.Entry<Long, Pending>> entries = PENDING.entrySet().iterator();
        while (entries.hasNext()) {
            Pending pending = entries.next().getValue();
            if (pending == null || seenTick - pending.startedTick > SurfaceSnapshotFrames.ASSEMBLY_TIMEOUT) {
                entries.remove();
            }
        }
    }

    private static void dropOldest() {
        Long oldestId = null;
        long oldestTick = Long.MAX_VALUE;
        for (Map.Entry<Long, Pending> entry : PENDING.entrySet()) {
            Pending pending = entry.getValue();
            if (pending != null && pending.startedTick < oldestTick) {
                oldestTick = pending.startedTick;
                oldestId = entry.getKey();
            }
        }
        if (oldestId != null) {
            PENDING.remove(oldestId);
        }
    }

    public static final class Completed {
        public final long hostId;
        public final int version;
        public final String dimensionId;
        public final double originX;
        public final double originY;
        public final double originZ;
        public final long birthTick;
        public final long expireTick;
        public final int style;
        public final double[] slices;

        private Completed(long hostId, int version, String dimensionId, double originX, double originY, double originZ,
                          long birthTick, long expireTick, int style, double[] slices) {
            this.hostId = hostId;
            this.version = version;
            this.dimensionId = dimensionId;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.birthTick = birthTick;
            this.expireTick = expireTick;
            this.style = style;
            this.slices = slices;
        }
    }

    private static final class Ended {
        private final String dimensionId;
        private final long expireTick;

        private Ended(String dimensionId, long expireTick) {
            this.dimensionId = dimensionId;
            this.expireTick = expireTick;
        }
    }

    private static final class Pending {
        private final long hostId;
        private final int version;
        private final int fragmentCount;
        private final String dimensionId;
        private final double originX;
        private final double originY;
        private final double originZ;
        private final long birthTick;
        private final long expireTick;
        private final int style;
        private final double[][] fragments;
        private final boolean[] filled;
        private int filledCount;
        private final long startedTick;

        private Pending(long hostId, int version, int fragmentCount, String dimensionId, double originX, double originY,
                        double originZ, long birthTick, long expireTick, int style, long startedTick) {
            this.hostId = hostId;
            this.version = version;
            this.fragmentCount = fragmentCount;
            this.dimensionId = dimensionId;
            this.originX = originX;
            this.originY = originY;
            this.originZ = originZ;
            this.birthTick = birthTick;
            this.expireTick = expireTick;
            this.style = style;
            this.fragments = new double[fragmentCount][];
            this.filled = new boolean[fragmentCount];
            this.startedTick = startedTick;
        }

        private boolean sameFrame(FireVisualPacket packet) {
            return this.dimensionId.equals(packet.dimensionId())
                    && this.birthTick == packet.birthTick()
                    && this.expireTick == packet.expireTick()
                    && this.style == packet.furnaceCue()
                    && this.originX == packet.originX()
                    && this.originY == packet.originY()
                    && this.originZ == packet.originZ();
        }
    }
}

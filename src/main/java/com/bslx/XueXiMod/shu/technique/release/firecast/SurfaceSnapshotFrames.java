package com.bslx.XueXiMod.shu.technique.release.firecast;

/**
 * 灼地地表快照的传输上限。接触和采样不使用这里的上限。
 * 超出时调用方必须拒绝整版同步，不能截成一部分再当成成功。
 */
public final class SurfaceSnapshotFrames {
    /** 单包最多片数。每片 6 个 double，坐标正文 48×48=2304 字节。 */
    public static final int PER_PACKET_SLICES = 48;
    /** 单包坐标正文加上固定头的预算。 */
    public static final int PER_PACKET_BYTES = 2560;
    public static final int FRAGMENT_LIMIT = 64;
    public static final int OVERALL_SLICES = PER_PACKET_SLICES * FRAGMENT_LIMIT;
    public static final int OVERALL_BYTES = FRAGMENT_LIMIT * PER_PACKET_BYTES;
    public static final int EXPECTED_LIFE = 100;
    /** 客户端未收齐同一版本时，最多等这么多刻。 */
    public static final int ASSEMBLY_TIMEOUT = 80;
    public static final int PENDING_HOST_LIMIT = 16;

    private static final int COORDINATE_BYTES = 48;
    private static final int FRAGMENT_OVERHEAD = 128;

    private SurfaceSnapshotFrames() {
    }

    public static int transmissionBytes(int sliceCount) {
        int safeCount = Math.max(0, sliceCount);
        int fragments = safeCount == 0 ? 1 : (safeCount + PER_PACKET_SLICES - 1) / PER_PACKET_SLICES;
        return safeCount * COORDINATE_BYTES + fragments * FRAGMENT_OVERHEAD;
    }

    public static boolean withinBudget(int sliceCount) {
        if (sliceCount < 0) {
            return false;
        }
        int fragments = sliceCount == 0 ? 1 : (sliceCount + PER_PACKET_SLICES - 1) / PER_PACKET_SLICES;
        return sliceCount <= OVERALL_SLICES && fragments <= FRAGMENT_LIMIT && transmissionBytes(sliceCount) <= OVERALL_BYTES;
    }
}

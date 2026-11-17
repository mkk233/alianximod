package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 按实际脚点位移选择短距或远距端口。分界 8 格是结构界限，不是平衡键。
 * 选择半径和归位上限为 0 时只接受原地；远距分支不会把 0 当成不限距离。
 */
public final class RecallRoute {
    private static final double SAME_SPOT = 1.0E-8D;
    private static final double SAMPLE_EDGE = 1.0E-7D;

    private RecallRoute() {
    }

    public static RecallKind kindFor(double travelBlocks, double moveLimit) {
        if (!Double.isFinite(travelBlocks) || travelBlocks < 0.0D
                || !Double.isFinite(moveLimit) || moveLimit < 0.0D
                || moveLimit > CooperativeStructureLimits.MOVE_LIMIT_CAP) {
            return RecallKind.OUT_OF_RANGE;
        }
        double allowedSquared = moveLimit == 0.0D ? SAME_SPOT : moveLimit * moveLimit;
        if (travelBlocks * travelBlocks > allowedSquared + SAMPLE_EDGE) {
            return RecallKind.OUT_OF_RANGE;
        }
        if (travelBlocks <= CooperativeStructureLimits.REPOSITION_DISTANCE_CAP) {
            return RecallKind.SHORT;
        }
        return RecallKind.REMOTE_RECALL;
    }

    /** 短距端口只接收实际位移。冻结上限大于 8 格时不能拿它代替这一步的距离。 */
    public static double shortSweepLimit(double travelBlocks, double moveLimit) {
        if (kindFor(travelBlocks, moveLimit) != RecallKind.SHORT) {
            throw new IllegalArgumentException("超过 8 格或超出冻结上限的位移不能交给短距扫掠");
        }
        return travelBlocks;
    }
}

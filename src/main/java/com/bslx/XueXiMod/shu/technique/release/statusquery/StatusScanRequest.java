package com.bslx.XueXiMod.shu.technique.release.statusquery;

/** 一次范围查询的参数。半径必须是有限正数；全圆为真时不再要求朝向点积。 */
public record StatusScanRequest(
        double radius,
        StatusQuery query,
        RelationPolicy relation,
        boolean blockOcclusion,
        boolean fullCircle) {
    public StatusScanRequest {
        if (!(radius > 0.0D) || Double.isNaN(radius) || Double.isInfinite(radius)) {
            throw new IllegalArgumentException("radius");
        }
        if (query == null || relation == null) {
            throw new IllegalArgumentException("query");
        }
    }
}

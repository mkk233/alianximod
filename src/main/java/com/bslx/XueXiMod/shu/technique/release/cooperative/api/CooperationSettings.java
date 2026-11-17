package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 构造执行族时冻结的协同参数。合同字段 maxMove 在这里叫 moveLimit。
 * prepareTicks 是结构常数 2，不是平衡键。
 */
public record CooperationSettings(double selectionRadius, double moveLimit, double sideOffset, int prepareTicks) {
    public CooperationSettings {
        if (!Double.isFinite(selectionRadius) || selectionRadius < 0.0D
                || selectionRadius > CooperativeStructureLimits.SELECTION_RADIUS_CAP) {
            throw new IllegalArgumentException("协同选择半径超出 0 到 48 格");
        }
        if (!Double.isFinite(moveLimit) || moveLimit < 0.0D
                || moveLimit > CooperativeStructureLimits.MOVE_LIMIT_CAP) {
            throw new IllegalArgumentException("协同归位距离超出 0 到 64 格");
        }
        if (!Double.isFinite(sideOffset)
                || sideOffset < CooperativeStructureLimits.SIDE_OFFSET_LOW
                || sideOffset > CooperativeStructureLimits.SIDE_OFFSET_HIGH) {
            throw new IllegalArgumentException("协同侧距超出 0.75 到 2 格");
        }
        if (prepareTicks != CooperativeStructureLimits.PREPARE_TICK_CAP) {
            throw new IllegalArgumentException("协同准备刻数固定为 2");
        }
    }
}

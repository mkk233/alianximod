package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.Optional;

/**
 * 预览余额之后、扣款之前，用本次快照的点数和档数生成计划。
 * 实现必须是纯函数，不能烘焙炉心，也不能在这里消费火种。
 */
@FunctionalInterface
public interface BurnPlanner {
    Optional<SegmentPlan> plan(int amount, int tiers, int remainder);
}

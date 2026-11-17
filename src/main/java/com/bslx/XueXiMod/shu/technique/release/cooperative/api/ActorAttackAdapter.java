package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.List;

/**
 * 纯攻击适配。prepare 和 emit 都不得执行玩家攻击、分身表演、体积运行时或任何伤害。
 * 每个演员的每个逻辑副本必须且只能返回 {@link CooperativeStructureLimits#EMIT_PER_COPY_PER_ACTOR} 个发射，
 * 其它数量由计划构建拒绝。
 */
public interface ActorAttackAdapter {
    PreparedAttack prepare(AttackPreparation input);

    List<PreparedEmit> emit(PreparedAttack attack, LogicalCopyKey copy, ActorFrame actor,
                            ResolvedCopyGeometry geometry);
}

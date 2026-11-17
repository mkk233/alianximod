package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 一名攻击成员的已降低计划。
 * copyCount 和 spawnCount 都至少为 1。spawnCount 是规划时读到的全局实体倍率，不得在这里改小。
 * delayTicks 是已经按原成员间隔降低的刻数。
 */
public record AttackMemberPlan(int chainIndex, int memberOrdinal, String techniqueId, PreparedAttack prepared,
                               int copyCount, int spawnCount, int delayTicks, boolean stagger,
                               @org.jetbrains.annotations.Nullable HitContinueLink continueLink) {
    public AttackMemberPlan {
        if (chainIndex < 0 || memberOrdinal < 0 || techniqueId == null || techniqueId.isBlank() || prepared == null) {
            throw new IllegalArgumentException("攻击成员计划不完整");
        }
        if (!techniqueId.equals(prepared.preparation().techniqueId())
                || chainIndex != prepared.preparation().chainIndex()) {
            throw new IllegalArgumentException("攻击成员和已准备攻击不一致");
        }
        if (copyCount < 1 || spawnCount < 1 || delayTicks < 0) {
            throw new IllegalArgumentException("攻击成员的份数、倍率或延迟无效");
        }
        if (continueLink != null && continueLink.childChainIndex() != chainIndex) {
            throw new IllegalArgumentException("命中接续的子下标不是这个成员");
        }
    }
}

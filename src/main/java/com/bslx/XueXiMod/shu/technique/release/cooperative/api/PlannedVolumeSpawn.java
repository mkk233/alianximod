package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 交给显式生成入口的一份体积。
 * 构造这份数据就表示关闭隐式全局实体倍率和环境释放帧的二次变换。
 * 不能靠额外两个可关闭开关宣称已经隔离。整合前不要调用旧的隐式生成循环。
 */
public record PlannedVolumeSpawn(PreparedEmit emit, int settlementOrdinal, SettlementLedgerRef ledger,
                                 SettlementGuard guard) {
    public PlannedVolumeSpawn {
        if (emit == null || ledger == null || guard == null) {
            throw new IllegalArgumentException("显式生成不完整");
        }
        if (settlementOrdinal < 0) {
            throw new IllegalArgumentException("密集结算序号不能为负");
        }
        if (!ledger.member().equals(emit.copy().member())) {
            throw new IllegalArgumentException("显式生成的账本和逻辑副本不是同一个成员");
        }
    }
}

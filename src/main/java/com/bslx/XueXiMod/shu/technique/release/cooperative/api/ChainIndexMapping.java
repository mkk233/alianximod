package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 原链下标到执行描述槽位的映射。memberScoped 为 false 时 memberOrdinal 固定为 -1。 */
public record ChainIndexMapping(int chainIndex, String techniqueId, PlanSlotKind slotKind, int memberOrdinal,
                                boolean memberScoped) {
    public ChainIndexMapping {
        if (chainIndex < 0 || techniqueId == null || techniqueId.isBlank() || slotKind == null) {
            throw new IllegalArgumentException("链下标映射不完整");
        }
        if (memberScoped) {
            if (memberOrdinal < 0) {
                throw new IllegalArgumentException("成员链槽缺少序号");
            }
        } else if (memberOrdinal != -1) {
            throw new IllegalArgumentException("族级链槽不能带成员序号");
        }
    }
}

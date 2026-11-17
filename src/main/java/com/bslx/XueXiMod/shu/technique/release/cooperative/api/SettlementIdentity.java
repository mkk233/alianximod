package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/**
 * 结算去重身份。字段对应 familyId、memberId、roundIndex、logicalCopyId、spawnOrdinal、targetUUID。
 * 不包含演员序号，也不使用实体 UUID 充当副本编号。静止斩击的轮次由调用方传 0。
 */
public record SettlementIdentity(FamilyKey family, int memberOrdinal, int roundIndex, int copyOrdinal,
                                 int spawnOrdinal, UUID targetId) {
    public SettlementIdentity {
        if (family == null || targetId == null) {
            throw new IllegalArgumentException("结算身份不完整");
        }
        if (memberOrdinal < 0 || roundIndex < 0 || copyOrdinal < 0 || spawnOrdinal < 0) {
            throw new IllegalArgumentException("结算身份序号不能为负");
        }
    }
}

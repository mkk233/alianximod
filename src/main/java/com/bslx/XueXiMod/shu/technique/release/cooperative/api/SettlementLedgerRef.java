package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/** 一个成员账本的引用。不同伤害或附魔成员不得共用同一个账本标识。整合时才绑到现有 Tally。 */
public record SettlementLedgerRef(MemberKey member, UUID ledgerId) {
    public SettlementLedgerRef {
        if (member == null || ledgerId == null) {
            throw new IllegalArgumentException("结算账本引用不完整");
        }
    }
}

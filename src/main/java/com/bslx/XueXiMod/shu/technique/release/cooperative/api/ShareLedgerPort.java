package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import net.minecraft.server.level.ServerPlayer;

/**
 * 把计划里的份额索引绑到已经存在的父收据。
 * 提交走未提交份额的提交和收据的 retainShare；退款走 refundShare 或份额池 close。
 * 禁止用魔力池直接恢复来代替原法杖退款。本接口不调用扣款。
 */
public interface ShareLedgerPort {
    void bind(FamilyKey family, PlanShareSchedule schedule, ManaPaymentReceipt receipt);

    void commit(FamilyKey family, ShareRole role, int memberOrdinal, boolean familyLevel);

    void refundUncommitted(FamilyKey family, ShareRole role, int memberOrdinal, boolean familyLevel,
                           ServerPlayer owner);

    void closeUntaken(FamilyKey family, ServerPlayer owner);
}

package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import net.minecraft.server.level.ServerPlayer;

/** 一轮预留。只保存 charge 返回的那张收据，之后不再读上一张收据。 */
public final class GroupBudget {
    final ManaPaymentReceipt receipt;
    int openShare;
    boolean settled;

    private GroupBudget(ManaPaymentReceipt receipt, int openShare) {
        this.receipt = receipt;
        this.openShare = openShare;
    }

    public static GroupBudget reserve(ServerPlayer owner, int cost) {
        return reserve(owner, cost, true);
    }

    public static GroupBudget reserve(ServerPlayer owner, int cost, boolean tellPlayer) {
        int billed = Math.max(0, cost);
        ManaPaymentReceipt charged = ManaPaymentReceipt.charge(owner, billed, tellPlayer);
        if (charged == null) {
            return null;
        }
        return new GroupBudget(charged, billed);
    }

    /** 把这一截从本轮待退余额移走，留给已经接住同一张收据的份额稍后提交或退回。 */
    int detach(int amount) {
        if (settled || amount <= 0) {
            return 0;
        }
        int moved = Math.min(amount, openShare);
        openShare -= moved;
        return moved;
    }

    void commit(int share) {
        if (settled || share <= 0) {
            return;
        }
        int keeping = Math.min(share, openShare);
        openShare -= keeping;
        if (keeping > 0 && receipt != null) {
            receipt.retainShare(keeping);
            com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.afterShareCommitted(receipt);
        }
    }

    void settle(ServerPlayer owner) {
        if (settled) {
            return;
        }
        settled = true;
        if (receipt != null) {
            receipt.refundShare(owner, openShare);
        }
        openShare = 0;
    }
}

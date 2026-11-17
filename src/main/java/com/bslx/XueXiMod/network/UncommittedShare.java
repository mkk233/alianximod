package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.player.ManaPaymentReceipt;
import net.minecraft.server.level.ServerPlayer;

/** 一笔已经付过、还没提交的份额。同一份只终结一次，不靠收据剩余额度去判断该退哪一段。 */
public final class UncommittedShare {
    private final ManaPaymentReceipt receipt;
    private final int share;
    private boolean settled;
    private boolean kept;

    private UncommittedShare(ManaPaymentReceipt receipt, int share, boolean settled) {
        this.receipt = receipt;
        this.share = Math.max(0, share);
        this.settled = settled;
    }

    public static UncommittedShare open(ManaPaymentReceipt receipt, int share) {
        return new UncommittedShare(receipt, share, false);
    }

    /** 没有拨出的子预算。提交和取消都不动父收据。 */
    public static UncommittedShare none() {
        return new UncommittedShare(null, 0, true);
    }

    public static Pool reserve(ManaPaymentReceipt receipt, int shareEach, int memberCount) {
        int safeCount = Math.max(0, memberCount);
        UncommittedShare[] members = new UncommittedShare[safeCount];
        for (int memberIndex = 0; memberIndex < safeCount; memberIndex++) {
            members[memberIndex] = open(receipt, shareEach);
        }
        return new Pool(members);
    }

    public void commit() {
        if (settled) {
            return;
        }
        settled = true;
        kept = true;
        com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.afterShareCommitted(this.receipt);
    }

    /** 提交并把这一笔从收据可退余额划走。已经终结的份额不会再划一次。 */
    public void commitRetained() {
        if (settled) {
            return;
        }
        settled = true;
        kept = true;
        if (receipt != null && share > 0) {
            receipt.retainShare(share);
        }
        com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime.afterShareCommitted(this.receipt);
    }

    public boolean kept() {
        return kept;
    }

    public void cancel(ServerPlayer player) {
        if (settled) {
            return;
        }
        settled = true;
        if (receipt != null && share > 0) {
            receipt.refundShare(player, share);
        }
    }

    public static final class Pool {
        private final UncommittedShare[] members;
        private int cursor;
        private boolean closed;

        private Pool(UncommittedShare[] members) {
            this.members = members;
        }

        public boolean isClosed() {
            return closed;
        }

        /** 关闭后不再拨出份额。调用方拿到 null 必须停，不能当成空份额继续放。 */
        public UncommittedShare take() {
            if (closed || cursor >= members.length) {
                return null;
            }
            UncommittedShare taken = members[cursor];
            cursor += 1;
            return taken;
        }

        /** 只退还没取走的份额，并封池。已经交给命中子组的份额不动。 */
        public void close(ServerPlayer player) {
            if (closed) {
                return;
            }
            closed = true;
            while (cursor < members.length) {
                members[cursor].cancel(player);
                cursor += 1;
            }
        }

        public void cancelUntaken(ServerPlayer player) {
            close(player);
        }

        public boolean keptAny() {
            for (UncommittedShare member : members) {
                if (member != null && member.kept()) {
                    return true;
                }
            }
            return false;
        }
    }
}

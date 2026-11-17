package com.bslx.XueXiMod.status.fire.seed;

/**
 * 同一拍里把不足的整数燃料按需求比例分完。
 * 先按比例向下取整，余数再按权重分；权重相同则用回合打乱次序，避免固定第一层长期独占。
 */
public final class FuelShare {
    private FuelShare() {
    }

    /** 燃料够就按需求全给；不够时先按比例取整，再把余下的整数逐个分给权重更高的需求。 */
    public static int[] divide(int balance, int[] demands, long[] identities, long turn) {
        if (demands == null || identities == null || demands.length != identities.length) {
            return new int[0];
        }
        int count = demands.length;
        int[] shares = new int[count];
        if (balance <= 0 || count == 0) {
            return shares;
        }
        long totalDemand = 0L;
        for (int cursor = 0; cursor < count; cursor++) {
            if (demands[cursor] > 0) {
                totalDemand += demands[cursor];
            }
        }
        if (totalDemand <= 0L) {
            return shares;
        }
        if (totalDemand <= balance) {
            for (int cursor = 0; cursor < count; cursor++) {
                if (demands[cursor] > 0) {
                    shares[cursor] = demands[cursor];
                }
            }
            return shares;
        }
        int[] weights = new int[count];
        long allocated = 0L;
        for (int cursor = 0; cursor < count; cursor++) {
            if (demands[cursor] <= 0) {
                continue;
            }
            long product = (long) demands[cursor] * (long) balance;
            shares[cursor] = (int) (product / totalDemand);
            weights[cursor] = (int) (product % totalDemand);
            allocated += shares[cursor];
        }
        int leftover = balance - (int) allocated;
        int[] order = new int[count];
        for (int cursor = 0; cursor < count; cursor++) {
            order[cursor] = cursor;
        }
        for (int cursor = 1; cursor < count; cursor++) {
            int hand = order[cursor];
            int scan = cursor - 1;
            while (scan >= 0 && sortsAfter(order[scan], hand, weights, identities, turn, demands)) {
                order[scan + 1] = order[scan];
                scan -= 1;
            }
            order[scan + 1] = hand;
        }
        int given = 0;
        for (int cursor = 0; cursor < count && given < leftover; cursor++) {
            int slot = order[cursor];
            if (demands[slot] > 0 && shares[slot] < demands[slot]) {
                shares[slot] += 1;
                given += 1;
            }
        }
        return shares;
    }

    private static boolean sortsAfter(int left, int right, int[] weights, long[] identities, long turn, int[] demands) {
        boolean leftIdle = demands[left] <= 0;
        boolean rightIdle = demands[right] <= 0;
        if (leftIdle != rightIdle) {
            return leftIdle;
        }
        if (weights[left] != weights[right]) {
            return weights[left] < weights[right];
        }
        return Long.compareUnsigned(mix(identities[left], turn), mix(identities[right], turn)) > 0;
    }

    static long mix(long identity, long turn) {
        long mixed = identity + turn * 0x9E3779B97F4A7C15L;
        mixed ^= mixed >>> 30;
        mixed *= 0xBF58476D1CE4E5B9L;
        mixed ^= mixed >>> 27;
        mixed *= 0x94D049BB133111EBL;
        mixed ^= mixed >>> 31;
        return mixed;
    }
}

package com.bslx.XueXiMod.shu.technique.release.fire;

/**
 * 引燃余数的轮换分配。比例向下取整与 FuelShare.divide 的前半段相同，
 * 余数改按层标识的稳定次序轮转，不再用回合散列。
 * 旧的 FuelShare.divide 不走这里。
 */
public final class RemainderRotation {
    private RemainderRotation() {
    }

    public static int[] share(int balance, int[] demands, long[] identities, long turn) {
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
        long allocated = 0L;
        for (int cursor = 0; cursor < count; cursor++) {
            if (demands[cursor] <= 0) {
                continue;
            }
            long product = (long) demands[cursor] * (long) balance;
            shares[cursor] = (int) (product / totalDemand);
            allocated += shares[cursor];
        }
        int leftover = balance - (int) allocated;
        int[] ring = new int[count];
        int ringCount = 0;
        for (int cursor = 0; cursor < count; cursor++) {
            if (demands[cursor] > 0) {
                ring[ringCount] = cursor;
                ringCount += 1;
            }
        }
        for (int cursor = 1; cursor < ringCount; cursor++) {
            int hand = ring[cursor];
            int scan = cursor - 1;
            while (scan >= 0 && laterIdentity(ring[scan], hand, identities)) {
                ring[scan + 1] = ring[scan];
                scan -= 1;
            }
            ring[scan + 1] = hand;
        }
        if (ringCount == 0 || leftover <= 0) {
            return shares;
        }
        int start = ringStart(turn, ringCount);
        for (int given = 0; given < leftover; given++) {
            int seat = start + given;
            if (seat >= ringCount) {
                seat -= ringCount;
            }
            int slot = ring[seat];
            if (shares[slot] < demands[slot]) {
                shares[slot] += 1;
            }
        }
        return shares;
    }

    private static boolean laterIdentity(int left, int right, long[] identities) {
        int compared = Long.compareUnsigned(identities[left], identities[right]);
        if (compared != 0) {
            return compared > 0;
        }
        return left > right;
    }

    private static int ringStart(long turn, int count) {
        long remainder = turn % count;
        if (remainder < 0L) {
            remainder += count;
        }
        return (int) remainder;
    }
}

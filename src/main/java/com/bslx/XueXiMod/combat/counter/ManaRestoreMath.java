package com.bslx.XueXiMod.combat.counter;

/**
 * 玩家池补蓝的纯计算。用差值而不是 int 相加，避免接近上限时溢出。
 * 不读法杖，也不决定存盘。
 */
public final class ManaRestoreMath {
    private ManaRestoreMath() {
    }

    public static int accepted(int current, int upperBound, int requested) {
        if (requested <= 0) {
            return 0;
        }
        long room = (long) upperBound - (long) current;
        if (room <= 0L) {
            return 0;
        }
        long granted = requested;
        if (room < granted) {
            granted = room;
        }
        if (granted <= 0L || granted > Integer.MAX_VALUE) {
            return 0;
        }
        return (int) granted;
    }
}

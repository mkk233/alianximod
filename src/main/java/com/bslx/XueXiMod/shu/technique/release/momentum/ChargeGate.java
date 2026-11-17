package com.bslx.XueXiMod.shu.technique.release.momentum;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 蓄力会话。死亡、下线、换维度和术式自己的打断都走 {@link #interrupt}。 */
public final class ChargeGate {
    private static final ConcurrentHashMap<UUID, Integer> EPOCH = new ConcurrentHashMap<>();
    private static final Set<UUID> ARMED = ConcurrentHashMap.newKeySet();

    private ChargeGate() {
    }

    /** 这位玩家的蓄力被打断过几次。 */
    public static int epoch(UUID playerId) {
        return EPOCH.getOrDefault(playerId, 0);
    }

    /** 这次蓄力的代数还是不是当前代数。 */
    public static boolean same(UUID playerId, int epoch) {
        return epoch(playerId) == epoch;
    }

    /** 蓄力是不是还处于武装状态。 */
    public static boolean armed(UUID playerId) {
        return ARMED.contains(playerId);
    }

    /** 把这位玩家的蓄力标成武装。 */
    public static void arm(UUID playerId) {
        ARMED.add(playerId);
    }

    /** 去掉武装，不增加打断代数。 */
    public static void disarm(UUID playerId) {
        ARMED.remove(playerId);
    }

    /** 代数加一、去掉武装，并释放这位玩家还没提交的水势预留。 */
    public static void interrupt(UUID playerId) {
        EPOCH.merge(playerId, 1, Integer::sum);
        ARMED.remove(playerId);
        WaterMomentum.releaseAll(playerId);
    }
}

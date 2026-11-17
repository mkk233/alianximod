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

    public static int epoch(UUID playerId) {
        return EPOCH.getOrDefault(playerId, 0);
    }

    public static boolean same(UUID playerId, int epoch) {
        return epoch(playerId) == epoch;
    }

    public static boolean armed(UUID playerId) {
        return ARMED.contains(playerId);
    }

    public static void arm(UUID playerId) {
        ARMED.add(playerId);
    }

    public static void disarm(UUID playerId) {
        ARMED.remove(playerId);
    }

    public static void interrupt(UUID playerId) {
        EPOCH.merge(playerId, 1, Integer::sum);
        ARMED.remove(playerId);
        WaterMomentum.releaseAll(playerId);
    }
}

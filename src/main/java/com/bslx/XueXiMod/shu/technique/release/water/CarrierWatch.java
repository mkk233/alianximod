package com.bslx.XueXiMod.shu.technique.release.water;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 主手栈引用和选中槽的观察。相等看引用，不看物品注册名，也不看栈里的耐久或 NBT。
 * 同一个 tick 内先换走又完全换回、中间没有观察时，下一次观察仍看见原引用，不会记成变化。
 */
public final class CarrierWatch {
    private static final ConcurrentHashMap<UUID, Seen> SEEN = new ConcurrentHashMap<>();

    private CarrierWatch() {
    }

    public static int observe(UUID playerId, Object stackRef, int selectedSlot) {
        if (playerId == null) {
            return 0;
        }
        Seen seen = SEEN.computeIfAbsent(playerId, ignored -> new Seen());
        if (!seen.started) {
            seen.started = true;
            seen.stackRef = stackRef;
            seen.selectedSlot = selectedSlot;
            seen.generation = 0;
            return 0;
        }
        if (seen.stackRef != stackRef || seen.selectedSlot != selectedSlot) {
            seen.generation++;
            seen.stackRef = stackRef;
            seen.selectedSlot = selectedSlot;
            WaterBeatLedger.clearPlayer(playerId);
        }
        return seen.generation;
    }

    public static boolean allows(UUID playerId, CastSource source) {
        if (playerId == null || source == null) {
            return false;
        }
        Seen seen = SEEN.get(playerId);
        if (seen == null || !seen.started) {
            return false;
        }
        return source.sameCarrier(seen.stackRef, seen.selectedSlot, seen.generation);
    }

    public static int generation(UUID playerId) {
        Seen seen = playerId == null ? null : SEEN.get(playerId);
        if (seen == null || !seen.started) {
            return 0;
        }
        return seen.generation;
    }

    public static void clearPlayer(UUID playerId) {
        if (playerId != null) {
            SEEN.remove(playerId);
        }
    }

    public static void clearAll() {
        SEEN.clear();
    }

    private static final class Seen {
        private boolean started;
        private Object stackRef;
        private int selectedSlot;
        private int generation;
    }
}

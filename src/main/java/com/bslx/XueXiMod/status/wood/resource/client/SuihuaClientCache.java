package com.bslx.XueXiMod.status.wood.resource.client;

import com.bslx.XueXiMod.status.wood.resource.SuihuaSnapshot;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 客户端只读岁华缓存。不引用 Minecraft 客户端类，专用服务器即使加载这一类也不会去拉客户端。
 * 服务端账本不读这里。断线时由整合作者调用 clearAll。
 */
public final class SuihuaClientCache {
    private static final Map<UUID, SuihuaSnapshot> SNAPSHOTS = new HashMap<>();

    private SuihuaClientCache() {
    }

    public static void replace(UUID ownerId, int peach, int bamboo, int wisteria, int plum, int orchid, int higan, int rose) {
        if (ownerId == null) {
            return;
        }
        SNAPSHOTS.put(ownerId, SuihuaSnapshot.clamped(peach, bamboo, wisteria, plum, orchid, higan, rose));
    }

    public static void clearOwner(UUID ownerId) {
        if (ownerId != null) {
            SNAPSHOTS.remove(ownerId);
        }
    }

    public static void clearAll() {
        SNAPSHOTS.clear();
    }

    public static boolean hasOwner(UUID ownerId) {
        return ownerId != null && SNAPSHOTS.containsKey(ownerId);
    }

    public static SuihuaSnapshot read(UUID ownerId) {
        if (ownerId == null) {
            return SuihuaSnapshot.empty();
        }
        SuihuaSnapshot snapshot = SNAPSHOTS.get(ownerId);
        return snapshot == null ? SuihuaSnapshot.empty() : snapshot;
    }
}

package com.bslx.XueXiMod.shu.technique.release.batch;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次真实施法的目标去重。分支切账和账本退休都不会清掉。
 * 产种实现由后续作者调用 {@link #claim(UUID, UUID)}，这里只保留机会。
 */
public final class RootSeedLedger {
    private static final ConcurrentHashMap<UUID, Set<UUID>> CLAIMED = new ConcurrentHashMap<>();

    private RootSeedLedger() {
    }

    /** 这个目标第一次领取产种机会时为真。 */
    public static boolean claim(UUID rootCastId, UUID targetId) {
        if (rootCastId == null || targetId == null) {
            return false;
        }
        Set<UUID> targets = CLAIMED.computeIfAbsent(rootCastId, ignored -> ConcurrentHashMap.newKeySet());
        return targets.add(targetId);
    }

    /** 这本账上该目标是否已经领过。 */
    public static boolean alreadyClaimed(UUID rootCastId, UUID targetId) {
        if (rootCastId == null || targetId == null) {
            return false;
        }
        Set<UUID> targets = CLAIMED.get(rootCastId);
        return targets != null && targets.contains(targetId);
    }

    /** 根组结束时摘掉表项。已经记过的目标不会因此重新获得一次产种。 */
    public static void release(UUID rootCastId) {
        if (rootCastId != null) {
            CLAIMED.remove(rootCastId);
        }
    }
}

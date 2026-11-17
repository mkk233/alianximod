package com.bslx.XueXiMod.shu.technique.release.batch;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * 一个实际载体副本的命中钟。本体和留焰片段共用这一只，父子各有各的。
 * 间隔不大于 0 时，每个目标只记一次。
 */
public final class HitClock {
    private final UUID clockId = UUID.randomUUID();
    private final Set<UUID> seenTargets = new HashSet<>();
    private final Map<UUID, Long> lastGameTime = new HashMap<>();

    public UUID clockId() {
        return clockId;
    }

    /** 间隔内或已经记过一次时拒绝。通过后写下这次时间。 */
    public boolean tryMark(UUID targetId, long gameTime, int intervalTicks) {
        if (targetId == null) {
            return false;
        }
        if (intervalTicks <= 0) {
            return seenTargets.add(targetId);
        }
        Long previousTime = lastGameTime.get(targetId);
        if (previousTime != null && gameTime - previousTime < intervalTicks) {
            return false;
        }
        lastGameTime.put(targetId, gameTime);
        seenTargets.add(targetId);
        return true;
    }

    /** 这个目标是否已经进过这只命中钟。 */
    public boolean alreadySeen(UUID targetId) {
        return targetId != null && seenTargets.contains(targetId);
    }

    /** 不写入。间隔内或已经记过一次的目标算占用。 */
    public boolean occupied(UUID targetId, long gameTime, int intervalTicks) {
        if (targetId == null) {
            return true;
        }
        if (intervalTicks <= 0) {
            return seenTargets.contains(targetId);
        }
        Long previousTime = lastGameTime.get(targetId);
        return previousTime != null && gameTime - previousTime < intervalTicks;
    }
}

package com.bslx.XueXiMod.shu.technique.release.entity;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 按实体 UUID 保留一份只读快照。相同 UUID 再次放入直接拒绝，避免同步刷新重播。
 * 这里不生成实体，也不处理命中。命中实体正常移除不会从这里删掉仍在播放的记录。
 */
public final class StillSheetCache {
    private static final List<StillSheetSnapshot> NO_SNAPSHOTS = List.of();
    private final Map<UUID, StillSheetSnapshot> retained = new LinkedHashMap<>();

    /** 描述不完整或这个实体已经留过快照时拒绝。 */
    public boolean offer(StillSheetSnapshot incoming) {
        if (incoming == null || incoming.entityId() == null || incoming.birthGameTime() < 0L || incoming.frameCount() <= 0) {
            return false;
        }
        if (incoming.texturePath() == null || incoming.texturePath().isEmpty()) {
            return false;
        }
        if (this.retained.containsKey(incoming.entityId())) {
            return false;
        }
        this.retained.put(incoming.entityId(), incoming);
        return true;
    }

    public StillSheetSnapshot find(UUID entityId) {
        if (entityId == null) {
            return null;
        }
        return this.retained.get(entityId);
    }

    /** 取消这个实体还留着的快照。 */
    public boolean cancel(UUID entityId) {
        if (entityId == null) {
            return false;
        }
        return this.retained.remove(entityId) != null;
    }

    public void clear() {
        this.retained.clear();
    }

    public int size() {
        return this.retained.size();
    }

    public List<StillSheetSnapshot> copyRetained() {
        if (this.retained.isEmpty()) {
            return NO_SNAPSHOTS;
        }
        return new ArrayList<>(this.retained.values());
    }

    /** 播完或出生时间异常超前的记录删掉。 */
    public int dropExpired(long gameTime, float partialTick) {
        int removed = 0;
        Iterator<Map.Entry<UUID, StillSheetSnapshot>> iterator = this.retained.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StillSheetSnapshot> entry = iterator.next();
            StillSheetSnapshot snapshot = entry.getValue();
            boolean finished = StillSheetClock.expired(gameTime, partialTick, snapshot.birthGameTime(),
                    snapshot.frameCount(), snapshot.ticksPerFrame());
            boolean tooEarly = StillSheetClock.stranded(gameTime, partialTick, snapshot.birthGameTime());
            if (finished || tooEarly) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }

    /** 不是这个世界的快照删掉。世界标识为空时全部删掉。 */
    public int dropOtherWorlds(String worldIdentity) {
        int removed = 0;
        Iterator<Map.Entry<UUID, StillSheetSnapshot>> iterator = this.retained.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, StillSheetSnapshot> entry = iterator.next();
            if (worldIdentity == null || !worldIdentity.equals(entry.getValue().worldIdentity())) {
                iterator.remove();
                removed++;
            }
        }
        return removed;
    }
}

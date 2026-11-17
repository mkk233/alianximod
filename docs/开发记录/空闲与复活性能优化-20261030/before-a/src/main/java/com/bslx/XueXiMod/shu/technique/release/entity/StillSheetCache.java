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
    private final Map<UUID, StillSheetSnapshot> retained = new LinkedHashMap<>();

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
        return new ArrayList<>(this.retained.values());
    }

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

package com.bslx.XueXiMod.animation.runtime;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 每个主人最多保留 256 条攻击组记录，默认 1200 个服务端 tick 后过期。
 * 正常结束只把记录标成终态；死亡、离线、换维和停服才整组清掉。
 */
public final class GroupReplayLedger {
    public static final int OWNER_LIMIT = 256;
    public static final long RETAIN_TICKS = 1200L;

    private static final Map<UUID, ArrayDeque<Mark>> MARKS = new HashMap<>();

    private GroupReplayLedger() {
    }

    public static void remember(UUID ownerId, UUID groupId, long serverTick, boolean terminal) {
        if (ownerId == null || groupId == null) {
            return;
        }
        expire(ownerId, serverTick);
        ArrayDeque<Mark> marks = MARKS.computeIfAbsent(ownerId, ignored -> new ArrayDeque<>());
        while (marks.size() >= OWNER_LIMIT) {
            marks.removeFirst();
        }
        marks.addLast(new Mark(groupId, serverTick + RETAIN_TICKS, terminal));
    }

    public static boolean covers(UUID ownerId, UUID groupId, long serverTick) {
        if (ownerId == null || groupId == null) {
            return false;
        }
        expire(ownerId, serverTick);
        ArrayDeque<Mark> marks = MARKS.get(ownerId);
        if (marks == null) {
            return false;
        }
        for (Mark mark : marks) {
            if (groupId.equals(mark.groupId())) {
                return true;
            }
        }
        return false;
    }

    public static void markTerminal(UUID ownerId, UUID groupId) {
        ArrayDeque<Mark> marks = MARKS.get(ownerId);
        if (marks == null || groupId == null) {
            return;
        }
        for (Mark mark : marks) {
            if (groupId.equals(mark.groupId())) {
                mark.markTerminal();
            }
        }
    }

    public static int retained(UUID ownerId) {
        ArrayDeque<Mark> marks = MARKS.get(ownerId);
        return marks == null ? 0 : marks.size();
    }

    public static void expire(UUID ownerId, long serverTick) {
        ArrayDeque<Mark> marks = MARKS.get(ownerId);
        if (marks == null) {
            return;
        }
        marks.removeIf(mark -> serverTick >= mark.expireTick());
        if (marks.isEmpty()) {
            MARKS.remove(ownerId);
        }
    }

    public static void expireAll(long serverTick) {
        for (UUID ownerId : List.copyOf(MARKS.keySet())) {
            expire(ownerId, serverTick);
        }
    }

    public static void clearOwner(UUID ownerId) {
        if (ownerId != null) {
            MARKS.remove(ownerId);
        }
    }

    public static void clearAll() {
        MARKS.clear();
    }

    public static final class Mark {
        private final UUID groupId;
        private final long expireTick;
        private boolean terminal;

        private Mark(UUID groupId, long expireTick, boolean terminal) {
            this.groupId = groupId;
            this.expireTick = expireTick;
            this.terminal = terminal;
        }

        public UUID groupId() {
            return this.groupId;
        }

        public long expireTick() {
            return this.expireTick;
        }

        public boolean terminal() {
            return this.terminal;
        }

        private void markTerminal() {
            this.terminal = true;
        }
    }
}

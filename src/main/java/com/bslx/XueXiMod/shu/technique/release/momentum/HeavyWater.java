package com.bslx.XueXiMod.shu.technique.release.momentum;

import net.minecraft.server.level.ServerPlayer;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 旧重水扣费已经停用。新的进入不再预留，也不再提交水势。
 * 进入、当前会话、第一次提交、结束和清当前这些签名都留着，会话字段也留着，调用链不用拆。
 * 单价和计划份数仍留在进入签名上，但不再换成预算。
 * 早先已经握在会话上、尚未提交的预留，只由原持有人释放，不改余额。
 * 新会话一律不是重水。深海领域不走这里。
 */
public final class HeavyWater {
    private static final ConcurrentHashMap<String, Session> OPEN = new ConcurrentHashMap<>();
    private static final ThreadLocal<Session> CURRENT = new ThreadLocal<>();

    private HeavyWater() {
    }

    /** 按分组取出已有会话，没有就建一个零额、非重水的会话，并设成当前会话。 */
    public static Session enter(ServerPlayer player, String groupKey, float perCopy, int plannedCopies) {
        Session existing = OPEN.get(groupKey);
        if (existing != null) {
            CURRENT.set(existing);
            return existing;
        }
        WaterMomentum.Hold hold = new WaterMomentum.Hold(player.getUUID(), 0f);
        Session session = new Session(groupKey, hold, false, UUID.randomUUID());
        OPEN.put(groupKey, session);
        CURRENT.set(session);
        return session;
    }

    /** 当前线程正在用的会话。 */
    public static Session current() {
        return CURRENT.get();
    }

    /**
     * 不再把预留提交成扣费。会话若仍是旧的重水并且预留还在，只在调用者就是这笔预留的原持有人时释放。
     * 持有人不符就留给结束时按未提交预留释放，不从余额里扣。
     */
    public static void commitFirst(ServerPlayer player) {
        Session session = CURRENT.get();
        if (session == null || session.committed || !session.heavy) {
            return;
        }
        if (player == null || session.hold == null || !session.hold.playerId.equals(player.getUUID())) {
            return;
        }
        WaterMomentum.release(session.hold);
        session.committed = true;
    }

    /** 结束会话。还没提交时释放它的预留，并在它仍是当前会话时清掉当前标记。 */
    public static void finish(Session session) {
        if (session == null || session.closed) {
            return;
        }
        session.closed = true;
        OPEN.remove(session.groupKey, session);
        if (!session.committed) {
            WaterMomentum.release(session.hold);
        }
        if (CURRENT.get() == session) {
            CURRENT.remove();
        }
    }

    /** 只清当前线程的会话标记，不结束会话，也不释放预留。 */
    public static void clearCurrent() {
        CURRENT.remove();
    }

    public static final class Session {
        final String groupKey;
        final WaterMomentum.Hold hold;
        final boolean heavy;
        final UUID groupId;
        boolean committed;
        boolean closed;

        Session(String groupKey, WaterMomentum.Hold hold, boolean heavy, UUID groupId) {
            this.groupKey = groupKey;
            this.hold = hold;
            this.heavy = heavy;
            this.groupId = groupId;
        }

        public boolean heavy() {
            return this.heavy;
        }

        public UUID groupId() {
            return this.groupId;
        }
    }
}

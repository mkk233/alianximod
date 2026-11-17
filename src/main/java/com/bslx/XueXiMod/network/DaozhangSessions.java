package com.bslx.XueXiMod.network;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 序号只属于当前菜单。关窗后会话结束，不跟玩家跨窗口保存。
 */
public final class DaozhangSessions {
    private static final Map<UUID, Session> OPEN = new ConcurrentHashMap<>();

    private DaozhangSessions() {
    }

    public static UUID open(UUID playerId, UUID daozhangId) {
        if (playerId == null || daozhangId == null) {
            return null;
        }
        UUID nonce = UUID.randomUUID();
        OPEN.put(playerId, new Session(nonce, daozhangId, -1));
        return nonce;
    }

    public static boolean accept(UUID playerId, UUID nonce, UUID daozhangId, int sequence) {
        if (playerId == null || nonce == null || daozhangId == null || sequence < 0) {
            return false;
        }
        Session[] accepted = new Session[1];
        boolean valid = OPEN.computeIfPresent(playerId, (id, session) -> {
            if (!session.nonce.equals(nonce) || !session.daozhangId.equals(daozhangId)
                    || sequence <= session.lastSequence) {
                return session;
            }
            Session next = new Session(session.nonce, session.daozhangId, sequence);
            accepted[0] = next;
            return next;
        }) != null && accepted[0] != null;
        return valid;
    }

    public static void close(UUID playerId, UUID nonce) {
        if (playerId != null && nonce != null) {
            OPEN.computeIfPresent(playerId, (id, session) -> session.nonce.equals(nonce) ? null : session);
        }
    }

    private record Session(UUID nonce, UUID daozhangId, int lastSequence) {
    }
}

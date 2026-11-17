package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.status.fire.seed.FireCastContext;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 一次真实释放只持有一份 {@link FireCastContext}，标识就是根释放标识。
 * 费用结算不清这份账。存活攻击、未结束火径和待出生子段各自持有一次，最后一次释放后才从表里拿掉。
 */
public final class RootCastBindings {
    private static final ConcurrentHashMap<UUID, Hold> OPEN = new ConcurrentHashMap<>();

    private RootCastBindings() {
    }

    public static final class Hold {
        private final FireCastContext context;
        private int holders;

        private Hold(FireCastContext context) {
            this.context = context;
        }

        public FireCastContext context() {
            return context;
        }

        public int holders() {
            return holders;
        }
    }

    /** 打开根账并记一次会话持有。重复打开同一标识时返回已有上下文，不另建一本账。 */
    public static FireCastContext open(UUID rootCastId, UUID casterId) {
        if (rootCastId == null) {
            return null;
        }
        Hold created = new Hold(new FireCastContext(rootCastId, casterId));
        Hold present = OPEN.putIfAbsent(rootCastId, created);
        Hold hold = present == null ? created : present;
        hold.holders++;
        return hold.context;
    }

    /** 武器或载体已经有上下文时放进同一张表，供火径结束后释放。 */
    public static void adopt(FireCastContext context) {
        if (context == null || context.castId() == null) {
            return;
        }
        Hold created = new Hold(context);
        Hold present = OPEN.putIfAbsent(context.castId(), created);
        Hold hold = present == null ? created : present;
        if (present != null && present.context != context) {
            return;
        }
        hold.holders++;
    }

    public static FireCastContext lookup(UUID rootCastId) {
        Hold hold = rootCastId == null ? null : OPEN.get(rootCastId);
        return hold == null ? null : hold.context;
    }

    /** 多记一次持有。没有这本账时不变。 */
    public static void retain(UUID rootCastId) {
        Hold hold = rootCastId == null ? null : OPEN.get(rootCastId);
        if (hold != null) {
            hold.holders++;
        }
    }

    /** 持有归零后从表里拿掉。 */
    public static void release(UUID rootCastId) {
        if (rootCastId == null) {
            return;
        }
        OPEN.computeIfPresent(rootCastId, (ignored, hold) -> {
            hold.holders--;
            return hold.holders <= 0 ? null : hold;
        });
    }
}

package com.bslx.XueXiMod.shu.technique.release.deflection.melee;

import java.util.IdentityHashMap;

/**
 * 同一次受击事件的重入账。键是事件对象本身，不用玩家加 tick。
 * 史莱姆下一次 dealDamage 是另一个事件，不受这里挡住。
 * 服务端收尾时丢掉引用，避免一直抓着事件。
 */
public final class DirectMeleeAttempts {
    private static final ThreadLocal<IdentityHashMap<Object, Boolean>> SEEN =
            ThreadLocal.withInitial(IdentityHashMap::new);

    private DirectMeleeAttempts() {
    }

    /** 这个事件对象是否已经进入过补充入口。 */
    public static boolean seen(Object eventIdentity) {
        return eventIdentity != null && SEEN.get().containsKey(eventIdentity);
    }

    /** 记下这个事件对象，防止同一次重入再买一单。 */
    public static void mark(Object eventIdentity) {
        if (eventIdentity == null) {
            return;
        }
        SEEN.get().put(eventIdentity, Boolean.TRUE);
    }

    /** 服务端收尾时丢掉事件引用。 */
    public static void discard() {
        SEEN.remove();
    }
}

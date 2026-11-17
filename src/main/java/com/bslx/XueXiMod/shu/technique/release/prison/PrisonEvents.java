package com.bslx.XueXiMod.shu.technique.release.prison;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/** 同线程把牢的创建和结束交给监听方。单个回调失败不影响牢是否已经建立，也不回滚耗湿。 */
public final class PrisonEvents {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final ArrayList<Listener> LISTENERS = new ArrayList<>();

    private PrisonEvents() {
    }

    /** 登记同一线程的创建和结束监听。重复或空监听不入表。 */
    public static void register(Listener listener) {
        if (listener == null || LISTENERS.contains(listener)) {
            return;
        }
        LISTENERS.add(listener);
    }

    public interface Listener {
        /** 牢已经建立后通知。回调失败不撤销牢，也不退款。 */
        void created(PrisonView view);

        /** 牢已经结束后通知。回调失败不改变结束结果。 */
        void ended(PrisonView view, EndReason reason);
    }

    static void created(PrisonView view) {
        if (view == null) {
            return;
        }
        for (Listener listener : copy()) {
            try {
                listener.created(view);
            } catch (RuntimeException failure) {
                LOGGER.error("water prison created listener failed for {}", view.prisonId(), failure);
            }
        }
    }

    static void ended(PrisonView view, EndReason reason) {
        if (view == null) {
            return;
        }
        EndReason actual = reason == null ? EndReason.REMOVED : reason;
        for (Listener listener : copy()) {
            try {
                listener.ended(view, actual);
            } catch (RuntimeException failure) {
                LOGGER.error("water prison ended listener failed for {}", view.prisonId(), failure);
            }
        }
    }

    private static List<Listener> copy() {
        return new ArrayList<>(LISTENERS);
    }
}

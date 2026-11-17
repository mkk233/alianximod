package com.bslx.XueXiMod.shu.technique.release.event;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Iterator;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BooleanSupplier;

/**
 * 有期限的通用事件窗口。一个归属同时只保留一个窗口，区间是
 * {@code [startTick, startTick + durationTicks)}。到期、死亡、离线、换维和停止时撤销。
 */
public final class ResponseWindowService {
    private static final ConcurrentHashMap<UUID, Window> WINDOWS = new ConcurrentHashMap<>();

    private ResponseWindowService() {
    }

    /** 开启或替换这个归属的窗口。旧窗口先撤销。 */
    public static void open(UUID ownerId, String windowId, long startTick, int durationTicks,
                            BooleanSupplier active, Runnable onClose) {
        if (ownerId == null || windowId == null || windowId.isBlank() || durationTicks <= 0 || active == null) {
            return;
        }
        Window opened = new Window(ownerId, windowId, startTick, durationTicks, active, onClose);
        Window replaced = WINDOWS.put(ownerId, opened);
        if (replaced != null && replaced != opened) {
            replaced.closeOnce();
        }
    }

    /** 这个归属的窗口现在是否仍在区间内。到期或失效时会撤销。 */
    public static boolean open(UUID ownerId, long gameTick) {
        return open(ownerId, null, gameTick);
    }

    /** 只检查指定窗口。标识不符时不撤销别人的窗口。 */
    public static boolean open(UUID ownerId, String windowId, long gameTick) {
        Window window = WINDOWS.get(ownerId);
        if (window == null) {
            return false;
        }
        if (windowId != null && !windowId.equals(window.windowId)) {
            return false;
        }
        if (!window.live(gameTick)) {
            close(ownerId);
            return false;
        }
        return true;
    }

    /** 撤销这个归属的窗口。没有窗口时什么都不做。 */
    public static void close(UUID ownerId) {
        if (ownerId == null) {
            return;
        }
        Window window = WINDOWS.remove(ownerId);
        if (window != null) {
            window.closeOnce();
        }
    }

    /** 每拍收掉到期或归属失效的窗口。不提前关闭仍在区间内的窗口。 */
    public static void tick(MinecraftServer server) {
        if (server == null || WINDOWS.isEmpty()) {
            return;
        }
        ServerLevel overworld = server.overworld();
        long gameTick = overworld == null ? Long.MIN_VALUE : overworld.getGameTime();
        Iterator<Window> windows = WINDOWS.values().iterator();
        while (windows.hasNext()) {
            Window window = windows.next();
            if (window.live(gameTick)) {
                continue;
            }
            windows.remove();
            window.closeOnce();
        }
    }

    /** 清掉全部窗口。停止时调用。 */
    public static void clear() {
        for (Window window : WINDOWS.values()) {
            window.closeOnce();
        }
        WINDOWS.clear();
    }

    private static final class Window {
        private final UUID ownerId;
        private final String windowId;
        private final long startTick;
        private final int durationTicks;
        private final BooleanSupplier active;
        private final Runnable onClose;
        private boolean closed;

        private Window(UUID ownerId, String windowId, long startTick, int durationTicks,
                       BooleanSupplier active, Runnable onClose) {
            this.ownerId = ownerId;
            this.windowId = windowId;
            this.startTick = startTick;
            this.durationTicks = durationTicks;
            this.active = active;
            this.onClose = onClose;
        }

        private boolean live(long gameTick) {
            if (closed || gameTick < startTick || gameTick >= startTick + durationTicks) {
                return false;
            }
            try {
                return active.getAsBoolean();
            } catch (RuntimeException failure) {
                return false;
            }
        }

        private void closeOnce() {
            if (closed) {
                return;
            }
            closed = true;
            if (onClose == null) {
                return;
            }
            try {
                onClose.run();
            } catch (RuntimeException failure) {
                return;
            }
        }
    }
}

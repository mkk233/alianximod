package com.bslx.XueXiMod.debug;

import com.mojang.logging.LogUtils;
import net.minecraftforge.fml.loading.FMLLoader;
import org.slf4j.Logger;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * 本地调试记录。关闭时不保留任何内容。
 */
public final class DebugLog {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final int CAPACITY = 200;
    private static final ArrayDeque<String> LINES = new ArrayDeque<>();
    private static volatile boolean enabled = !FMLLoader.isProduction();

    private DebugLog() {
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setEnabled(boolean value) {
        enabled = value;
        if (!value) {
            clear();
        }
    }

    public static void log(String message) {
        if (!enabled) {
            return;
        }
        synchronized (LINES) {
            LINES.addLast(message);
            while (LINES.size() > CAPACITY) {
                LINES.removeFirst();
            }
        }
        LOGGER.info("[调试] {}", message);
    }

    public static List<String> lines() {
        synchronized (LINES) {
            return new ArrayList<>(LINES);
        }
    }

    public static void clear() {
        synchronized (LINES) {
            LINES.clear();
        }
    }
}

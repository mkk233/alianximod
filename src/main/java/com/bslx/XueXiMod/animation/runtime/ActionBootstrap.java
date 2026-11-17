package com.bslx.XueXiMod.animation.runtime;

import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;

import com.bslx.XueXiMod.animation.runtime.loader.DevActionLoader;
import com.bslx.XueXiMod.animation.runtime.loader.DevLoadResult;
import com.mojang.logging.LogUtils;

/** 发行默认只注册原创兜底。授权目录只在 runClient 显式传入时读取。 */
public final class ActionBootstrap {
    public static final String DEV_PROPERTY = "xueximod.devEpicFightAnimations";
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final AtomicBoolean STARTED = new AtomicBoolean();

    private ActionBootstrap() {
    }

    public static void load() {
        if (!STARTED.compareAndSet(false, true)) {
            return;
        }
        OriginalHumanoid.register();
        String configured = System.getProperty(DEV_PROPERTY);
        if (configured == null || configured.isBlank()) {
            note("授权动作未启用。没有设置 " + DEV_PROPERTY + "。清单中的 118 个授权 ID 都不可用。当前可播的是原创水刃兜底和示例，它们不是上游动作。");
            return;
        }
        Path directory = Path.of(configured);
        if (!directory.isAbsolute()) {
            note("授权动作未启用。开发目录不是绝对路径，118 个授权 ID 都不可用：" + configured);
            return;
        }
        try {
            DevLoadResult result = DevActionLoader.load(directory);
            ActionRegistry.publishDev(result);
            ActionRegistry.noteGate(result.summaryText());
            LOGGER.info("授权动作启用：{}", result.summaryText());
            LOGGER.info("数值对照、刚体适配、游戏观感都还没完成。降阶条目不能当成完整上游还原。原创兜底不是上游动作。");
            for (ActionStatus status : result.statuses()) {
                if (!status.posePlayable()) {
                    LOGGER.info("授权动作不可播放：{}", status.reportLine());
                }
            }
        } catch (RuntimeException failure) {
            note("授权动作加载失败，118 个授权 ID 不可用，原创兜底仍在：" + failure.getMessage());
        }
    }

    private static void note(String text) {
        ActionRegistry.noteGate(text);
        LOGGER.info(text);
    }
}

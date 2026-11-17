package com.bslx.XueXiMod.status.fire.furnace;

import com.mojang.logging.LogUtils;
import net.minecraft.world.entity.player.Player;
import org.slf4j.Logger;

/** 炉心修复日志。只记录运行态和效果对不上时做了什么。 */
final class FurnaceLog {
    private static final Logger LOGGER = LogUtils.getLogger();

    private FurnaceLog() {
    }

    /** 记下这次对哪位玩家做了修复。 */
    static void repair(String text, Player player) {
        LOGGER.warn("[furnace] {} player={}", text, player.getUUID());
    }
}

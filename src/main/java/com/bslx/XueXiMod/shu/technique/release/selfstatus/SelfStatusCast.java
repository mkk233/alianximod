package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import com.bslx.XueXiMod.status.fire.furnace.FurnaceMath;
import net.minecraft.server.level.ServerPlayer;

/**
 * 自身状态的一次提交行为。链上只看这个能力，不按术式编号分支。
 * {@link #prepare} 只预检。扣款、安装和安装后的视觉由提交器按顺序调用准备结果。
 * {@link #action} 只留给炉心三式的既有注册检查；提交器不读它，有限效果保持空。
 */
public interface SelfStatusCast {
    /** 只预检这次状态变更。通过时给出可提交对象，不扣款。 */
    StatusPreparation prepare(ServerPlayer player);

    /** 炉心三式的既有动作。提交器不读它。 */
    default FurnaceMath.Action action() {
        return null;
    }
}

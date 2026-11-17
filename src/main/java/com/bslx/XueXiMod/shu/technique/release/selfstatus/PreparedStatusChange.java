package com.bslx.XueXiMod.shu.technique.release.selfstatus;

import net.minecraft.server.level.ServerPlayer;

/**
 * 一次已经预检过的状态变更。
 * {@link #recheck} 在扣款前再确认；返回空表示仍可安装。
 * {@link #commit} 才真正安装。{@link #afterCommit} 只做安装成功后的视觉，失败不得当成状态没装上。
 */
public interface PreparedStatusChange {
    /** 扣款前再确认。返回空表示仍可安装。 */
    String recheck(ServerPlayer player);

    /** 真正安装状态。失败由提交器退款。 */
    boolean commit(ServerPlayer player);

    /** 安装成功后的视觉。这里失败不撤销状态、不退款。 */
    void afterCommit(ServerPlayer player);

    /** 安装失败时给玩家的提示。 */
    String failureText();
}

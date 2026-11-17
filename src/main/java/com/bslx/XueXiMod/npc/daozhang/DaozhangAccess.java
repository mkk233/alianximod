package com.bslx.XueXiMod.npc.daozhang;

import net.minecraft.server.level.ServerPlayer;

/** 对话、拜师和学习共用的距离与视线。 */
public final class DaozhangAccess {
    public static final double TALK_DISTANCE = 6.0D;

    private DaozhangAccess() {
    }

    public static boolean canTalk(DaozhangEntity daozhang, ServerPlayer player) {
        if (daozhang == null || player == null || !daozhang.isAlive() || daozhang.isRemoved()) {
            return false;
        }
        if (!player.isAlive() || player.isRemoved() || player.isSpectator() || player.level() != daozhang.level()) {
            return false;
        }
        return player.distanceTo(daozhang) <= TALK_DISTANCE && daozhang.hasLineOfSight(player);
    }
}

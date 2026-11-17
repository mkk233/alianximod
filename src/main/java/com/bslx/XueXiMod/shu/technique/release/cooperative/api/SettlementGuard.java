package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;

/**
 * 本批显式账本的可选保护。旧调用不传它时行为保持不变。
 * 实现必须在每次记录和刷新时复查服务器会话、世界、父执行、主人和目标权限。
 * 取消丢弃尚未结算的载荷并关掉后续回调；已经结算的伤害不倒扣。
 * 允许判断不能把某一刻的布尔结果冻结进计划。
 */
public interface SettlementGuard {
    boolean active(ServerLevel level);

    boolean allows(ServerLevel level, LivingEntity target);

    void cancel(CancelReason reason);
}

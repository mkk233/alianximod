package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.cooperative.ExecutionFamilyPlan;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;

import java.util.UUID;

/**
 * 按服务器实例隔离的执行端口。实现类在整合阶段落地，不使用跨服静态队列。
 * start 拒绝不完整计划；tick 推进准备、动作门、成员提交和尾部结算。
 */
public interface FamilyExecutionRuntime {
    FamilyRuntimeHandle start(ExecutionFamilyPlan plan);

    void tick(ServerLevel level, long tick);

    void cancel(FamilyKey family, CancelReason reason);

    void cancelOwner(UUID serverSession, UUID ownerId, CancelReason reason);

    void cancelWorld(UUID serverSession, ResourceKey<Level> dimension, CancelReason reason);

    void stopServer(UUID serverSession, CancelReason reason);
}

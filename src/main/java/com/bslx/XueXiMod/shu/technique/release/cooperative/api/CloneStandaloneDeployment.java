package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.server.level.ServerPlayer;

/**
 * 普通分身独立部署端口，由分身核心实现。
 * {@link #deployStandalone} 是玩家路线唯一的扣款、发布、退款和生成冷却入口。
 * 实现内部再拆无副作用准备和已付费发布，供旧开发生成共用；开发生成不得调用这个方法绕过权限。
 * 来源不是 {@link DeploymentSource#PLAYER_STANDALONE} 时必须拒绝。
 * {@link #dismissStandalone} 只结束该主人具备普通部署能力的身体及其外部预约，返回实际结束数量，不扣钱、不重置已有生成冷却。
 */
public interface CloneStandaloneDeployment {
    CloneDeploymentResult deployStandalone(ServerPlayer owner, CloneDeploymentInput input);

    int dismissStandalone(ServerPlayer owner, StandaloneDeploymentKind kind);
}

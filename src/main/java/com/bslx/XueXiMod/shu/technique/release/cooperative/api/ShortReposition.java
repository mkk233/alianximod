package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 可复用短距移动端口，由分身核心实现。
 * 只能移动传入的分身句柄，不能把玩家当自动位移目标，也不依赖协同术式标识。
 * 每一实际步做保守包围盒扫掠，采样不超过 {@link CooperativeStructureLimits#SWEEP_SAMPLE_CAP}，
 * 不加载区块，不穿实体或实心方块。cancel 可重复调用。
 * {@code distanceLimit} 是这一步的实际位移，不能大于 {@link CooperativeStructureLimits#REPOSITION_DISTANCE_CAP}。
 * 大于 8 格的召回走 {@link RemoteRecall}，不要把冻结的 moveLimit 填进这个参数。
 */
public interface ShortReposition {
    RepositionAttempt prepare(ServerLevel level, CloneHandle handle, Vec3 destination, double distanceLimit,
                              int durationTicks);

    RepositionMotion advance(ServerLevel level, AcceptedReposition plan, long tick);

    void cancel(AcceptedReposition plan);
}

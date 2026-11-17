package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/**
 * 大于 8 格、且不超过冻结 moveLimit 的瞬时归位。由分身核心实现。
 * 只移动传入的普通 REPEAT 身体，不传送玩家，不生成新分身，不沿路径造成伤害或接触。
 * 验证和搬移在同一次调用里完成。成功结果表示身体已经位于目的站位；调用方随后才标 READY。
 * 失败返回拒绝，不把未检查完的尾部当成安全。预算不足不排队到下一刻。
 * 通道检查使用有界网格，不使用源盒与目标盒的大并集，也不加载区块。
 * 一次协同先做最多 4 具身体、每具 2 个站位的廉价检查；选定第一条端点合法的候选后只查这一条通道。
 * 端点与支撑合计不超过 {@link CooperativeStructureLimits#REMOTE_ENDPOINT_CELL_CAP} 个去重方块。
 * 这条通道单独不超过 512 个方块、128 次中心线推进、16 个区块和 64 个实体候选。
 * 全服远距检查共享 {@link RemoteRecallMeter} 的每刻额度，并仍消耗既有 32 个归位工作单位。
 */
public interface RemoteRecall {
    RecallAttempt recall(ServerLevel level, CloneHandle handle, Vec3 destination, double frozenMoveLimit);
}

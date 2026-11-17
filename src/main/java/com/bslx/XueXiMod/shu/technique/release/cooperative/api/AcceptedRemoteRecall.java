package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 同一服务端任务里已经完成有界检查并搬到目的站位的身体。
 * 调用方拿到这个结果后才标 READY。结果本身不缓存到下一刻。
 */
public record AcceptedRemoteRecall(UUID planId, CloneHandle handle, Vec3 destination, double travelBlocks,
                                    double frozenMoveLimit, RemoteSpaceGrant grant) implements RecallAttempt {
    public AcceptedRemoteRecall {
        if (planId == null || handle == null || grant == null) {
            throw new IllegalArgumentException("远距归位结果不完整");
        }
        destination = FrozenValues.finite(destination, "远距目的地");
        if (grant.phase() != RemoteCheckPhase.CHANNEL) {
            throw new IllegalArgumentException("远距归位必须持有通道额度");
        }
        if (!Double.isFinite(travelBlocks)
                || travelBlocks <= CooperativeStructureLimits.REPOSITION_DISTANCE_CAP
                || travelBlocks > CooperativeStructureLimits.MOVE_LIMIT_CAP) {
            throw new IllegalArgumentException("远距位移必须大于 8 格且不超过 64 格");
        }
        if (!Double.isFinite(frozenMoveLimit) || travelBlocks > frozenMoveLimit
                || frozenMoveLimit > CooperativeStructureLimits.MOVE_LIMIT_CAP) {
            throw new IllegalArgumentException("远距位移超过冻结归位上限");
        }
    }
}

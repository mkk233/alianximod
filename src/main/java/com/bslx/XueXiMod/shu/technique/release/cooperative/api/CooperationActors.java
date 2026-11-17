package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * 分身核心实现的协同端口。
 * tryReserve 只查主人索引，最多 {@link CooperativeStructureLimits#OWNER_BODY_LOOKUP_CAP} 具；
 * 距离相同取右侧，右侧是冻结水平瞄准的右手法线。站位是脚点加前向乘
 * {@link CooperativeStructureLimits#STANCE_FORWARD_BLOCKS}，再加减侧距。
 * 冻结的 moveLimit 可以大于 8 格。实现必须先用 {@link #recallKind} 看实际脚点位移：
 * 不超过 8 格才把该实际位移交给 {@link ShortReposition}，大于 8 格只走 {@link RemoteRecall}。
 * 不得把放大后的冻结上限原样交给短距扫掠，也不得由执行族再搬一次。
 * markCommitted 只能在同一调用栈已经取得全有或全无发布许可后调用。
 * finish 和 cancel 以实现自己记下的提交状态决定冷却，调用方传 false 不能恢复已提交预算。
 */
public interface CooperationActors {
    CooperationReservation tryReserve(ServerPlayer owner, FamilyKey family, ActorFrame ownerFrame,
                                      CooperationSettings settings, long expiryTick);

    PreparationState advance(ServerLevel level, CooperationReservation reservation, ActorFrame currentOwnerFrame,
                             long tick);

    Optional<ActorFrame> readyFrame(CooperationReservation reservation);

    boolean validate(CooperationReservation reservation, ActorFrame ownerFrame);

    void markCommitted(CooperationReservation reservation, MemberKey member);

    void finish(CooperationReservation reservation, boolean anyCommitted);

    void cancel(CooperationReservation reservation, CancelReason reason);

    /**
     * 按实际位移选择归位端口。0 仍是 0，不会因为存在远距分支而变成不限距离。
     */
    default RecallKind recallKind(double travelBlocks, CooperationSettings settings) {
        if (settings == null) {
            return RecallKind.OUT_OF_RANGE;
        }
        return RecallRoute.kindFor(travelBlocks, settings.moveLimit());
    }
}

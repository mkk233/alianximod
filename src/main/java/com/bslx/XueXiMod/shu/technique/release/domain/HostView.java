package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.bind.RoundHandle;
import net.minecraft.server.level.ServerLevel;

import javax.annotation.Nullable;
import java.util.UUID;

/**
 * 绑定调度看见的宿主。旧领域和新的显式地表宿主都走这一面，调度本身不按术式编号分支。
 * 这是宿主状态合同，存续由各实现关闭；球形场地和护体都不作为绑定宿主。
 */
public interface HostView {
    UUID instanceId();

    UUID ownerId();

    ServerLevel hostLevel();

    DomainGeometry geometry();

    @Nullable
    BoundReleasePlan boundPlan();

    int startedAt();

    int expiresAt();

    int nextBindingBeat();

    void advanceBindingBeat(int followingBeat);

    boolean active();

    /**
     * 主人死亡、离线或不在宿主维度时，跳过已经到期的绑定节拍，不执行也不收费。
     * 默认关闭，旧领域仍保持原来的「人不在就不推进」。
     */
    default boolean skipsAbsentOwnerBeats() {
        return false;
    }

    boolean roundStillHosted(int now);

    void trackRound(RoundHandle handle, int now);

    void sweepFinishedRounds();

    void cancelOpenRounds();
}

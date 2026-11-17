package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.UUID;

/**
 * 表现端口，由客户端作者实现。
 * start 和 stop 只驱动姿态。旧序号的 stop 不得清掉更新的动作。这个端口不能请求提交伤害。
 */
public interface ActorActionPublisher {
    void start(ActionPlayback playback);

    void stop(FamilyKey family, UUID actorId, long sequence);
}

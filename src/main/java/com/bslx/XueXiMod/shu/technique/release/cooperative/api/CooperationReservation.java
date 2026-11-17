package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneHandle;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * 一次协同预约。找不到演员时 actorPresent 为 false、body 为空，对象本身仍然返回，不留下锁。
 * 消费者不能通过这个对象改分身运行状态。
 */
public record CooperationReservation(UUID token, long bodyGeneration, FamilyKey family, @Nullable CloneHandle body,
                                     long expiryTick, boolean actorPresent) {
    public CooperationReservation {
        if (token == null || family == null) {
            throw new IllegalArgumentException("协同预约不完整");
        }
        if (actorPresent == (body == null)) {
            throw new IllegalArgumentException("协同预约的身体和占用标记不一致");
        }
    }
}

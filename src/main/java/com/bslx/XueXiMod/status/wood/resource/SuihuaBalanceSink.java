package com.bslx.XueXiMod.status.wood.resource;

import java.util.UUID;

/**
 * 岁华变更后的只读通知。snapshot 是变更之后的绝对六项，不是增量。
 * 三度消费的返回值才是清零前的构成。回调里不要再改账；若改了，下一次服务端 tick 才会再通知。
 */
@FunctionalInterface
public interface SuihuaBalanceSink {
    void onChanged(UUID ownerId, SuihuaSnapshot snapshot);
}

package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 链上确实带了协同规则时的冻结指令。没有可用演员时仍可保留指令，但份额表可以不预约协同费。 */
public record CooperationOrder(CooperationSettings settings, long expiryTick) {
    public CooperationOrder {
        if (settings == null) {
            throw new IllegalArgumentException("协同指令缺少参数");
        }
    }
}

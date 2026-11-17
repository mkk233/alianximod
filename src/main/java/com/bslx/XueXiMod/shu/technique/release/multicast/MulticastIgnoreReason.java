package com.bslx.XueXiMod.shu.technique.release.multicast;

/**
 * 被展开读到、但不进入费用、冷却或复制的原始格。
 */
public enum MulticastIgnoreReason {
    /** 复制直接面对多重，消费这一格并丢掉复制。 */
    IGNORED_COPY_ON_MULTICAST,
    /** 作用域结束仍没有宿主的非附魔修饰，或没有产出成员的内层多重。 */
    UNUSED_MODIFIER,
    /** 已从装配移除的规则残留，不占用本层读取次数。 */
    REMOVED_RULE
}

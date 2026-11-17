package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 执行描述里的链槽种类。
 * 整合完成前 {@code ChainRole} 还没有协同角色，消费者用这个种类保留原下标，不要写术式字符串分支。
 */
public enum PlanSlotKind {
    SEEK,
    TRAJECTORY,
    RULE,
    COOPERATIVE_RULE,
    TIMED_ACTION,
    CONTACT_ACTION,
    IMMEDIATE_ATTACK,
    ENCHANT,
    OTHER
}

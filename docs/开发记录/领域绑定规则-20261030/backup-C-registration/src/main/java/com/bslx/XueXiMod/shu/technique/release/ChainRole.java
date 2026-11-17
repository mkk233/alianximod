package com.bslx.XueXiMod.shu.technique.release;

/**
 * 底座类型。新术式只要登记成同一类，就走这一类的收集和结算。
 */
public enum ChainRole {
    ATTACK,
    ACTION,
    SEEK_SPACE,
    SEEK_ENTITY,
    TRAJECTORY_TRACKING,
    TRAJECTORY_DROP,
    TRAJECTORY_SCATTER,
    RULE_TRIPLE,
    RULE_DUAL
}

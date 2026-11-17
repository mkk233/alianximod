package com.bslx.XueXiMod.shu.balance;

/**
 * 平衡快照的逻辑侧。集成服务器里两侧同时存在，断线只清理客户端。
 */
public enum BalanceSide {
    SERVER,
    CLIENT
}

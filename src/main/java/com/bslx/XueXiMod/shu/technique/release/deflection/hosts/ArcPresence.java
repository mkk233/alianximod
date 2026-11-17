package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

/**
 * 缺月宿主是否还在活动窗里。路线只借这个布尔，不把已移除的实体继续当碰撞体。
 */
public interface ArcPresence {
    /** 这个游戏时刻宿主是否仍在活动窗里。 */
    boolean present(long gameTick);
}

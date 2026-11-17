package com.bslx.XueXiMod.shu.technique.release.bind;

/** 领域仍有效、未到期、主人存活在线且与宿主同维。 */
public interface LifetimeToken {
    /** 当前刻仍满足上述条件时为真。 */
    boolean valid(int now);
}

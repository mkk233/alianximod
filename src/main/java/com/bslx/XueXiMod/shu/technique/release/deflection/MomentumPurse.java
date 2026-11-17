package com.bslx.XueXiMod.shu.technique.release.deflection;

/**
 * 只给这次偏转收购使用的预留口。
 * 生产实现调用既有 {@code WaterMomentum.reserve/commit/release}，不改整套水势账。
 */
interface MomentumPurse {
    float available();

    /** 拒绝时返回空，并且不进预留表。 */
    Object reserve(float amount);

    /** 同一笔只在仍为预留时扣一次。没扣成返回假。 */
    boolean commit(Object reservation);

    /** 只退还没提交的预留。 */
    void release(Object reservation);
}

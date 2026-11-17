package com.bslx.XueXiMod.shu.technique.release.batch;

/**
 * 一次命中的四种结果。取消和免疫都不是“已经打出正伤”。
 * 账本未冲刷前只允许返回等待，不能提前当成成功。
 */
public enum HitVerdict {
    /** 目标失效、关系过滤或伤害事件被取消。不接续，也不消耗扫掠上的第一次机会。 */
    INVALID_OR_CANCELLED,
    /** 目标合法，但最终伤害不是正数。可以接续，不产种。 */
    ESTABLISHED_NO_DAMAGE,
    /** 事件没取消，并且最终伤害为正。 */
    DAMAGE_ACCEPTED,
    /** 还在等账本冲刷。不能当成已经成功。 */
    PENDING;

    /** 无正伤或正伤都可以让后续继续。 */
    public boolean continues() {
        return this == ESTABLISHED_NO_DAMAGE || this == DAMAGE_ACCEPTED;
    }

    /** 只有最终伤害为正才产种。 */
    public boolean grantsSeeds() {
        return this == DAMAGE_ACCEPTED;
    }
}

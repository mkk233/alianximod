package com.bslx.XueXiMod.shu.technique.release.prison;

/** 水牢结束原因。锁定档位仍决定耗湿规则，自然清湿不按破裂减半。 */
public enum EndReason {
    DURATION,
    WET_CLEARED,
    TELEPORT,
    DIMENSION,
    LOGOUT,
    DEATH,
    CASTER_DEATH,
    CASTER_LOGOUT,
    CASTER_DIMENSION,
    REMOVED
}

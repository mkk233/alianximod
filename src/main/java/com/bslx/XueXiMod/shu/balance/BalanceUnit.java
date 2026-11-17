package com.bslx.XueXiMod.shu.balance;

/**
 * 参数单位。单位参与合并判断，也决定魔力键能否走统一费用入口。
 */
public enum BalanceUnit {
    MULTIPLIER,
    MANA,
    TICKS,
    BLOCKS,
    BLOCKS_PER_TICK,
    RATIO,
    COUNT,
    DAMAGE,
    /** 弧度每刻。旋流环绕角速度使用这一单位。 */
    RADIANS_PER_TICK,
    /** 立方格。占位体积使用这一单位。 */
    CUBIC_BLOCKS,
    /** 水势点数。入卷和弹体兜底使用这一单位。 */
    WATER_MOMENTUM
}

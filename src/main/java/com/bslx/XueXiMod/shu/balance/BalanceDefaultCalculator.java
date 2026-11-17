package com.bslx.XueXiMod.shu.balance;

/**
 * 在封存目录时求一次原始默认值。
 * 只许读取已经完成登记的代码常量或术式原始字段，例如 {@code Technique#getBaseCooldown()}。
 * 禁止读取 {@link BalanceService}、禁止嵌套求值、禁止在计算器里再声明参数。
 * 不要在类静态初始化期间触发求值。
 */
@FunctionalInterface
public interface BalanceDefaultCalculator {
    double rawDefault(String techniqueId);
}

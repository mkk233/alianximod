package com.bslx.XueXiMod.shu.balance;

import java.util.Objects;

/**
 * 已经完成术式伤害倍率和全局伤害倍率各一次的金额。
 * 之后只能相加，或按命中份数相乘；不能再送回 {@link BalanceService#damageContribution}。
 * 读档上的旧最终数值用 {@link #retained}，不再追乘。
 */
public final class ConfiguredDamage {
    private final float amount;

    private ConfiguredDamage(float amount) {
        this.amount = amount;
    }

    static ConfiguredDamage preserve(float amount) {
        if (!Float.isFinite(amount) || Float.compare(amount, 0.0f) < 0) {
            throw new BalanceValueException("已配置伤害必须是有限非负数");
        }
        return new ConfiguredDamage(amount);
    }

    static ConfiguredDamage scaled(float amount) {
        if (!Float.isFinite(amount) || Float.compare(amount, 0.0f) < 0 || amount > 1_000_000_000.0f) {
            throw new BalanceValueException("已配置伤害超出可保存范围");
        }
        return new ConfiguredDamage(amount);
    }

    public static ConfiguredDamage retained(float storedAmount) {
        if (!Float.isFinite(storedAmount) || Float.compare(storedAmount, 0.0f) < 0) {
            throw new BalanceValueException("已保存的伤害必须是有限非负数");
        }
        return new ConfiguredDamage(storedAmount);
    }

    public float amount() {
        return amount;
    }

    public ConfiguredDamage plus(ConfiguredDamage other) {
        Objects.requireNonNull(other, "other");
        float sum = amount + other.amount;
        if (!Float.isFinite(sum) || Float.compare(sum, 0.0f) < 0) {
            throw new BalanceValueException("已配置伤害相加后无法表示");
        }
        return new ConfiguredDamage(sum);
    }

    public ConfiguredDamage timesHitCount(int hitCount) {
        if (hitCount < 1) {
            throw new BalanceValueException("命中份数至少为 1");
        }
        float product = amount * (float) hitCount;
        if (!Float.isFinite(product) || Float.compare(product, 0.0f) < 0) {
            throw new BalanceValueException("命中份数相乘后的伤害无法表示");
        }
        return new ConfiguredDamage(product);
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof ConfiguredDamage other)) {
            return false;
        }
        return Float.compare(amount, other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Float.hashCode(amount);
    }

    @Override
    public String toString() {
        return "ConfiguredDamage[" + amount + "]";
    }
}

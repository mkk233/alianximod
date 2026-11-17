package com.bslx.XueXiMod.shu.balance;

/**
 * 尚未做平衡缩放的一笔伤害贡献。结构底伤、武器、法杖和原公式都可以先算进这个数。
 * 把它交给 {@link BalanceService#damageContribution} 之后，才变成已配置金额。
 */
public final class RawDamage {
    private final double amount;

    private RawDamage(double amount) {
        this.amount = amount;
    }

    public static RawDamage of(double amount) {
        if (!Double.isFinite(amount) || Double.compare(amount, 0.0d) < 0) {
            throw new BalanceValueException("原始伤害贡献必须是有限非负数");
        }
        return new RawDamage(amount);
    }

    public double amount() {
        return amount;
    }

    @Override
    public boolean equals(Object candidate) {
        if (this == candidate) {
            return true;
        }
        if (!(candidate instanceof RawDamage other)) {
            return false;
        }
        return Double.compare(amount, other.amount) == 0;
    }

    @Override
    public int hashCode() {
        return Double.hashCode(amount);
    }

    @Override
    public String toString() {
        return "RawDamage[" + amount + "]";
    }
}

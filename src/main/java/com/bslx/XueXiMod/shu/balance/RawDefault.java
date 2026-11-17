package com.bslx.XueXiMod.shu.balance;

import java.util.Objects;

/**
 * 参数的原始默认值来源。数字只写在现有代码常量或术式字段里，声明处引用它。
 * 字面量和计算器会在封存时冻结；调用方来源留到 {@code manaCost} 或 {@code cooldownTicks} 的参数上。
 */
public final class RawDefault {
    public enum Mode {
        LITERAL,
        CALCULATOR,
        CALL_SITE
    }

    private static final RawDefault CALL_SITE = new RawDefault(Mode.CALL_SITE, 0.0d, null);

    private final Mode mode;
    private final double literalAmount;
    private final BalanceDefaultCalculator calculator;

    private RawDefault(Mode mode, double literalAmount, BalanceDefaultCalculator calculator) {
        this.mode = mode;
        this.literalAmount = literalAmount;
        this.calculator = calculator;
    }

    public static RawDefault literal(int amount) {
        return literal((double) amount);
    }

    public static RawDefault literal(double amount) {
        if (!Double.isFinite(amount)) {
            throw new BalanceDeclarationException("原始默认值必须是有限数");
        }
        return new RawDefault(Mode.LITERAL, amount, null);
    }

    public static RawDefault calculator(BalanceDefaultCalculator calculator) {
        Objects.requireNonNull(calculator, "calculator");
        return new RawDefault(Mode.CALCULATOR, 0.0d, calculator);
    }

    public static RawDefault callSite() {
        return CALL_SITE;
    }

    public Mode mode() {
        return mode;
    }

    public double literalAmount() {
        if (mode != Mode.LITERAL) {
            throw new BalanceDeclarationException("这个原始默认值不是字面量");
        }
        return literalAmount;
    }

    public BalanceDefaultCalculator calculator() {
        if (mode != Mode.CALCULATOR || calculator == null) {
            throw new BalanceDeclarationException("这个原始默认值不是计算器");
        }
        return calculator;
    }

    public boolean sameSource(RawDefault other) {
        if (mode != other.mode) {
            return false;
        }
        if (mode == Mode.LITERAL) {
            return Double.compare(literalAmount, other.literalAmount) == 0;
        }
        return mode != Mode.CALCULATOR;
    }

    @Override
    public String toString() {
        if (mode == Mode.LITERAL) {
            return "literal(" + literalAmount + ")";
        }
        if (mode == Mode.CALCULATOR) {
            return "calculator";
        }
        return "callSite";
    }
}

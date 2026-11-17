package com.bslx.XueXiMod.shu.balance;

/**
 * 调用方给出的原始金额、未声明键，或缩放结果无法表示。
 */
public class BalanceValueException extends BalanceException {
    public BalanceValueException(String detail) {
        super(detail);
    }
}

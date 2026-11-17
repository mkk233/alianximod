package com.bslx.XueXiMod.shu.balance;

/**
 * 声明、封存或原始默认计算器不合法。这是代码错误，不是玩家配置错误。
 */
public class BalanceDeclarationException extends BalanceException {
    public BalanceDeclarationException(String detail) {
        super(detail);
    }

    public BalanceDeclarationException(String detail, Throwable cause) {
        super(detail, cause);
    }
}

package com.bslx.XueXiMod.shu.balance;

/**
 * 平衡目录、快照或金额合同被破坏时抛出的基类。
 */
public class BalanceException extends RuntimeException {
    public BalanceException(String detail) {
        super(detail);
    }

    public BalanceException(String detail, Throwable cause) {
        super(detail, cause);
    }
}

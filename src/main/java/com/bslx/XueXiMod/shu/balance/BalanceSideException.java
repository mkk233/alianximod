package com.bslx.XueXiMod.shu.balance;

/**
 * 当前线程没有绑定逻辑侧，不能在已安装的两侧快照之间猜测。
 */
public final class BalanceSideException extends BalanceException {
    public BalanceSideException(String detail) {
        super(detail);
    }
}

package com.bslx.XueXiMod.shu.balance;

/**
 * 同一术式的同一参数键出现了无法视为同一次声明的第二份合同。
 */
public final class BalanceConflictException extends BalanceDeclarationException {
    private final String techniqueId;
    private final String parameterKey;

    public BalanceConflictException(String techniqueId, String parameterKey, String detail) {
        super("术式 " + techniqueId + " 的参数 " + parameterKey + " 冲突: " + detail);
        this.techniqueId = techniqueId;
        this.parameterKey = parameterKey;
    }

    public String techniqueId() {
        return techniqueId;
    }

    public String parameterKey() {
        return parameterKey;
    }
}

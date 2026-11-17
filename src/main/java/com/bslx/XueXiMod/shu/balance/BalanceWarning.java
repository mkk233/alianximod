package com.bslx.XueXiMod.shu.balance;

import java.util.Objects;

/**
 * 单个配置字段被跳过或回退时的记录。全局字段的术式编号为空字符串。
 * 警告不改写玩家文件。
 */
public final class BalanceWarning {
    public enum Kind {
        UNKNOWN_TECHNIQUE,
        UNKNOWN_PARAMETER,
        INVALID_VALUE,
        ALIAS_IGNORED,
        ALIAS_CANONICALIZED
    }

    private final Kind kind;
    private final String techniqueId;
    private final String parameterKey;
    private final String detail;

    private BalanceWarning(Kind kind, String techniqueId, String parameterKey, String detail) {
        this.kind = kind;
        this.techniqueId = techniqueId;
        this.parameterKey = parameterKey;
        this.detail = detail;
    }

    public static BalanceWarning of(Kind kind, String techniqueId, String parameterKey, String detail) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(detail, "detail");
        return new BalanceWarning(kind,
                techniqueId == null ? "" : techniqueId,
                parameterKey == null ? "" : parameterKey,
                detail);
    }

    public Kind kind() {
        return kind;
    }

    public String techniqueId() {
        return techniqueId;
    }

    public String parameterKey() {
        return parameterKey;
    }

    public String detail() {
        return detail;
    }

    @Override
    public String toString() {
        return kind + " " + techniqueId + " " + parameterKey + " " + detail;
    }
}

package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 普通分身是否接受外部协同。默认关闭。
 * 分身规格以后增加同名组件时必须使用这个类型：普通分身用 {@link #OPEN}，水幻形和开发幻影用 {@link #CLOSED}。
 */
public record ExternalCooperation(boolean enabled) {
    public static final ExternalCooperation CLOSED = new ExternalCooperation(false);
    public static final ExternalCooperation OPEN = new ExternalCooperation(true);
}

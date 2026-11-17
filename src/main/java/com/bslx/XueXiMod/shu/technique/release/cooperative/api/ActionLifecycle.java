package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 未提交态都可以进入 CANCELLED。RELEASED 之后不能再退回未出刀。 */
public enum ActionLifecycle {
    PREPARED,
    RUNNING,
    RELEASED,
    FINISHED,
    CANCELLED
}

package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 一次已经挂上服务器实例的攻击族。cancel 可重复调用。 */
public interface FamilyRuntimeHandle {
    FamilyKey family();

    ActionLifecycle lifecycle();

    void cancel(CancelReason reason);
}

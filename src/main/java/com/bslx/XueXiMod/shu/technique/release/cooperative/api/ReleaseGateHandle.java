package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 一把只成功触发一次的门。重复 trigger 返回 false。cancel 可重复调用。
 * 冲刺的每一次合法接触使用另一把新门，不把同一把门的失败回调当成第二次播放或扣费。
 */
public interface ReleaseGateHandle {
    FamilyKey family();

    ReleaseGateKind kind();

    boolean trigger(TriggerGeometry geometry);

    void cancel(CancelReason reason);

    boolean closed();

    ActionLifecycle lifecycle();
}

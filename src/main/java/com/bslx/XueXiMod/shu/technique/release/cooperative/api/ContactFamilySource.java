package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 接触窗派生子族的端口。实现放在整合阶段，本接口不打开窗口、不发布体积。
 * 每次 onContact 调用一次 {@link #openSubFamily(TriggerGeometry)}，再对该门 trigger 一次。
 */
public interface ContactFamilySource {
    ReleaseGateHandle openSubFamily(TriggerGeometry geometry);

    void onWindowEnded();

    void onWindowFinished();

    boolean windowClosed();
}

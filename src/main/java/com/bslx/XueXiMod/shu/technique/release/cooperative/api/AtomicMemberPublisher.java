package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/**
 * 低层原子发布端口，由整合作者实现。
 * stage 只取得宿主组许可并暂存；commit 在同一服务端调用栈发布全部几何；discard 丢弃未发布暂存。
 * 实现不得调用隐式全局倍率循环，也不得让环境释放帧再改一次原点。
 */
public interface AtomicMemberPublisher {
    MemberStage stage(MemberPublishRequest request);

    void commit(MemberStage stage);

    void discard(MemberStage stage, CancelReason reason);
}

package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 硬取消结束整族未提交部分。软失败只释放协同并回退尚未发布的单人路径。 */
public enum CancelReason {
    OWNER_DEATH(true),
    OWNER_OFFLINE(true),
    OWNER_DIMENSION(true),
    OWNER_COPIED(true),
    CARRIER_INVALID(true),
    PARENT_CANCELLED(true),
    REGION_UNLOADED(true),
    SERVER_STOP(true),
    NO_ACTOR(false),
    ACTOR_DEATH(false),
    ACTOR_DISMISSED(false),
    ACTOR_BLOCKED(false),
    ACTOR_TOO_FAR(false),
    UNSAFE_STANCE(false),
    PREPARE_TIMEOUT(false),
    BUDGET_EXHAUSTED(false),
    ADAPTER_MISSING(false),
    PUBLISH_FAILED(false),
    STRUCTURE_REJECTED(false),
    VALIDATION_FAILED(false);

    private final boolean hard;

    CancelReason(boolean hard) {
        this.hard = hard;
    }

    public boolean hard() {
        return hard;
    }
}

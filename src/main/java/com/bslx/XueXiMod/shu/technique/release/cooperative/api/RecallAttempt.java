package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 远距瞬时归位的结果。拒绝不是空引用，也不能拿短距计划冒充。 */
public sealed interface RecallAttempt permits AcceptedRemoteRecall, RejectedRecall {
}

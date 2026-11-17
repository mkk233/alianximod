package com.bslx.XueXiMod.shu.technique.release;

/** 释放定义的大类。调用方据此区分动作、命中体、规则和自身状态；具体行为仍读定义上的能力。 */
public enum ReleaseKind {
    ACTION,
    HIT_VOLUME,
    RULE,
    /** 自身状态。不生成命中体；链上角色另由释放定义申报。 */
    SELF_STATUS
}

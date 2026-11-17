package com.bslx.XueXiMod.status.wood.resource;

/** 拒绝原因。NONE 只和成功结果一起出现。消费接口用空 Optional，不另带这个枚举。 */
public enum WoodReject {
    NONE,
    THREAD,
    ACTOR,
    SEALED,
    TICKET,
    VALIDATION,
    CAPACITY,
    REVISION,
    OWNER,
    DUPLICATE,
    REENTRY,
    REASON,
    NOT_FULL,
    READY,
    MISSING,
    LIMIT
}

package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 一次身体位移该走哪条归位端口。超出冻结上限时不归位。 */
public enum RecallKind {
    SHORT,
    REMOTE_RECALL,
    OUT_OF_RANGE
}

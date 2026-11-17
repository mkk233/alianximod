package com.bslx.XueXiMod.shu.technique.release.capture;

/**
 * 整链结构检查的三种结果。普通旧链是不适用，不进入捕获。
 */
public enum ActorCaptureStatus {
    NOT_APPLICABLE,
    REFUSED,
    PLAN
}

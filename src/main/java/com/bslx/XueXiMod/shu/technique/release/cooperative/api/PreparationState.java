package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 协同准备的三态。UNAVAILABLE 表示回退单人，不是抛空指针。 */
public enum PreparationState {
    PREPARING,
    READY,
    UNAVAILABLE
}

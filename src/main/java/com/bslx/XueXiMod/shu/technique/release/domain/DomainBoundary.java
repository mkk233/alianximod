package com.bslx.XueXiMod.shu.technique.release.domain;

/**
 * 领域边界态度合同。只区分封闭和开放，不保存实例，也不管理存续。
 * 球形场地和护体没有这份六面进出开关。
 */
public enum DomainBoundary {
    SEAL,
    OPEN
}

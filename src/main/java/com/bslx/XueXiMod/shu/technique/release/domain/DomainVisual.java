package com.bslx.XueXiMod.shu.technique.release.domain;

/**
 * 客户端领域画法合同。只提供分派种类，不持有画面状态，生命周期由渲染方管理。
 * 球形场地画锚点粒子，护体画随身环，都不按这份体积画法分派。
 */
public enum DomainVisual {
    VOLUME,
    PRESSURE,
    QUIET
}

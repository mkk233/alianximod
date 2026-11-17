package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.Optional;

/** 一段火伤的基础值 B 和资源增量 A。炉心 F 由账本在挂载时合入一次。 */
public final class BurnComponent {
    private final double base;
    private final double resourceAboveOne;

    private BurnComponent(double base, double resourceAboveOne) {
        this.base = base;
        this.resourceAboveOne = resourceAboveOne;
    }

    public static Optional<BurnComponent> of(double base, double resourceAboveOne) {
        if (!FireLayer.acceptableDamage(base) || !Double.isFinite(resourceAboveOne) || resourceAboveOne < 0.0d) {
            return Optional.empty();
        }
        return Optional.of(new BurnComponent(base, resourceAboveOne));
    }

    public double base() {
        return this.base;
    }

    public double resourceAboveOne() {
        return this.resourceAboveOne;
    }
}

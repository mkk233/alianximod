package com.bslx.XueXiMod.shu.technique.release.domain;

/** 领域自己的周期效果。不执行外接附魔，也不靠受伤去换水势。 */
public final class DomainNativePolicy {
    private final String effectKey;
    private final int wetPeriod;
    private final int wetAmount;
    private final int wetDelay;
    private final int momentumPeriod;
    private final int momentumAmount;
    private final int momentumDelay;
    private final int slowAmplifier;
    private final int slowDuration;
    private final int slowPeriod;
    private final int slowDelay;
    private final float breathMultiplier;

    public DomainNativePolicy(String effectKey, int wetPeriod, int wetAmount, int wetDelay,
                              int momentumPeriod, int momentumAmount, int momentumDelay) {
        this(effectKey, wetPeriod, wetAmount, wetDelay, momentumPeriod, momentumAmount, momentumDelay,
                -1, 0, 0, 0, 0f);
    }

    public DomainNativePolicy(String effectKey, int wetPeriod, int wetAmount, int wetDelay,
                              int momentumPeriod, int momentumAmount, int momentumDelay,
                              int slowAmplifier, int slowDuration, int slowPeriod, int slowDelay,
                              float breathMultiplier) {
        this.effectKey = effectKey == null ? "" : effectKey;
        this.wetPeriod = wetPeriod;
        this.wetAmount = wetAmount;
        this.wetDelay = wetDelay;
        this.momentumPeriod = momentumPeriod;
        this.momentumAmount = momentumAmount;
        this.momentumDelay = momentumDelay;
        this.slowAmplifier = slowAmplifier;
        this.slowDuration = slowDuration;
        this.slowPeriod = slowPeriod;
        this.slowDelay = slowDelay;
        this.breathMultiplier = breathMultiplier;
    }

    public String effectKey() {
        return effectKey;
    }

    public int wetPeriod() {
        return wetPeriod;
    }

    public int wetAmount() {
        return wetAmount;
    }

    public int wetDelay() {
        return wetDelay;
    }

    public int momentumPeriod() {
        return momentumPeriod;
    }

    public int momentumAmount() {
        return momentumAmount;
    }

    public int momentumDelay() {
        return momentumDelay;
    }

    public int slowAmplifier() {
        return slowAmplifier;
    }

    public int slowDuration() {
        return slowDuration;
    }

    public int slowPeriod() {
        return slowPeriod;
    }

    public int slowDelay() {
        return slowDelay;
    }

    public float breathMultiplier() {
        return breathMultiplier;
    }
}

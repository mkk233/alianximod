package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;

/**
 * 领域原生周期效果合同。参数随声明保存，真正发放由领域脉冲服务执行。
 * 球形场地把效果记在场地实例上，护体没有潮湿、减速或水势周期。
 */
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
    private String balanceTechniqueId = "";

    /** 建只有潮湿和水势的周期参数。减速和耗氧保持关闭，不扣资源。 */
    public DomainNativePolicy(String effectKey, int wetPeriod, int wetAmount, int wetDelay,
                              int momentumPeriod, int momentumAmount, int momentumDelay) {
        this(effectKey, wetPeriod, wetAmount, wetDelay, momentumPeriod, momentumAmount, momentumDelay,
                -1, 0, 0, 0, 0f);
    }

    /** 建完整周期参数。空效果键记成空串，数值原样保存，不扣资源。 */
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

    /** 绑定平衡术式。空编号记成空串，之后周期读取才走平衡回退。 */
    public DomainNativePolicy boundTo(String techniqueId) {
        this.balanceTechniqueId = techniqueId == null ? "" : techniqueId;
        return this;
    }

    public String effectKey() {
        return effectKey;
    }

    /** 读取潮湿周期。绑定术式时改读平衡值，否则用声明原值。 */
    public int wetPeriod() {
        return balanceTechniqueId.isEmpty()
                ? wetPeriod
                : SpecialBalance.integer(balanceTechniqueId, DomainDefinition.WET_PERIOD, wetPeriod);
    }

    public int wetAmount() {
        return wetAmount;
    }

    public int wetDelay() {
        return wetDelay;
    }

    /** 读取水势周期。绑定术式时改读平衡值，否则用声明原值。 */
    public int momentumPeriod() {
        return balanceTechniqueId.isEmpty()
                ? momentumPeriod
                : SpecialBalance.integer(balanceTechniqueId, DomainDefinition.MOMENTUM_PERIOD, momentumPeriod);
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

    /** 读取减速周期。绑定术式时改读平衡值，否则用声明原值。 */
    public int slowPeriod() {
        return balanceTechniqueId.isEmpty()
                ? slowPeriod
                : SpecialBalance.integer(balanceTechniqueId, DomainDefinition.SLOW_PERIOD, slowPeriod);
    }

    public int slowDelay() {
        return slowDelay;
    }

    /** 读取耗氧倍率。绑定术式时改读平衡值，否则用声明原值。 */
    public float breathMultiplier() {
        return balanceTechniqueId.isEmpty()
                ? breathMultiplier
                : SpecialBalance.decimal(balanceTechniqueId, DomainDefinition.BREATH_MULTIPLIER, breathMultiplier);
    }
}

package com.bslx.XueXiMod.shu.technique.release.domain;

import java.util.UUID;

/**
 * 一次命中选中的领域附加伤害快照。只保存实例、版本和数值，不拥有领域生命周期。
 * 球形场地按份数乘伤，护体记卸力水势，都不使用这份快照。
 */
public final class AuraContribution {
    private final UUID instanceId;
    private final int revision;
    private final float bonusDamage;

    /** 记下当时选中的领域、版本和附加伤害。不扣资源，也不校验领域是否仍在。 */
    public AuraContribution(UUID instanceId, int revision, float bonusDamage) {
        this.instanceId = instanceId;
        this.revision = revision;
        this.bonusDamage = bonusDamage;
    }

    public UUID instanceId() {
        return instanceId;
    }

    public int revision() {
        return revision;
    }

    public float bonusDamage() {
        return bonusDamage;
    }
}

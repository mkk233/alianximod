package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;

/**
 * 领域友方附加伤害合同。只保存附魔编号和伤害，不持有实例，也不由它自己开关领域。
 * 球形场地按重叠份数乘伤，护体按比例卸伤，都不读这份合同。
 */
public final class DomainAuraSpec {
    private final String enchantmentId;
    private final float bonusDamage;

    /** 固定这一份附魔编号和附加伤害。不扣资源，编号为空或伤害为零都原样保存。 */
    public DomainAuraSpec(String enchantmentId, float bonusDamage) {
        this.enchantmentId = enchantmentId;
        this.bonusDamage = bonusDamage;
    }

    /** 读取潮印的中性水反应伤害。登记缺失时退回 2 点，不扣资源。 */
    public static DomainAuraSpec tideNeutral() {
        EnchantmentDefinition definition = EnchantmentRegistry.get("tide_mark");
        float damage = 2f;
        if (definition != null) {
            damage = definition.magicDamage(ElementReaction.of(ShuAttribute.NONE, ShuAttribute.WATER));
        }
        return new DomainAuraSpec("tide_mark", damage);
    }

    public String enchantmentId() {
        return enchantmentId;
    }

    public float bonusDamage() {
        return bonusDamage;
    }
}

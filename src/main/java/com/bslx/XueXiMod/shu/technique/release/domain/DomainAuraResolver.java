package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/**
 * 命中时的领域附加伤害服务。不保存选择结果，领域存续仍由领域服务管理。
 * 球形场地按重叠份数乘伤，护体改成卸力入账，都不在这里合并。
 */
public final class DomainAuraResolver {
    private DomainAuraResolver() {
    }

    /** 给允许供给的根攻击加上一份最大领域伤害。不扣资源；种类、端侧或领域条件不符就退回原伤害。 */
    public static float merge(float baseAmount, Entity attacker, AttackEnchantmentContext.Kind kind) {
        if (!AttackEnchantmentContext.grantsAura(kind) || attacker == null || attacker.level().isClientSide()) {
            return baseAmount;
        }
        AuraContribution chosen = choose(attacker);
        if (chosen == null || chosen.bonusDamage() <= 0f) {
            return baseAmount;
        }
        return baseAmount + chosen.bonusDamage();
    }

    /** 在攻击者完整身处的友方领域里选最大伤害。不扣资源；没有合格领域时返回空。 */
    public static AuraContribution choose(Entity attacker) {
        if (!(attacker instanceof LivingEntity living)) {
            return null;
        }
        AuraContribution chosen = null;
        for (DomainInstance domain : DomainService.active()) {
            if (!domain.active() || domain.hostLevel() != living.level()) {
                continue;
            }
            if (!Relations.permits(domain.ownerId(), living) && !Relations.ally(domain.ownerId(), living)) {
                continue;
            }
            if (!domain.geometry().fits(living.getBoundingBox())
                    || !domain.geometry().fullyContains(living.getBoundingBox())) {
                continue;
            }
            AuraContribution candidate = configuredContribution(domain);
            if (chosen == null || candidate.bonusDamage() > chosen.bonusDamage()
                    || (candidate.bonusDamage() == chosen.bonusDamage()
                    && candidate.instanceId().compareTo(chosen.instanceId()) < 0)) {
                chosen = candidate;
            }
        }
        return chosen;
    }

    private static AuraContribution configuredContribution(DomainInstance domain) {
        float rawBonus = domain.auraDamage();
        String sourceId = "";
        if (domain.definition() != null && domain.definition().auraSpec() != null) {
            sourceId = domain.definition().auraSpec().enchantmentId();
        }
        float configuredBonus = BalanceDisplay.damage(sourceId, rawBonus);
        return new AuraContribution(domain.instanceId(), domain.revision(), configuredBonus);
    }
}

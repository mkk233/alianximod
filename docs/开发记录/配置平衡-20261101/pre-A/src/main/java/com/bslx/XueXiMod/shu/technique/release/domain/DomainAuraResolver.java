package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.relations.Relations;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** 命中时现查攻击者还在不在域里。所有领域只取一份最大附加伤害。 */
public final class DomainAuraResolver {
    private DomainAuraResolver() {
    }

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
            AuraContribution candidate = new AuraContribution(domain.instanceId(), domain.revision(), domain.auraDamage());
            if (chosen == null || candidate.bonusDamage() > chosen.bonusDamage()
                    || (candidate.bonusDamage() == chosen.bonusDamage()
                    && candidate.instanceId().compareTo(chosen.instanceId()) < 0)) {
                chosen = candidate;
            }
        }
        return chosen;
    }
}

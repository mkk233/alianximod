package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
import com.bslx.XueXiMod.skill.Skill;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/** 附魔能不能绑到这一发零伤害宿主上。不能绑就在扣费和推进之前拒绝。 */
final class ContactCast {
    private ContactCast() {
    }

    static String refusal(Skill skill, List<ChainSegment> segments) {
        List<Technique> techniques = skill.getTechniques();
        for (ChainSegment segment : segments) {
            if (segment.enchantIndexes().isEmpty()) {
                continue;
            }
            Technique host = techniques.get(segment.actionIndex());
            if (host.isEnchantment()) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(host.getId());
            boolean explicitContact = definition != null && definition.explicitDomainHost();
            if (host.getBaseDamage() > 0f && !explicitContact) {
                continue;
            }
            if (explicitContact) {
                if (EnchantmentSnapshot.damageOf(skill, segment.enchantIndexes()) <= 0f
                        && !gloomOnDomainHost(skill, host, segment.enchantIndexes())) {
                    return "没有附加伤害的接触附魔暂不支持，这次没有释放";
                }
                continue;
            }
            if (!carriesContact(definition)) {
                return host.getName() + "还不能承载附魔，这次没有释放";
            }
            if (EnchantmentSnapshot.damageOf(skill, segment.enchantIndexes()) <= 0f
                    && !gloomOnDomainHost(skill, host, segment.enchantIndexes())) {
                return "没有附加伤害的接触附魔暂不支持，这次没有释放";
            }
        }
        return null;
    }

    /**
     * 多重按编译后的成员附魔，以及前导动作前面真正属于它的附魔来拒绝。
     * 不使用旧分段把作用域尾部的独立附魔挂到后面的宿主上。
     */
    static String refusal(Skill skill, List<Technique> techniques, List<ChainSegment> segments, CompiledReleaseStep plan) {
        String memberRefusal = refusal(skill, plan.members);
        if (memberRefusal != null) {
            return memberRefusal;
        }
        if (!plan.actionLed || plan.actionSegment < 0 || segments == null || plan.actionSegment >= segments.size()) {
            return null;
        }
        int actionIndex = segments.get(plan.actionSegment).actionIndex();
        if (techniques == null || actionIndex < 0 || actionIndex >= techniques.size()) {
            return null;
        }
        List<Integer> leadingEnchants = new java.util.ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < actionIndex; techniqueIndex++) {
            if (techniques.get(techniqueIndex).isEnchantment()) {
                leadingEnchants.add(techniqueIndex);
            }
        }
        if (leadingEnchants.isEmpty()) {
            return null;
        }
        return refusal(skill, List.of(new ChainSegment(actionIndex, leadingEnchants)));
    }

    static boolean keptForContact(List<Technique> techniques, List<ChainSegment> segments, int from) {
        for (int index = from; index < segments.size(); index++) {
            ChainSegment segment = segments.get(index);
            Technique host = techniques.get(segment.actionIndex());
            if (host.isEnchantment() || SkillSegmentMath.isChainModifier(host)) {
                continue;
            }
            if (host.getBaseDamage() > 0f || segment.enchantIndexes().isEmpty()) {
                return false;
            }
            return carriesContact(ReleaseRegistry.get(host.getId()));
        }
        return false;
    }

    static EnchantmentSnapshot snapshot(Skill skill, ChainSegment segment, ServerPlayer player) {
        if (segment.enchantIndexes().isEmpty()) {
            return null;
        }
        Technique host = skill.getTechniques().get(segment.actionIndex());
        boolean domainGloom = gloomOnDomainHost(skill, host, segment.enchantIndexes());
        if (host.getBaseDamage() > 0f && !domainGloom) {
            return null;
        }
        if (!domainGloom && !carriesContact(ReleaseRegistry.get(host.getId()))) {
            return null;
        }
        EnchantmentSnapshot built = EnchantmentSnapshot.from(skill, segment, player);
        if (built == null) {
            return null;
        }
        if (built.contactDamage <= 0f && !domainGloom) {
            return null;
        }
        if (domainGloom && built.contactDamage <= 0f && built.basicTypeIds().isEmpty()) {
            return null;
        }
        return built;
    }

    /** 只有领域或灼地带幽冥时，才放行零接触伤。别的宿主仍按旧拒绝。 */
    private static boolean gloomOnDomainHost(Skill skill, Technique host, List<Integer> indexes) {
        if (skill == null || host == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(host.getId());
        if (definition == null || (definition.domain() == null && !definition.explicitDomainHost())) {
            return false;
        }
        EnchantBonus bonus = EnchantBonus.resolve(skill, indexes, host.getBaseDamage(), 1);
        return bonus.resourceLoad != null && bonus.resourceLoad.gloom();
    }

    private static boolean carriesContact(ReleaseDefinition definition) {
        return definition != null && (definition.barrier() != null || definition.domain() != null
                || definition.fieldRadius() > 0f || definition.guard() != null
                || ConditionalEffects.supportsContact(definition));
    }
}

package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
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
            if (host.getBaseDamage() > 0f || host.isEnchantment()) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(host.getId());
            if (!carriesContact(definition)) {
                return host.getName() + "还不能承载附魔，这次没有释放";
            }
            if (EnchantmentSnapshot.damageOf(skill, segment.enchantIndexes()) <= 0f) {
                return "没有附加伤害的接触附魔暂不支持，这次没有释放";
            }
        }
        return null;
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
        if (host.getBaseDamage() > 0f) {
            return null;
        }
        if (!carriesContact(ReleaseRegistry.get(host.getId()))) {
            return null;
        }
        EnchantmentSnapshot built = EnchantmentSnapshot.from(skill, segment, player);
        if (built == null || built.contactDamage <= 0f) {
            return null;
        }
        return built;
    }

    private static boolean carriesContact(ReleaseDefinition definition) {
        return definition != null && (definition.barrier() != null || definition.domain() != null
                || definition.fieldRadius() > 0f || definition.guard() != null
                || ConditionalEffects.supportsContact(definition));
    }
}

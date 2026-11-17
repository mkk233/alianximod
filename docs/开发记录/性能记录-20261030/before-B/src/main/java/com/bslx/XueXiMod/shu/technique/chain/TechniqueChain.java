package com.bslx.XueXiMod.shu.technique.chain;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

public final class TechniqueChain {
    private TechniqueChain() {}

    public static List<ChainSegment> split(Skill skill) {
        List<ChainSegment> segments = new ArrayList<>();
        List<Integer> enchantBuffer = new ArrayList<>();
        List<Technique> techniques = skill.getTechniques();

        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            Technique technique = techniques.get(techniqueIndex);
            if (technique.isEnchantment()) {
                enchantBuffer.add(techniqueIndex);
            } else if (waitsForHost(technique)) {
                segments.add(new ChainSegment(techniqueIndex, List.of()));
            } else {
                segments.add(new ChainSegment(techniqueIndex, enchantBuffer));
                enchantBuffer.clear();
            }
        }

        for (int index : enchantBuffer) {
            segments.add(new ChainSegment(index, List.of()));
        }
        return segments;
    }

    /** 规则要等到后面的攻击、零伤害术式或单独附魔，不把前面的附魔吞掉。 */
    private static boolean waitsForHost(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        ChainRole role = definition.role();
        if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE || role == ChainRole.RULE_DUAL) {
            return true;
        }
        float supplied = definition.chainEnergyScale();
        return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
    }

    public static int gapBefore(ChainSegment next, ChainSegment previous, Skill skill) {
        List<Technique> techniques = skill.getTechniques();

        int enchantCooldown = 0;
        for (int index : next.enchantIndexes()) {
            Technique enchant = techniques.get(index);
            EnchantmentDefinition definition = EnchantmentRegistry.get(enchant.getId());
            if (definition == null) {
                continue;
            }
            ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
            int cooldown = (int) Math.round(enchant.getBaseCooldown() * reaction.cooldownMultiplier());
            enchantCooldown += Math.max(1, cooldown);
        }
        if (enchantCooldown > 0) {
            return enchantCooldown;
        }

        Technique previousAction = techniques.get(previous.actionIndex());
        if (previousAction.getBaseDamage() > 0) {
            return previousAction.getBaseCooldown();
        }
        return 0;
    }
}

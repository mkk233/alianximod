package com.bslx.XueXiMod.shu.technique.chain;

import com.bslx.XueXiMod.debug.DebugLog;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

public final class TechniqueChain {
    private TechniqueChain() {}

    public static List<ChainSegment> split(Skill skill) {
        List<ChainSegment> segments = new ArrayList<>();
        List<Integer> enchantBuffer = new ArrayList<>();
        List<Technique> techniques = skill.getTechniques();

        for (int i = 0; i < techniques.size(); i++) {
            Technique technique = techniques.get(i);
            if (technique.isEnchantment()) {
                enchantBuffer.add(i);
            } else if (technique.getBaseDamage() > 0) {
                segments.add(new ChainSegment(i, enchantBuffer));
                enchantBuffer.clear();
            } else {
                segments.add(new ChainSegment(i, List.of()));
            }
        }

        for (int index : enchantBuffer) {
            DebugLog.log("附魔未作用：" + techniques.get(index).getId() + " 后面没有伤害术");
        }
        return segments;
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

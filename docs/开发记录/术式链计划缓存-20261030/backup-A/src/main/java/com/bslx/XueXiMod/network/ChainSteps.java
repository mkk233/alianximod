package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

/** 一条链拆成依次释放的几发。规则和前置附魔并进后面的那一发。 */
final class ChainSteps {
    private ChainSteps() {
    }

    static List<List<Integer>> list(Skill skill) {
        List<Technique> techniques = skill.getTechniques();
        List<List<Integer>> steps = new ArrayList<>();
        int techniqueIndex = 0;
        while (techniqueIndex < techniques.size()) {
            List<Integer> step = new ArrayList<>();
            int extraHosts = 0;
            boolean sawHost = false;
            while (techniqueIndex < techniques.size()) {
                Technique technique = techniques.get(techniqueIndex);
                if (SkillSegmentMath.isRemovedRule(technique.getId())) {
                    techniqueIndex++;
                    continue;
                }
                if (waitsForHost(technique)) {
                    extraHosts = Math.max(extraHosts, extraHostsAfter(technique));
                    step.add(techniqueIndex);
                    techniqueIndex++;
                    continue;
                }
                if (technique.isEnchantment() && laterHost(techniques, techniqueIndex + 1) >= 0) {
                    step.add(techniqueIndex);
                    techniqueIndex++;
                    continue;
                }
                step.add(techniqueIndex);
                techniqueIndex++;
                sawHost = true;
                if (extraHosts > 0) {
                    extraHosts--;
                    continue;
                }
                if (actionOpensDual(techniques, technique, techniqueIndex)) {
                    continue;
                }
                break;
            }
            if (sawHost) {
                steps.add(step);
            }
        }
        return steps;
    }

    static String signature(Skill skill) {
        StringBuilder builder = new StringBuilder();
        for (Technique technique : skill.getTechniques()) {
            if (builder.length() > 0) {
                builder.append('|');
            }
            builder.append(technique.getId());
        }
        return builder.toString();
    }

    static Skill slice(Skill skill, List<Integer> indexes) {
        List<Technique> techniques = new ArrayList<>();
        List<ShuAttribute> embryos = new ArrayList<>();
        for (int index : indexes) {
            techniques.add(skill.getTechniques().get(index));
            embryos.add(skill.getEmbryo(index));
        }
        return new Skill(skill.getName(), skill.getType(), techniques, embryos);
    }

    /** 这一发的冷却：本体加上依附的规则和前置附魔，再做一次缩放。 */
    static int cooldown(Skill skill, List<Integer> indexes, double cooldownReduction) {
        int total = 0;
        for (int index : indexes) {
            Technique technique = skill.getTechniques().get(index);
            if (technique.isEnchantment()) {
                total += enchantCooldown(skill, index, technique);
            } else {
                total += technique.getBaseCooldown();
            }
        }
        return SkillSegmentMath.scaleCooldown(total, cooldownReduction);
    }

    private static int enchantCooldown(Skill skill, int index, Technique technique) {
        EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
        if (definition == null) {
            return technique.getBaseCooldown();
        }
        ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
        int cooldown = (int) Math.round(technique.getBaseCooldown() * reaction.cooldownMultiplier());
        return technique.getBaseCooldown() > 0 ? Math.max(1, cooldown) : 0;
    }

    private static int laterHost(List<Technique> techniques, int from) {
        for (int techniqueIndex = from; techniqueIndex < techniques.size(); techniqueIndex++) {
            Technique technique = techniques.get(techniqueIndex);
            if (SkillSegmentMath.isRemovedRule(technique.getId()) || technique.isEnchantment() || waitsForHost(technique)) {
                continue;
            }
            return techniqueIndex;
        }
        return -1;
    }

    /** 双重释放在这一发里再带上后面几段，和原来一次按键拉走的段数一致。 */
    private static int extraHostsAfter(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null || definition.role() != ChainRole.RULE_DUAL) {
            return 0;
        }
        int pull = definition.pullAfterA();
        return pull < 0 ? 64 : pull;
    }

    /** 动作紧挨双重释放时合成同一步。后面没有双重的普通动作仍自己结束。 */
    private static boolean actionOpensDual(List<Technique> techniques, Technique action, int from) {
        ReleaseDefinition definition = ReleaseRegistry.get(action.getId());
        if (definition == null || definition.role() != ChainRole.ACTION) {
            return false;
        }
        for (int laterIndex = from; laterIndex < techniques.size(); laterIndex++) {
            Technique later = techniques.get(laterIndex);
            if (SkillSegmentMath.isRemovedRule(later.getId()) || later.isEnchantment()) {
                continue;
            }
            ReleaseDefinition laterDefinition = ReleaseRegistry.get(later.getId());
            if (laterDefinition != null && laterDefinition.role() == ChainRole.RULE_DUAL) {
                return true;
            }
            if (waitsForHost(later)) {
                continue;
            }
            return false;
        }
        return false;
    }

    private static boolean waitsForHost(Technique technique) {
        if (technique.isEnchantment()) {
            return false;
        }
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
}

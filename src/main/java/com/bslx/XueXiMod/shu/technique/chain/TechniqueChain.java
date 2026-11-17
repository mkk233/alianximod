package com.bslx.XueXiMod.shu.technique.chain;

import com.bslx.XueXiMod.debug.performance.PerfMetric;
import com.bslx.XueXiMod.debug.performance.PerformanceRecorder;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.balance.BalanceDisplay;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

/** 把技能槽拆成链段并计算相邻间隔。释放、命中和扣费不在这里做。 */
public final class TechniqueChain {
    private TechniqueChain() {}

    /** 附魔先暂存；等宿主的规则单独成段，普通动作带走暂存附魔，尾部附魔再各成一段。 */
    public static List<ChainSegment> split(Skill skill) {
        long splitStart = PerformanceRecorder.begin();
        try {
            List<ChainSegment> segments = new ArrayList<>();
            List<Integer> enchantBuffer = new ArrayList<>();
            List<Technique> techniques = skill.getTechniques();

            for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
                Technique technique = techniques.get(techniqueIndex);
        if (technique.isEnchantment() && !isDomainHost(technique)) {
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
        } finally {
            PerformanceRecorder.end(PerfMetric.CHAIN_SPLIT, splitStart);
        }
    }

    /** 规则要等到后面的攻击、零伤害术式或单独附魔，不把前面的附魔吞掉。 */
    private static boolean waitsForHost(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        ChainRole role = definition.role();
        if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_PARALLEL || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE || role == ChainRole.RULE_DUAL
                || role == ChainRole.RULE_COOPERATIVE) {
            return true;
        }
        float supplied = definition.chainEnergyScale();
        return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
    }

    /** 带领域声明或显式领域宿主标记的术式自己成段。普通附魔仍先暂存。 */
    private static boolean isDomainHost(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        return definition.domain() != null || definition.explicitDomainHost();
    }

    /** 下一段附魔冷却累加；没有附魔时，前一段正伤害返回其冷却，否则返回 0。 */
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
            int configured = BalanceDisplay.cooldown(enchant.getId(), enchant.getBaseCooldown());
            int cooled = (int) Math.round(configured * reaction.cooldownMultiplier());
            enchantCooldown += Math.max(1, cooled);
        }
        if (enchantCooldown > 0) {
            return enchantCooldown;
        }

        Technique previousAction = techniques.get(previous.actionIndex());
        if (previousAction.getBaseDamage() > 0) {
            return BalanceDisplay.cooldown(previousAction.getId(), previousAction.getBaseCooldown());
        }
        return 0;
    }
}

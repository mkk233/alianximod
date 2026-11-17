package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastParser;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastSpec;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 一条链拆成依次释放的几发。多重按原始格展开的下一格分步，普通一发仍是一个宿主。 */
final class ChainSteps {
    private ChainSteps() {
    }

    static List<List<Integer>> list(Skill skill) {
        List<Technique> techniques = skill.getTechniques();
        List<List<Integer>> steps = new ArrayList<>();
        int techniqueIndex = 0;
        while (techniqueIndex < techniques.size()) {
            if (isRemoved(techniques, techniqueIndex)) {
                techniqueIndex++;
                continue;
            }
            MulticastHit multicastHit = findMulticast(techniques, techniqueIndex);
            if (multicastHit != null) {
                steps.add(multicastHit.stepIndexes);
                techniqueIndex = multicastHit.nextIndex > techniqueIndex ? multicastHit.nextIndex : techniqueIndex + 1;
                continue;
            }
            List<Integer> step = new ArrayList<>();
            boolean sawHost = false;
            while (techniqueIndex < techniques.size()) {
                Technique technique = techniques.get(techniqueIndex);
                if (isRemoved(techniques, techniqueIndex)) {
                    techniqueIndex++;
                    continue;
                }
                if (multicastSpec(technique) != null) {
                    break;
                }
                if (waitsForHost(technique)) {
                    step.add(techniqueIndex);
                    techniqueIndex++;
                    continue;
                }
                if (technique.isEnchantment() && laterHost(techniques, techniqueIndex + 1) >= 0) {
                    if (!isDomainHost(technique)) {
                    step.add(techniqueIndex);
                    techniqueIndex++;
                    continue;
                    }
                }
                step.add(techniqueIndex);
                techniqueIndex++;
                sawHost = true;
                break;
            }
            if (sawHost) {
                steps.add(step);
            }
        }
        return steps;
    }

    /**
     * 从这一格看是否打开一步多重。动作紧挨多重时，传入动作下标也能拿到它后面的展开。
     * 下标属于传入列表。没有多重时返回 null。
     */
    static MulticastExpansion expansionFrom(List<Technique> techniques, int fromIndex) {
        MulticastHit multicastHit = findMulticast(techniques, fromIndex);
        if (multicastHit == null) {
            return null;
        }
        return multicastHit.expansion;
    }

    /** 两个原始宿主是否落在同一次多重展开里。不再用 pullAfterA 的旧窗口。 */
    static boolean hostsShareExpansion(List<Technique> techniques, int leftIndex, int rightIndex) {
        if (techniques == null || leftIndex < 0 || rightIndex < 0 || leftIndex == rightIndex) {
            return false;
        }
        int techniqueIndex = 0;
        while (techniqueIndex < techniques.size()) {
            MulticastHit multicastHit = findMulticast(techniques, techniqueIndex);
            if (multicastHit == null) {
                techniqueIndex++;
                continue;
            }
            boolean leftFound = false;
            boolean rightFound = false;
            for (MulticastMember member : multicastHit.expansion.orderedMembers()) {
                if (member.hostIndex() == leftIndex) {
                    leftFound = true;
                }
                if (member.hostIndex() == rightIndex) {
                    rightFound = true;
                }
            }
            if (leftFound && rightFound) {
                return true;
            }
            techniqueIndex = multicastHit.nextIndex > techniqueIndex ? multicastHit.nextIndex : techniqueIndex + 1;
        }
        return false;
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

    /** 步进下标的不可变副本。缓存布局和未命中回退都从 {@link #list} 来，不推进游标。 */
    static List<List<Integer>> freezeListed(List<List<Integer>> listed) {
        List<List<Integer>> frozen = new ArrayList<>(listed.size());
        for (List<Integer> step : listed) {
            frozen.add(List.copyOf(step));
        }
        return List.copyOf(frozen);
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

    /**
     * 这一发的冷却。普通步仍是传入下标之和。
     * 多重步只加计费下标，再加前导动作和它前面的附魔。空展开只留这组动作冷却。
     */
    static int cooldown(Skill skill, List<Integer> indexes, double cooldownReduction) {
        int total = 0;
        for (int index : billedIndexes(skill, indexes)) {
            Technique technique = skill.getTechniques().get(index);
            if (technique.isEnchantment()) {
                total += enchantCooldown(skill, index, technique);
            } else {
                List<Integer> attached = enchantIndexes(indexes, index);
                total += DomainSpawnInheritance.displayedAttackCooldown(technique,
                        DomainSpawnInheritance.matchedByOwn(technique.getAttribute(), skill, attached), null);
            }
        }
        return SkillSegmentMath.scaleCooldown(total, cooldownReduction);
    }

    private static List<Integer> billedIndexes(Skill skill, List<Integer> indexes) {
        if (skill == null || indexes == null || indexes.isEmpty()) {
            return List.of();
        }
        int anchor = indexes.get(0);
        for (int index : indexes) {
            if (index < anchor) {
                anchor = index;
            }
        }
        MulticastHit multicastHit = findMulticast(skill.getTechniques(), anchor);
        if (multicastHit == null) {
            return indexes;
        }
        if (!multicastHit.expansion.releases()) {
            List<Integer> actionIndexes = new ArrayList<>();
            for (int index : indexes) {
                if (belongsToLeadingAction(skill, multicastHit.actionSlot, index)) {
                    actionIndexes.add(index);
                }
            }
            return actionIndexes;
        }
        Set<Integer> chargedSlots = new HashSet<>(multicastHit.expansion.chargedIndexes());
        List<Integer> kept = new ArrayList<>();
        for (int index : indexes) {
            if (chargedSlots.contains(index) || belongsToLeadingAction(skill, multicastHit.actionSlot, index)) {
                kept.add(index);
            }
        }
        return kept;
    }

    private static boolean belongsToLeadingAction(Skill skill, int actionSlot, int index) {
        if (actionSlot < 0 || index < 0 || index >= skill.getTechniques().size()) {
            return false;
        }
        if (index == actionSlot) {
            return true;
        }
        return index < actionSlot && skill.getTechniques().get(index).isEnchantment();
    }

    private static int enchantCooldown(Skill skill, int index, Technique technique) {
        EnchantmentDefinition definition = EnchantmentRegistry.get(technique.getId());
        if (definition == null) {
            return SkillSegmentMath.configuredBaseCooldown(technique);
        }
        ElementReaction reaction = ElementReaction.of(skill.getEmbryo(index), definition.element());
        int configured = SkillSegmentMath.configuredBaseCooldown(technique);
        int cooldown = (int) Math.round(configured * reaction.cooldownMultiplier());
        return configured > 0 ? Math.max(1, cooldown) : 0;
    }

    private static List<Integer> enchantIndexes(List<Integer> indexes, int hostIndex) {
        List<Integer> enchantIndexes = new ArrayList<>();
        for (int index : indexes) {
            if (index < hostIndex) {
                enchantIndexes.add(index);
            }
        }
        return enchantIndexes;
    }

    private static MulticastHit findMulticast(List<Technique> techniques, int fromIndex) {
        if (techniques == null || fromIndex < 0 || fromIndex >= techniques.size()) {
            return null;
        }
        int probed = indexAfterPrefixes(techniques, fromIndex);
        int actionSlot = -1;
        int opened = -1;
        if (probed < techniques.size() && isAction(techniques.get(probed))) {
            int afterAction = MulticastParser.openingIndex(techniques, probed + 1);
            if (afterAction >= 0) {
                actionSlot = probed;
                opened = afterAction;
            }
        }
        if (opened < 0) {
            opened = MulticastParser.openingIndex(techniques, fromIndex);
        }
        if (opened < 0) {
            return null;
        }
        MulticastExpansion expansion = MulticastParser.parse(techniques, opened);
        if (expansion == null || !expansion.applicable()) {
            return null;
        }
        List<Integer> stepIndexes = new ArrayList<>();
        if (actionSlot >= 0) {
            for (int slotIndex = fromIndex; slotIndex <= actionSlot && slotIndex < techniques.size(); slotIndex++) {
                if (!isRemoved(techniques, slotIndex)) {
                    stepIndexes.add(slotIndex);
                }
            }
        }
        for (int consumedIndex : expansion.consumedIndexes()) {
            if (consumedIndex >= fromIndex && (actionSlot < 0 || consumedIndex > actionSlot)) {
                stepIndexes.add(consumedIndex);
            }
        }
        if (stepIndexes.isEmpty()) {
            return null;
        }
        int nextIndex = expansion.nextIndex();
        if (nextIndex < fromIndex) {
            nextIndex = fromIndex;
        }
        return new MulticastHit(expansion, List.copyOf(stepIndexes), nextIndex, actionSlot);
    }

    private static int indexAfterPrefixes(List<Technique> techniques, int fromIndex) {
        int probeIndex = fromIndex;
        while (probeIndex < techniques.size() && prefixBeforeHost(techniques.get(probeIndex))) {
            probeIndex++;
        }
        return probeIndex;
    }

    private static boolean prefixBeforeHost(Technique technique) {
        if (technique == null) {
            return false;
        }
        if (SkillSegmentMath.isRemovedRule(technique.getId()) || technique.isEnchantment()) {
            return !isDomainHost(technique);
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null || definition.multicastSpec() != null) {
            return false;
        }
        if (definition.replication() != null) {
            return true;
        }
        ChainRole role = definition.role();
        if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_PARALLEL || role == ChainRole.TRAJECTORY_SCATTER) {
            return true;
        }
        float supplied = definition.chainEnergyScale();
        return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
    }

    private static int laterHost(List<Technique> techniques, int from) {
        for (int techniqueIndex = from; techniqueIndex < techniques.size(); techniqueIndex++) {
            Technique technique = techniques.get(techniqueIndex);
            if (isRemoved(techniques, techniqueIndex) || technique.isEnchantment() || waitsForHost(technique)) {
                if (technique == null || !technique.isEnchantment() || !isDomainHost(technique)) {
                    continue;
                }
            }
            if (multicastSpec(technique) != null) {
                return -1;
            }
            return techniqueIndex;
        }
        return -1;
    }

    private static boolean waitsForHost(Technique technique) {
        if ((technique.isEnchantment() && !isDomainHost(technique)) || multicastSpec(technique) != null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return false;
        }
        ChainRole role = definition.role();
        if (role == ChainRole.SEEK_SPACE || role == ChainRole.SEEK_ENTITY
                || role == ChainRole.TRAJECTORY_TRACKING || role == ChainRole.TRAJECTORY_DROP
                || role == ChainRole.TRAJECTORY_PARALLEL || role == ChainRole.TRAJECTORY_SCATTER
                || role == ChainRole.RULE_TRIPLE || definition.replication() != null) {
            return true;
        }
        float supplied = definition.chainEnergyScale();
        return Float.isFinite(supplied) && Math.abs(supplied - 1f) > 0.001f;
    }

    /** 自带领域或显式领域宿主标记。普通附魔两者都没有。 */
    private static boolean isDomainHost(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && (definition.domain() != null || definition.explicitDomainHost());
    }

    private static boolean isAction(Technique technique) {
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        return definition != null && definition.role() == ChainRole.ACTION;
    }

    private static boolean isRemoved(List<Technique> techniques, int techniqueIndex) {
        Technique technique = techniques.get(techniqueIndex);
        return technique != null && SkillSegmentMath.isRemovedRule(technique.getId());
    }

    private static MulticastSpec multicastSpec(Technique technique) {
        if (technique == null) {
            return null;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return null;
        }
        return definition.multicastSpec();
    }

    private static final class MulticastHit {
        private final MulticastExpansion expansion;
        private final List<Integer> stepIndexes;
        private final int nextIndex;
        private final int actionSlot;

        private MulticastHit(MulticastExpansion expansion, List<Integer> stepIndexes, int nextIndex, int actionSlot) {
            this.expansion = expansion;
            this.stepIndexes = stepIndexes;
            this.nextIndex = nextIndex;
            this.actionSlot = actionSlot;
        }
    }
}

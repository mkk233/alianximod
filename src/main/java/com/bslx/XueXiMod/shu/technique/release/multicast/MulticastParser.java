package com.bslx.XueXiMod.shu.technique.release.multicast;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 唯一的原始装配格多重解析器。读取 {@code Skill.getTechniques()} 的下标，不先做 {@code TechniqueChain.split}。
 * 普通非多重返回不适用的空展开，调用者保持原路径。
 */
public final class MulticastParser {
    private MulticastParser() {
    }

    /**
     * 从链游标向后找本步打开的多重。跳过紧邻的已移除规则、前缀修饰、复制和附魔。
     * 没有多重时返回 -1。
     */
    public static int openingIndex(List<Technique> techniques, int fromIndex) {
        if (techniques == null || fromIndex < 0) {
            return -1;
        }
        int scanIndex = fromIndex;
        while (scanIndex < techniques.size()) {
            Technique technique = techniques.get(scanIndex);
            if (technique == null) {
                return -1;
            }
            if (prefixSlot(technique)) {
                scanIndex++;
                continue;
            }
            if (multicastOf(technique) != null) {
                return scanIndex;
            }
            return -1;
        }
        return -1;
    }

    /**
     * 解析根多重。{@code rootIndex} 必须是多重声明那一格，通常先问 {@link #openingIndex}。
     * 结果只含下标和规则形状。
     */
    public static MulticastExpansion parse(List<Technique> techniques, int rootIndex) {
        if (techniques == null || rootIndex < 0 || rootIndex >= techniques.size()) {
            return MulticastExpansion.notApplicable(rootIndex);
        }
        Technique rootTechnique = techniques.get(rootIndex);
        MulticastSpec rootSpec = multicastOf(rootTechnique);
        if (rootSpec == null) {
            return MulticastExpansion.notApplicable(rootIndex);
        }
        ParseState state = new ParseState(techniques);
        List<Integer> inherited = new ArrayList<>();
        List<PendingMark> seeded = new ArrayList<>();
        absorbPrefix(state, techniques, rootIndex, inherited, seeded);
        state.consumed.add(rootIndex);
        state.cursorIndex = rootIndex + 1;
        Set<Integer> producedMulticasts = new LinkedHashSet<>();
        ScopeOutcome outcome = readScope(state, rootSpec, seeded, inherited, producedMulticasts);
        resolveReturned(state, outcome.returnedPending);
        if (!state.members.isEmpty()) {
            producedMulticasts.add(rootIndex);
        }
        return finish(state, rootIndex, inherited, producedMulticasts);
    }

    /** 从根多重向前收紧邻前缀：移除规则和面对多重的复制忽略，附魔暂存，索敌弹道增幅留给整段。 */
    private static void absorbPrefix(ParseState state, List<Technique> techniques, int rootIndex,
                                      List<Integer> inherited, List<PendingMark> seeded) {
        List<Integer> prefixReverse = new ArrayList<>();
        int scanIndex = rootIndex - 1;
        while (scanIndex >= 0) {
            Technique earlier = techniques.get(scanIndex);
            if (!prefixSlot(earlier)) {
                break;
            }
            prefixReverse.add(scanIndex);
            scanIndex--;
        }
        for (int reverseIndex = prefixReverse.size() - 1; reverseIndex >= 0; reverseIndex--) {
            int slotIndex = prefixReverse.get(reverseIndex);
            Technique technique = techniques.get(slotIndex);
            state.consumed.add(slotIndex);
            if (removedRule(technique)) {
                ignore(state, slotIndex, MulticastIgnoreReason.REMOVED_RULE);
                continue;
            }
            if (replicationOf(technique) != null) {
                ignore(state, slotIndex, MulticastIgnoreReason.IGNORED_COPY_ON_MULTICAST);
                continue;
            }
            if (technique.isEnchantment()) {
                if (domainHost(technique)) {
                    continue;
                }
                seeded.add(new PendingMark(slotIndex, true));
                continue;
            }
            if (inheritableModifier(technique)) {
                inherited.add(slotIndex);
            }
        }
    }

    /** 固定次数读指定格，读到链尾则一直读。宿主产出成员，修饰暂存，内层多重递归。 */
    private static ScopeOutcome readScope(ParseState state, MulticastSpec spec, List<PendingMark> incoming,
                                           List<Integer> inherited, Set<Integer> producedMulticasts) {
        List<PendingMark> pending = new ArrayList<>();
        if (incoming != null) {
            for (PendingMark mark : incoming) {
                pending.add(new PendingMark(mark.slotIndex, mark.originatedHere));
            }
        }
        boolean untilEnd = spec.kind() == MulticastSpec.Kind.REMAINING;
        int readsLeft = untilEnd ? 0 : spec.fixedCount();
        boolean produced = false;
        while (state.cursorIndex < state.slotCount() && (untilEnd || readsLeft > 0)) {
            Technique technique = state.techniques.get(state.cursorIndex);
            if (technique == null) {
                break;
            }
            int slotIndex = state.cursorIndex;
            state.cursorIndex++;
            state.consumed.add(slotIndex);
            if (removedRule(technique)) {
                ignore(state, slotIndex, MulticastIgnoreReason.REMOVED_RULE);
                continue;
            }
            if (!untilEnd) {
                readsLeft--;
            }
            MulticastSpec nestedSpec = multicastOf(technique);
            if (nestedSpec != null) {
                if (openNested(state, slotIndex, nestedSpec, pending, inherited, producedMulticasts)) {
                    produced = true;
                }
                continue;
            }
            if (replicationOf(technique) != null || technique.isEnchantment() || inheritableModifier(technique)) {
                if (domainHost(technique)) {
                    pending.clear();
                    continue;
                }
                pending.add(new PendingMark(slotIndex, true));
                continue;
            }
            emitHost(state, slotIndex, technique, pending, inherited);
            produced = true;
        }
        List<PendingMark> returnedPending = new ArrayList<>();
        for (PendingMark mark : pending) {
            if (!mark.originatedHere) {
                returnedPending.add(mark);
                continue;
            }
            Technique pendingTechnique = state.techniques.get(mark.slotIndex);
            if (pendingTechnique != null && pendingTechnique.isEnchantment()) {
                if (domainHost(pendingTechnique)) {
                    continue;
                }
                emitIndependent(state, mark.slotIndex);
                produced = true;
                continue;
            }
            ignore(state, mark.slotIndex, MulticastIgnoreReason.UNUSED_MODIFIER);
        }
        return new ScopeOutcome(returnedPending, produced);
    }

    /** 把当前暂存交给内层。面对多重的复制直接丢掉，未用完的再按原来的归属交还。 */
    private static boolean openNested(ParseState state, int slotIndex, MulticastSpec nestedSpec,
                                       List<PendingMark> pending, List<Integer> inherited,
                                       Set<Integer> producedMulticasts) {
        Map<Integer, Boolean> originOwned = new LinkedHashMap<>();
        List<PendingMark> handed = new ArrayList<>();
        for (PendingMark mark : pending) {
            Technique pendingTechnique = state.techniques.get(mark.slotIndex);
            if (replicationOf(pendingTechnique) != null) {
                ignore(state, mark.slotIndex, MulticastIgnoreReason.IGNORED_COPY_ON_MULTICAST);
                continue;
            }
            originOwned.put(mark.slotIndex, mark.originatedHere);
            handed.add(new PendingMark(mark.slotIndex, false));
        }
        pending.clear();
        ScopeOutcome childOutcome = readScope(state, nestedSpec, handed, inherited, producedMulticasts);
        for (PendingMark returnedMark : childOutcome.returnedPending) {
            Boolean ownedFlag = originOwned.get(returnedMark.slotIndex);
            boolean ownedHere = ownedFlag != null && ownedFlag;
            pending.add(new PendingMark(returnedMark.slotIndex, ownedHere));
        }
        if (childOutcome.producedMember) {
            producedMulticasts.add(slotIndex);
        }
        return childOutcome.producedMember;
    }

    /** 根层退回的附魔单独执行；其他退回修饰记为未使用。 */
    private static void resolveReturned(ParseState state, List<PendingMark> returnedPending) {
        for (PendingMark mark : returnedPending) {
            Technique technique = state.techniques.get(mark.slotIndex);
            if (technique != null && technique.isEnchantment()) {
                if (domainHost(technique)) {
                    continue;
                }
                emitIndependent(state, mark.slotIndex);
                continue;
            }
            ignore(state, mark.slotIndex, MulticastIgnoreReason.UNUSED_MODIFIER);
        }
    }

    /** 生成宿主成员，并把能接上的暂存分成局部修饰、复制和附魔。 */
    private static void emitHost(ParseState state, int slotIndex, Technique technique, List<PendingMark> pending,
                                  List<Integer> inherited) {
        List<Integer> localModifiers = new ArrayList<>();
        List<Integer> replicationIndexes = new ArrayList<>();
        List<Integer> attachedEnchants = new ArrayList<>();
        attachPending(state, pending, technique, inherited, localModifiers, replicationIndexes, attachedEnchants);
        state.members.add(new MulticastMember(state.members.size(), slotIndex, false,
                inheritedFor(state, technique, inherited), localModifiers, replicationIndexes, attachedEnchants));
    }

    /** 没有宿主的附魔单独成为成员，不继承修饰、复制或附魔。 */
    private static void emitIndependent(ParseState state, int slotIndex) {
        state.members.add(new MulticastMember(state.members.size(), slotIndex, true,
                List.of(), List.of(), List.of(), List.of()));
    }

    /** 从前缀修饰里留下这个宿主能够接受的下标。 */
    private static List<Integer> inheritedFor(ParseState state, Technique technique, List<Integer> inherited) {
        List<Integer> accepted = new ArrayList<>();
        for (int slotIndex : inherited) {
            if (slotIndex < 0 || slotIndex >= state.slotCount()) {
                continue;
            }
            Technique modifier = state.techniques.get(slotIndex);
            if (acceptsModifier(technique, modifier)) {
                accepted.add(slotIndex);
            }
        }
        return List.copyOf(accepted);
    }

    /** 按宿主能力摘走暂存。接不上的留下，交给后面的宿主。 */
    private static void attachPending(ParseState state, List<PendingMark> pending, Technique host,
                                       List<Integer> inherited, List<Integer> localModifiers,
                                       List<Integer> replicationIndexes, List<Integer> attachedEnchants) {
        boolean enchantHost = host.canReceiveEnchantment();
        int markIndex = 0;
        while (markIndex < pending.size()) {
            PendingMark mark = pending.get(markIndex);
            Technique pendingTechnique = state.techniques.get(mark.slotIndex);
            if (pendingTechnique == null) {
                markIndex++;
                continue;
            }
            boolean taken = false;
            if (replicationOf(pendingTechnique) != null && acceptsReplication(host)) {
                replicationIndexes.add(mark.slotIndex);
                taken = true;
            } else if (acceptsModifier(host, pendingTechnique)) {
                if (!inherited.contains(mark.slotIndex)) {
                    localModifiers.add(mark.slotIndex);
                }
                taken = true;
            } else if (enchantHost && pendingTechnique.isEnchantment()) {
                attachedEnchants.add(mark.slotIndex);
                taken = true;
            }
            if (taken) {
                pending.remove(markIndex);
                continue;
            }
            markIndex++;
        }
    }

    /** 非附魔且释放定义接受复制时为真。 */
    private static boolean acceptsReplication(Technique host) {
        if (host == null || (host.isEnchantment() && !domainHost(host))) {
            return false;
        }
        ReleaseDefinition definition = definitionOf(host);
        return definition != null && definition.acceptsReplication();
    }

    /** 复制、索敌、弹道和增幅各看宿主已有能力。接不上的留给后面第一个适用宿主。 */
    private static boolean acceptsModifier(Technique host, Technique modifier) {
        if (host == null || modifier == null || host.isEnchantment() || !inheritableModifier(modifier)) {
            return false;
        }
        ReleaseDefinition hostDefinition = definitionOf(host);
        ReleaseDefinition modifierDefinition = definitionOf(modifier);
        if (hostDefinition == null || modifierDefinition == null) {
            return false;
        }
        ChainRole role = modifierDefinition.role();
        if (role == ChainRole.SEEK_SPACE || role == ChainRole.TRAJECTORY_SCATTER) {
            return hostDefinition.acceptsPlacementModifier();
        }
        if (role == ChainRole.SEEK_ENTITY) {
            return hostDefinition.acceptsInitialAim() || hostDefinition.acceptsTargetOverride()
                    || hostDefinition.trajectoryMutable();
        }
        if (role == ChainRole.TRAJECTORY_DROP || role == ChainRole.TRAJECTORY_PARALLEL
                || role == ChainRole.TRAJECTORY_TRACKING) {
            return hostDefinition.acceptsPathOverride() || hostDefinition.trajectoryMutable();
        }
        return acceptsChainEnergy(host, hostDefinition, modifierDefinition);
    }

    /** 只有法术命中体接受偏离 1 的有限正链聚能。 */
    private static boolean acceptsChainEnergy(Technique host, ReleaseDefinition hostDefinition,
                                               ReleaseDefinition modifierDefinition) {
        float supplied = modifierDefinition.chainEnergyScale();
        if (!Float.isFinite(supplied) || supplied <= 0f || Math.abs(supplied - 1f) <= 0.001f) {
            return false;
        }
        if (host.getType() != TechniqueType.MAGIC || host.isEnchantment()) {
            return false;
        }
        ShuAttribute attribute = host.getAttribute();
        if (attribute == null || attribute == ShuAttribute.NONE) {
            return false;
        }
        return hostDefinition.kind() == ReleaseKind.HIT_VOLUME;
    }

    /** 有成员才汇总计费下标；读过但未计费、未忽略的格子补记为未使用。 */
    private static MulticastExpansion finish(ParseState state, int rootIndex, List<Integer> inherited,
                                              Set<Integer> producedMulticasts) {
        boolean releases = !state.members.isEmpty();
        List<Integer> charged = new ArrayList<>();
        if (releases) {
            LinkedHashSet<Integer> chargeSet = new LinkedHashSet<>();
            chargeSet.addAll(producedMulticasts);
            for (MulticastMember member : state.members) {
                chargeSet.add(member.hostIndex());
                chargeSet.addAll(member.inheritedModifierIndexes());
                chargeSet.addAll(member.localModifierIndexes());
                chargeSet.addAll(member.replicationIndexes());
                chargeSet.addAll(member.attachedEnchantIndexes());
            }
            charged.addAll(chargeSet);
            Collections.sort(charged);
        }
        Set<Integer> chargedLookup = new HashSet<>(charged);
        for (int consumedIndex : state.consumed) {
            if (chargedLookup.contains(consumedIndex) || alreadyIgnored(state, consumedIndex)) {
                continue;
            }
            if (!releases && consumedIndex == rootIndex) {
                continue;
            }
            ignore(state, consumedIndex, MulticastIgnoreReason.UNUSED_MODIFIER);
        }
        state.ignored.sort(Comparator.comparingInt(MulticastIgnore::slotIndex));
        List<Integer> consumed = new ArrayList<>(state.consumed);
        Collections.sort(consumed);
        return new MulticastExpansion(true, releases, rootIndex, state.cursorIndex,
                consumed, charged, state.ignored, state.members, inherited);
    }

    private static void ignore(ParseState state, int slotIndex, MulticastIgnoreReason reason) {
        if (alreadyIgnored(state, slotIndex)) {
            return;
        }
        state.ignored.add(new MulticastIgnore(slotIndex, reason));
    }

    private static boolean alreadyIgnored(ParseState state, int slotIndex) {
        for (MulticastIgnore ignored : state.ignored) {
            if (ignored.slotIndex() == slotIndex) {
                return true;
            }
        }
        return false;
    }

    private static boolean prefixSlot(Technique technique) {
        return technique != null && !domainHost(technique) && (removedRule(technique) || technique.isEnchantment()
                || inheritableModifier(technique) || replicationOf(technique) != null);
    }

    /** 领域附魔自己开域，不进入多重的附魔槽。 */
    private static boolean domainHost(Technique technique) {
        if (technique == null) {
            return false;
        }
        ReleaseDefinition definition = definitionOf(technique);
        return definition != null && (definition.domain() != null || definition.explicitDomainHost());
    }

    private static boolean inheritableModifier(Technique technique) {
        if (technique == null || technique.isEnchantment() || removedRule(technique)) {
            return false;
        }
        ReleaseDefinition definition = definitionOf(technique);
        if (definition == null || definition.multicastSpec() != null || definition.replication() != null) {
            return false;
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

    private static boolean removedRule(Technique technique) {
        if (technique == null) {
            return false;
        }
        String techniqueId = technique.getId();
        return "rule_interleave".equals(techniqueId) || "rule_damage_tally".equals(techniqueId);
    }

    private static MulticastSpec multicastOf(Technique technique) {
        ReleaseDefinition definition = definitionOf(technique);
        if (definition == null) {
            return null;
        }
        return definition.multicastSpec();
    }

    private static ReplicationSpec replicationOf(Technique technique) {
        ReleaseDefinition definition = definitionOf(technique);
        if (definition == null || definition.multicastSpec() != null) {
            return null;
        }
        return definition.replication();
    }

    private static ReleaseDefinition definitionOf(Technique technique) {
        if (technique == null) {
            return null;
        }
        return ReleaseRegistry.get(technique.getId());
    }

    private static final class PendingMark {
        private final int slotIndex;
        private final boolean originatedHere;

        private PendingMark(int slotIndex, boolean originatedHere) {
            this.slotIndex = slotIndex;
            this.originatedHere = originatedHere;
        }
    }

    private static final class ScopeOutcome {
        private final List<PendingMark> returnedPending;
        private final boolean producedMember;

        private ScopeOutcome(List<PendingMark> returnedPending, boolean producedMember) {
            this.returnedPending = returnedPending;
            this.producedMember = producedMember;
        }
    }

    private static final class ParseState {
        private final List<Technique> techniques;
        private int cursorIndex;
        private final List<Integer> consumed = new ArrayList<>();
        private final List<MulticastIgnore> ignored = new ArrayList<>();
        private final List<MulticastMember> members = new ArrayList<>();

        private ParseState(List<Technique> techniques) {
            this.techniques = techniques;
        }

        private int slotCount() {
            return techniques.size();
        }
    }
}

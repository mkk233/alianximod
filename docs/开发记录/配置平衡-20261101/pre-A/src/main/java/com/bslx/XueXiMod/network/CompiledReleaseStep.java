package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 同一次按键的选段、费用和冷却都读这一份。
 * 多重成员按展开的原始宿主排列，不再截成两个。普通动作加单一条件仍走原来的段下标。
 */
final class CompiledReleaseStep {
    static final String EMPTY_MULTICAST_REFUSAL = "这次多重没有可释放的成员，没有释放";

    final int actionSegment;
    final int dualSegment;
    final List<ChainSegment> members;
    final List<Integer> memberShares;
    final int ruleShare;
    final int actionShare;
    final int fullCooldown;
    final int actionCooldown;
    final boolean actionLed;
    final String chargeRefusal;
    final List<ReleaseInput> memberInputs;
    final ReleaseInput actionInput;
    final MulticastExpansion expansion;
    final List<MemberCastView> memberViews;

    private CompiledReleaseStep(int actionSegment, int dualSegment, List<ChainSegment> members,
                                List<Integer> memberShares, int ruleShare, int actionShare,
                                int fullCooldown, int actionCooldown, boolean actionLed, String chargeRefusal,
                                List<ReleaseInput> memberInputs, ReleaseInput actionInput,
                                MulticastExpansion expansion, List<MemberCastView> memberViews) {
        this.actionSegment = actionSegment;
        this.dualSegment = dualSegment;
        this.members = List.copyOf(members);
        this.memberShares = List.copyOf(memberShares);
        this.ruleShare = ruleShare;
        this.actionShare = actionShare;
        this.fullCooldown = fullCooldown;
        this.actionCooldown = actionCooldown;
        this.actionLed = actionLed;
        this.chargeRefusal = chargeRefusal;
        this.memberInputs = List.copyOf(memberInputs);
        this.actionInput = actionInput == null ? ReleaseInput.BASE : actionInput;
        this.expansion = expansion;
        this.memberViews = List.copyOf(memberViews);
    }

    static CompiledReleaseStep compile(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                       double cooldownReduction) {
        return compile(skill, techniques, segments, cooldownReduction, null);
    }

    static CompiledReleaseStep compile(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                       double cooldownReduction, CastPlanContext planContext) {
        if (planContext != null && planContext.canReuseStructure() && planContext.releaseResolved()) {
            return applyChoice(skill, techniques, segments, cooldownReduction, planContext.releaseChoice(), planContext);
        }
        return applyChoice(skill, techniques, segments, cooldownReduction,
                chooseStructure(skill, techniques, segments), null);
    }

    /** 多重或动作后单一条件的结构选择。普通链返回 null。费用和冷却留到 {@link #applyChoice}。 */
    static StructureChoice chooseStructure(Skill skill, List<Technique> techniques, List<ChainSegment> segments) {
        MulticastExpansion expansion = ChainSteps.expansionFrom(techniques, 0);
        if (expansion != null && expansion.applicable()) {
            return chooseExpansion(skill, techniques, segments, expansion);
        }
        int actionSegment = -1;
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            Technique technique = techniques.get(segments.get(segmentIndex).actionIndex());
            if (SkillSegmentMath.roleOf(technique) == ChainRole.ACTION && actionSegment < 0) {
                actionSegment = segmentIndex;
            }
        }
        return chooseActionConditional(skill, techniques, segments, actionSegment);
    }

    /**
     * 没有多重、动作后面只有一个条件释放时，沿用动作会话。
     * 前面或后面还有别的可释放段时返回空，留给原来的按键路径。
     */
    private static StructureChoice chooseActionConditional(Skill skill, List<Technique> techniques,
                                                           List<ChainSegment> segments, int actionSegment) {
        if (actionSegment < 0) {
            return null;
        }
        for (int earlierIndex = 0; earlierIndex < actionSegment; earlierIndex++) {
            Technique earlier = techniques.get(segments.get(earlierIndex).actionIndex());
            if (SkillSegmentMath.isRemovedRule(earlier.getId()) || SkillSegmentMath.isChainModifier(earlier)) {
                continue;
            }
            return null;
        }
        ChainSegment conditionalMember = null;
        for (int segmentIndex = actionSegment + 1; segmentIndex < segments.size(); segmentIndex++) {
            ChainSegment candidate = segments.get(segmentIndex);
            Technique technique = techniques.get(candidate.actionIndex());
            if (SkillSegmentMath.isRemovedRule(technique.getId()) || SkillSegmentMath.isChainModifier(technique)) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (technique.getBaseDamage() > 0f || definition == null || definition.conditionalEffect() == null) {
                return null;
            }
            if (conditionalMember != null) {
                return null;
            }
            conditionalMember = candidate;
        }
        if (conditionalMember == null) {
            return null;
        }
        List<Integer> memberIndexes = List.of(segments.indexOf(conditionalMember));
        return new StructureChoice(actionSegment, -1, memberIndexes,
                freezeIndexes(skill, techniques, segments, memberIndexes),
                freezeAction(skill, techniques, segments, actionSegment),
                true, null);
    }

    private static StructureChoice chooseExpansion(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                                   MulticastExpansion expansion) {
        int actionSlot = leadingActionSlot(techniques, expansion);
        int actionSegment = segmentWithAction(segments, actionSlot);
        int dualSegment = segmentWithAction(segments, expansion.startIndex());
        List<Integer> memberIndexes = new ArrayList<>();
        List<ReleaseInput> memberInputs = new ArrayList<>();
        if (expansion.releases()) {
            for (MulticastMember member : expansion.orderedMembers()) {
                MemberCastView view = MemberCastView.from(skill, techniques, member);
                memberIndexes.add(view.hostIndex);
                memberInputs.add(view.chainInput);
            }
        }
        ReleaseInput actionInput = actionSlot < 0
                ? ReleaseInput.BASE
                : SkillChainPlan.frozenChainInput(skill, techniques, actionSlot);
        return new StructureChoice(actionSegment, dualSegment, memberIndexes, memberInputs, actionInput,
                actionSlot >= 0, expansion);
    }

    private static CompiledReleaseStep applyChoice(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                                   double cooldownReduction, StructureChoice choice,
                                                   CastPlanContext planContext) {
        if (choice == null) {
            return null;
        }
        if (choice.expansion != null) {
            return applyExpansion(skill, techniques, segments, cooldownReduction, choice);
        }
        List<ChainSegment> members = new ArrayList<>();
        for (int memberIndex : choice.memberIndexes) {
            members.add(segments.get(memberIndex));
        }
        List<Integer> memberShares = new ArrayList<>();
        for (ChainSegment member : members) {
            memberShares.add(memberShare(techniques, segments, member, planContext));
        }
        int ruleShare = choice.dualSegment < 0
                ? 0
                : SkillSegmentMath.segmentManaCost(techniques, segments.get(choice.dualSegment));
        int actionShare = choice.actionSegment < 0
                ? 0
                : SkillSegmentMath.segmentManaCost(techniques, segments.get(choice.actionSegment));
        List<Integer> everyIndex = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            everyIndex.add(techniqueIndex);
        }
        int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
        int actionCooldown = 0;
        if (choice.actionSegment >= 0) {
            ChainSegment action = segments.get(choice.actionSegment);
            List<Integer> actionIndexes = new ArrayList<>();
            actionIndexes.add(action.actionIndex());
            actionIndexes.addAll(action.enchantIndexes());
            actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
        }
        String refusal = chargeRefusal(techniques, members);
        return new CompiledReleaseStep(choice.actionSegment, choice.dualSegment, members, memberShares, ruleShare, actionShare,
                fullCooldown, actionCooldown, choice.actionLed, refusal, choice.memberInputs, choice.actionInput,
                null, List.of());
    }

    private static CompiledReleaseStep applyExpansion(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                                      double cooldownReduction, StructureChoice choice) {
        MulticastExpansion expansion = choice.expansion;
        List<MemberCastView> views = new ArrayList<>();
        List<ChainSegment> members = new ArrayList<>();
        if (expansion.releases()) {
            for (MulticastMember member : expansion.orderedMembers()) {
                MemberCastView view = MemberCastView.from(skill, techniques, member);
                views.add(view);
                members.add(new ChainSegment(view.hostIndex, view.attachedEnchantIndexes));
            }
        }
        List<Integer> memberShares = new ArrayList<>();
        int ruleShare = 0;
        int actionShare = 0;
        boolean quoteOverflow = false;
        if (expansion.releases()) {
            for (MemberCastView view : views) {
                if (view.quoteOverflow) {
                    quoteOverflow = true;
                    break;
                }
            }
            if (!quoteOverflow) {
                Integer ruled = exactExpansionRuleShare(techniques, expansion);
                if (ruled == null) {
                    quoteOverflow = true;
                } else {
                    ruleShare = ruled;
                    int runningBill = ruleShare;
                    for (int viewIndex = 0; viewIndex < views.size(); viewIndex++) {
                        Integer share = memberShareExact(techniques, members.get(viewIndex), views.get(viewIndex).copyCount);
                        if (share == null) {
                            quoteOverflow = true;
                            break;
                        }
                        Integer nextBill = MulticastArithmetic.add(runningBill, share);
                        if (nextBill == null) {
                            quoteOverflow = true;
                            break;
                        }
                        runningBill = nextBill;
                        memberShares.add(share);
                    }
                }
            }
        }
        int actionSlot = leadingActionSlot(techniques, expansion);
        if (!quoteOverflow && actionSlot >= 0) {
            Integer actionBill = actionShareExact(techniques, actionSlot);
            if (actionBill == null) {
                quoteOverflow = true;
            } else {
                actionShare = actionBill;
            }
        }
        if (quoteOverflow) {
            memberShares.clear();
            ruleShare = 0;
            actionShare = 0;
        }
        List<Integer> everyIndex = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            everyIndex.add(techniqueIndex);
        }
        int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
        int actionCooldown = actionSlotCooldown(skill, techniques, actionSlot, cooldownReduction);
        String refusal = !expansion.releases()
                ? EMPTY_MULTICAST_REFUSAL
                : quoteOverflow
                ? MulticastArithmetic.OVERFLOW_REFUSAL
                : chargeRefusal(techniques, members);
        int actionSegment = choice.actionSegment >= 0 && choice.actionSegment < segments.size()
                ? choice.actionSegment
                : segmentWithAction(segments, actionSlot);
        return new CompiledReleaseStep(actionSegment, choice.dualSegment, members, memberShares, ruleShare, actionShare,
                fullCooldown, actionCooldown, choice.actionLed, refusal, choice.memberInputs, choice.actionInput,
                expansion, views);
    }

    static final class StructureChoice {
        final int actionSegment;
        final int dualSegment;
        /** 非多重时是 {@code segments} 的下标。多重时是成员宿主的术式下标，不能再拿去 {@code segments.get}。 */
        final List<Integer> memberIndexes;
        final List<ReleaseInput> memberInputs;
        final ReleaseInput actionInput;
        final boolean actionLed;
        final MulticastExpansion expansion;

        private StructureChoice(int actionSegment, int dualSegment, List<Integer> memberIndexes,
                                List<ReleaseInput> memberInputs, ReleaseInput actionInput, boolean actionLed,
                                MulticastExpansion expansion) {
            this.actionSegment = actionSegment;
            this.dualSegment = dualSegment;
            this.memberIndexes = List.copyOf(memberIndexes);
            this.memberInputs = List.copyOf(memberInputs);
            this.actionInput = actionInput == null ? ReleaseInput.BASE : actionInput;
            this.actionLed = actionLed;
            this.expansion = expansion;
        }

        MulticastExpansion expansion() {
            return expansion;
        }
    }

    private static List<ReleaseInput> freezeIndexes(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                                    List<Integer> memberIndexes) {
        List<ReleaseInput> frozenInputs = new ArrayList<>();
        for (int memberIndex : memberIndexes) {
            frozenInputs.add(SkillChainPlan.frozenChainInput(skill, techniques, segments.get(memberIndex).actionIndex()));
        }
        return frozenInputs;
    }

    private static ReleaseInput freezeAction(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                             int actionSegment) {
        if (actionSegment < 0 || actionSegment >= segments.size()) {
            return ReleaseInput.BASE;
        }
        return SkillChainPlan.frozenChainInput(skill, techniques, segments.get(actionSegment).actionIndex());
    }

    ReleaseInput memberInput(int memberIndex) {
        if (memberIndex < 0 || memberIndex >= this.memberInputs.size()) {
            return ReleaseInput.BASE;
        }
        return this.memberInputs.get(memberIndex);
    }

    MulticastExpansion expansion() {
        return expansion;
    }

    boolean multicast() {
        return expansion != null && expansion.applicable();
    }

    int memberCount() {
        return members.size();
    }

    MemberCastView memberView(int memberIndex) {
        if (memberIndex < 0 || memberIndex >= memberViews.size()) {
            return null;
        }
        return memberViews.get(memberIndex);
    }

    List<MemberCastView> memberViews() {
        return memberViews;
    }

    int gapTicks(int memberOrdinal) {
        if (expansion == null) {
            return 0;
        }
        return expansion.gapTicks(memberOrdinal);
    }

    boolean chargesTechnique(int techniqueIndex) {
        if (expansion == null) {
            return false;
        }
        for (int chargedIndex : expansion.chargedIndexes()) {
            if (chargedIndex == techniqueIndex) {
                return true;
            }
        }
        return false;
    }

    private static String chargeRefusal(List<Technique> techniques, List<ChainSegment> members) {
        for (ChainSegment member : members) {
            Technique technique = techniques.get(member.actionIndex());
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.chargeTicks() > 0) {
                return "这次组合里有还不能按时提交的蓄力，没有释放";
            }
        }
        return null;
    }

    int groupBill() {
        int bill = Math.max(0, ruleShare);
        for (int share : memberShares) {
            bill += Math.max(0, share);
        }
        return bill;
    }

    private static int memberShare(List<Technique> techniques, List<ChainSegment> segments, ChainSegment member,
                                    CastPlanContext planContext) {
        int memberIndex = segments.indexOf(member);
        int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, memberIndex, planContext));
        return memberShare(techniques, member, copies);
    }

    private static int memberShare(List<Technique> techniques, ChainSegment member, int copyCount) {
        Technique technique = techniques.get(member.actionIndex());
        int copies = Math.max(1, copyCount);
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int enchantAndHost = SkillSegmentMath.segmentManaCost(techniques, member);
        if (SkillSegmentMath.unitManaCost(technique) > 0) {
            return SkillSegmentMath.enchantOnlyCost(techniques, member);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
            return enchantAndHost + scaleByCopies(definition.domain().initialCost(), copies);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
            return enchantAndHost + scaleByCopies(definition.barrier().spawnCost(), copies);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
            return enchantAndHost + scaleByCopies(definition.guard().spawnCost(), copies);
        }
        if (technique.getBaseDamage() <= 0f && ConditionalEffects.copiesCoalesce(definition)) {
            return enchantAndHost;
        }
        int share = scaleByCopies(enchantAndHost, copies);
        if (definition != null && definition.fieldRadius() > 0f && definition.fieldSpawnCost() > 0) {
            share += scaleByCopies(definition.fieldSpawnCost(), copies);
        }
        return share;
    }

    private static Integer exactExpansionRuleShare(List<Technique> techniques, MulticastExpansion expansion) {
        Set<Integer> memberOwned = new HashSet<>();
        for (MulticastMember member : expansion.orderedMembers()) {
            memberOwned.add(member.hostIndex());
            memberOwned.addAll(member.attachedEnchantIndexes());
        }
        int share = 0;
        for (int chargedIndex : expansion.chargedIndexes()) {
            if (memberOwned.contains(chargedIndex) || chargedIndex < 0 || chargedIndex >= techniques.size()) {
                continue;
            }
            int slotCost = SkillSegmentMath.segmentManaCost(techniques, new ChainSegment(chargedIndex, List.of()));
            Integer nextShare = MulticastArithmetic.add(share, slotCost);
            if (nextShare == null) {
                return null;
            }
            share = nextShare;
        }
        return share;
    }

    private static Integer memberShareExact(List<Technique> techniques, ChainSegment member, int copyCount) {
        Technique technique = techniques.get(member.actionIndex());
        int copies = Math.max(1, copyCount);
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int enchantAndHost = SkillSegmentMath.segmentManaCost(techniques, member);
        if (SkillSegmentMath.unitManaCost(technique) > 0) {
            return SkillSegmentMath.enchantOnlyCost(techniques, member);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
            return addScaled(enchantAndHost, definition.domain().initialCost(), copies);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
            return addScaled(enchantAndHost, definition.barrier().spawnCost(), copies);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
            return addScaled(enchantAndHost, definition.guard().spawnCost(), copies);
        }
        if (technique.getBaseDamage() <= 0f && ConditionalEffects.copiesCoalesce(definition)) {
            return enchantAndHost;
        }
        Integer share = MulticastArithmetic.multiply(enchantAndHost, copies);
        if (share == null) {
            return null;
        }
        if (definition != null && definition.fieldRadius() > 0f && definition.fieldSpawnCost() > 0) {
            Integer fieldCost = MulticastArithmetic.multiply(definition.fieldSpawnCost(), copies);
            if (fieldCost == null) {
                return null;
            }
            return MulticastArithmetic.add(share, fieldCost);
        }
        return share;
    }

    private static Integer actionShareExact(List<Technique> techniques, int actionSlot) {
        int share = 0;
        Integer hostCost = MulticastArithmetic.add(share, SkillSegmentMath.segmentManaCost(
                techniques, new ChainSegment(actionSlot, List.of())));
        if (hostCost == null) {
            return null;
        }
        share = hostCost;
        for (int enchantIndex : enchantsBefore(techniques, actionSlot)) {
            Integer nextShare = MulticastArithmetic.add(share, SkillSegmentMath.segmentManaCost(
                    techniques, new ChainSegment(enchantIndex, List.of())));
            if (nextShare == null) {
                return null;
            }
            share = nextShare;
        }
        return share;
    }

    private static Integer addScaled(int baseCost, int unitCost, int copyCount) {
        Integer scaled = MulticastArithmetic.multiply(unitCost, copyCount);
        if (scaled == null) {
            return null;
        }
        return MulticastArithmetic.add(baseCost, scaled);
    }

    private static int leadingActionSlot(List<Technique> techniques, MulticastExpansion expansion) {
        int found = -1;
        int limit = Math.min(Math.max(expansion.startIndex(), 0), techniques.size());
        for (int techniqueIndex = 0; techniqueIndex < limit; techniqueIndex++) {
            if (SkillSegmentMath.roleOf(techniques.get(techniqueIndex)) == ChainRole.ACTION) {
                found = techniqueIndex;
            }
        }
        return found;
    }

    private static int segmentWithAction(List<ChainSegment> segments, int techniqueIndex) {
        if (techniqueIndex < 0) {
            return -1;
        }
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            if (segments.get(segmentIndex).actionIndex() == techniqueIndex) {
                return segmentIndex;
            }
        }
        return -1;
    }

    private static List<Integer> enchantsBefore(List<Technique> techniques, int actionSlot) {
        List<Integer> enchantIndexes = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < actionSlot && techniqueIndex < techniques.size(); techniqueIndex++) {
            if (techniques.get(techniqueIndex).isEnchantment()) {
                enchantIndexes.add(techniqueIndex);
            }
        }
        return enchantIndexes;
    }

    private static int actionSlotCooldown(Skill skill, List<Technique> techniques, int actionSlot, double cooldownReduction) {
        if (actionSlot < 0) {
            return 0;
        }
        List<Integer> actionIndexes = enchantsBefore(techniques, actionSlot);
        actionIndexes.add(actionSlot);
        return ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
    }

    private static int scaleByCopies(int baseCost, int copyCount) {
        if (baseCost <= 0 || copyCount <= 1) {
            return Math.max(0, baseCost);
        }
        if (baseCost > Integer.MAX_VALUE / copyCount) {
            return Integer.MAX_VALUE;
        }
        return baseCost * copyCount;
    }
}

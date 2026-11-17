package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.conditional.ConditionalEffects;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * 同一次按键的选段、费用和冷却都读这一份。双重只编译两个终结成员，C 留到下一步。
 */
final class CompiledReleaseStep {
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

    private CompiledReleaseStep(int actionSegment, int dualSegment, List<ChainSegment> members,
                                List<Integer> memberShares, int ruleShare, int actionShare,
                                int fullCooldown, int actionCooldown, boolean actionLed, String chargeRefusal,
                                List<ReleaseInput> memberInputs, ReleaseInput actionInput) {
        this.actionSegment = actionSegment;
        this.dualSegment = dualSegment;
        this.members = members;
        this.memberShares = memberShares;
        this.ruleShare = ruleShare;
        this.actionShare = actionShare;
        this.fullCooldown = fullCooldown;
        this.actionCooldown = actionCooldown;
        this.actionLed = actionLed;
        this.chargeRefusal = chargeRefusal;
        this.memberInputs = List.copyOf(memberInputs);
        this.actionInput = actionInput == null ? ReleaseInput.BASE : actionInput;
    }

    static CompiledReleaseStep compile(Skill skill, List<Technique> techniques, List<ChainSegment> segments,
                                       double cooldownReduction) {
        int dualSegment = -1;
        int actionSegment = -1;
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            Technique technique = techniques.get(segments.get(segmentIndex).actionIndex());
            ChainRole role = SkillSegmentMath.roleOf(technique);
            if (role == ChainRole.RULE_DUAL && dualSegment < 0) {
                dualSegment = segmentIndex;
            }
            if (role == ChainRole.ACTION && actionSegment < 0 && dualSegment < 0) {
                actionSegment = segmentIndex;
            }
        }
        if (dualSegment < 0) {
            return actionConditional(skill, techniques, segments, cooldownReduction, actionSegment);
        }
        List<ChainSegment> released = SkillChainPlan.releasedSegments(segments, dualSegment, techniques);
        List<ChainSegment> members = new ArrayList<>();
        for (int memberIndex = 0; memberIndex < released.size() && members.size() < 2; memberIndex++) {
            members.add(released.get(memberIndex));
        }
        List<Integer> memberShares = new ArrayList<>();
        for (ChainSegment member : members) {
            memberShares.add(memberShare(techniques, segments, member));
        }
        int ruleShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(dualSegment));
        int actionShare = actionSegment < 0
                ? 0
                : SkillSegmentMath.segmentManaCost(techniques, segments.get(actionSegment));
        List<Integer> everyIndex = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            everyIndex.add(techniqueIndex);
        }
        int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
        int actionCooldown = 0;
        if (actionSegment >= 0) {
            ChainSegment action = segments.get(actionSegment);
            List<Integer> actionIndexes = new ArrayList<>();
            actionIndexes.add(action.actionIndex());
            actionIndexes.addAll(action.enchantIndexes());
            actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
        }
        String chargeRefusal = chargeRefusal(techniques, members);
        return new CompiledReleaseStep(actionSegment, dualSegment, members, memberShares, ruleShare, actionShare,
                fullCooldown, actionCooldown, actionSegment >= 0, chargeRefusal,
                freezeMembers(skill, techniques, members),
                freezeAction(skill, techniques, segments, actionSegment));
    }

    /**
     * 没有双重、动作后面只有一个条件释放时，沿用动作会话。
     * 前面或后面还有别的可释放段时返回空，留给原来的按键路径。
     */
    private static CompiledReleaseStep actionConditional(Skill skill, List<Technique> techniques,
                                                         List<ChainSegment> segments, double cooldownReduction,
                                                         int actionSegment) {
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
        List<ChainSegment> members = new ArrayList<>();
        members.add(conditionalMember);
        List<Integer> memberShares = new ArrayList<>();
        memberShares.add(memberShare(techniques, segments, conditionalMember));
        int actionShare = SkillSegmentMath.segmentManaCost(techniques, segments.get(actionSegment));
        List<Integer> everyIndex = new ArrayList<>();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            everyIndex.add(techniqueIndex);
        }
        int fullCooldown = ChainSteps.cooldown(skill, everyIndex, cooldownReduction);
        ChainSegment action = segments.get(actionSegment);
        List<Integer> actionIndexes = new ArrayList<>();
        actionIndexes.add(action.actionIndex());
        actionIndexes.addAll(action.enchantIndexes());
        int actionCooldown = ChainSteps.cooldown(skill, actionIndexes, cooldownReduction);
        String refusal = chargeRefusal(techniques, members);
        return new CompiledReleaseStep(actionSegment, -1, members, memberShares, 0, actionShare,
                fullCooldown, actionCooldown, true, refusal,
                freezeMembers(skill, techniques, members),
                freezeAction(skill, techniques, segments, actionSegment));
    }

    private static List<ReleaseInput> freezeMembers(Skill skill, List<Technique> techniques, List<ChainSegment> members) {
        List<ReleaseInput> frozenInputs = new ArrayList<>();
        for (ChainSegment member : members) {
            frozenInputs.add(SkillChainPlan.frozenChainInput(skill, techniques, member.actionIndex()));
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

    private static int memberShare(List<Technique> techniques, List<ChainSegment> segments, ChainSegment member) {
        int memberIndex = segments.indexOf(member);
        Technique technique = techniques.get(member.actionIndex());
        int copies = Math.max(1, SkillChainPlan.copiesBefore(segments, techniques, memberIndex));
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        int enchantAndHost = SkillSegmentMath.segmentManaCost(techniques, member);
        if (SkillSegmentMath.unitManaCost(technique) > 0) {
            return SkillSegmentMath.enchantOnlyCost(techniques, member);
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.domain() != null) {
            return enchantAndHost + definition.domain().initialCost() * copies;
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.barrier() != null) {
            return enchantAndHost + definition.barrier().spawnCost() * copies;
        }
        if (definition != null && technique.getBaseDamage() <= 0f && definition.guard() != null) {
            return enchantAndHost + definition.guard().spawnCost() * copies;
        }
        if (technique.getBaseDamage() <= 0f && ConditionalEffects.copiesCoalesce(definition)) {
            return enchantAndHost;
        }
        int share = enchantAndHost * copies;
        if (definition != null && definition.fieldRadius() > 0f && definition.fieldSpawnCost() > 0) {
            share += definition.fieldSpawnCost() * copies;
        }
        return share;
    }
}

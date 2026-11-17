package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.ChainStructureEpoch;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 一条已解析技能的不可变结构计划。只保存切片、下标、标志、份数和链输入，不保存当次技能或行为对象。
 */
final class CompiledCastPlan {
    private final long structureEpoch;
    private final String progressSignature;
    private final int stepCount;
    private final List<List<Integer>> stepIndexGroups;
    private final Slice wholeSlice;
    private final List<Slice> stepSlices;

    private CompiledCastPlan(long structureEpoch, String progressSignature, int stepCount,
                             List<List<Integer>> stepIndexGroups, Slice wholeSlice, List<Slice> stepSlices) {
        this.structureEpoch = structureEpoch;
        this.progressSignature = progressSignature;
        this.stepCount = stepCount;
        this.stepIndexGroups = stepIndexGroups;
        this.wholeSlice = wholeSlice;
        this.stepSlices = stepSlices;
    }

    static CompiledCastPlan compile(Skill skill, long structureEpoch) {
        CastPlanCache.StructureBudget budget = new CastPlanCache.StructureBudget();
        List<List<Integer>> listed = ChainSteps.list(skill);
        int stepUnits = listed.size();
        for (List<Integer> step : listed) {
            stepUnits += step.size();
        }
        if (!budget.charge(stepUnits)) {
            return null;
        }
        List<List<Integer>> stepIndexGroups = ChainSteps.freezeListed(listed);
        String signature = ChainSteps.signature(skill);
        if (stepIndexGroups.isEmpty()) {
            Slice wholeSlice = compileSlice(skill, structureEpoch, budget);
            if (wholeSlice == null) {
                return null;
            }
            return new CompiledCastPlan(structureEpoch, signature, 0, List.of(), wholeSlice, List.of());
        }
        List<Slice> stepSlices = new ArrayList<>(stepIndexGroups.size());
        for (List<Integer> stepIndexes : stepIndexGroups) {
            Skill slicedSkill = ChainSteps.slice(skill, stepIndexes);
            Slice stepSlice = compileSlice(slicedSkill, structureEpoch, budget);
            if (stepSlice == null) {
                return null;
            }
            stepSlices.add(stepSlice);
        }
        return new CompiledCastPlan(structureEpoch, signature, stepSlices.size(), stepIndexGroups, null, List.copyOf(stepSlices));
    }

    int stepCount() {
        return stepCount;
    }

    String progressSignature() {
        return progressSignature;
    }

    List<Integer> stepIndexes(int stepIndex) {
        if (stepIndex < 0 || stepIndex >= stepIndexGroups.size()) {
            return List.of();
        }
        return stepIndexGroups.get(stepIndex);
    }

    Slice slice(int stepIndex) {
        if (stepCount == 0) {
            return wholeSlice;
        }
        if (stepIndex < 0 || stepIndex >= stepSlices.size()) {
            return null;
        }
        return stepSlices.get(stepIndex);
    }

    Skill sliceSkill(Skill currentSkill, int stepIndex) {
        if (stepCount == 0 || stepIndex < 0 || stepIndex >= stepIndexGroups.size()) {
            return currentSkill;
        }
        return ChainSteps.slice(currentSkill, stepIndexGroups.get(stepIndex));
    }

    private static Slice compileSlice(Skill skill, long structureEpoch, CastPlanCache.StructureBudget budget) {
        List<ChainSegment> segments = List.copyOf(TechniqueChain.split(skill));
        int segmentCount = segments.size();
        int techniqueCount = skill.getTechniques().size();
        int baseUnits = segmentCount
                + (segmentCount + 1)
                + techniqueCount
                + segmentCount
                + techniqueCount
                + techniqueCount
                + techniqueCount
                + techniqueCount;
        if (!budget.charge(baseUnits)) {
            return null;
        }
        List<com.bslx.XueXiMod.shu.Technique> techniques = skill.getTechniques();
        List<SkillChainPlan.ModifierTemplate> modifierTemplates = new ArrayList<>(segmentCount + 1);
        for (int fromIndex = 0; fromIndex <= segmentCount; fromIndex++) {
            SkillChainPlan.ModifierTemplate modifierTemplate = SkillChainPlan.captureModifierTemplate(
                    segments, techniques, fromIndex, skill);
            if (!budget.charge(modifierTemplate.storedUnits())) {
                return null;
            }
            modifierTemplates.add(modifierTemplate);
        }
        CompiledReleaseStep.StructureChoice releaseChoice = CompiledReleaseStep.chooseStructure(skill, techniques, segments);
        int expansionUnits = structureUnits(releaseChoice);
        if (!budget.charge(expansionUnits)) {
            return null;
        }
        int[] copiesBySegment = new int[segmentCount];
        for (int segmentIndex = 0; segmentIndex < segmentCount; segmentIndex++) {
            copiesBySegment[segmentIndex] = copiesForSlice(skill, techniques, segments, segmentIndex, releaseChoice);
        }
        int releasedUpper = 0;
        for (int ruleIndex = 0; ruleIndex < segmentCount; ruleIndex++) {
            releasedUpper += SkillChainPlan.releasedRoom(segments, ruleIndex, techniques);
        }
        if (!budget.charge(releasedUpper)) {
            return null;
        }
        List<List<Integer>> releasedByRule = new ArrayList<>(segmentCount);
        boolean[] expansionMemberRules = new boolean[segmentCount];
        List<ChainSegment> expansionMembers = membersOf(releaseChoice);
        int largestRoom = expansionMembers == null ? 0 : expansionMembers.size();
        for (int ruleIndex = 0; ruleIndex < segmentCount; ruleIndex++) {
            if (ruleUsesExpansion(releaseChoice, segments, techniques, ruleIndex)) {
                expansionMemberRules[ruleIndex] = true;
                releasedByRule.add(List.of());
                continue;
            }
            int room = SkillChainPlan.releasedRoom(segments, ruleIndex, techniques);
            if (room > largestRoom) {
                largestRoom = room;
            }
            List<ChainSegment> released = SkillChainPlan.releasedSegments(segments, ruleIndex, techniques);
            List<Integer> memberIndexes = new ArrayList<>(released.size());
            for (ChainSegment member : released) {
                int memberIndex = segmentIndexOf(segments, member);
                if (memberIndex >= 0) {
                    memberIndexes.add(memberIndex);
                }
            }
            releasedByRule.add(List.copyOf(memberIndexes));
        }
        int groupUpper = techniqueCount * Math.max(largestRoom, 1);
        if (!budget.charge(groupUpper)) {
            return null;
        }
        List<List<Integer>> groups = new ArrayList<>(techniqueCount);
        boolean[] lockByAction = new boolean[techniqueCount];
        boolean[] dropByAction = new boolean[techniqueCount];
        List<ReleaseInput> inputs = new ArrayList<>(techniqueCount);
        List<SkillSegmentRunner.RoleScan> roles = new ArrayList<>(techniqueCount);
        for (int actionIndex = 0; actionIndex < techniqueCount; actionIndex++) {
            List<Integer> group = SkillChainPlan.sameReleaseGroup(segments, actionIndex, techniques);
            if (group == null) {
                groups.add(null);
            } else {
                groups.add(List.copyOf(group));
            }
            MemberCastView memberView = memberOnChoice(skill, techniques, releaseChoice, actionIndex);
            if (memberView != null) {
                lockByAction[actionIndex] = memberView.entityLock;
                dropByAction[actionIndex] = memberView.drop;
                inputs.add(memberView.chainInput);
            } else {
                lockByAction[actionIndex] = SkillChainPlan.lockThisCast(techniques, actionIndex);
                dropByAction[actionIndex] = SkillChainPlan.dropThisCast(techniques, segments, actionIndex);
                inputs.add(SkillChainPlan.frozenChainInput(skill, techniques, actionIndex));
            }
            roles.add(SkillSegmentRunner.scanRoles(techniques, actionIndex));
        }
        return new Slice(structureEpoch, segments, List.copyOf(modifierTemplates), copiesBySegment,
                List.copyOf(releasedByRule), expansionMembers, expansionMemberRules, groups, lockByAction, dropByAction,
                List.copyOf(inputs), List.copyOf(roles), true, releaseChoice);
    }

    private static int structureUnits(CompiledReleaseStep.StructureChoice choice) {
        int units = 1;
        if (choice == null) {
            return units;
        }
        units += choice.memberIndexes.size();
        units += choice.memberInputs.size();
        MulticastExpansion expansion = choice.expansion;
        if (expansion == null) {
            return units;
        }
        units += expansion.consumedIndexes().size();
        units += expansion.chargedIndexes().size();
        units += expansion.ignoredSlots().size();
        units += expansion.inheritedModifierIndexes().size();
        units += expansion.orderedMembers().size();
        for (MulticastMember member : expansion.orderedMembers()) {
            units += member.inheritedModifierIndexes().size();
            units += member.localModifierIndexes().size();
            units += member.replicationIndexes().size();
            units += member.attachedEnchantIndexes().size();
        }
        return units;
    }

    private static int copiesForSlice(Skill skill, List<com.bslx.XueXiMod.shu.Technique> techniques,
                                       List<ChainSegment> segments, int segmentIndex,
                                       CompiledReleaseStep.StructureChoice releaseChoice) {
        MemberCastView memberView = memberOnChoice(skill, techniques, releaseChoice, segments.get(segmentIndex).actionIndex());
        if (memberView != null) {
            return Math.max(1, memberView.copyCount);
        }
        return SkillChainPlan.copiesBefore(segments, techniques, segmentIndex);
    }

    private static MemberCastView memberOnChoice(Skill skill, List<com.bslx.XueXiMod.shu.Technique> techniques,
                                                  CompiledReleaseStep.StructureChoice releaseChoice, int hostIndex) {
        if (releaseChoice == null || releaseChoice.expansion == null || !releaseChoice.expansion.releases()) {
            return null;
        }
        for (MulticastMember member : releaseChoice.expansion.orderedMembers()) {
            if (member.hostIndex() == hostIndex) {
                return MemberCastView.from(skill, techniques, member);
            }
        }
        return null;
    }

    private static List<ChainSegment> membersOf(CompiledReleaseStep.StructureChoice releaseChoice) {
        if (releaseChoice == null || releaseChoice.expansion == null || !releaseChoice.expansion.applicable()) {
            return null;
        }
        if (!releaseChoice.expansion.releases()) {
            return List.of();
        }
        List<ChainSegment> members = new ArrayList<>();
        for (MulticastMember member : releaseChoice.expansion.orderedMembers()) {
            members.add(new ChainSegment(member.hostIndex(), member.attachedEnchantIndexes()));
        }
        return List.copyOf(members);
    }

    private static boolean ruleUsesExpansion(CompiledReleaseStep.StructureChoice releaseChoice, List<ChainSegment> segments,
                                              List<com.bslx.XueXiMod.shu.Technique> techniques, int ruleIndex) {
        if (releaseChoice == null || releaseChoice.expansion == null || !releaseChoice.expansion.applicable()) {
            return false;
        }
        if (ruleIndex < 0 || ruleIndex >= segments.size()) {
            return false;
        }
        int actionIndex = segments.get(ruleIndex).actionIndex();
        MulticastExpansion expansion = releaseChoice.expansion;
        if (actionIndex == expansion.startIndex()) {
            return true;
        }
        if (actionIndex < 0 || actionIndex >= techniques.size() || !expansion.consumedIndexes().contains(actionIndex)) {
            return false;
        }
        com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition definition =
                com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry.get(techniques.get(actionIndex).getId());
        return definition != null && definition.multicastSpec() != null;
    }

    private static int segmentIndexOf(List<ChainSegment> segments, ChainSegment member) {
        int found = segments.indexOf(member);
        if (found >= 0) {
            return found;
        }
        for (int segmentIndex = 0; segmentIndex < segments.size(); segmentIndex++) {
            if (segments.get(segmentIndex).actionIndex() == member.actionIndex()) {
                return segmentIndex;
            }
        }
        return -1;
    }

    static final class Slice {
        private final long structureEpoch;
        private final List<ChainSegment> segments;
        private final List<SkillChainPlan.ModifierTemplate> modifierTemplates;
        private final int[] copiesBySegment;
        private final List<List<Integer>> releasedByRule;
        private final List<ChainSegment> expansionMembers;
        private final boolean[] expansionMemberRules;
        private final List<List<Integer>> groupsByAction;
        private final boolean[] lockByAction;
        private final boolean[] dropByAction;
        private final List<ReleaseInput> inputsByAction;
        private final List<SkillSegmentRunner.RoleScan> rolesByAction;
        private final boolean releaseResolved;
        private final CompiledReleaseStep.StructureChoice releaseChoice;

        private Slice(long structureEpoch, List<ChainSegment> segments,
                      List<SkillChainPlan.ModifierTemplate> modifierTemplates, int[] copiesBySegment,
                      List<List<Integer>> releasedByRule, List<ChainSegment> expansionMembers,
                      boolean[] expansionMemberRules, List<List<Integer>> groupsByAction,
                      boolean[] lockByAction, boolean[] dropByAction, List<ReleaseInput> inputsByAction,
                      List<SkillSegmentRunner.RoleScan> rolesByAction, boolean releaseResolved,
                      CompiledReleaseStep.StructureChoice releaseChoice) {
            this.structureEpoch = structureEpoch;
            this.segments = segments;
            this.modifierTemplates = modifierTemplates;
            this.copiesBySegment = copiesBySegment.clone();
            this.releasedByRule = releasedByRule;
            this.expansionMembers = expansionMembers;
            this.expansionMemberRules = expansionMemberRules.clone();
            this.groupsByAction = Collections.unmodifiableList(new ArrayList<>(groupsByAction));
            this.lockByAction = lockByAction.clone();
            this.dropByAction = dropByAction.clone();
            this.inputsByAction = inputsByAction;
            this.rolesByAction = rolesByAction;
            this.releaseResolved = releaseResolved;
            this.releaseChoice = releaseChoice;
        }

        boolean matchesCurrentEpoch() {
            return ChainStructureEpoch.current() == structureEpoch;
        }

        List<ChainSegment> segments() {
            return segments;
        }

        boolean releaseResolved() {
            return releaseResolved;
        }

        CompiledReleaseStep.StructureChoice releaseChoice() {
            return releaseChoice;
        }

        SkillChainPlan.ModifierPlan modifierPlan(int from, boolean realInputs) {
            if (from < 0 || from >= modifierTemplates.size()) {
                return null;
            }
            return modifierTemplates.get(from).instantiate(realInputs);
        }

        int copiesAt(int segmentIndex) {
            if (segmentIndex < 0 || segmentIndex >= copiesBySegment.length) {
                return -1;
            }
            return copiesBySegment[segmentIndex];
        }

        List<ChainSegment> releasedMembers(int ruleIndex) {
            if (expansionMemberRules != null && ruleIndex >= 0 && ruleIndex < expansionMemberRules.length
                    && expansionMemberRules[ruleIndex]) {
                return expansionMembers == null ? List.of() : expansionMembers;
            }
            if (ruleIndex < 0 || ruleIndex >= releasedByRule.size()) {
                return null;
            }
            List<Integer> memberIndexes = releasedByRule.get(ruleIndex);
            List<ChainSegment> members = new ArrayList<>(memberIndexes.size());
            for (int memberIndex : memberIndexes) {
                if (memberIndex < 0 || memberIndex >= segments.size()) {
                    return null;
                }
                members.add(segments.get(memberIndex));
            }
            return members;
        }

        boolean knowsGroup(int actionIndex) {
            return actionIndex >= 0 && actionIndex < groupsByAction.size();
        }

        List<Integer> sameGroup(int actionIndex) {
            if (!knowsGroup(actionIndex)) {
                return null;
            }
            return groupsByAction.get(actionIndex);
        }

        boolean knowsLock(int actionIndex) {
            return actionIndex >= 0 && actionIndex < lockByAction.length;
        }

        boolean lockAt(int actionIndex) {
            return knowsLock(actionIndex) && lockByAction[actionIndex];
        }

        boolean knowsDrop(int actionIndex) {
            return actionIndex >= 0 && actionIndex < dropByAction.length;
        }

        boolean dropAt(int actionIndex) {
            return knowsDrop(actionIndex) && dropByAction[actionIndex];
        }

        ReleaseInput frozenInput(int actionIndex) {
            if (actionIndex < 0 || actionIndex >= inputsByAction.size()) {
                return null;
            }
            return inputsByAction.get(actionIndex);
        }

        SkillSegmentRunner.RoleScan roleScan(int actionIndex) {
            if (actionIndex < 0 || actionIndex >= rolesByAction.size()) {
                return null;
            }
            return rolesByAction.get(actionIndex);
        }
    }
}

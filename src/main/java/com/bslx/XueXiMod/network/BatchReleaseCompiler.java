package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.batch.BirthClearance;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.skill.Skill;

import java.util.ArrayList;
import java.util.List;

/**
 * 只在链上出现本批新能力时编译。普通旧链返回 null，报价和段顺序仍走原来的路径。
 */
public final class BatchReleaseCompiler {
    private BatchReleaseCompiler() {
    }

    public static BatchReleasePlan compile(Skill skill, double cooldownReduction) {
        if (skill == null) {
            return null;
        }
        List<Technique> techniques = skill.getTechniques();
        if (techniques == null || techniques.isEmpty() || !declaresBatch(techniques)) {
            return null;
        }
        if (ordinaryDomainQuote(techniques)) {
            return null;
        }
        if (containsDomainEnchantment(techniques)) {
            return null;
        }
        ParseState state = new ParseState();
        int techniqueIndex = 0;
        while (techniqueIndex < techniques.size()) {
            int currentIndex = techniqueIndex;
            techniqueIndex += 1;
            Technique technique = techniques.get(currentIndex);
            if (technique == null) {
                continue;
            }
            MulticastExpansion expansion = MulticastCompileAdapter.expansionOpeningAt(techniques, currentIndex);
            if (expansion != null) {
                int resumeAt = MulticastCompileAdapter.resumeIndex(expansion, currentIndex);
                if (resumeAt < 0) {
                    return BatchReleasePlan.refused(MulticastCompileAdapter.boundaryRefusal(expansion));
                }
                String refusalKey = adoptSharedExpansion(skill, techniques, expansion, state);
                if (refusalKey != null) {
                    return BatchReleasePlan.refused(refusalKey);
                }
                techniqueIndex = resumeAt;
                continue;
            }
            if (SkillSegmentMath.isRemovedRule(technique.getId())) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (technique.isEnchantment()) {
                state.pendingEnchants.add(currentIndex);
                continue;
            }
            if (definition == null) {
                return BatchReleasePlan.refused("gui.xueximod.batch.unsupported_host");
            }
            ChainRole role = definition.role();
            if (role == ChainRole.RULE_HIT_CONTINUE) {
                if (state.expectingChild || state.branches.isEmpty()) {
                    return BatchReleasePlan.refused(state.branches.isEmpty()
                            ? "gui.xueximod.batch.missing_parent"
                            : "gui.xueximod.batch.continuation_structure");
                }
                if (state.branches.get(state.branches.size() - 1).child != null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.nested");
                }
                state.expectingChild = true;
                state.resetPending();
                continue;
            }
            if (isHost(technique, definition)) {
                String hostRefusal = hostRefusal(definition);
                if (hostRefusal != null) {
                    return BatchReleasePlan.refused(hostRefusal);
                }
                if (suppliesDefaultMotion(definition, state)) {
                    state.motions += 1;
                }
                if (state.motions > 1) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.motion_conflict");
                }
                if (state.expectingChild) {
                    BranchDraft parent = state.branches.get(state.branches.size() - 1);
                    String childRefusal = finishChild(techniques, parent, currentIndex, state);
                    if (childRefusal != null) {
                        return BatchReleasePlan.refused(childRefusal);
                    }
                    state.expectingChild = false;
                    state.resetPending();
                    continue;
                }
                BranchDraft branch = new BranchDraft();
                String closed = closeBranch(techniques, branch, technique, currentIndex, state);
                if (closed != null) {
                    return BatchReleasePlan.refused(closed);
                }
                state.branches.add(branch);
                state.resetPending();
                continue;
            }
            if (!SkillSegmentMath.isChainModifier(technique) && definition.replication() == null) {
                if (role == ChainRole.RULE_DOMAIN_BIND) {
                    continue;
                }
                return BatchReleasePlan.refused("gui.xueximod.batch.unsupported_host");
            }
            String noted = noteLooseModifier(state, definition, currentIndex, true, true);
            if (noted != null) {
                return BatchReleasePlan.refused(noted);
            }
        }
        if (state.expectingChild) {
            return BatchReleasePlan.refused("gui.xueximod.batch.missing_child");
        }
        if (state.branches.isEmpty()) {
            if (state.pendingModifiers.isEmpty() && onlyDomainEnchants(techniques, state.pendingEnchants)) {
                return null;
            }
            return BatchReleasePlan.refused("gui.xueximod.batch.missing_parent");
        }
        return expand(techniques, state, cooldownReduction);
    }

    /**
     * 幽冥只是旧领域或灼地的附魔时，报价标记不能把整段拉进新编译。
     * 接续、地表、喷涌、抛物线仍走新编译，不支持的组合在那里拒绝。
     */
    private static boolean ordinaryDomainQuote(List<Technique> techniques) {
        boolean sawQuoteEnchant = false;
        boolean sawStructural = false;
        boolean sawDomainHost = false;
        boolean sawOtherHost = false;
        for (Technique technique : techniques) {
            if (technique == null || SkillSegmentMath.isRemovedRule(technique.getId())) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            Trajectory trajectory = definition.trajectory();
            if (definition.role().batchLifecycle()
                    || trajectory == Trajectory.PARABOLA
                    || trajectory == Trajectory.UPWELL) {
                sawStructural = true;
            }
            if (technique.isEnchantment()) {
                if (definition.declaresBatchQuote()) {
                    if (definition.domain() != null) {
                        sawOtherHost = true;
                        continue;
                    }
                    sawQuoteEnchant = true;
                }
                continue;
            }
            if (SkillSegmentMath.isChainModifier(technique) || definition.replication() != null) {
                continue;
            }
            if (definition.domain() != null || definition.explicitDomainHost()) {
                sawDomainHost = true;
            } else {
                sawOtherHost = true;
            }
        }
        return sawQuoteEnchant && sawDomainHost && !sawStructural && !sawOtherHost;
    }

    /** 领域附魔自己开域。交给绑定和普通链，避免批量先报不支持宿主。 */
    private static boolean containsDomainEnchantment(List<Technique> techniques) {
        for (Technique technique : techniques) {
            if (technique == null || !technique.isEnchantment()) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.domain() != null) {
                return true;
            }
        }
        return false;
    }

    private static boolean onlyDomainEnchants(List<Technique> techniques, List<Integer> pendingIndexes) {
        if (pendingIndexes == null || pendingIndexes.isEmpty()) {
            return false;
        }
        for (int pendingIndex = 0; pendingIndex < pendingIndexes.size(); pendingIndex++) {
            int techniqueIndex = pendingIndexes.get(pendingIndex);
            if (techniqueIndex < 0 || techniqueIndex >= techniques.size()) {
                return false;
            }
            Technique listed = techniques.get(techniqueIndex);
            ReleaseDefinition definition = listed == null ? null : ReleaseRegistry.get(listed.getId());
            if (definition == null || definition.domain() == null) {
                return false;
            }
        }
        return true;
    }

    private static boolean declaresBatch(List<Technique> techniques) {
        for (Technique technique : techniques) {
            if (technique == null) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition == null) {
                continue;
            }
            if (definition.declaresBatchQuote() || definition.role().batchLifecycle()) {
                return true;
            }
            Trajectory trajectory = definition.trajectory();
            if (trajectory == Trajectory.PARABOLA || trajectory == Trajectory.UPWELL) {
                return true;
            }
        }
        return false;
    }

    private static boolean isHost(Technique technique, ReleaseDefinition definition) {
        return !technique.isEnchantment()
                && definition.role() == ChainRole.ATTACK
                && definition.kind() == ReleaseKind.HIT_VOLUME
                && technique.getBaseDamage() > 0f;
    }

    private static String hostRefusal(ReleaseDefinition definition) {
        if (definition.domain() != null || definition.barrier() != null || definition.guard() != null
                || definition.fieldRadius() > 0f || definition.conditionalEffect() != null
                || definition.selfStatus() != null) {
            if (!definition.acceptsBatchPlan()) {
                return "gui.xueximod.batch.unsupported_host";
            }
        }
        if (definition.domain() != null && !definition.acceptsHitContinue()) {
            return "gui.xueximod.batch.no_continue_support";
        }
        return null;
    }

    private static boolean fullMotion(ReleaseDefinition definition) {
        ChainRole role = definition.role();
        if (role == ChainRole.TRAJECTORY_DROP || role == ChainRole.TRAJECTORY_UPWELL
                || role == ChainRole.TRAJECTORY_PARALLEL || role == ChainRole.TRAJECTORY_TRACKING) {
            return true;
        }
        return nativeFullTrajectory(definition.trajectory());
    }

    /**
     * 修饰仍按完整弹道计数。宿主只有在没有有效链上路径覆盖，或宿主不允许改弹道时，
     * 才把自带抛物线、喷涌、螺旋再计成一段。
     */
    private static boolean suppliesDefaultMotion(ReleaseDefinition definition, ParseState state) {
        if (!fullMotion(definition)) {
            return false;
        }
        if (!definition.trajectoryMutable() || !hasChainPathOverride(state)) {
            return true;
        }
        return !nativeFullTrajectory(definition.trajectory());
    }

    private static boolean hasChainPathOverride(ParseState state) {
        return state.dropPath || state.upwellPath || state.parallelPath;
    }

    private static boolean nativeFullTrajectory(Trajectory trajectory) {
        return trajectory == Trajectory.PARABOLA || trajectory == Trajectory.UPWELL
                || trajectory == Trajectory.VORTEX || trajectory == Trajectory.PARALLEL;
    }

    private static String closeBranch(List<Technique> techniques, BranchDraft branch, Technique host, int hostIndex,
                                      ParseState state) {
        branch.hostIndex = hostIndex;
        branch.enchants.addAll(state.pendingEnchants);
        branch.copies = state.copies;
        branch.stagger = state.stagger;
        branch.surface = state.surface;
        branch.entityLock = state.entityLock;
        branch.dropPath = state.dropPath;
        branch.upwellPath = state.upwellPath;
        branch.parallelPath = state.parallelPath;
        branch.retainedPrice = state.retainedPrice;
        for (int enchantIndex : branch.enchants) {
            Integer retained = exactAdd(branch.retainedPrice, SkillSegmentMath.quotedMana(techniques.get(enchantIndex)));
            if (retained == null) {
                return "gui.xueximod.batch.quote_overflow";
            }
            branch.retainedPrice = retained;
        }
        branch.hostPrice = SkillSegmentMath.quotedMana(host);
        branch.lifetimeTicks = Math.max(1, ReleaseRegistry.get(host.getId()).lifetimeTicks());
        branch.earliestSweep = prefersSweep(ReleaseRegistry.get(host.getId()));
        if (state.surface && !surfaceAllowed(techniques, hostIndex)) {
            return "gui.xueximod.batch.domain_surface";
        }
        return null;
    }

    private static String finishChild(List<Technique> techniques, BranchDraft parent, int hostIndex, ParseState state) {
        Technique host = techniques.get(hostIndex);
        ReleaseDefinition definition = ReleaseRegistry.get(host.getId());
        if (definition != null && !definition.acceptsHitContinue()
                && (definition.domain() != null || definition.conditionalEffect() != null
                || definition.barrier() != null || definition.guard() != null || definition.fieldRadius() > 0f)) {
            return "gui.xueximod.batch.no_continue_support";
        }
        int retained = state.retainedPrice;
        for (int enchantIndex : state.pendingEnchants) {
            Integer next = exactAdd(retained, SkillSegmentMath.quotedMana(techniques.get(enchantIndex)));
            if (next == null) {
                return "gui.xueximod.batch.quote_overflow";
            }
            retained = next;
        }
        parent.child = new BatchReleasePlan.ChildPlan(
                new ChainSegment(hostIndex, List.copyOf(state.pendingEnchants)),
                state.copies,
                SkillSegmentMath.quotedMana(host),
                retained,
                definition == null ? 1.5D : Math.max(1.0D, definition.thickness() * Math.max(1f, definition.spanScale())),
                state.entityLock,
                state.dropPath,
                state.upwellPath,
                state.parallelPath,
                state.stagger,
                definition == null ? 20 : Math.max(1, definition.lifetimeTicks()));
        return null;
    }

    private static boolean surfaceAllowed(List<Technique> techniques, int hostIndex) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(hostIndex).getId());
        return definition != null && (definition.domain() == null || definition.acceptsSurfaceAnchor());
    }

    private static boolean prefersSweep(ReleaseDefinition definition) {
        if (definition == null) {
            return false;
        }
        if (definition.prefersEarliestContact()) {
            return true;
        }
        Trajectory trajectory = definition.trajectory();
        return trajectory == Trajectory.PARABOLA || trajectory == Trajectory.UPWELL;
    }

    private static BatchReleasePlan expand(List<Technique> techniques, ParseState state,
                                           double cooldownReduction) {
        int instances = 0;
        int rawCooldown = 0;
        BirthClearance groupClearance = BirthClearance.empty();
        boolean needsSurface = false;
        List<Technique> fired = new ArrayList<>();
        List<BatchReleasePlan.ParentBirth> parents = new ArrayList<>();
        int reserved = 0;
        for (int branchOrdinal = 0; branchOrdinal < state.branches.size(); branchOrdinal++) {
            BranchDraft branch = state.branches.get(branchOrdinal);
            int childInstances = 0;
            if (branch.child != null) {
                Integer scaledChildren = exactMultiply(branch.copies, branch.child.ownCopies);
                if (scaledChildren == null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                }
                childInstances = scaledChildren;
            }
            Integer nextInstances = exactAdd(instances, branch.copies);
            if (nextInstances == null) {
                return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
            }
            nextInstances = exactAdd(nextInstances, childInstances);
            if (nextInstances == null || nextInstances > BatchReleasePlan.INSTANCE_CAP) {
                return BatchReleasePlan.refused("gui.xueximod.batch.too_many");
            }
            instances = nextInstances;
            Technique host = techniques.get(branch.hostIndex);
            fired.add(host);
            rawCooldown += SkillSegmentMath.configuredBaseCooldown(host);
            if (branch.child != null) {
                fired.add(techniques.get(branch.child.segment.actionIndex()));
                rawCooldown += SkillSegmentMath.configuredBaseCooldown(
                        techniques.get(branch.child.segment.actionIndex()));
            }
            if (branch.surface) {
                needsSurface = true;
            }
            groupClearance = groupClearance.cover(ReleaseRegistry.get(host.getId()));
            if (branch.child != null) {
                Technique childTechnique = techniques.get(branch.child.segment.actionIndex());
                groupClearance = groupClearance.cover(childTechnique == null ? null : ReleaseRegistry.get(childTechnique.getId()));
            }
            for (int copyOrdinal = 0; copyOrdinal < branch.copies; copyOrdinal++) {
                int delay = branch.memberDelay + (branch.stagger ? BatchReleasePlan.STAGGER_GAP * copyOrdinal : 0);
                int retained = copyOrdinal == 0 ? branch.retainedPrice : 0;
                Integer withHost = exactAdd(reserved, branch.hostPrice);
                Integer withRetained = withHost == null ? null : exactAdd(withHost, retained);
                if (withRetained == null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                }
                reserved = withRetained;
                if (branch.child != null && copyOrdinal == 0) {
                    Integer childRetain = exactAdd(reserved, branch.child.retainedPrice);
                    if (childRetain == null) {
                        return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                    }
                    reserved = childRetain;
                }
                if (branch.child != null) {
                    Integer childHosts = exactMultiply(branch.child.hostPrice, branch.child.ownCopies);
                    if (childHosts == null) {
                        return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                    }
                    Integer withChildren = exactAdd(reserved, childHosts);
                    if (withChildren == null) {
                        return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                    }
                    reserved = withChildren;
                }
                parents.add(new BatchReleasePlan.ParentBirth(
                        branchOrdinal, copyOrdinal, branch.copies, delay,
                        new ChainSegment(branch.hostIndex, List.copyOf(branch.enchants)),
                        branch.hostPrice, retained, branch.entityLock, branch.dropPath, branch.upwellPath,
                        branch.parallelPath, branch.child,
                        branch.lifetimeTicks, branch.earliestSweep));
            }
        }
        if (needsSurface) {
            for (Technique technique : techniques) {
                ReleaseDefinition definition = technique == null ? null : ReleaseRegistry.get(technique.getId());
                if (definition != null && definition.domain() != null && !definition.acceptsSurfaceAnchor()) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.domain_surface");
                }
            }
        }
        int scaled = SkillSegmentMath.scaleCooldown(Math.max(rawCooldown, 1), cooldownReduction);
        return BatchReleasePlan.ready(needsSurface, groupClearance.horizontalRadius(), groupClearance.verticalHalf(),
                reserved, scaled, List.copyOf(parents), List.copyOf(fired));
    }

    private static Integer exactAdd(int left, int right) {
        try {
            return Math.addExact(left, right);
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    private static Integer exactMultiply(int left, int right) {
        try {
            return Math.multiplyExact(left, right);
        } catch (ArithmeticException overflow) {
            return null;
        }
    }

    /**
     * 已读区间里的宿主改用展开边界和成员延迟。不再按固定两段计数，也不把已读成员再走一遍。
     */
    private static String adoptSharedExpansion(Skill skill, List<Technique> techniques, MulticastExpansion expansion,
                                               ParseState state) {
        List<Integer> pricedSlots = new ArrayList<>();
        for (MulticastMember member : expansion.orderedMembers()) {
            String refusalKey = adoptSharedMember(skill, techniques, expansion, state, member, pricedSlots);
            if (refusalKey != null) {
                return refusalKey;
            }
        }
        return null;
    }

    private static String adoptSharedMember(Skill skill, List<Technique> techniques, MulticastExpansion expansion,
                                            ParseState state, MulticastMember member, List<Integer> pricedSlots) {
        int hostIndex = member.hostIndex();
        if (hostIndex < 0 || hostIndex >= techniques.size()) {
            return "gui.xueximod.batch.unsupported_host";
        }
        Technique technique = techniques.get(hostIndex);
        if (technique == null || SkillSegmentMath.isRemovedRule(technique.getId())) {
            return null;
        }
        if (member.independentEnchant() || technique.isEnchantment()) {
            state.pendingEnchants.add(hostIndex);
            return null;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        if (definition == null) {
            return "gui.xueximod.batch.unsupported_host";
        }
        ChainRole role = definition.role();
        if (role == ChainRole.RULE_HIT_CONTINUE) {
            if (state.expectingChild || state.branches.isEmpty()) {
                return state.branches.isEmpty()
                        ? "gui.xueximod.batch.missing_parent"
                        : "gui.xueximod.batch.continuation_structure";
            }
            if (state.branches.get(state.branches.size() - 1).child != null) {
                return "gui.xueximod.batch.nested";
            }
            state.expectingChild = true;
            state.resetPending();
            return null;
        }
        if (isHost(technique, definition)) {
            String prepared = prepareMemberState(skill, techniques, state, member, pricedSlots);
            if (prepared != null) {
                return prepared;
            }
            String hostRefusal = hostRefusal(definition);
            if (hostRefusal != null) {
                return hostRefusal;
            }
            if (suppliesDefaultMotion(definition, state)) {
                state.motions += 1;
            }
            if (state.motions > 1) {
                return "gui.xueximod.batch.motion_conflict";
            }
            if (state.expectingChild) {
                if (state.branches.isEmpty()) {
                    return "gui.xueximod.batch.missing_parent";
                }
                BranchDraft parent = state.branches.get(state.branches.size() - 1);
                String childRefusal = finishChild(techniques, parent, hostIndex, state);
                if (childRefusal != null) {
                    return childRefusal;
                }
                state.expectingChild = false;
                state.resetPending();
                return null;
            }
            BranchDraft branch = new BranchDraft();
            String closed = closeBranch(techniques, branch, technique, hostIndex, state);
            if (closed != null) {
                return closed;
            }
            branch.memberDelay = MulticastCompileAdapter.memberDelayTicks(expansion, member.ordinal());
            state.branches.add(branch);
            state.resetPending();
            return null;
        }
        if (SkillSegmentMath.isOperationHost(technique)) {
            return "gui.xueximod.batch.unsupported_host";
        }
        if (!SkillSegmentMath.isChainModifier(technique) && definition.replication() == null) {
            return "gui.xueximod.batch.unsupported_host";
        }
        boolean chargePrice = claimSlot(pricedSlots, hostIndex);
        return noteLooseModifier(state, definition, hostIndex, false, chargePrice);
    }

    private static String prepareMemberState(Skill skill, List<Technique> techniques, ParseState state,
                                             MulticastMember member, List<Integer> pricedSlots) {
        List<Integer> seenSlots = new ArrayList<>();
        String noted = noteMemberSlots(techniques, state, member.inheritedModifierIndexes(), seenSlots, pricedSlots);
        if (noted != null) {
            return noted;
        }
        noted = noteMemberSlots(techniques, state, member.localModifierIndexes(), seenSlots, pricedSlots);
        if (noted != null) {
            return noted;
        }
        noted = noteMemberSlots(techniques, state, member.replicationIndexes(), seenSlots, pricedSlots);
        if (noted != null) {
            return noted;
        }
        for (int enchantIndex : member.attachedEnchantIndexes()) {
            state.pendingEnchants.add(enchantIndex);
        }
        MulticastCompileAdapter.CopyRead copies = MulticastCompileAdapter.memberCopies(skill, techniques, member);
        if (copies.overflow || copies.copyCount < 1) {
            return "gui.xueximod.batch.quote_overflow";
        }
        state.copies = copies.copyCount;
        return null;
    }

    private static String noteMemberSlots(List<Technique> techniques, ParseState state, List<Integer> slotIndexes,
                                          List<Integer> seenSlots, List<Integer> pricedSlots) {
        for (int slotIndex : slotIndexes) {
            if (slotIndex < 0 || slotIndex >= techniques.size() || seenSlots.contains(slotIndex)) {
                continue;
            }
            seenSlots.add(slotIndex);
            Technique listed = techniques.get(slotIndex);
            if (listed == null || SkillSegmentMath.isRemovedRule(listed.getId())) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(listed.getId());
            if (definition == null) {
                return "gui.xueximod.batch.unsupported_host";
            }
            boolean chargePrice = claimSlot(pricedSlots, slotIndex);
            String noted = noteLooseModifier(state, definition, slotIndex, false, chargePrice);
            if (noted != null) {
                return noted;
            }
        }
        return null;
    }

    private static boolean claimSlot(List<Integer> claimedSlots, int slotIndex) {
        if (claimedSlots.contains(slotIndex)) {
            return false;
        }
        claimedSlots.add(slotIndex);
        return true;
    }

    /**
     * 批量原有的因子连乘只留在展开外。展开成员的份数由复制积替换，不在这里再乘。
     */
    private static String noteLooseModifier(ParseState state, ReleaseDefinition definition, int techniqueIndex,
                                            boolean applyReplication, boolean chargePrice) {
        if (applyReplication && definition.replication() != null) {
            Integer scaled = exactMultiply(state.copies, definition.replication().factor());
            if (scaled == null) {
                return "gui.xueximod.batch.quote_overflow";
            }
            state.copies = scaled;
        }
        ChainRole role = definition.role();
        if (role == ChainRole.RULE_STAGGER) {
            state.stagger = true;
        }
        if (role == ChainRole.SEEK_SURFACE) {
            state.surface = true;
        }
        if (role == ChainRole.SEEK_ENTITY) {
            state.entityLock = true;
        }
        if (role == ChainRole.TRAJECTORY_DROP) {
            state.dropPath = true;
        }
        if (role == ChainRole.TRAJECTORY_UPWELL || definition.trajectory() == Trajectory.UPWELL) {
            state.upwellPath = true;
        }
        if (role == ChainRole.TRAJECTORY_PARALLEL || definition.trajectory() == Trajectory.PARALLEL) {
            state.parallelPath = true;
        }
        if (fullMotion(definition)) {
            state.motions += 1;
            if (state.motions > 1) {
                return "gui.xueximod.batch.motion_conflict";
            }
        }
        if (chargePrice && definition.explicitManaCost() > 0) {
            Integer retained = exactAdd(state.retainedPrice, definition.explicitManaCost());
            if (retained == null) {
                return "gui.xueximod.batch.quote_overflow";
            }
            state.retainedPrice = retained;
        }
        state.pendingModifiers.add(techniqueIndex);
        return null;
    }

    private static final class BranchDraft {
        private int hostIndex = -1;
        private int copies = 1;
        private int hostPrice;
        private int retainedPrice;
        private int memberDelay;
        private int lifetimeTicks = 20;
        private boolean stagger;
        private boolean surface;
        private boolean entityLock;
        private boolean dropPath;
        private boolean upwellPath;
        private boolean parallelPath;
        private boolean earliestSweep;
        private final List<Integer> enchants = new ArrayList<>();
        private BatchReleasePlan.ChildPlan child;
    }

    private static final class ParseState {
        private final List<BranchDraft> branches = new ArrayList<>();
        private final List<Integer> pendingEnchants = new ArrayList<>();
        private final List<Integer> pendingModifiers = new ArrayList<>();
        private int copies = 1;
        private int motions;
        private int retainedPrice;
        private boolean stagger;
        private boolean surface;
        private boolean entityLock;
        private boolean dropPath;
        private boolean upwellPath;
        private boolean parallelPath;
        private boolean expectingChild;

        private void resetPending() {
            pendingEnchants.clear();
            pendingModifiers.clear();
            copies = 1;
            motions = 0;
            retainedPrice = 0;
            stagger = false;
            surface = false;
            entityLock = false;
            dropPath = false;
            upwellPath = false;
            parallelPath = false;
        }
    }
}

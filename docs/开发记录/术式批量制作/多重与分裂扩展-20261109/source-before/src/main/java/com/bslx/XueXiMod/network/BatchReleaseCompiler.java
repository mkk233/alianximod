package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
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
        ParseState state = new ParseState();
        for (int techniqueIndex = 0; techniqueIndex < techniques.size(); techniqueIndex++) {
            Technique technique = techniques.get(techniqueIndex);
            if (technique == null || SkillSegmentMath.isRemovedRule(technique.getId())) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (technique.isEnchantment()) {
                state.pendingEnchants.add(techniqueIndex);
                continue;
            }
            if (definition == null) {
                return BatchReleasePlan.refused("gui.xueximod.batch.unsupported_host");
            }
            ChainRole role = definition.role();
            if (role == ChainRole.RULE_HIT_CONTINUE) {
                if (state.dualRemaining > 0 || state.expectingChild || state.branches.isEmpty()) {
                    return BatchReleasePlan.refused(state.branches.isEmpty()
                            ? "gui.xueximod.batch.missing_parent"
                            : "gui.xueximod.batch.dual_incomplete");
                }
                if (state.branches.get(state.branches.size() - 1).child != null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.nested");
                }
                state.expectingChild = true;
                state.resetPending();
                continue;
            }
            if (role == ChainRole.RULE_DUAL || definition.multicastSpec() != null && role == ChainRole.RULE_DUAL) {
                if (state.dualRemaining > 0 || state.expectingChild) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.dual_incomplete");
                }
                state.dualRemaining = 2;
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
                BranchDraft branch = state.expectingChild ? null : new BranchDraft();
                if (state.expectingChild) {
                    BranchDraft parent = state.branches.get(state.branches.size() - 1);
                    String childRefusal = finishChild(techniques, parent, techniqueIndex, state);
                    if (childRefusal != null) {
                        return BatchReleasePlan.refused(childRefusal);
                    }
                    state.expectingChild = false;
                    state.resetPending();
                    continue;
                }
                String closed = closeBranch(techniques, branch, technique, techniqueIndex, state);
                if (closed != null) {
                    return BatchReleasePlan.refused(closed);
                }
                state.branches.add(branch);
                state.resetPending();
                continue;
            }
            if (!SkillSegmentMath.isChainModifier(technique) && definition.replication() == null) {
                return BatchReleasePlan.refused("gui.xueximod.batch.unsupported_host");
            }
            if (definition.replication() != null) {
                Integer scaled = exactMultiply(state.copies, definition.replication().factor());
                if (scaled == null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                }
                state.copies = scaled;
            }
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
            if (role == ChainRole.TRAJECTORY_UPWELL
                    || definition.trajectory() == Trajectory.UPWELL) {
                state.upwellPath = true;
            }
            if (fullMotion(definition)) {
                state.motions += 1;
                if (state.motions > 1) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.motion_conflict");
                }
            }
            if (definition.explicitManaCost() > 0) {
                Integer retained = exactAdd(state.retainedPrice, definition.explicitManaCost());
                if (retained == null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                }
                state.retainedPrice = retained;
            }
            state.pendingModifiers.add(techniqueIndex);
        }
        if (state.expectingChild) {
            return BatchReleasePlan.refused("gui.xueximod.batch.missing_child");
        }
        if (state.dualRemaining > 0) {
            return BatchReleasePlan.refused("gui.xueximod.batch.dual_incomplete");
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

    /** 整段都是自带领域声明的附魔时，交回旧的开域路径，不报缺少父段。 */
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
                || role == ChainRole.TRAJECTORY_TRACKING) {
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
        return state.dropPath || state.upwellPath;
    }

    private static boolean nativeFullTrajectory(Trajectory trajectory) {
        return trajectory == Trajectory.PARABOLA || trajectory == Trajectory.UPWELL || trajectory == Trajectory.VORTEX;
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
        if (state.dualRemaining > 0) {
            branch.memberDelay = (2 - state.dualRemaining) * BatchReleasePlan.MEMBER_GAP;
            state.dualRemaining -= 1;
        }
        if (state.surface && !surfaceAllowed(techniques, hostIndex)) {
            return "gui.xueximod.batch.domain_surface";
        }
        return null;
    }

    private static String finishChild(List<Technique> techniques, BranchDraft parent, int hostIndex, ParseState state) {
        Technique host = techniques.get(hostIndex);
        ReleaseDefinition definition = ReleaseRegistry.get(host.getId());
        if (definition != null && definition.domain() != null && !definition.acceptsHitContinue()) {
            return "gui.xueximod.batch.no_continue_support";
        }
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
                state.stagger,
                definition == null ? 20 : Math.max(1, definition.lifetimeTicks()));
        return null;
    }

    private static boolean surfaceAllowed(List<Technique> techniques, int hostIndex) {
        ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(hostIndex).getId());
        if (definition == null) {
            return false;
        }
        if (definition.domain() != null && !definition.acceptsSurfaceAnchor()) {
            return false;
        }
        return definition.domain() == null || definition.acceptsSurfaceAnchor();
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
        com.bslx.XueXiMod.shu.technique.release.batch.BirthClearance groupClearance =
                com.bslx.XueXiMod.shu.technique.release.batch.BirthClearance.empty();
        boolean needsSurface = false;
        List<Technique> fired = new ArrayList<>();
        List<BatchReleasePlan.ParentBirth> parents = new ArrayList<>();
        int reserved = 0;
        for (int branchOrdinal = 0; branchOrdinal < state.branches.size(); branchOrdinal++) {
            BranchDraft branch = state.branches.get(branchOrdinal);
            Integer parentInstances = exactMultiply(1, branch.copies);
            if (parentInstances == null) {
                return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
            }
            int childInstances = 0;
            if (branch.child != null) {
                Integer scaledChildren = exactMultiply(branch.copies, branch.child.ownCopies);
                if (scaledChildren == null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                }
                childInstances = scaledChildren;
            }
            Integer nextInstances = exactAdd(instances, parentInstances);
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
                Integer hostBill = exactMultiply(branch.hostPrice, 1);
                if (hostBill == null) {
                    return BatchReleasePlan.refused("gui.xueximod.batch.quote_overflow");
                }
                Integer withHost = exactAdd(reserved, hostBill);
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
                        branch.hostPrice, retained, branch.entityLock, branch.dropPath, branch.upwellPath, branch.child,
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
        private int dualRemaining;
        private boolean stagger;
        private boolean surface;
        private boolean entityLock;
        private boolean dropPath;
        private boolean upwellPath;
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
        }
    }
}

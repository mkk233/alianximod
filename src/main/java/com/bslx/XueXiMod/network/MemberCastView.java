package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.multicast.ReplicationSpec;
import com.bslx.XueXiMod.skill.Skill;

import java.util.List;

/**
 * 一个多重成员的结构修饰。只由定义和原始下标折出来，不含玩家、报价或目标。
 * 折叠顺序是继承下标，然后本地下标。路径覆盖以后者为准；索敌和散布碰到一次即生效。
 */
final class MemberCastView {
    final int ordinal;
    final int hostIndex;
    final boolean independentEnchant;
    final List<Integer> inheritedModifierIndexes;
    final List<Integer> localModifierIndexes;
    final List<Integer> replicationIndexes;
    final List<Integer> attachedEnchantIndexes;
    final int copyCount;
    /** 连乘超出 int。份数不是截断后的最大整数，编译步骤会拒绝付款。 */
    final boolean quoteOverflow;
    final ReleaseInput chainInput;
    final boolean entityLock;
    final boolean spaceLock;
    final boolean tracking;
    final boolean scatter;
    final boolean drop;
    final Trajectory pathOverride;

    private MemberCastView(int ordinal, int hostIndex, boolean independentEnchant,
                           List<Integer> inheritedModifierIndexes, List<Integer> localModifierIndexes,
                           List<Integer> replicationIndexes, List<Integer> attachedEnchantIndexes,
                           int copyCount, boolean quoteOverflow, ReleaseInput chainInput, boolean entityLock,
                           boolean spaceLock, boolean tracking, boolean scatter, boolean drop,
                           Trajectory pathOverride) {
        this.ordinal = ordinal;
        this.hostIndex = hostIndex;
        this.independentEnchant = independentEnchant;
        this.inheritedModifierIndexes = List.copyOf(inheritedModifierIndexes);
        this.localModifierIndexes = List.copyOf(localModifierIndexes);
        this.replicationIndexes = List.copyOf(replicationIndexes);
        this.attachedEnchantIndexes = List.copyOf(attachedEnchantIndexes);
        this.copyCount = copyCount;
        this.quoteOverflow = quoteOverflow;
        this.chainInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        this.entityLock = entityLock;
        this.spaceLock = spaceLock;
        this.tracking = tracking;
        this.scatter = scatter;
        this.drop = drop;
        this.pathOverride = pathOverride;
    }

    static MemberCastView from(Skill skill, List<Technique> techniques, MulticastMember member) {
        int hostIndex = member.hostIndex();
        List<Integer> inherited = member.inheritedModifierIndexes();
        List<Integer> local = member.localModifierIndexes();
        ModifierFold fold = new ModifierFold();
        if (!member.independentEnchant()) {
            fold.readAll(techniques, inherited);
            fold.readAll(techniques, local);
        }
        CopyCount copies = member.independentEnchant()
                ? CopyCount.single()
                : copyProduct(techniques, member.replicationIndexes());
        ReleaseInput chainInput = chainInput(skill, techniques, hostIndex, fold.gatheredEnergy, member.independentEnchant());
        return new MemberCastView(member.ordinal(), hostIndex, member.independentEnchant(), inherited, local,
                member.replicationIndexes(), member.attachedEnchantIndexes(), copies.count, copies.overflow, chainInput,
                fold.entityLock, fold.spaceLock, fold.tracking, fold.scatter, fold.drop, fold.pathOverride);
    }

    private static ReleaseInput chainInput(Skill skill, List<Technique> techniques, int hostIndex,
                                            float gatheredEnergy, boolean independentEnchant) {
        if (independentEnchant || !SkillChainPlan.consumesElementMagic(skill, techniques, hostIndex)) {
            return ReleaseInput.BASE;
        }
        if (!Float.isFinite(gatheredEnergy) || gatheredEnergy <= 0f) {
            return ReleaseInput.BASE;
        }
        return new ReleaseInput(gatheredEnergy);
    }

    private static CopyCount copyProduct(List<Technique> techniques, List<Integer> replicationIndexes) {
        int runningCopies = 1;
        for (int slotIndex : replicationIndexes) {
            if (slotIndex < 0 || slotIndex >= techniques.size()) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(slotIndex).getId());
            ReplicationSpec replication = definition == null ? null : definition.replication();
            if (replication == null) {
                continue;
            }
            Integer product = MulticastArithmetic.multiply(runningCopies, replication.factor());
            if (product == null || product < 1) {
                return CopyCount.overflow();
            }
            runningCopies = product;
        }
        return new CopyCount(runningCopies, false);
    }

    private static final class CopyCount {
        private final int count;
        private final boolean overflow;

        private CopyCount(int count, boolean overflow) {
            this.count = count;
            this.overflow = overflow;
        }

        private static CopyCount single() {
            return new CopyCount(1, false);
        }

        private static CopyCount overflow() {
            return new CopyCount(1, true);
        }
    }

    private static final class ModifierFold {
        private float gatheredEnergy = 1f;
        private boolean entityLock;
        private boolean spaceLock;
        private boolean tracking;
        private boolean scatter;
        private boolean drop;
        private Trajectory pathOverride;

        private void readAll(List<Technique> techniques, List<Integer> slotIndexes) {
            for (int slotIndex : slotIndexes) {
                if (slotIndex < 0 || slotIndex >= techniques.size()) {
                    continue;
                }
                ReleaseDefinition definition = ReleaseRegistry.get(techniques.get(slotIndex).getId());
                if (definition == null) {
                    continue;
                }
                readOne(definition);
            }
        }

        private void readOne(ReleaseDefinition definition) {
            float supplied = definition.chainEnergyScale();
            if (Float.isFinite(supplied) && supplied > 0f && Math.abs(supplied - 1f) > 0.001f) {
                gatheredEnergy *= supplied;
            }
            ChainRole role = definition.role();
            if (role == ChainRole.SEEK_ENTITY) {
                entityLock = true;
            } else if (role == ChainRole.SEEK_SPACE) {
                spaceLock = true;
            } else if (role == ChainRole.TRAJECTORY_TRACKING) {
                tracking = true;
            } else if (role == ChainRole.TRAJECTORY_SCATTER) {
                scatter = true;
            } else if (role == ChainRole.TRAJECTORY_DROP || role == ChainRole.TRAJECTORY_PARALLEL) {
                pathOverride = definition.trajectory();
                drop = pathOverride == Trajectory.DROP;
            }
        }
    }
}

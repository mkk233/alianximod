package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastExpansion;
import com.bslx.XueXiMod.shu.technique.release.multicast.MulticastMember;
import com.bslx.XueXiMod.shu.technique.release.multicast.ReplicationSpec;
import com.bslx.XueXiMod.skill.Skill;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 把已有多重展开转成协同编译和批量编译都能消费的边界、份数和延迟。
 * 不另写解析、不挑选成员、不改复制范围，也不调度或扣费。
 */
final class MulticastCompileAdapter {
    private MulticastCompileAdapter() {
    }

    /** 当前链含任何已声明的多重，含 fixed(2) 和 remaining。 */
    static boolean chainSharesExpansion(List<Technique> techniques) {
        if (techniques == null) {
            return false;
        }
        for (Technique technique : techniques) {
            if (technique == null) {
                continue;
            }
            ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
            if (definition != null && definition.multicastSpec() != null) {
                return true;
            }
        }
        return false;
    }

    /**
     * 这一格正好是某次已有展开读过的最早原始下标时，返回那次展开。
     * 后面才打开的多重、或落在展开中间的格子，保持 null，调用方继续原流程。
     */
    static @Nullable MulticastExpansion expansionOpeningAt(List<Technique> techniques, int chainIndex) {
        if (techniques == null || chainIndex < 0 || chainIndex >= techniques.size()) {
            return null;
        }
        MulticastExpansion expansion = ChainSteps.expansionFrom(techniques, chainIndex);
        if (expansion == null || !expansion.applicable() || expansion.consumedIndexes().isEmpty()) {
            return null;
        }
        int earliestConsumed = Integer.MAX_VALUE;
        boolean coveredHere = false;
        for (int consumedIndex : expansion.consumedIndexes()) {
            if (consumedIndex == chainIndex) {
                coveredHere = true;
            }
            if (consumedIndex < earliestConsumed) {
                earliestConsumed = consumedIndex;
            }
        }
        if (!coveredHere || earliestConsumed != chainIndex) {
            return null;
        }
        return expansion;
    }

    /**
     * 可释放展开的下一原始格。空展开、未适用或游标不前进时返回 -1。
     * 调用方不得据此退回旧的两段或三份。
     */
    static int resumeIndex(MulticastExpansion expansion, int chainIndex) {
        if (expansion == null || !expansion.applicable() || !expansion.releases()) {
            return -1;
        }
        int resumeAt = expansion.nextIndex();
        if (resumeAt <= chainIndex) {
            return -1;
        }
        return resumeAt;
    }

    /**
     * 空展开、未适用或游标不前进时返回的拒绝键。
     * 有实际成员但读取没有离开当前格，与一个成员都没有分开。
     */
    static String boundaryRefusal(MulticastExpansion expansion) {
        if (expansion == null || !expansion.applicable()) {
            return "gui.xueximod.batch.multicast_boundary";
        }
        if (!expansion.releases()) {
            return "gui.xueximod.batch.multicast_empty";
        }
        return "gui.xueximod.batch.multicast_boundary";
    }

    /** 没有复制声明时返回 -1。调用方不得改回写死的 3。 */
    static int replicationFactor(Technique technique) {
        if (technique == null) {
            return -1;
        }
        ReleaseDefinition definition = ReleaseRegistry.get(technique.getId());
        ReplicationSpec replication = definition == null ? null : definition.replication();
        if (replication == null) {
            return -1;
        }
        return replication.factor();
    }

    /** 成员份数只取一次 {@link MemberCastView} 里的复制积，不再叠乘调用方留下的份数。 */
    static CopyRead memberCopies(Skill skill, List<Technique> techniques, MulticastMember member) {
        MemberCastView view = MemberCastView.from(skill, techniques, member);
        return new CopyRead(view.copyCount, view.quoteOverflow);
    }

    /** 延迟用展开成员自己的序号，不按本编译器接纳了第几个攻击重新编号。 */
    static int memberDelayTicks(MulticastExpansion expansion, int memberOrdinal) {
        return expansion.gapTicks(memberOrdinal);
    }

    static final class CopyRead {
        final int copyCount;
        final boolean overflow;

        private CopyRead(int copyCount, boolean overflow) {
            this.copyCount = copyCount;
            this.overflow = overflow;
        }
    }
}

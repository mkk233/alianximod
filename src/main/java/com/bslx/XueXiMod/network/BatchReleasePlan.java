package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;

import java.util.List;

/** 含本批新能力时的整组计划。没有新能力时编译器返回 null，旧链不读这份。 */
public final class BatchReleasePlan {
    public static final int INSTANCE_CAP = 64;
    public static final int STAGGER_GAP = 10;
    public static final int MEMBER_GAP = 3;

    final String refusalKey;
    final boolean needsSurface;
    final double clearanceHorizontal;
    final double clearanceVertical;
    final int reservedTotal;
    final int scaledCooldown;
    final List<ParentBirth> parents;
    final List<Technique> fired;

    private BatchReleasePlan(String refusalKey, boolean needsSurface, double clearanceHorizontal,
                             double clearanceVertical, int reservedTotal, int scaledCooldown,
                             List<ParentBirth> parents, List<Technique> fired) {
        this.refusalKey = refusalKey;
        this.needsSurface = needsSurface;
        this.clearanceHorizontal = clearanceHorizontal;
        this.clearanceVertical = clearanceVertical;
        this.reservedTotal = reservedTotal;
        this.scaledCooldown = scaledCooldown;
        this.parents = parents;
        this.fired = fired;
    }

    static BatchReleasePlan refused(String refusalKey) {
        return new BatchReleasePlan(refusalKey, false, 0.0D, 0.0D, 0, 0, List.of(), List.of());
    }

    static BatchReleasePlan ready(boolean needsSurface, double clearanceHorizontal, double clearanceVertical,
                                  int reservedTotal, int scaledCooldown, List<ParentBirth> parents,
                                  List<Technique> fired) {
        return new BatchReleasePlan(null, needsSurface, clearanceHorizontal, clearanceVertical, reservedTotal,
                scaledCooldown, parents, fired);
    }

    public String refusalKey() {
        return refusalKey;
    }

    static final class ChildPlan {
        final ChainSegment segment;
        final int ownCopies;
        final int hostPrice;
        final int retainedPrice;
        final double radius;
        final boolean entityLock;
        final boolean dropPath;
        final boolean upwellPath;
        final boolean parallelPath;
        final boolean stagger;
        final int lifetimeTicks;

        ChildPlan(ChainSegment segment, int ownCopies, int hostPrice, int retainedPrice, double radius,
                  boolean entityLock, boolean dropPath, boolean upwellPath, boolean parallelPath, boolean stagger,
                  int lifetimeTicks) {
            this.segment = segment;
            this.ownCopies = ownCopies;
            this.hostPrice = hostPrice;
            this.retainedPrice = retainedPrice;
            this.radius = radius;
            this.entityLock = entityLock;
            this.dropPath = dropPath;
            this.upwellPath = upwellPath;
            this.parallelPath = parallelPath;
            this.stagger = stagger;
            this.lifetimeTicks = lifetimeTicks;
        }
    }

    static final class ParentBirth {
        final int branchOrdinal;
        final int copyOrdinal;
        final int spreadCount;
        final int birthDelay;
        final ChainSegment segment;
        final int hostPrice;
        final int retainedPrice;
        final boolean entityLock;
        final boolean dropPath;
        final boolean upwellPath;
        final boolean parallelPath;
        final ChildPlan child;
        final int lifetimeTicks;
        final boolean earliestSweep;

        ParentBirth(int branchOrdinal, int copyOrdinal, int spreadCount, int birthDelay, ChainSegment segment,
                    int hostPrice, int retainedPrice, boolean entityLock, boolean dropPath, boolean upwellPath,
                    boolean parallelPath, ChildPlan child, int lifetimeTicks, boolean earliestSweep) {
            this.branchOrdinal = branchOrdinal;
            this.copyOrdinal = copyOrdinal;
            this.spreadCount = spreadCount;
            this.birthDelay = birthDelay;
            this.segment = segment;
            this.hostPrice = hostPrice;
            this.retainedPrice = retainedPrice;
            this.entityLock = entityLock;
            this.dropPath = dropPath;
            this.upwellPath = upwellPath;
            this.parallelPath = parallelPath;
            this.child = child;
            this.lifetimeTicks = lifetimeTicks;
            this.earliestSweep = earliestSweep;
        }
    }
}

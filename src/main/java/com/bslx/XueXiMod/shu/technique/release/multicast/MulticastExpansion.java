package com.bslx.XueXiMod.shu.technique.release.multicast;

import java.util.List;
import java.util.Objects;

/**
 * 一次多重的不可变展开。全项目只用 {@link MulticastParser} 产生它。
 * 下标都是原始装配顺序，不是 {@code TechniqueChain.split} 之后的段号。
 */
public final class MulticastExpansion {
    /** 多重队列里，后一个实际成员比前一个晚的刻数。第 0 个成员不延迟。 */
    public static final int MEMBER_GAP_TICKS = 3;

    private final boolean applicable;
    private final boolean releases;
    private final int startIndex;
    private final int nextIndex;
    private final List<Integer> consumedIndexes;
    private final List<Integer> chargedIndexes;
    private final List<MulticastIgnore> ignoredSlots;
    private final List<MulticastMember> orderedMembers;
    private final List<Integer> inheritedModifierIndexes;

    MulticastExpansion(boolean applicable, boolean releases, int startIndex, int nextIndex,
                       List<Integer> consumedIndexes, List<Integer> chargedIndexes,
                       List<MulticastIgnore> ignoredSlots, List<MulticastMember> orderedMembers,
                       List<Integer> inheritedModifierIndexes) {
        this.applicable = applicable;
        this.releases = releases;
        this.startIndex = startIndex;
        this.nextIndex = nextIndex;
        this.consumedIndexes = List.copyOf(consumedIndexes);
        this.chargedIndexes = List.copyOf(chargedIndexes);
        this.ignoredSlots = List.copyOf(ignoredSlots);
        this.orderedMembers = List.copyOf(orderedMembers);
        this.inheritedModifierIndexes = List.copyOf(inheritedModifierIndexes);
    }

    static MulticastExpansion notApplicable(int startIndex) {
        return new MulticastExpansion(false, false, startIndex, startIndex,
                List.of(), List.of(), List.of(), List.of(), List.of());
    }

    /** 根下标不是多重声明时为假。调用者保持原来的单段路径。 */
    public boolean applicable() {
        return applicable;
    }

    /** 至少有一个实际执行成员。假表示空展开，付款前失败。 */
    public boolean releases() {
        return releases;
    }

    /** 根多重所在的原始下标。 */
    public int startIndex() {
        return startIndex;
    }

    /** 展开没有读到的下一格。空展开只供查看，调用者不得据此推进游标。 */
    public int nextIndex() {
        return nextIndex;
    }

    /** 本次读过的原始下标，升序，含根多重、前缀和内层。 */
    public List<Integer> consumedIndexes() {
        return consumedIndexes;
    }

    /** 可以各计一次费用和冷却的原始下标，升序。空展开为空。 */
    public List<Integer> chargedIndexes() {
        return chargedIndexes;
    }

    /** 读过但不计费、不进冷却、也不再后附的格。 */
    public List<MulticastIgnore> ignoredSlots() {
        return ignoredSlots;
    }

    /** 实际执行队列。修饰不是成员。复制份不拆成多个成员。 */
    public List<MulticastMember> orderedMembers() {
        return orderedMembers;
    }

    /**
     * 根多重之前、作用于整段展开的索敌、弹道和增幅下标，从前到后。
     * 正伤成员各自持有同一份，不按嵌套层重复。
     */
    public List<Integer> inheritedModifierIndexes() {
        return inheritedModifierIndexes;
    }

    /** 该序号成员相对出手时刻的延迟。0 以及更小的序号立即执行。 */
    public int gapTicks(int memberOrdinal) {
        if (memberOrdinal <= 0) {
            return 0;
        }
        long delayedTicks = (long) memberOrdinal * MEMBER_GAP_TICKS;
        if (delayedTicks > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) delayedTicks;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof MulticastExpansion expansion)) {
            return false;
        }
        return applicable == expansion.applicable
                && releases == expansion.releases
                && startIndex == expansion.startIndex
                && nextIndex == expansion.nextIndex
                && consumedIndexes.equals(expansion.consumedIndexes)
                && chargedIndexes.equals(expansion.chargedIndexes)
                && ignoredSlots.equals(expansion.ignoredSlots)
                && orderedMembers.equals(expansion.orderedMembers)
                && inheritedModifierIndexes.equals(expansion.inheritedModifierIndexes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(applicable, releases, startIndex, nextIndex, consumedIndexes, chargedIndexes,
                ignoredSlots, orderedMembers, inheritedModifierIndexes);
    }
}

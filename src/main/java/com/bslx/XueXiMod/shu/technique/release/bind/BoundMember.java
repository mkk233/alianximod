package com.bslx.XueXiMod.shu.technique.release.bind;

import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.cooperative.CooperativeCastRecipe;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * 冻结子计划里的一名攻击宿主。附魔下标相对冻结序列。
 * 展开成员另存原始槽上的序号、修饰归属和复制份数；0 份表示旧路径还没解析复制。
 */
public final class BoundMember {
    private final int localIndex;
    private final int originalIndex;
    private final Technique technique;
    private final List<Integer> enchantLocalIndexes;
    private final ReleaseInput chainInput;
    private final int memberOrdinal;
    private final List<Integer> inheritedModifierIndexes;
    private final List<Integer> localModifierIndexes;
    private final List<Integer> replicationIndexes;
    private final List<Integer> attachedEnchantIndexes;
    private final int copyCount;
    private final @Nullable CooperativeCastRecipe cooperativeRecipe;
    private final boolean cooperativeAbsorbed;

    private BoundMember(int localIndex, int originalIndex, Technique technique,
                        List<Integer> enchantLocalIndexes, ReleaseInput chainInput,
                        int memberOrdinal, List<Integer> inheritedModifierIndexes,
                        List<Integer> localModifierIndexes, List<Integer> replicationIndexes,
                        List<Integer> attachedEnchantIndexes, int copyCount,
                        @Nullable CooperativeCastRecipe cooperativeRecipe, boolean cooperativeAbsorbed) {
        this.localIndex = localIndex;
        this.originalIndex = originalIndex;
        this.technique = technique;
        this.enchantLocalIndexes = enchantLocalIndexes;
        this.chainInput = chainInput;
        this.memberOrdinal = memberOrdinal;
        this.inheritedModifierIndexes = inheritedModifierIndexes;
        this.localModifierIndexes = localModifierIndexes;
        this.replicationIndexes = replicationIndexes;
        this.attachedEnchantIndexes = attachedEnchantIndexes;
        this.copyCount = copyCount;
        this.cooperativeRecipe = cooperativeRecipe;
        this.cooperativeAbsorbed = cooperativeAbsorbed;
    }

    /** 旧的单攻击捕获。复制份数留 0，表示还没解析。 */
    public static BoundMember create(int localIndex, int originalIndex, Technique technique,
                                     List<Integer> enchantLocalIndexes, ReleaseInput chainInput) {
        return create(localIndex, originalIndex, technique, enchantLocalIndexes, chainInput,
                0, List.of(), List.of(), List.of(), List.of(), 0);
    }

    /** 展开后的一名宿主。名单都复制成不可变列表。 */
    public static BoundMember create(int localIndex, int originalIndex, Technique technique,
                                     List<Integer> enchantLocalIndexes, ReleaseInput chainInput,
                                     int memberOrdinal, List<Integer> inheritedModifierIndexes,
                                     List<Integer> localModifierIndexes, List<Integer> replicationIndexes,
                                     List<Integer> attachedEnchantIndexes, int copyCount) {
        List<Integer> copiedIndexes = enchantLocalIndexes == null ? List.of() : List.copyOf(enchantLocalIndexes);
        ReleaseInput frozenInput = chainInput == null ? ReleaseInput.BASE : chainInput;
        return new BoundMember(localIndex, originalIndex, technique, copiedIndexes, frozenInput,
                memberOrdinal,
                inheritedModifierIndexes == null ? List.of() : List.copyOf(inheritedModifierIndexes),
                localModifierIndexes == null ? List.of() : List.copyOf(localModifierIndexes),
                replicationIndexes == null ? List.of() : List.copyOf(replicationIndexes),
                attachedEnchantIndexes == null ? List.of() : List.copyOf(attachedEnchantIndexes),
                Math.max(0, copyCount), null, false);
    }

    /** 挂上协同配方，其余冻结字段保持不变。 */
    public BoundMember withCooperative(@Nullable CooperativeCastRecipe recipe, boolean absorbed) {
        return new BoundMember(localIndex, originalIndex, technique, enchantLocalIndexes, chainInput,
                memberOrdinal, inheritedModifierIndexes, localModifierIndexes, replicationIndexes,
                attachedEnchantIndexes, copyCount, recipe, absorbed);
    }

    public int localIndex() {
        return localIndex;
    }

    public int originalIndex() {
        return originalIndex;
    }

    public Technique technique() {
        return technique;
    }

    public List<Integer> enchantLocalIndexes() {
        return enchantLocalIndexes;
    }

    public ReleaseInput chainInput() {
        return chainInput;
    }

    /** 展开队列序号。旧的单攻击捕获固定为 0。 */
    public int memberOrdinal() {
        return memberOrdinal;
    }

    /** 外置索敌、弹道、增幅的原始装配下标。只属于这一名成员。 */
    public List<Integer> inheritedModifierIndexes() {
        return inheritedModifierIndexes;
    }

    /** 本作用域挂到这一名宿主的索敌、弹道、增幅，原始装配下标。 */
    public List<Integer> localModifierIndexes() {
        return localModifierIndexes;
    }

    /** 只作用于这一名宿主的一变多原始下标。空列表不是“再去整段里找复制”。 */
    public List<Integer> replicationIndexes() {
        return replicationIndexes;
    }

    /** 挂到这一名宿主的附魔原始下标。 */
    public List<Integer> attachedEnchantIndexes() {
        return attachedEnchantIndexes;
    }

    /**
     * 展开成员的复制份数，至少为 1。
     * 0 表示这条成员来自旧工厂，份数还要等后续接线，不能当成已经算出的 1 份。
     */
    public int copyCount() {
        return copyCount;
    }

    public @Nullable CooperativeCastRecipe cooperativeRecipe() {
        return cooperativeRecipe;
    }

    public boolean cooperativeAbsorbed() {
        return cooperativeAbsorbed;
    }
}

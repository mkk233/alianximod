package com.bslx.XueXiMod.shu.technique.chain;

import java.util.List;

/** 一段链的动作下标和已挂附魔下标。拆链时生成，本身不判断角色或费用。 */
public final class ChainSegment {
    private final int actionIndex;
    private final List<Integer> enchantIndexes;

    /** 复制附魔下标；传入空列表会失败。 */
    public ChainSegment(int actionIndex, List<Integer> enchantIndexes) {
        this.actionIndex = actionIndex;
        this.enchantIndexes = List.copyOf(enchantIndexes);
    }

    public int actionIndex() {
        return actionIndex;
    }

    public List<Integer> enchantIndexes() {
        return enchantIndexes;
    }
}

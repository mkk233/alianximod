package com.bslx.XueXiMod.shu.technique.chain;

import java.util.List;

public final class ChainSegment {
    private final int actionIndex;
    private final List<Integer> enchantIndexes;

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

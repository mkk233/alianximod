package com.bslx.XueXiMod.status.wood.resource;

/**
 * 分割结果。kept 是原片段，区间变成低端 leftUnits 长，修订号加一。
 * created 是新片段，接着高端，修订号为 1。两边继承同一根、来源和到期。
 */
public record SplitResult(boolean accepted, WoodReject reject, FragmentHandle kept, FragmentHandle created) {
    public static SplitResult rejected(WoodReject reject) {
        return new SplitResult(false, reject, null, null);
    }
}

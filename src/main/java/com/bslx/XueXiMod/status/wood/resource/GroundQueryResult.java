package com.bslx.XueXiMod.status.wood.resource;

import java.util.List;

/** 空间查询结果。读操作没有副作用。truncated 表示还有匹配项没有放进列表。 */
public record GroundQueryResult(
        boolean accepted,
        WoodReject reject,
        boolean truncated,
        List<GroundFragmentView> fragments) {
    public GroundQueryResult {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
    }

    public static GroundQueryResult rejected(WoodReject reject) {
        return new GroundQueryResult(false, reject, false, List.of());
    }
}

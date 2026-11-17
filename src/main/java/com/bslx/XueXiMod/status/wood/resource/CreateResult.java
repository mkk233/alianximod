package com.bslx.XueXiMod.status.wood.resource;

import java.util.List;
import java.util.UUID;

/**
 * 发布结果。成功时 fragments 与提交的根顺序一致，修订号都是 1。
 * 空列表成功会消耗票据和编号，但不留下活跃批次。失败同样作废已识别的票据。
 */
public record CreateResult(
        boolean accepted,
        WoodReject reject,
        UUID batchId,
        long serial,
        List<FragmentHandle> fragments) {
    public CreateResult {
        fragments = fragments == null ? List.of() : List.copyOf(fragments);
    }

    public static CreateResult rejected(WoodReject reject) {
        return new CreateResult(false, reject, null, -1L, List.of());
    }

    public static CreateResult rejected(WoodReject reject, long serial) {
        return new CreateResult(false, reject, null, serial, List.of());
    }
}

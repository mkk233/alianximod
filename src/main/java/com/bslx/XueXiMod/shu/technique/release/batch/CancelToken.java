package com.bslx.XueXiMod.shu.technique.release.batch;

import java.util.UUID;

/** 根组的取消令牌。失效后未出生的份和还没触发的子段不再生成。 */
public final class CancelToken {
    private final UUID tokenId = UUID.randomUUID();
    private boolean cancelled;

    public UUID tokenId() {
        return tokenId;
    }

    public boolean cancelled() {
        return cancelled;
    }

    /** 标记为已取消。未出生的份由调用方再看这个标志。 */
    public void cancel() {
        this.cancelled = true;
    }
}

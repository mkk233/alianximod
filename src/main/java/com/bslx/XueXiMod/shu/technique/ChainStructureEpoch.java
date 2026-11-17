package com.bslx.XueXiMod.shu.technique;

import java.util.concurrent.atomic.AtomicLong;

/**
 * 进程内单调结构代际。成功登记，或已公开的结构配置写完之后，调用 {@link #changed()}。
 * 初值保持 {@link AtomicLong} 的构造默认值，重进服务端不归零。本类不操作计划缓存。
 */
public final class ChainStructureEpoch {
    private static final AtomicLong EPOCH = new AtomicLong();

    private ChainStructureEpoch() {
    }

    public static long current() {
        return EPOCH.get();
    }

    public static void changed() {
        EPOCH.incrementAndGet();
    }
}

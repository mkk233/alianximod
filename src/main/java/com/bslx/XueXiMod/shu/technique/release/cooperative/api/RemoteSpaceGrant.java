package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 远距空间检查已经计入本刻额度。只有计量器能签发，调用方不能自己构造。 */
public final class RemoteSpaceGrant {
    private final long serverTick;
    private final RemoteCheckPhase phase;
    private final int cellCount;
    private final int stepCount;
    private final int entityCount;
    private final int chunkCount;

    RemoteSpaceGrant(long serverTick, RemoteCheckPhase phase, int cellCount, int stepCount,
                     int entityCount, int chunkCount) {
        this.serverTick = serverTick;
        this.phase = phase;
        this.cellCount = cellCount;
        this.stepCount = stepCount;
        this.entityCount = entityCount;
        this.chunkCount = chunkCount;
    }

    public long serverTick() {
        return serverTick;
    }

    public RemoteCheckPhase phase() {
        return phase;
    }

    public int cellCount() {
        return cellCount;
    }

    public int stepCount() {
        return stepCount;
    }

    public int entityCount() {
        return entityCount;
    }

    public int chunkCount() {
        return chunkCount;
    }
}

package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import java.util.Optional;

/**
 * 全服远距空间检查的每刻额度。端点方块和通道方块、实体都算进同一对上限。
 * 不代替既有每刻 32 个归位工作单位，也不另开一份 32。额度不足时返回空，不截断后当作安全。
 * 只在当前服务器主线程调用。换刻时清零，不把上一刻的结论留到下一刻。
 */
public final class RemoteRecallMeter {
    private static long openTick = Long.MIN_VALUE;
    private static int usedCells;
    private static int usedEntities;

    private RemoteRecallMeter() {
    }

    public static Optional<RemoteSpaceGrant> take(long serverTick, RemoteCheckPhase phase, int cellCount,
                                                  int stepCount, int entityCount, int chunkCount) {
        if (!admits(phase, cellCount, stepCount, entityCount, chunkCount)) {
            return Optional.empty();
        }
        synchronized (RemoteRecallMeter.class) {
            if (serverTick != openTick) {
                openTick = serverTick;
                usedCells = 0;
                usedEntities = 0;
            }
            if ((long) usedCells + cellCount > CooperativeStructureLimits.REMOTE_SERVER_CELL_BUDGET
                    || (long) usedEntities + entityCount > CooperativeStructureLimits.REMOTE_SERVER_ENTITY_BUDGET) {
                return Optional.empty();
            }
            usedCells += cellCount;
            usedEntities += entityCount;
            return Optional.of(new RemoteSpaceGrant(serverTick, phase, cellCount, stepCount, entityCount, chunkCount));
        }
    }

    private static boolean admits(RemoteCheckPhase phase, int cellCount, int stepCount, int entityCount, int chunkCount) {
        if (phase == null || cellCount < 0 || stepCount < 0 || entityCount < 0 || chunkCount < 0) {
            return false;
        }
        if (phase == RemoteCheckPhase.ENDPOINT) {
            return stepCount == 0 && entityCount == 0 && chunkCount == 0
                    && cellCount > 0 && cellCount <= CooperativeStructureLimits.REMOTE_ENDPOINT_CELL_CAP;
        }
        return cellCount > 0 && cellCount <= CooperativeStructureLimits.REMOTE_CHANNEL_CELL_CAP
                && stepCount <= CooperativeStructureLimits.REMOTE_CHANNEL_STEP_CAP
                && entityCount <= CooperativeStructureLimits.REMOTE_CHANNEL_ENTITY_CAP
                && chunkCount <= CooperativeStructureLimits.REMOTE_CHANNEL_CHUNK_CAP;
    }
}

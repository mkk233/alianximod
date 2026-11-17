package com.bslx.XueXiMod.shu.technique.release.parting;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import java.util.UUID;

/**
 * 一格的原状态和所属操作。推荐规则：原状态以第一次取得控制时的记录为准。
 */
public final class PartingCellLease {
    public enum SourceKind {
        SOURCE,
        FLOWING
    }

    private final BlockPos cell;
    private final BlockState originalState;
    private final SourceKind sourceKind;
    private final UUID operationKey;
    private boolean controlled = true;

    public PartingCellLease(BlockPos cell, BlockState originalState, SourceKind sourceKind, UUID operationKey) {
        this.cell = cell.immutable();
        this.originalState = originalState;
        this.sourceKind = sourceKind;
        this.operationKey = operationKey;
    }

    public BlockPos cell() {
        return cell;
    }

    public BlockState originalState() {
        return originalState;
    }

    public SourceKind sourceKind() {
        return sourceKind;
    }

    public UUID operationKey() {
        return operationKey;
    }

    public boolean controlled() {
        return controlled;
    }

    public void releaseControl() {
        controlled = false;
    }
}

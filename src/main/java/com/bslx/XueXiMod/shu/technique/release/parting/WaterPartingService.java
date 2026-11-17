package com.bslx.XueXiMod.shu.technique.release.parting;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingSavedData.PendingColumn;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.material.Fluids;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 让锋的真实干通道。其它术式不会因为编号被拉进来。
 * <p>
 * 下列是推荐规则，不是已确认玩法：一分为三共享一次扣费并取几何并集；双重释放各扣一次；
 * 只扫过旧干通道不续期也不扣费；含水方块、海草、海带不改；提交前离开就释放预留，提交后不退款。
 */
public final class WaterPartingService {
    public static final float RESERVE_COST = 50f;
    public static final int CHANNEL_TICKS = 200;

    private static final Map<UUID, PartingOperation> OPERATIONS = new HashMap<>();
    private static final Map<UUID, PartingOperation> ENTITIES = new HashMap<>();
    private static final Map<Long, PartingCellLease> TENTATIVE = new LinkedHashMap<>();

    private WaterPartingService() {
    }

    /**
     * 可用水势必须严格大于 50。等于 50 时 reserve(50) 仍会成功，所以这里先判断再预留。
     * 同一个操作身份再次进入时不再预留。
     */
    public static PartingOperation prepare(ServerPlayer player, PartingOperationId operationId) {
        if (player == null || operationId == null) {
            return null;
        }
        PartingOperation existing = OPERATIONS.get(operationId.operationKey());
        if (existing != null && !existing.cancelled()) {
            return existing;
        }
        if (!(WaterMomentum.available(player) > RESERVE_COST)) {
            return null;
        }
        WaterMomentum.Hold hold = WaterMomentum.reserve(player, RESERVE_COST);
        if (!hold.reserved()) {
            return null;
        }
        PartingOperation created = new PartingOperation(operationId, player.getUUID(), player.serverLevel().dimension(), hold);
        OPERATIONS.put(operationId.operationKey(), created);
        return created;
    }

    public static void attach(PartingOperation operation, TechniqueHitEntity hitEntity) {
        if (operation == null || hitEntity == null || operation.cancelled() || operation.charged()) {
            return;
        }
        operation.attach(hitEntity);
        ENTITIES.put(hitEntity.getUUID(), operation);
    }

    public static void noteSpawn(PartingOperation operation, int acceptedCount) {
        if (operation == null || operation.cancelled() || operation.charged()) {
            return;
        }
        if (acceptedCount <= 0 && !operation.hasLivingAttachment()) {
            discard(operation);
        }
    }

    /** 命中实体 age 第一次达到就绪时调用。没有登记的实体什么都不做。 */
    public static void activated(TechniqueHitEntity hitEntity) {
        if (hitEntity == null || !(hitEntity.level() instanceof ServerLevel level)) {
            return;
        }
        PartingOperation operation = ENTITIES.get(hitEntity.getUUID());
        if (operation == null || operation.cancelled() || operation.scanned()) {
            return;
        }
        operation.capture(hitEntity.getUUID(), PartingGeometrySnapshot.capture(hitEntity));
        tryCommit(level, operation);
    }

    public static void tick(MinecraftServer server) {
        if (server == null) {
            return;
        }
        for (ServerLevel level : server.getAllLevels()) {
            finishWaiting(level);
            WaterPartingSavedData.get(level).observeAndExpire(level);
        }
    }

    public static void onChunkLoad(ServerLevel level, ChunkAccess chunk) {
        if (level == null || chunk == null) {
            return;
        }
        WaterPartingSavedData data = WaterPartingSavedData.get(level);
        List<PendingColumn> claimed = data.claimPending(chunk.getPos());
        if (claimed.isEmpty()) {
            return;
        }
        List<PendingColumn> unfinished = new ArrayList<>();
        long gameTime = level.getGameTime();
        for (PendingColumn column : claimed) {
            long expireGameTime = data.deadline(column.operationKey);
            if (expireGameTime < 0L || gameTime >= expireGameTime) {
                continue;
            }
            if (!level.hasChunkAt(new BlockPos(column.blockX, level.getMinBuildHeight(), column.blockZ))) {
                unfinished.add(column);
                continue;
            }
            List<BlockPos> waterCells = new ArrayList<>();
            collectColumn(level, column.snapshot, column.blockX, column.blockZ, waterCells, unfinished, column);
            applyCharged(level, data, column.operationKey, waterCells);
        }
        data.restorePending(unfinished);
    }

    public static void cancelUncommitted(UUID playerId) {
        if (playerId == null) {
            return;
        }
        List<PartingOperation> owned = new ArrayList<>();
        for (PartingOperation operation : OPERATIONS.values()) {
            if (playerId.equals(operation.playerId()) && !operation.charged()) {
                owned.add(operation);
            }
        }
        for (PartingOperation operation : owned) {
            discard(operation);
        }
    }

    /** 租约仍由服务控制、格子仍是这次写出的空气时，挡住水再次写入。 */
    public static boolean blocksIncoming(ServerLevel level, BlockPos cell) {
        if (level == null || cell == null) {
            return false;
        }
        PartingCellLease tentative = TENTATIVE.get(cell.asLong());
        if (tentative != null && tentative.controlled() && level.getBlockState(cell).isAir()) {
            return true;
        }
        return WaterPartingSavedData.get(level).blocks(level, cell);
    }

    private static void finishWaiting(ServerLevel level) {
        List<PartingOperation> ready = new ArrayList<>();
        for (PartingOperation operation : OPERATIONS.values()) {
            if (!operation.cancelled() && !operation.scanned() && operation.dimension().equals(level.dimension())) {
                ready.add(operation);
            }
        }
        for (PartingOperation operation : ready) {
            tryCommit(level, operation);
        }
    }

    private static void tryCommit(ServerLevel level, PartingOperation operation) {
        if (operation.cancelled() || operation.charged() || operation.waitingForActivation()) {
            return;
        }
        if (operation.snapshots().isEmpty()) {
            discard(operation);
            return;
        }
        List<BlockPos> waterCells = new ArrayList<>();
        List<PendingColumn> unloaded = new ArrayList<>();
        LinkedHashSet<Long> seenColumns = new LinkedHashSet<>();
        for (PartingGeometrySnapshot snapshot : operation.snapshots()) {
            collectSnapshot(level, operation.operationId().operationKey(), snapshot, waterCells, unloaded, seenColumns);
        }
        List<PartingCellLease> written = writeWater(level, operation.operationId().operationKey(), waterCells);
        if (written.isEmpty()) {
            if (unloaded.isEmpty()) {
                discard(operation);
            }
            return;
        }
        ServerPlayer player = level.getServer().getPlayerList().getPlayer(operation.playerId());
        boolean committed = WaterMomentum.commitReserved(player, operation.hold());
        if (!committed) {
            rollback(level, written);
            discard(operation);
            return;
        }
        operation.markCharged();
        operation.markScanned();
        long expireGameTime = level.getGameTime() + CHANNEL_TICKS;
        WaterPartingSavedData.get(level).adopt(operation.operationId().operationKey(), expireGameTime, written, unloaded);
        forgetTentative(written);
        forgetBook(operation);
    }

    private static void applyCharged(ServerLevel level, WaterPartingSavedData data, UUID operationKey, List<BlockPos> waterCells) {
        if (waterCells.isEmpty()) {
            return;
        }
        List<PartingCellLease> written = writeWater(level, operationKey, waterCells);
        if (written.isEmpty()) {
            return;
        }
        long expireGameTime = data.deadline(operationKey);
        data.adopt(operationKey, expireGameTime, written, List.of());
        forgetTentative(written);
    }

    private static void collectSnapshot(ServerLevel level, UUID operationKey, PartingGeometrySnapshot snapshot,
                                        List<BlockPos> waterCells, List<PendingColumn> unloaded, LinkedHashSet<Long> seenColumns) {
        double reach = snapshot.halfLength() + PartingGeometrySnapshot.CHANNEL_HALF_WIDTH;
        int west = Mth.floor(snapshot.center().x - reach);
        int east = Mth.floor(snapshot.center().x + reach);
        int north = Mth.floor(snapshot.center().z - reach);
        int south = Mth.floor(snapshot.center().z + reach);
        for (int blockX = west; blockX <= east; blockX++) {
            for (int blockZ = north; blockZ <= south; blockZ++) {
                if (!snapshot.containsColumn(blockX, blockZ)) {
                    continue;
                }
                long columnKey = BlockPos.asLong(blockX, 0, blockZ);
                if (!seenColumns.add(columnKey)) {
                    continue;
                }
                PendingColumn marker = new PendingColumn(operationKey, snapshot, blockX, blockZ);
                collectColumn(level, snapshot, blockX, blockZ, waterCells, unloaded, marker);
            }
        }
    }

    /**
     * 从斩击竖向覆盖里碰到的纯水往上找到水表，再往下直到固体或世界底界。
     * 含水方块、海草、海带不是纯水液体块，整格跳过。未加载区块不强加载。
     */
    private static void collectColumn(ServerLevel level, PartingGeometrySnapshot snapshot, int blockX, int blockZ,
                                      List<BlockPos> waterCells, List<PendingColumn> unloaded, PendingColumn marker) {
        BlockPos probe = new BlockPos(blockX, level.getMinBuildHeight(), blockZ);
        if (!level.hasChunkAt(probe)) {
            unloaded.add(marker);
            return;
        }
        int lower = Mth.floor(snapshot.lowerCut());
        int upper = Mth.ceil(snapshot.upperCut()) - 1;
        int ceiling = level.getMaxBuildHeight() - 1;
        int floor = level.getMinBuildHeight();
        java.util.HashSet<Integer> included = new java.util.HashSet<>();
        for (int cursor = lower; cursor <= upper; cursor++) {
            if (!pureWater(level.getBlockState(new BlockPos(blockX, cursor, blockZ))) || included.contains(cursor)) {
                continue;
            }
            int surface = cursor;
            while (surface < ceiling && pureWater(level.getBlockState(new BlockPos(blockX, surface + 1, blockZ)))) {
                surface++;
            }
            for (int step = surface; step >= floor; step--) {
                BlockPos cell = new BlockPos(blockX, step, blockZ);
                if (!pureWater(level.getBlockState(cell))) {
                    break;
                }
                if (!included.add(step)) {
                    break;
                }
                waterCells.add(cell.immutable());
            }
        }
    }

    private static List<PartingCellLease> writeWater(ServerLevel level, UUID operationKey, List<BlockPos> waterCells) {
        List<PartingCellLease> written = new ArrayList<>();
        for (BlockPos cell : waterCells) {
            BlockState current = level.getBlockState(cell);
            if (!pureWater(current)) {
                continue;
            }
            PartingCellLease lease = new PartingCellLease(cell, current,
                    current.getFluidState().isSource() ? PartingCellLease.SourceKind.SOURCE : PartingCellLease.SourceKind.FLOWING,
                    operationKey);
            TENTATIVE.put(cell.asLong(), lease);
            BlockState again = level.getBlockState(cell);
            if (!pureWater(again)) {
                TENTATIVE.remove(cell.asLong());
                break;
            }
            boolean placed = level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            if (!placed || !level.getBlockState(cell).isAir()) {
                TENTATIVE.remove(cell.asLong());
                break;
            }
            written.add(lease);
        }
        return written;
    }

    private static void rollback(ServerLevel level, List<PartingCellLease> written) {
        for (PartingCellLease lease : written) {
            TENTATIVE.remove(lease.cell().asLong());
            lease.releaseControl();
            if (level.getBlockState(lease.cell()).isAir()) {
                level.setBlock(lease.cell(), lease.originalState(), 3);
            }
        }
    }

    private static void forgetTentative(List<PartingCellLease> written) {
        for (PartingCellLease lease : written) {
            TENTATIVE.remove(lease.cell().asLong());
        }
    }

    private static void forgetBook(PartingOperation operation) {
        OPERATIONS.remove(operation.operationId().operationKey());
        Iterator<Map.Entry<UUID, PartingOperation>> iterator = ENTITIES.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue() == operation) {
                iterator.remove();
            }
        }
    }

    private static void discard(PartingOperation operation) {
        operation.cancel();
        forgetBook(operation);
        Iterator<PartingCellLease> leases = TENTATIVE.values().iterator();
        while (leases.hasNext()) {
            if (operation.operationId().operationKey().equals(leases.next().operationKey())) {
                leases.remove();
            }
        }
    }

    private static boolean pureWater(BlockState state) {
        return state.getBlock() instanceof LiquidBlock && state.getFluidState().getType().isSame(Fluids.WATER);
    }
}

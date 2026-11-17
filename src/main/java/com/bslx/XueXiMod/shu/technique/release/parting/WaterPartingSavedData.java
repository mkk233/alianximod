package com.bslx.XueXiMod.shu.technique.release.parting;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 按维度保存已提交的分水租约。到期时间用该维度的游戏时间，停服不把 200 tick 走完。
 * 玩家数据和这份存档没有跨文件崩溃原子性，不能当成事务。
 */
public final class WaterPartingSavedData extends SavedData {
    private static final String STORAGE_ID = "xueximod_water_parting";

    private final Map<Long, HeldCell> cells = new HashMap<>();
    private final Map<UUID, Long> deadlines = new HashMap<>();
    private final List<PendingColumn> pending = new ArrayList<>();
    private CompoundTag unread;

    public static WaterPartingSavedData get(ServerLevel level) {
        WaterPartingSavedData data = level.getDataStorage().computeIfAbsent(
                WaterPartingSavedData::load, WaterPartingSavedData::fresh, STORAGE_ID);
        data.ensureLoaded(level);
        return data;
    }

    public static WaterPartingSavedData fresh() {
        return new WaterPartingSavedData();
    }

    public static WaterPartingSavedData load(CompoundTag tag) {
        WaterPartingSavedData data = new WaterPartingSavedData();
        data.unread = tag;
        return data;
    }

    private void ensureLoaded(ServerLevel level) {
        if (unread == null) {
            return;
        }
        CompoundTag tag = unread;
        unread = null;
        HolderGetter<Block> lookup = level.holderLookup(Registries.BLOCK);
        ListTag leaseList = tag.getList("leases", Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < leaseList.size(); cursor++) {
            CompoundTag leaseTag = leaseList.getCompound(cursor);
            BlockPos cell = new BlockPos(leaseTag.getInt("blockX"), leaseTag.getInt("blockY"), leaseTag.getInt("blockZ"));
            HeldCell held = cells.computeIfAbsent(cell.asLong(), ignored -> new HeldCell(cell));
            if (leaseTag.contains("original")) {
                held.originalState = NbtUtils.readBlockState(lookup, leaseTag.getCompound("original"));
                held.sourceWater = leaseTag.getBoolean("sourceWater");
                held.controlled = leaseTag.getBoolean("controlled");
            }
            UUID operationKey = leaseTag.getUUID("operation");
            if (!held.holders.contains(operationKey)) {
                held.holders.add(operationKey);
            }
        }
        ListTag deadlineList = tag.getList("deadlines", Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < deadlineList.size(); cursor++) {
            CompoundTag deadlineTag = deadlineList.getCompound(cursor);
            deadlines.put(deadlineTag.getUUID("operation"), deadlineTag.getLong("expireGameTime"));
        }
        ListTag pendingList = tag.getList("pending", Tag.TAG_COMPOUND);
        for (int cursor = 0; cursor < pendingList.size(); cursor++) {
            CompoundTag pendingTag = pendingList.getCompound(cursor);
            pending.add(new PendingColumn(
                    pendingTag.getUUID("operation"),
                    PartingGeometrySnapshot.load(pendingTag.getCompound("snapshot")),
                    pendingTag.getInt("blockX"),
                    pendingTag.getInt("blockZ")));
        }
    }

    boolean blocks(ServerLevel level, BlockPos cell) {
        HeldCell held = cells.get(cell.asLong());
        if (held == null || !held.controlled || held.holders.isEmpty()) {
            return false;
        }
        return level.getBlockState(cell).isAir();
    }

    void adopt(UUID operationKey, long expireGameTime, List<PartingCellLease> leases, List<PendingColumn> columns) {
        deadlines.put(operationKey, expireGameTime);
        for (PartingCellLease lease : leases) {
            HeldCell held = cells.computeIfAbsent(lease.cell().asLong(), ignored -> new HeldCell(lease.cell()));
            if (held.holders.isEmpty()) {
                held.originalState = lease.originalState();
                held.sourceWater = lease.sourceKind() == PartingCellLease.SourceKind.SOURCE;
                held.controlled = true;
            }
            if (!held.holders.contains(operationKey)) {
                held.holders.add(operationKey);
            }
        }
        pending.addAll(columns);
        setDirty();
    }

    List<PendingColumn> claimPending(ChunkPos chunk) {
        List<PendingColumn> claimed = new ArrayList<>();
        Iterator<PendingColumn> iterator = pending.iterator();
        while (iterator.hasNext()) {
            PendingColumn column = iterator.next();
            if ((column.blockX >> 4) == chunk.x && (column.blockZ >> 4) == chunk.z) {
                iterator.remove();
                claimed.add(column);
            }
        }
        if (!claimed.isEmpty()) {
            setDirty();
        }
        return claimed;
    }

    void restorePending(List<PendingColumn> columns) {
        if (columns.isEmpty()) {
            return;
        }
        pending.addAll(columns);
        setDirty();
    }

    boolean hasDeadline(UUID operationKey) {
        return deadlines.containsKey(operationKey);
    }

    long deadline(UUID operationKey) {
        Long expireGameTime = deadlines.get(operationKey);
        return expireGameTime == null ? -1L : expireGameTime;
    }

    void observeAndExpire(ServerLevel level) {
        long gameTime = level.getGameTime();
        boolean changed = false;
        Iterator<Map.Entry<Long, HeldCell>> cellIterator = cells.entrySet().iterator();
        while (cellIterator.hasNext()) {
            HeldCell held = cellIterator.next().getValue();
            BlockState current = level.getBlockState(held.cell);
            if (held.controlled && !current.isAir()) {
                held.controlled = false;
                changed = true;
            }
            Iterator<UUID> holders = held.holders.iterator();
            while (holders.hasNext()) {
                UUID operationKey = holders.next();
                Long expireGameTime = deadlines.get(operationKey);
                if (expireGameTime != null && gameTime >= expireGameTime) {
                    holders.remove();
                    changed = true;
                }
            }
            if (!held.holders.isEmpty()) {
                continue;
            }
            restore(level, held, current);
            cellIterator.remove();
            changed = true;
        }
        Iterator<Map.Entry<UUID, Long>> deadlineIterator = deadlines.entrySet().iterator();
        while (deadlineIterator.hasNext()) {
            Map.Entry<UUID, Long> entry = deadlineIterator.next();
            if (gameTime >= entry.getValue() && !stillHeld(entry.getKey())) {
                deadlineIterator.remove();
                pending.removeIf(column -> column.operationKey.equals(entry.getKey()));
                changed = true;
            }
        }
        if (changed) {
            setDirty();
        }
    }

    private boolean stillHeld(UUID operationKey) {
        for (HeldCell held : cells.values()) {
            if (held.holders.contains(operationKey)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 推荐规则：最后一个租约到期才恢复。水源写回水源；流水不逐格铺回，交给原版重算。
     * 玩家已经改掉的格子保持现状。
     */
    private static void restore(ServerLevel level, HeldCell held, BlockState current) {
        if (!held.controlled || !current.isAir()) {
            return;
        }
        held.controlled = false;
        if (held.sourceWater) {
            level.setBlock(held.cell, Fluids.WATER.getSource(false).createLegacyBlock(), 3);
            return;
        }
        for (Direction facing : Direction.values()) {
            BlockPos neighbor = held.cell.relative(facing);
            FluidState neighborFluid = level.getFluidState(neighbor);
            if (neighborFluid.getType().isSame(Fluids.WATER)) {
                level.scheduleTick(neighbor, neighborFluid.getType(), 1);
            }
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        if (unread != null) {
            return unread.copy();
        }
        ListTag leaseList = new ListTag();
        for (HeldCell held : cells.values()) {
            for (UUID operationKey : held.holders) {
                CompoundTag leaseTag = new CompoundTag();
                leaseTag.putInt("blockX", held.cell.getX());
                leaseTag.putInt("blockY", held.cell.getY());
                leaseTag.putInt("blockZ", held.cell.getZ());
                leaseTag.putUUID("operation", operationKey);
                leaseTag.putBoolean("sourceWater", held.sourceWater);
                leaseTag.putBoolean("controlled", held.controlled);
                if (held.originalState != null) {
                    leaseTag.put("original", NbtUtils.writeBlockState(held.originalState));
                }
                leaseList.add(leaseTag);
            }
        }
        tag.put("leases", leaseList);
        ListTag deadlineList = new ListTag();
        for (Map.Entry<UUID, Long> entry : deadlines.entrySet()) {
            CompoundTag deadlineTag = new CompoundTag();
            deadlineTag.putUUID("operation", entry.getKey());
            deadlineTag.putLong("expireGameTime", entry.getValue());
            deadlineList.add(deadlineTag);
        }
        tag.put("deadlines", deadlineList);
        ListTag pendingList = new ListTag();
        for (PendingColumn column : pending) {
            CompoundTag pendingTag = new CompoundTag();
            pendingTag.putUUID("operation", column.operationKey);
            pendingTag.put("snapshot", column.snapshot.save());
            pendingTag.putInt("blockX", column.blockX);
            pendingTag.putInt("blockZ", column.blockZ);
            pendingList.add(pendingTag);
        }
        tag.put("pending", pendingList);
        return tag;
    }

    static final class PendingColumn {
        final UUID operationKey;
        final PartingGeometrySnapshot snapshot;
        final int blockX;
        final int blockZ;

        PendingColumn(UUID operationKey, PartingGeometrySnapshot snapshot, int blockX, int blockZ) {
            this.operationKey = operationKey;
            this.snapshot = snapshot;
            this.blockX = blockX;
            this.blockZ = blockZ;
        }
    }

    private static final class HeldCell {
        final BlockPos cell;
        BlockState originalState = Blocks.AIR.defaultBlockState();
        boolean sourceWater;
        boolean controlled = true;
        final List<UUID> holders = new ArrayList<>();

        HeldCell(BlockPos cell) {
            this.cell = cell.immutable();
        }
    }
}

package com.bslx.XueXiMod.shu.technique.release.domain.motion;

import com.bslx.XueXiMod.block.StillDomainFluids;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.TickTask;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.LiquidBlockContainer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 静水租约服务。按领域实例保存可恢复格子，空气和原版水会冲毁的附着物可放入，玩家改过的格子不回填。
 * 球形场地和护体都不改世界方块。
 */
public final class StillWaterLease {
    private static final String STORAGE = "xueximod_still_domain_water";

    private StillWaterLease() {
    }

    /** 为声明了静水的领域占用盒内格子。未加载格子只记账，不符合条件的方块不替换。 */
    public static void fill(DomainInstance domain) {
        if (domain == null || domain.definition() == null || !domain.definition().placesStillWater()) {
            return;
        }
        ServerLevel level = domain.hostLevel();
        if (level == null) {
            return;
        }
        LeaseData data = LeaseData.get(level);
        AABB bounds = domain.geometry().bounds();
        int firstX = (int) Math.floor(bounds.minX);
        int lastX = (int) Math.ceil(bounds.maxX) - 1;
        int firstY = (int) Math.floor(bounds.minY);
        int lastY = (int) Math.ceil(bounds.maxY) - 1;
        int firstZ = (int) Math.floor(bounds.minZ);
        int lastZ = (int) Math.ceil(bounds.maxZ) - 1;
        for (int blockX = firstX; blockX <= lastX; blockX++) {
            for (int blockY = firstY; blockY <= lastY; blockY++) {
                for (int blockZ = firstZ; blockZ <= lastZ; blockZ++) {
                    if (!contained(bounds, blockX, blockY, blockZ)) {
                        continue;
                    }
                    BlockPos cell = new BlockPos(blockX, blockY, blockZ);
                    if (!level.isLoaded(cell)) {
                        data.hold(domain.instanceId(), cell, false, false);
                        continue;
                    }
                    placeIfAir(level, data, domain.instanceId(), cell);
                }
            }
        }
    }

    /** 释放这个领域的静水。其他持有者仍在的格子保留，已卸载格子延后清理。 */
    public static void release(DomainInstance domain) {
        if (domain == null || domain.hostLevel() == null || domain.definition() == null
                || !domain.definition().placesStillWater()) {
            return;
        }
        LeaseData.get(domain.hostLevel()).releaseOwner(domain.hostLevel(), domain.instanceId());
    }

    /** 判断格子是否仍由活动静水领域持有。非服务端、空格或待清理都返回假。 */
    public static boolean liveAt(Level level, BlockPos cell) {
        if (!(level instanceof ServerLevel server) || cell == null) {
            return false;
        }
        return LeaseData.get(server).live(cell);
    }

    /** 区块加载时对账静水。空世界或空区块直接返回，实际改块延后到服务器任务。 */
    public static void onChunkLoad(ServerLevel level, ChunkAccess chunk) {
        if (level == null || chunk == null) {
            return;
        }
        LeaseData.get(level).reconcile(level, chunk);
    }

    private static void placeIfAir(ServerLevel level, LeaseData data, UUID ownerId, BlockPos cell) {
        BlockState state = level.getBlockState(cell);
        if (state.getBlock() == StillDomainFluids.BLOCK.get()) {
            data.hold(ownerId, cell, true, false);
            return;
        }
        boolean openAir = state.isAir() && level.getBlockEntity(cell) == null;
        if (!openAir && !washesAttachment(level, cell, state)) {
            return;
        }
        if (!openAir) {
            Block.dropResources(state, level, cell, level.getBlockEntity(cell));
        }
        level.setBlock(cell, StillDomainFluids.BLOCK.get().defaultBlockState(), 3);
        level.scheduleTick(cell, StillDomainFluids.SOURCE.get(), StillDomainFluids.SOURCE.get().getTickDelay(level));
        data.hold(ownerId, cell, true, false);
    }

    /** 与 1.20.1 {@code FlowingFluid.canHoldFluid} 的破坏集合一致，不含水方块和容器。 */
    private static boolean washesAttachment(BlockGetter reader, BlockPos cell, BlockState state) {
        if (state.isAir() || state.getBlock() == StillDomainFluids.BLOCK.get() || !state.getFluidState().isEmpty()) {
            return false;
        }
        if (state.hasBlockEntity() || reader.getBlockEntity(cell) != null) {
            return false;
        }
        if (state.getBlock() instanceof LiquidBlockContainer || state.getBlock() instanceof DoorBlock) {
            return false;
        }
        if (state.is(BlockTags.SIGNS) || state.is(Blocks.LADDER) || state.is(Blocks.SUGAR_CANE)
                || state.is(Blocks.BUBBLE_COLUMN) || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL)
                || state.is(Blocks.END_GATEWAY) || state.is(Blocks.STRUCTURE_VOID)) {
            return false;
        }
        return !state.blocksMotion();
    }

    private static boolean contained(AABB bounds, int blockX, int blockY, int blockZ) {
        return blockX >= bounds.minX - 1.0E-6D && blockX + 1.0D <= bounds.maxX + 1.0E-6D
                && blockY >= bounds.minY - 1.0E-6D && blockY + 1.0D <= bounds.maxY + 1.0E-6D
                && blockZ >= bounds.minZ - 1.0E-6D && blockZ + 1.0D <= bounds.maxZ + 1.0E-6D;
    }

    static final class LeaseData extends SavedData {
        private final Map<Long, Held> cells = new HashMap<>();

        static LeaseData get(ServerLevel level) {
            return level.getDataStorage().computeIfAbsent(LeaseData::load, LeaseData::fresh, STORAGE);
        }

        static LeaseData fresh() {
            return new LeaseData();
        }

        static LeaseData load(CompoundTag tag) {
            LeaseData data = new LeaseData();
            ListTag listed = tag.getList("cells", Tag.TAG_COMPOUND);
            for (int cursor = 0; cursor < listed.size(); cursor++) {
                CompoundTag cellTag = listed.getCompound(cursor);
                Held held = new Held(cellTag.getInt("blockX"), cellTag.getInt("blockY"), cellTag.getInt("blockZ"));
                held.placed = cellTag.getBoolean("placed");
                held.pendingClear = cellTag.getBoolean("pendingClear");
                ListTag owners = cellTag.getList("owners", Tag.TAG_COMPOUND);
                for (int ownerCursor = 0; ownerCursor < owners.size(); ownerCursor++) {
                    CompoundTag ownerTag = owners.getCompound(ownerCursor);
                    if (ownerTag.hasUUID("owner")) {
                        held.owners.add(ownerTag.getUUID("owner"));
                    }
                }
                data.cells.put(held.key(), held);
            }
            return data;
        }

        @Override
        public CompoundTag save(CompoundTag tag) {
            ListTag listed = new ListTag();
            for (Held held : cells.values()) {
                CompoundTag cellTag = new CompoundTag();
                cellTag.putInt("blockX", held.blockX);
                cellTag.putInt("blockY", held.blockY);
                cellTag.putInt("blockZ", held.blockZ);
                cellTag.putBoolean("placed", held.placed);
                cellTag.putBoolean("pendingClear", held.pendingClear);
                ListTag owners = new ListTag();
                for (UUID ownerId : held.owners) {
                    CompoundTag ownerTag = new CompoundTag();
                    ownerTag.putUUID("owner", ownerId);
                    owners.add(ownerTag);
                }
                cellTag.put("owners", owners);
                listed.add(cellTag);
            }
            tag.put("cells", listed);
            return tag;
        }

        void hold(UUID ownerId, BlockPos cell, boolean placedNow, boolean clearNow) {
            Held held = cells.computeIfAbsent(cell.asLong(), ignored -> new Held(cell.getX(), cell.getY(), cell.getZ()));
            if (placedNow) {
                held.placed = true;
                held.pendingClear = false;
            }
            if (clearNow) {
                held.pendingClear = true;
                held.owners.clear();
            } else if (ownerId != null) {
                held.owners.add(ownerId);
            }
            setDirty();
        }

        boolean live(BlockPos cell) {
            Held held = cells.get(cell.asLong());
            if (held == null || held.pendingClear) {
                return false;
            }
            for (UUID ownerId : held.owners) {
                DomainInstance domain = DomainService.find(ownerId);
                if (domain != null && domain.active() && domain.definition().placesStillWater()) {
                    return true;
                }
            }
            return false;
        }

        void releaseOwner(ServerLevel level, UUID ownerId) {
            Iterator<Held> iterator = cells.values().iterator();
            while (iterator.hasNext()) {
                Held held = iterator.next();
                if (!held.owners.remove(ownerId)) {
                    continue;
                }
                if (!held.owners.isEmpty()) {
                    setDirty();
                    continue;
                }
                BlockPos cell = held.cell();
                if (!level.isLoaded(cell)) {
                    held.pendingClear = true;
                    setDirty();
                    continue;
                }
                clearIfOurs(level, cell);
                iterator.remove();
                setDirty();
            }
        }

        void reconcile(ServerLevel level, ChunkAccess chunk) {
            ChunkPos loadedChunk = chunk.getPos();
            List<LoadedEdit> edits = new ArrayList<>();
            Iterator<Held> iterator = cells.values().iterator();
            while (iterator.hasNext()) {
                Held held = iterator.next();
                if (held.blockX < loadedChunk.getMinBlockX() || held.blockX > loadedChunk.getMaxBlockX()
                        || held.blockZ < loadedChunk.getMinBlockZ() || held.blockZ > loadedChunk.getMaxBlockZ()) {
                    continue;
                }
                BlockPos cell = held.cell();
                boolean ownerLives = false;
                for (UUID ownerId : held.owners) {
                    DomainInstance domain = DomainService.find(ownerId);
                    if (domain != null && domain.active()) {
                        ownerLives = true;
                        break;
                    }
                }
                BlockState state = chunk.getBlockState(cell);
                boolean ours = state.getBlock() == StillDomainFluids.BLOCK.get();
                if (!ownerLives || held.pendingClear) {
                    if (ours) {
                        edits.add(new LoadedEdit(cell, null, true));
                    }
                    iterator.remove();
                    setDirty();
                    continue;
                }
                if (ours) {
                    held.placed = true;
                    setDirty();
                    continue;
                }
                boolean openAir = state.isAir() && chunk.getBlockEntity(cell) == null;
                if (!held.placed && (openAir || washesAttachment(chunk, cell, state))) {
                    edits.add(new LoadedEdit(cell, firstOwner(held), false));
                    continue;
                }
                iterator.remove();
                setDirty();
            }
            sweepOrphans(level, chunk, edits);
            flushLoadedEdits(level, edits);
        }

        private void sweepOrphans(ServerLevel level, ChunkAccess chunk, List<LoadedEdit> edits) {
            ChunkPos loadedChunk = chunk.getPos();
            int low = level.getMinBuildHeight();
            int high = level.getMaxBuildHeight();
            for (int blockX = loadedChunk.getMinBlockX(); blockX <= loadedChunk.getMaxBlockX(); blockX++) {
                for (int blockZ = loadedChunk.getMinBlockZ(); blockZ <= loadedChunk.getMaxBlockZ(); blockZ++) {
                    for (int blockY = low; blockY < high; blockY++) {
                        BlockPos cell = new BlockPos(blockX, blockY, blockZ);
                        BlockState state = chunk.getBlockState(cell);
                        if (state.getBlock() != StillDomainFluids.BLOCK.get()) {
                            continue;
                        }
                        if (!live(cell)) {
                            edits.add(new LoadedEdit(cell, null, true));
                            cells.remove(cell.asLong());
                            setDirty();
                        }
                    }
                }
            }
        }

        private void flushLoadedEdits(ServerLevel level, List<LoadedEdit> edits) {
            if (edits.isEmpty() || level.getServer() == null) {
                return;
            }
            List<LoadedEdit> queued = List.copyOf(edits);
            level.getServer().tell(new TickTask(level.getServer().getTickCount(), () -> {
                for (LoadedEdit edit : queued) {
                    if (!level.isLoaded(edit.cell)) {
                        continue;
                    }
                    if (edit.clearing) {
                        clearIfOurs(level, edit.cell);
                        continue;
                    }
                    placeIfAir(level, this, edit.ownerId, edit.cell);
                }
            }));
        }

        private static UUID firstOwner(Held held) {
            for (UUID ownerId : held.owners) {
                return ownerId;
            }
            return null;
        }

        private static void clearIfOurs(ServerLevel level, BlockPos cell) {
            if (level.getBlockState(cell).getBlock() == StillDomainFluids.BLOCK.get()) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            }
        }
    }

    private static final class LoadedEdit {
        final BlockPos cell;
        final UUID ownerId;
        final boolean clearing;

        LoadedEdit(BlockPos cell, UUID ownerId, boolean clearing) {
            this.cell = cell;
            this.ownerId = ownerId;
            this.clearing = clearing;
        }
    }

    static final class Held {
        final int blockX;
        final int blockY;
        final int blockZ;
        boolean placed;
        boolean pendingClear;
        final HashSet<UUID> owners = new HashSet<>();

        Held(int blockX, int blockY, int blockZ) {
            this.blockX = blockX;
            this.blockY = blockY;
            this.blockZ = blockZ;
        }

        long key() {
            return BlockPos.asLong(blockX, blockY, blockZ);
        }

        BlockPos cell() {
            return new BlockPos(blockX, blockY, blockZ);
        }
    }
}

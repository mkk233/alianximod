package com.bslx.XueXiMod.npc.daozhang;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.CompoundContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.ClipContext;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 显式绑定的保管箱。未写标记的箱子永远不是道长私产。 */
public final class DaozhangChests {
    public static final String KEY = "xueximod:daozhang_guard";
    public static final int VERSION = 1;
    private static final long CANDIDATE_TICKS = 40L;
    private static final double WITNESS_DISTANCE = 16.0D;
    private static final Map<UUID, Candidate> CANDIDATES = new ConcurrentHashMap<>();

    private DaozhangChests() {
    }

    public static boolean bind(BlockEntity blockEntity, UUID owner) {
        if (blockEntity == null || owner == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) {
            return false;
        }
        CompoundTag tag = new CompoundTag();
        tag.putUUID("Owner", owner);
        tag.putInt("Version", VERSION);
        blockEntity.getPersistentData().put(KEY, tag);
        blockEntity.setChanged();
        return true;
    }

    public static boolean unbind(BlockEntity blockEntity) {
        if (blockEntity == null || !blockEntity.getPersistentData().contains(KEY)) {
            return false;
        }
        blockEntity.getPersistentData().remove(KEY);
        blockEntity.setChanged();
        return true;
    }

    public static @Nullable UUID owner(@Nullable BlockEntity blockEntity) {
        if (blockEntity == null) {
            return null;
        }
        CompoundTag data = blockEntity.getPersistentData();
        if (!data.contains(KEY, Tag.TAG_COMPOUND)) {
            return null;
        }
        CompoundTag tag = data.getCompound(KEY);
        if (tag.getInt("Version") != VERSION || !tag.hasUUID("Owner")) {
            return null;
        }
        return tag.getUUID("Owner");
    }

    public static void noteClick(ServerPlayer player, BlockPos pos) {
        if (player == null || pos == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        List<BlockPos> halves = halves(level, pos);
        if (halves.isEmpty() || owners(level, halves).isEmpty()) {
            return;
        }
        CANDIDATES.put(player.getUUID(), new Candidate(
                level.dimension(), List.copyOf(halves), level.getGameTime() + CANDIDATE_TICKS));
    }

    public static void confirmOpen(ServerPlayer player, Container opened) {
        if (player == null || opened == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Candidate candidate = CANDIDATES.get(player.getUUID());
        if (candidate == null || !candidate.dimension.equals(level.dimension())
                || candidate.expiresAt < level.getGameTime()) {
            CANDIDATES.remove(player.getUUID());
            return;
        }
        List<BlockPos> matched = new ArrayList<>();
        for (BlockPos pos : candidate.positions) {
            BlockEntity blockEntity = level.getBlockEntity(pos);
            if (blockEntity instanceof Container container && contains(opened, container)) {
                matched.add(pos);
            }
        }
        CANDIDATES.remove(player.getUUID());
        if (matched.isEmpty()) {
            return;
        }
        for (UUID owner : owners(level, matched)) {
            witness(level, player, owner, matched);
        }
    }

    public static void clear(UUID playerId) {
        if (playerId != null) {
            CANDIDATES.remove(playerId);
        }
    }

    public static void expire(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        Candidate candidate = CANDIDATES.get(player.getUUID());
        if (candidate != null && candidate.expiresAt < level.getGameTime()) {
            CANDIDATES.remove(player.getUUID());
        }
    }

    private static void witness(ServerLevel level, ServerPlayer player, UUID owner, List<BlockPos> chests) {
        Entity entity = level.getEntity(owner);
        if (!(entity instanceof DaozhangEntity daozhang) || !daozhang.isAlive() || daozhang.isRemoved()) {
            return;
        }
        if (player.distanceTo(daozhang) > WITNESS_DISTANCE || !daozhang.hasLineOfSight(player)) {
            return;
        }
        if (daozhang.relation(player.getUUID()) != DaozhangRelation.NEUTRAL) {
            return;
        }
        for (BlockPos pos : chests) {
            if (owner.equals(owner(level.getBlockEntity(pos))) && visible(level, daozhang, pos)) {
                daozhang.becomeHostile(player.getUUID());
                return;
            }
        }
    }

    private static boolean visible(ServerLevel level, DaozhangEntity daozhang, BlockPos pos) {
        Vec3 from = daozhang.getEyePosition();
        Vec3 to = Vec3.atCenterOf(pos);
        BlockHitResult hit = level.clip(new ClipContext(
                from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, daozhang));
        return hit.getType() == HitResult.Type.MISS || hit.getBlockPos().equals(pos);
    }

    private static boolean contains(Container opened, Container chest) {
        if (opened == chest) {
            return true;
        }
        return opened instanceof CompoundContainer compound && compound.contains(chest);
    }

    private static Set<UUID> owners(Level level, List<BlockPos> positions) {
        Set<UUID> owners = new LinkedHashSet<>();
        for (BlockPos pos : positions) {
            UUID owner = owner(level.getBlockEntity(pos));
            if (owner != null) {
                owners.add(owner);
            }
        }
        return owners;
    }

    private static List<BlockPos> halves(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof ChestBlock) || !(level.getBlockEntity(pos) instanceof Container)) {
            return List.of();
        }
        List<BlockPos> positions = new ArrayList<>();
        positions.add(pos.immutable());
        if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE) {
            positions.add(pos.relative(ChestBlock.getConnectedDirection(state)).immutable());
        }
        return positions;
    }

    private record Candidate(net.minecraft.resources.ResourceKey<Level> dimension, List<BlockPos> positions, long expiresAt) {
    }
}

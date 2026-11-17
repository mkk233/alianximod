package com.bslx.XueXiMod.terrain;

import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.shu.technique.enchant.resource.BreakGroundShape;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.GameMasterBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.BlockSnapshot;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.level.BlockEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * 本批地形入口。崩地只读 {@link BreakGroundShape#offsets()}，不复制坑形。
 * 不读取施法帧，不造成生物伤害，不调用世界爆炸。
 * 破坏和安放分开：开关只挡住破坏已有块。
 * 放置按 Forge 1.20.1 的顺序先记快照、再改块、再发 {@code EntityPlaceEvent}，取消则还原。
 * 成功与否看事件没有取消且方块确实变成陨石，不把 setBlock 的返回值当成权限通过。
 */
public final class TerrainMutationService {
    public static final int PLACE_VERTICAL_RADIUS = 2;
    private static final int DESTROY_RECURSION = 512;

    private TerrainMutationService() {
    }

    public enum BreakGroundResult {
        INVALID_CALL,
        NOT_REQUESTED,
        ALREADY_DONE,
        OWNER_UNAVAILABLE,
        OWNER_RESTRICTED,
        SWITCH_CLOSED,
        EXECUTED
    }

    public enum PlaceResult {
        INVALID_CALL,
        OWNER_UNAVAILABLE,
        OWNER_RESTRICTED,
        NO_SITE,
        PLACED
    }

    public record PlaceOutcome(PlaceResult placeResult, BlockPos placedCell) {
    }

    /**
     * 一次崩地。主人不在、模式不允许或开关关闭时不调用 {@code markBeforeBlocks}，残骸可以以后重试。
     * 开关开启且主人可操作时，先记一次标记，再按 {@link BreakGroundShape#offsets()} 改块。
     */
    public static BreakGroundResult breakExistingOnce(ServerLevel level, UUID ownerIdentifier, BlockPos impact,
                                                       boolean allowOnce, boolean alreadyExecuted,
                                                       Runnable markBeforeBlocks) {
        if (level == null || ownerIdentifier == null || impact == null || markBeforeBlocks == null) {
            return BreakGroundResult.INVALID_CALL;
        }
        if (!allowOnce) {
            return BreakGroundResult.NOT_REQUESTED;
        }
        if (alreadyExecuted) {
            return BreakGroundResult.ALREADY_DONE;
        }
        ServerPlayer owner = resolveOwner(level, ownerIdentifier);
        if (owner == null) {
            return BreakGroundResult.OWNER_UNAVAILABLE;
        }
        if (!ownerCanMutate(owner)) {
            return BreakGroundResult.OWNER_RESTRICTED;
        }
        if (!BatchTerrainBreakSwitch.allowsBreakingExisting()) {
            return BreakGroundResult.SWITCH_CLOSED;
        }
        markBeforeBlocks.run();
        for (BreakGroundShape.BreakOffset offset : BreakGroundShape.offsets()) {
            BlockPos cell = impact.offset(offset.eastSteps(), -offset.downSteps(), offset.southSteps());
            breakOne(level, owner, cell);
        }
        return BreakGroundResult.EXECUTED;
    }

    /**
     * 在撞击块上方优先，再在水平半径 2、上下 2 里找完整空气。不覆盖已有方块，也不读破坏开关。
     */
    public static PlaceOutcome placeMeteorite(ServerLevel level, UUID ownerIdentifier, BlockPos impact) {
        if (level == null || ownerIdentifier == null || impact == null || !ModBlocks.METEORITE.isPresent()) {
            return new PlaceOutcome(PlaceResult.INVALID_CALL, null);
        }
        ServerPlayer owner = resolveOwner(level, ownerIdentifier);
        if (owner == null) {
            return new PlaceOutcome(PlaceResult.OWNER_UNAVAILABLE, null);
        }
        if (!ownerCanMutate(owner)) {
            return new PlaceOutcome(PlaceResult.OWNER_RESTRICTED, null);
        }
        BlockPos preferred = impact.above();
        if (tryPlace(level, owner, preferred)) {
            return new PlaceOutcome(PlaceResult.PLACED, preferred.immutable());
        }
        ArrayList<BlockPos> candidates = placementCandidates(impact);
        candidates.sort((left, right) -> compareSpan(left, right, impact));
        for (BlockPos candidate : candidates) {
            if (candidate.equals(preferred)) {
                continue;
            }
            if (tryPlace(level, owner, candidate)) {
                return new PlaceOutcome(PlaceResult.PLACED, candidate.immutable());
            }
        }
        return new PlaceOutcome(PlaceResult.NO_SITE, null);
    }

    private static void breakOne(ServerLevel level, ServerPlayer owner, BlockPos cell) {
        if (!cellIsReachable(level, cell) || !ownerMayAffect(level, owner, cell)) {
            return;
        }
        BlockState state = level.getBlockState(cell);
        if (skipsBreak(level, cell, state)) {
            return;
        }
        BlockEvent.BreakEvent breakEvent = new BlockEvent.BreakEvent(level, cell, state, owner);
        breakEvent.setExpToDrop(0);
        if (MinecraftForge.EVENT_BUS.post(breakEvent)) {
            return;
        }
        BlockState current = level.getBlockState(cell);
        if (current.isAir() || current.getBlock() != state.getBlock()) {
            return;
        }
        current.getBlock().playerWillDestroy(level, cell, current, owner);
        BlockState afterWill = level.getBlockState(cell);
        if (afterWill.isAir()) {
            return;
        }
        boolean dropWithoutTool = !afterWill.requiresCorrectToolForDrops();
        level.destroyBlock(cell, dropWithoutTool, owner, DESTROY_RECURSION);
    }

    private static boolean tryPlace(ServerLevel level, ServerPlayer owner, BlockPos cell) {
        if (!cellIsReachable(level, cell) || !ownerMayAffect(level, owner, cell)) {
            return false;
        }
        BlockState current = level.getBlockState(cell);
        if (!current.isAir() || !current.getFluidState().isEmpty()) {
            return false;
        }
        BlockPos supportCell = cell.below();
        if (!cellIsReachable(level, supportCell)) {
            return false;
        }
        BlockState supportState = level.getBlockState(supportCell);
        if (!supportState.isFaceSturdy(level, supportCell, Direction.UP)) {
            return false;
        }
        AABB occupied = new AABB(cell);
        List<LivingEntity> occupants = level.getEntitiesOfClass(LivingEntity.class, occupied, TerrainMutationService::livingBlocksPlacement);
        if (!occupants.isEmpty()) {
            return false;
        }
        BlockSnapshot snapshot = BlockSnapshot.create(level.dimension(), level, cell);
        BlockState meteoriteState = ModBlocks.METEORITE.get().defaultBlockState();
        if (!level.setBlock(cell, meteoriteState, Block.UPDATE_ALL)) {
            return false;
        }
        BlockEvent.EntityPlaceEvent placeEvent = new BlockEvent.EntityPlaceEvent(snapshot, supportState, owner);
        boolean cancelled = MinecraftForge.EVENT_BUS.post(placeEvent);
        if (cancelled || !level.getBlockState(cell).is(ModBlocks.METEORITE.get())) {
            snapshot.restore(true, false);
            return false;
        }
        return true;
    }

    private static boolean skipsBreak(ServerLevel level, BlockPos cell, BlockState state) {
        if (state.isAir() || state.liquid() || !state.getFluidState().isEmpty()) {
            return true;
        }
        if (state.getDestroySpeed(level, cell) < 0.0F) {
            return true;
        }
        if (state.hasBlockEntity()) {
            return true;
        }
        BlockEntity blockEntity = level.getBlockEntity(cell);
        if (blockEntity != null) {
            return true;
        }
        Block block = state.getBlock();
        if (block instanceof GameMasterBlock) {
            return true;
        }
        if (state.is(BlockTags.PORTALS) || state.is(BlockTags.WITHER_IMMUNE)) {
            return true;
        }
        return block == Blocks.NETHER_PORTAL
                || block == Blocks.END_PORTAL
                || block == Blocks.END_PORTAL_FRAME
                || block == Blocks.END_GATEWAY;
    }

    private static ArrayList<BlockPos> placementCandidates(BlockPos impact) {
        ArrayList<BlockPos> candidates = new ArrayList<>();
        int horizontalLimit = BreakGroundShape.HORIZONTAL_LIMIT;
        for (int eastOffset = -horizontalLimit; eastOffset <= horizontalLimit; eastOffset++) {
            for (int southOffset = -horizontalLimit; southOffset <= horizontalLimit; southOffset++) {
                long horizontalSpan = (long) eastOffset * eastOffset + (long) southOffset * southOffset;
                if (horizontalSpan > (long) horizontalLimit * horizontalLimit) {
                    continue;
                }
                for (int riseOffset = -PLACE_VERTICAL_RADIUS; riseOffset <= PLACE_VERTICAL_RADIUS; riseOffset++) {
                    candidates.add(impact.offset(eastOffset, riseOffset, southOffset));
                }
            }
        }
        return candidates;
    }

    private static int compareSpan(BlockPos left, BlockPos right, BlockPos impact) {
        int spanOrder = Long.compare(spanFrom(left, impact), spanFrom(right, impact));
        if (spanOrder != 0) {
            return spanOrder;
        }
        if (left.getY() != right.getY()) {
            return Integer.compare(left.getY(), right.getY());
        }
        if (left.getX() != right.getX()) {
            return Integer.compare(left.getX(), right.getX());
        }
        return Integer.compare(left.getZ(), right.getZ());
    }

    private static long spanFrom(BlockPos cell, BlockPos impact) {
        long eastDelta = (long) cell.getX() - impact.getX();
        long riseDelta = (long) cell.getY() - impact.getY();
        long southDelta = (long) cell.getZ() - impact.getZ();
        return eastDelta * eastDelta + riseDelta * riseDelta + southDelta * southDelta;
    }

    private static boolean livingBlocksPlacement(LivingEntity living) {
        return living.isAlive() && !living.isSpectator();
    }

    static ServerPlayer resolveOwner(ServerLevel level, UUID ownerIdentifier) {
        if (level.getServer() == null || ownerIdentifier == null) {
            return null;
        }
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerIdentifier);
        if (owner == null || owner instanceof FakePlayer || owner.hasDisconnected() || owner.isRemoved() || !owner.isAlive()) {
            return null;
        }
        return owner;
    }

    private static boolean ownerCanMutate(ServerPlayer owner) {
        GameType mode = owner.gameMode.getGameModeForPlayer();
        return owner.mayBuild() && !owner.isSpectator() && !mode.isBlockPlacingRestricted();
    }

    private static boolean ownerMayAffect(ServerLevel level, ServerPlayer owner, BlockPos cell) {
        GameType mode = owner.gameMode.getGameModeForPlayer();
        return !owner.blockActionRestricted(level, cell, mode) && level.mayInteract(owner, cell);
    }

    private static boolean cellIsReachable(ServerLevel level, BlockPos cell) {
        return level.isInWorldBounds(cell)
                && level.isLoaded(cell)
                && level.getWorldBorder().isWithinBounds(cell);
    }
}

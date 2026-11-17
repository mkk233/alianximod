package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.shu.ShuAttribute;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.level.BlockEvent;

import javax.annotation.Nullable;

/**
 * 既有无属性术原胚转五行原胚的服务端事务。
 * 任一项失败都不改原胚、材料、目标和背包。
 */
public final class EmbryoConversionService {
    private static java.util.UUID lastCommittedPlayerId;
    private static int lastCommittedGameTick = Integer.MIN_VALUE;

    public enum BlockKind {
        WOOD,
        WATER,
        EARTH
    }

    private EmbryoConversionService() {
    }

    public static boolean isBlankEmbryo(ItemStack stack) {
        return !stack.isEmpty()
                && stack.getItem() instanceof ShuEmbryoItem
                && ShuEmbryoItem.getAttribute(stack) == ShuAttribute.NONE;
    }

    public static boolean isMetalPair(ItemStack left, ItemStack right) {
        return left.getCount() >= 1
                && right.getCount() >= 1
                && isBlankEmbryo(left)
                && right.is(Items.GOLD_INGOT);
    }

    public static ItemStack metalResult(ItemStack blankEmbryo) {
        ItemStack product = blankEmbryo.copyWithCount(1);
        ShuEmbryoItem.setAttribute(product, ShuAttribute.METAL);
        return product;
    }

    /** 铁砧在点击当下重查两槽。成功后各减一，产物进真实背包，不扣经验。 */
    public static boolean commitMetal(AnvilMenu menu, ServerPlayer player) {
        if (menu == null || player == null || player.containerMenu != menu || alreadyCommitted(player)) {
            return false;
        }
        ItemStack left = menu.getSlot(AnvilMenu.INPUT_SLOT).getItem();
        ItemStack right = menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).getItem();
        if (!isMetalPair(left, right) || !menu.stillValid(player)) {
            return false;
        }
        ItemStack product = metalResult(left);
        if (!canAccept(player, null, product)) {
            return false;
        }
        left.shrink(1);
        right.shrink(1);
        menu.getSlot(AnvilMenu.INPUT_SLOT).set(left.isEmpty() ? ItemStack.EMPTY : left);
        menu.getSlot(AnvilMenu.ADDITIONAL_SLOT).set(right.isEmpty() ? ItemStack.EMPTY : right);
        if (!player.getInventory().add(product)) {
            throw new IllegalStateException("metal embryo inventory acceptance changed during commit");
        }
        markCommitted(player);
        menu.broadcastChanges();
        return true;
    }

    public static boolean matchesBlock(BlockState state, BlockKind kind) {
        return switch (kind) {
            case WOOD -> state.is(BlockTags.SAPLINGS)
                    || (state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state));
            case WATER -> state.is(Blocks.WATER_CAULDRON)
                    && state.getValue(LayeredCauldronBlock.LEVEL) == LayeredCauldronBlock.MAX_FILL_LEVEL;
            case EARTH -> state.is(Blocks.DIRT);
        };
    }

    /** 木、水、土共用的服务端事务。耕地保留，满水锅只清空。 */
    public static boolean commitBlock(ServerPlayer player, InteractionHand hand,
                                      BlockPos position, Vec3 sample, BlockKind kind) {
        if (player == null || hand == null || position == null || sample == null || kind == null
                || alreadyCommitted(player)) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        ServerLevel level = player.serverLevel();
        if (!isBlankEmbryo(held) || !level.isLoaded(position)) {
            return false;
        }
        BlockState before = level.getBlockState(position);
        if (!matchesBlock(before, kind) || !canAffect(player, position, sample)) {
            return false;
        }
        ItemStack product = attributedCopy(held, attributeFor(kind));
        if (!canAccept(player, held, product)) {
            return false;
        }
        BlockState replacement = kind == BlockKind.WATER
                ? Blocks.CAULDRON.defaultBlockState()
                : Blocks.AIR.defaultBlockState();
        if (!matchesBlock(level.getBlockState(position), kind)
                || !replaceWithoutDrops(player, position, replacement)) {
            return false;
        }
        held.shrink(1);
        if (!player.getInventory().add(product)) {
            throw new IllegalStateException("embryo inventory acceptance changed during block commit");
        }
        markCommitted(player);
        return true;
    }

    /**
     * 包含流体的合法射线。流动岩浆会拦住射线，不会处理它后面的方块。
     */
    @Nullable
    public static BlockHitResult traceIncludingFluid(Player player) {
        if (player == null) {
            return null;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 end = eye.add(player.getViewVector(1.0F).scale(reachDistance(player)));
        BlockHitResult hit = player.level().clip(new ClipContext(
                eye, end, ClipContext.Block.OUTLINE, ClipContext.Fluid.ANY, player));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return null;
        }
        return hit.getLocation().closerThan(eye, reachDistance(player) + 1.0E-4D) ? hit : null;
    }

    /** 只消耗射线直接命中的一个岩浆源。 */
    public static boolean commitLava(ServerPlayer player, InteractionHand hand, BlockHitResult hit) {
        if (player == null || hand == null || hit == null || hit.getType() != HitResult.Type.BLOCK
                || alreadyCommitted(player)) {
            return false;
        }
        ItemStack held = player.getItemInHand(hand);
        BlockPos position = hit.getBlockPos();
        ServerLevel level = player.serverLevel();
        if (!isBlankEmbryo(held) || !level.isLoaded(position)) {
            return false;
        }
        FluidState fluid = level.getFluidState(position);
        boolean sourceLava = fluid.getType() == Fluids.LAVA && fluid.isSource();
        if (!sourceLava || !canAffect(player, position, hit.getLocation())) {
            return false;
        }
        ItemStack product = attributedCopy(held, ShuAttribute.FIRE);
        if (!canAccept(player, held, product)) {
            return false;
        }
        FluidState checked = level.getFluidState(position);
        if (checked.getType() != Fluids.LAVA || !checked.isSource()
                || !replaceWithoutDrops(player, position, Blocks.AIR.defaultBlockState())) {
            return false;
        }
        held.shrink(1);
        if (!player.getInventory().add(product)) {
            throw new IllegalStateException("embryo inventory acceptance changed during lava commit");
        }
        markCommitted(player);
        return true;
    }

    private static ShuAttribute attributeFor(BlockKind kind) {
        return switch (kind) {
            case WOOD -> ShuAttribute.WOOD;
            case WATER -> ShuAttribute.WATER;
            case EARTH -> ShuAttribute.EARTH;
        };
    }

    private static ItemStack attributedCopy(ItemStack blankEmbryo, ShuAttribute attribute) {
        ItemStack product = new ItemStack(blankEmbryo.getItem(), 1);
        CompoundTag tag = blankEmbryo.getTag();
        if (tag != null) {
            product.setTag(tag.copy());
        }
        ShuEmbryoItem.setAttribute(product, attribute);
        return product;
    }

    private static boolean replaceWithoutDrops(ServerPlayer player, BlockPos position, BlockState replacement) {
        ServerLevel level = player.serverLevel();
        if (!level.isLoaded(position)) {
            return false;
        }
        BlockState current = level.getBlockState(position);
        if (current.hasBlockEntity() || level.getBlockEntity(position) != null) {
            return false;
        }
        BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, position, current, player);
        event.setExpToDrop(0);
        if (MinecraftForge.EVENT_BUS.post(event)) {
            return false;
        }
        if (level.getBlockState(position) != current) {
            return false;
        }
        return level.setBlock(position, replacement, Block.UPDATE_ALL);
    }

    private static boolean canAffect(ServerPlayer player, BlockPos position, Vec3 sample) {
        ServerLevel level = player.serverLevel();
        GameType mode = player.gameMode.getGameModeForPlayer();
        if (!player.mayBuild() || player.isSpectator() || mode.isBlockPlacingRestricted()) {
            return false;
        }
        if (!level.isInWorldBounds(position)
                || !level.isLoaded(position)
                || !level.getWorldBorder().isWithinBounds(position)) {
            return false;
        }
        if (player.blockActionRestricted(level, position, mode) || !level.mayInteract(player, position)) {
            return false;
        }
        return canReach(player, position, sample);
    }

    private static boolean canReach(Player player, Vec3 point) {
        return canReach(player, null, point);
    }

    private static boolean canReach(Player player, @Nullable BlockPos allowedBlock, Vec3 point) {
        double allowedDistance = reachDistance(player) + (player instanceof ServerPlayer ? 1.5D : 0.0D);
        if (player.getEyePosition().distanceToSqr(point) >= Mth.square(allowedDistance)) {
            return false;
        }
        BlockHitResult obstruction = player.level().clip(new ClipContext(
                player.getEyePosition(), point,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        return obstruction.getType() == HitResult.Type.MISS
                || (allowedBlock != null && obstruction.getBlockPos().equals(allowedBlock))
                || obstruction.getLocation().closerThan(point, 1.0E-4D);
    }

    private static double reachDistance(Player player) {
        return Math.max(0.0D, player.getBlockReach());
    }

    /**
     * 按原版主背包合并顺序预演。
     * 手持栈只剩一个时，它腾出的格子可以接受产物。
     */
    private static boolean canAccept(ServerPlayer player, @Nullable ItemStack consumedHand, ItemStack product) {
        Inventory inventory = player.getInventory();
        int incoming = product.getCount();
        int selected = inventory.selected;
        if (selected < 0 || selected >= inventory.items.size()) {
            return false;
        }
        boolean releaseSelected = consumedHand == inventory.getSelected() && consumedHand.getCount() == 1;
        int[] remainingSpace = new int[inventory.items.size()];
        for (int slotIndex = 0; slotIndex < remainingSpace.length; slotIndex++) {
            boolean releasedSlot = releaseSelected && slotIndex == selected;
            remainingSpace[slotIndex] = spaceIn(inventory.items.get(slotIndex), product, releasedSlot);
        }
        int selectedAccepted = Math.min(incoming, remainingSpace[selected]);
        incoming -= selectedAccepted;
        remainingSpace[selected] -= selectedAccepted;
        if (incoming == 0) {
            return true;
        }
        incoming -= Math.min(incoming, spaceIn(inventory.offhand.get(0), product, false));
        if (incoming == 0) {
            return true;
        }
        for (int slotIndex = 0; slotIndex < remainingSpace.length && incoming > 0; slotIndex++) {
            int acceptedAmount = Math.min(incoming, remainingSpace[slotIndex]);
            incoming -= acceptedAmount;
        }
        return incoming == 0;
    }

    private static int spaceIn(ItemStack destination, ItemStack incoming, boolean treatAsEmpty) {
        int stackLimit = Math.min(incoming.getMaxStackSize(), 64);
        if (treatAsEmpty || destination.isEmpty()) {
            return stackLimit;
        }
        if (!ItemStack.isSameItemSameTags(destination, incoming)) {
            return 0;
        }
        return Math.max(0, Math.min(stackLimit, destination.getMaxStackSize()) - destination.getCount());
    }

    private static boolean alreadyCommitted(ServerPlayer player) {
        if (player.serverLevel().getServer() == null || !player.getUUID().equals(lastCommittedPlayerId)) {
            return false;
        }
        int currentTick = player.serverLevel().getServer().getTickCount();
        return currentTick == lastCommittedGameTick || currentTick == lastCommittedGameTick + 1;
    }

    private static void markCommitted(ServerPlayer player) {
        if (player.serverLevel().getServer() == null) {
            return;
        }
        lastCommittedPlayerId = player.getUUID();
        lastCommittedGameTick = player.serverLevel().getServer().getTickCount();
    }
}

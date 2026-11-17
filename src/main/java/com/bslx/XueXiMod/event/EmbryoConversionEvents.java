package com.bslx.XueXiMod.event;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.EmbryoConversionService;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 木水火土只在服务端提交。主手处理过的目标不再交给副手。 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID)
public final class EmbryoConversionEvents {
    private EmbryoConversionEvents() {
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockUse(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (player == null || !EmbryoConversionService.isBlankEmbryo(event.getItemStack())) {
            return;
        }
        BlockHitResult trace = EmbryoConversionService.traceIncludingFluid(player);
        if (trace == null) {
            return;
        }
        Level level = player.level();
        if (isSourceLava(level, trace)) {
            event.setUseBlock(PlayerInteractEvent.Result.DENY);
            event.setUseItem(PlayerInteractEvent.Result.DENY);
            return;
        }
        if (!trace.getBlockPos().equals(event.getPos())) {
            return;
        }
        BlockState state = level.getBlockState(trace.getBlockPos());
        EmbryoConversionService.BlockKind kind = blockKind(state);
        if (kind == null) {
            return;
        }
        if (level.isClientSide()) {
            event.setUseBlock(PlayerInteractEvent.Result.DENY);
            event.setUseItem(PlayerInteractEvent.Result.DENY);
            return;
        }
        if (player instanceof ServerPlayer serverPlayer
                && EmbryoConversionService.commitBlock(
                        serverPlayer, event.getHand(), trace.getBlockPos(), eventHit(event), kind)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            event.setUseBlock(PlayerInteractEvent.Result.DENY);
            event.setUseItem(PlayerInteractEvent.Result.DENY);
        }
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onItemUse(PlayerInteractEvent.RightClickItem event) {
        Player player = event.getEntity();
        if (player == null || !EmbryoConversionService.isBlankEmbryo(event.getItemStack())) {
            return;
        }
        BlockHitResult trace = EmbryoConversionService.traceIncludingFluid(player);
        if (trace == null || !isSourceLava(player.level(), trace)) {
            return;
        }
        if (player.level().isClientSide()) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer
                && EmbryoConversionService.commitLava(serverPlayer, event.getHand(), trace)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.CONSUME);
            return;
        }
        if (event.getHand() == InteractionHand.MAIN_HAND) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
        }
    }

    private static Vec3 eventHit(PlayerInteractEvent.RightClickBlock event) {
        HitResult rawHit = event.getHitVec();
        return rawHit instanceof BlockHitResult blockHit ? blockHit.getLocation() : event.getPos().getCenter();
    }

    private static boolean isSourceLava(Level level, BlockHitResult trace) {
        if (trace.getType() != HitResult.Type.BLOCK) {
            return false;
        }
        FluidState fluid = level.getFluidState(trace.getBlockPos());
        return fluid.getType() == Fluids.LAVA && fluid.isSource();
    }

    private static EmbryoConversionService.BlockKind blockKind(BlockState state) {
        for (EmbryoConversionService.BlockKind kind : EmbryoConversionService.BlockKind.values()) {
            if (EmbryoConversionService.matchesBlock(state, kind)) {
                return kind;
            }
        }
        return null;
    }
}

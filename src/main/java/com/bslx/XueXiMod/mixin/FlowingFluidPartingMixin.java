package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.parting.WaterPartingService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 对照本机 1.20.1 映射源码：
 * {@code spreadTo} 是邻居水写入目标格的路径，含水容器的 placeLiquid 也在这个方法里。
 * {@code tick} 会按当时的流体状态原地 setBlock，已排空的格子若还留着流体 tick，会把水写回来。
 * 只拦仍由分水服务控制的空气格，不拦水桶或玩家放置。
 */
@Mixin(FlowingFluid.class)
public abstract class FlowingFluidPartingMixin {
    @Inject(method = "spreadTo", at = @At("HEAD"), cancellable = true)
    private void xuexiHoldPartingSpread(LevelAccessor level, BlockPos cell, BlockState blockState, Direction facing,
                                        FluidState fluidState, CallbackInfo callback) {
        if (fluidState.isEmpty() || !fluidState.getType().isSame(Fluids.WATER)) {
            return;
        }
        if (level instanceof ServerLevel serverLevel && WaterPartingService.blocksIncoming(serverLevel, cell)) {
            callback.cancel();
        }
    }

    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void xuexiHoldPartingTick(Level level, BlockPos cell, FluidState fluidState, CallbackInfo callback) {
        if (!(level instanceof ServerLevel serverLevel) || !fluidState.getType().isSame(Fluids.WATER)) {
            return;
        }
        if (WaterPartingService.blocksIncoming(serverLevel, cell)) {
            callback.cancel();
        }
    }
}

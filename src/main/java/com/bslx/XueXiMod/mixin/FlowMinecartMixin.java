package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** 有流场时这一拍走离轨物理，避免轨道投影清掉上升。没有流场时保持原分支。 */
@Mixin(AbstractMinecart.class)
public abstract class FlowMinecartMixin {
    @Shadow
    protected abstract void moveAlongTrack(BlockPos anchor, BlockState state);

    @Shadow
    protected abstract void comeOffTrack();

    @Redirect(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/vehicle/AbstractMinecart;moveAlongTrack(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void xuexiFlowTrack(AbstractMinecart cart, BlockPos anchor, BlockState state) {
        if (!cart.level().isClientSide() && FlowMotionIntents.hasLiveShift(cart)) {
            this.comeOffTrack();
            return;
        }
        this.moveAlongTrack(anchor, state);
    }
}

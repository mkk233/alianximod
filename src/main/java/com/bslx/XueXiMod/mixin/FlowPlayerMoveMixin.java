package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.network.FlowPlayerBridge;
import com.bslx.XueXiMod.registry.ModMobEffects;
import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import com.bslx.XueXiMod.status.wood.vine.VineMarkRuntime;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.ServerGamePacketListenerImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 玩家模拟不注入流场；回执包抑制纯气泡跳跃，并在有效上升时不累计悬浮踢出。 */
@Mixin(ServerGamePacketListenerImpl.class)
public abstract class FlowPlayerMoveMixin {
    @Shadow
    public ServerPlayer player;
    @Shadow
    private boolean clientIsFloating;
    @Shadow
    private int aboveGroundTickCount;
    @Shadow
    private boolean clientVehicleIsFloating;
    @Shadow
    private int aboveGroundVehicleTickCount;

    @Inject(method = "tick", at = @At("HEAD"))
    private void xuexiFlowClearFloat(CallbackInfo callback) {
        if (this.player == null) {
            return;
        }
        boolean rising = FlowMotionIntents.hasEffectiveRise(this.player)
                || FlowMotionIntents.hasEffectiveRise(this.player.getRootVehicle());
        if (!rising) {
            return;
        }
        this.clientIsFloating = false;
        this.aboveGroundTickCount = 0;
        this.clientVehicleIsFloating = false;
        this.aboveGroundVehicleTickCount = 0;
    }

    @Redirect(method = "tick",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;doTick()V"))
    private void xuexiFlowSimulate(ServerPlayer serverPlayer) {
        FlowMotionIntents.simulating(true);
        try {
            serverPlayer.doTick();
        } finally {
            FlowMotionIntents.simulating(false);
        }
    }

    @Inject(method = "handleMovePlayer", at = @At("HEAD"), cancellable = true)
    private void xuexiFlowRejectPlayer(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (FlowPlayerBridge.insideWrapped() || this.player == null) {
            return;
        }
        if (FlowPlayerBridge.blockVanilla(this.player)) {
            VineMarkRuntime.resetMove(this.player);
            FlowPlayerBridge.rejectVanilla(this.player, "vanilla-player");
            callback.cancel();
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At("HEAD"), cancellable = true)
    private void xuexiFlowRejectVehicle(ServerboundMoveVehiclePacket packet, CallbackInfo callback) {
        if (FlowPlayerBridge.insideWrapped() || this.player == null) {
            return;
        }
        if (FlowPlayerBridge.blockVanilla(this.player)) {
            FlowPlayerBridge.rejectVanilla(this.player, "vanilla-vehicle");
            callback.cancel();
        }
    }

    @Redirect(method = "handleMovePlayer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;jumpFromGround()V"))
    private void xuexiFlowJump(ServerPlayer serverPlayer) {
        if (FlowPlayerBridge.suppressJump() || serverPlayer.hasEffect(ModMobEffects.ROSE_BIND.get())) {
            return;
        }
        serverPlayer.jumpFromGround();
    }

    @Inject(method = "handleMovePlayer", at = @At("RETURN"))
    private void xuexiFlowPlayerFloat(ServerboundMovePlayerPacket packet, CallbackInfo callback) {
        if (callback.isCancelled()) {
            VineMarkRuntime.resetMove(this.player);
        }
        if (FlowPlayerBridge.suppressJump() || FlowMotionIntents.hasEffectiveRise(this.player)) {
            this.clientIsFloating = false;
        }
    }

    @Inject(method = "handleMoveVehicle", at = @At("RETURN"))
    private void xuexiFlowVehicleFloat(ServerboundMoveVehiclePacket packet, CallbackInfo callback) {
        if (this.player != null && FlowMotionIntents.hasEffectiveRise(this.player.getRootVehicle())) {
            this.clientVehicleIsFloating = false;
        }
    }
}

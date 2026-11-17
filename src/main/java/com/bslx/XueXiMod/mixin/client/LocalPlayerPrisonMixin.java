package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bslx.XueXiMod.client.PrisonClientState;

import net.minecraft.client.player.LocalPlayer;

/**
 * 本地预测只对齐被困的本地玩家。朝向包照常发，不改攻击。
 * tick 里乘客输入包在字节码 59 的 NEW 之前清掉位移；步行在 sendPosition()V 开头对齐锚点。
 * 观察的 1.20.1 LocalPlayer.tick：isPassenger 在 23，输入包在 59，载具包在 112，sendPosition 在 131。
 */
@Mixin(LocalPlayer.class)
public class LocalPlayerPrisonMixin {
    @Inject(method = "tick", at = @At(value = "NEW", target = "net/minecraft/network/protocol/game/ServerboundPlayerInputPacket"), require = 1)
    private void xuexiAlignPassenger(CallbackInfo callback) {
        PrisonClientState.holdLocal((LocalPlayer) (Object) this);
    }

    @Inject(method = "sendPosition", at = @At("HEAD"), require = 1)
    private void xuexiAlignWalk(CallbackInfo callback) {
        PrisonClientState.holdLocal((LocalPlayer) (Object) this);
    }

    @Inject(method = "tick", at = @At("RETURN"), require = 1)
    private void xuexiAlignAfterTick(CallbackInfo callback) {
        PrisonClientState.holdLocal((LocalPlayer) (Object) this);
    }
}

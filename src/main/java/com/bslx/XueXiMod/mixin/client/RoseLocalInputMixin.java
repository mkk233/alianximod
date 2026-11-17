package com.bslx.XueXiMod.mixin.client;

import com.bslx.XueXiMod.mixin.RoseLivingInputMixin;
import com.bslx.XueXiMod.registry.ModMobEffects;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** 本地玩家的键盘输入写入实体后、父类计算运动前清掉主动输入。 */
@Mixin(LocalPlayer.class)
public abstract class RoseLocalInputMixin {
    @Inject(method = "aiStep", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/player/AbstractClientPlayer;aiStep()V"))
    private void xuexiRoseBeforeLocalTravel(CallbackInfo callback) {
        LocalPlayer player = (LocalPlayer) (Object) this;
        if (!player.hasEffect(ModMobEffects.ROSE_BIND.get())) {
            return;
        }
        RoseLivingInputMixin input = (RoseLivingInputMixin) (Object) player;
        input.xuexiSetStrafeInput(0.0f);
        input.xuexiSetVerticalInput(0.0f);
        input.xuexiSetForwardInput(0.0f);
        player.setJumping(false);
        player.setSprinting(false);
        if (player.input != null) {
            player.input.forwardImpulse = 0.0f;
            player.input.leftImpulse = 0.0f;
            player.input.jumping = false;
            player.input.up = false;
            player.input.down = false;
            player.input.left = false;
            player.input.right = false;
        }
    }
}

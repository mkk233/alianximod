package com.bslx.XueXiMod.mixin.client;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bslx.XueXiMod.client.action.ActionRenderQueries;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.ThirdPersonPoseWriter;
import com.bslx.XueXiMod.client.action.cooperative.CooperativePoseApplier;

import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/** 原版基础姿态完成之后、外套复制之前写入动作。 */
@Mixin(PlayerModel.class)
public class PlayerModelActionMixin {
    @Inject(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/model/HumanoidModel;setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
                    shift = At.Shift.AFTER),
            require = 1)
    private void xuexiApplyActionPose(
            LivingEntity living,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback) {
        if (!(living instanceof Player player)) {
            return;
        }
        PlayerModel<?> model = (PlayerModel<?>) (Object) this;
        if (!RenderPoseScope.allowsThirdPerson(model, player)) {
            return;
        }
        if (RenderPoseScope.skipsRigidWriter(model, player)) {
            return;
        }
        float partialTick = ActionRenderQueries.partialTick(living, ageInTicks);
        ThirdPersonPoseWriter.applyScoped(model, player, ActionRenderQueries.thirdPersonPose(player, partialTick));
        CooperativePoseApplier.apply(model, player, partialTick);
    }
}

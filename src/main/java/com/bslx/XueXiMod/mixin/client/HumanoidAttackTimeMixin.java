package com.bslx.XueXiMod.mixin.client;

import java.util.UUID;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.bslx.XueXiMod.client.action.ActionClientPlayback;
import com.bslx.XueXiMod.client.action.ActionRenderQueries;
import com.bslx.XueXiMod.client.action.RenderPoseScope;
import com.bslx.XueXiMod.client.action.cooperative.CooperativeActionViews;
import com.bslx.XueXiMod.client.action.cooperative.CooperativePoseApplier;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * 条件替换 HumanoidModel 对 attackTime 的读取。读原值后决定返回 0 或原值，不写字段。
 * 常量池 #273 的 GETFIELD 所有者是 net/minecraft/client/model/HumanoidModel.attackTime:F。
 * setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V 捕获全部实参，require 2，字节码 870 与 897。
 * setupAttackAnimation(Lnet/minecraft/world/entity/LivingEntity;F)V 捕获自己的实参，require 5，字节码 1、23、212、249、322。
 */
@Mixin(HumanoidModel.class)
public class HumanoidAttackTimeMixin {
    @Redirect(
            method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/model/HumanoidModel;attackTime:F"),
            require = 2)
    @SuppressWarnings("unused")
    private float xuexiSetupAttackTime(
            HumanoidModel<?> receiver,
            LivingEntity living,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch) {
        return this.xuexiReadAttackTime(receiver, living, ageInTicks);
    }

    @Redirect(
            method = "setupAttackAnimation(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/model/HumanoidModel;attackTime:F"),
            require = 5)
    private float xuexiAttackAnimationTime(HumanoidModel<?> receiver, LivingEntity living, float ageInTicks) {
        return this.xuexiReadAttackTime(receiver, living, ageInTicks);
    }

    private float xuexiReadAttackTime(HumanoidModel<?> receiver, LivingEntity living, float ageInTicks) {
        if (CooperativeActionViews.holds(living)) {
            return 0.0F;
        }
        float actual = receiver.attackTime;
        if (xuexiNeedsSwingQuery(receiver, living)
                && ActionRenderQueries.suppressVanillaSwing(receiver, living, ageInTicks)) {
            return 0.0F;
        }
        return actual;
    }

    /** 非玩家仍问原查询。当前栈已有这份采样，或地图里还有记录时，不能跳过挥砍语义。 */
    private static boolean xuexiNeedsSwingQuery(HumanoidModel<?> receiver, LivingEntity living) {
        if (!(receiver instanceof PlayerModel) || !(living instanceof Player player)) {
            return true;
        }
        UUID ownerId = player.getUUID();
        if (RenderPoseScope.frozenSample(ownerId) != null) {
            return true;
        }
        return ActionClientPlayback.hasPlaybackRecord(ownerId);
    }

    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("RETURN"))
    private void xuexiApplyCooperativePose(
            LivingEntity living,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch,
            CallbackInfo callback) {
        float partialTick = ageInTicks - living.tickCount;
        if (partialTick < 0.0F || partialTick > 1.0F) {
            partialTick = 0.0F;
        }
        CooperativePoseApplier.apply((HumanoidModel<?>) (Object) this, living, partialTick);
    }
}

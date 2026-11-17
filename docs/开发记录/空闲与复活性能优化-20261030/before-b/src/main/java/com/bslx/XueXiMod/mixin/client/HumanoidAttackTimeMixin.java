package com.bslx.XueXiMod.mixin.client;

import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import com.bslx.XueXiMod.client.action.ActionRenderQueries;

import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;

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
        float actual = receiver.attackTime;
        if (ActionRenderQueries.suppressVanillaSwing(receiver, living, ageInTicks)) {
            return 0.0F;
        }
        return actual;
    }

    @Redirect(
            method = "setupAttackAnimation(Lnet/minecraft/world/entity/LivingEntity;F)V",
            at = @At(
                    value = "FIELD",
                    opcode = Opcodes.GETFIELD,
                    target = "Lnet/minecraft/client/model/HumanoidModel;attackTime:F"),
            require = 5)
    private float xuexiAttackAnimationTime(HumanoidModel<?> receiver, LivingEntity living, float ageInTicks) {
        float actual = receiver.attackTime;
        if (ActionRenderQueries.suppressVanillaSwing(receiver, living, ageInTicks)) {
            return 0.0F;
        }
        return actual;
    }
}

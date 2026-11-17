package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 本地 Forge 1.20.1-47.3.33 的 {@code LivingEntity.actuallyHurt} 顺序是：
 * {@code onLivingHurt}、护甲、{@code getDamageAfterMagicAbsorb}、吸收、{@code onLivingDamage}。
 * 抗性药水在 {@code getDamageAfterMagicAbsorb} 前半段，保护附魔在同一方法后半段。
 * 这里只让炉心玩家的抗性判定失败，保护附魔仍走原计算。不取消抗性效果，也不改全局公式。
 */
@Mixin(LivingEntity.class)
public abstract class FurnaceResistanceMixin {
    @Redirect(
            method = "getDamageAfterMagicAbsorb",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/entity/LivingEntity;hasEffect(Lnet/minecraft/world/effect/MobEffect;)Z"
            )
    )
    private boolean xuexiFurnaceSkipOwnResistance(LivingEntity entity, MobEffect effect) {
        if (effect == MobEffects.DAMAGE_RESISTANCE && FurnaceService.suppressesOwnResistance(entity)) {
            return false;
        }
        return entity.hasEffect(effect);
    }
}

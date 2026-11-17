package com.bslx.XueXiMod.mixin;

import com.bslx.XueXiMod.registry.ModMobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** 凋灵的两参数 addEffect 原版直接返回 false。只有 WETNESS 走 Monster 的父类实现。 */
@Mixin(WitherBoss.class)
public abstract class WetnessWitherMixin extends Monster {
    protected WetnessWitherMixin(EntityType<? extends Monster> entityType, Level level) {
        super(entityType, level);
    }

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At("HEAD"),
            cancellable = true)
    private void xuexiWetnessWitherEffect(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> callback) {
        if (effect != null && effect.getEffect() == ModMobEffects.WETNESS.get()) {
            callback.setReturnValue(super.addEffect(effect, source));
        }
    }
}

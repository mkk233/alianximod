package com.bslx.XueXiMod.mixin;

import net.minecraft.world.effect.MobEffectInstance;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** 炉心要改无限时长、合法档位，并拆掉隐藏效果链。这些字段没有公开写入器。 */
@Mixin(MobEffectInstance.class)
public interface FurnaceEffectAccess {
    @Accessor("duration")
    void xuexiSetDuration(int duration);

    @Accessor("amplifier")
    void xuexiSetAmplifier(int amplifier);

    @Accessor("hiddenEffect")
    void xuexiSetHiddenEffect(MobEffectInstance hidden);
}

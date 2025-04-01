package com.bslx.XueXiMod.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

public class foodItem {
    //.nutrition(2)设置营养
    //.saturationMod(0.6f)饱食度调节器
    public static final FoodProperties food = new FoodProperties.Builder().nutrition(200).fast()
            .saturationMod(0.6f).effect(() ->new MobEffectInstance(MobEffects.HEAL,100000),1).build();
}

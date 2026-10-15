package com.bslx.XueXiMod.item;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

/**
 * 食物属性定义 - shu_food 物品的 FoodProperties 配置
 *
 * 属性：营养值 200、快速食用、饱和度 0.6、附加生命恢复效果（MobEffects.HEAL）
 * 注意：当前为测试用超高属性，正式版本需调整数值
 */
public class FoodItem {
    //.nutrition(2)设置营养
    //.saturationMod(0.6f)饱食度调节器
    public static final FoodProperties food = new FoodProperties.Builder().nutrition(200).fast()
            .saturationMod(0.6f).effect(() ->new MobEffectInstance(MobEffects.HEAL,100000),1).build();
}

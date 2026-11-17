package com.bslx.XueXiMod.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

/**
 * 可燃料物品 - 自定义燃烧时间的 Item 子类
 *
 * 核心职责：
 * - 重写 getBurnTime() 返回构造时指定的燃烧时间
 * - 用于 shu_canzha（术残渣）等可作为熔炉燃料的物品
 *
 * 使用方式：new FuelItem(properties, 4000) 创建 4000 ticks 燃烧时间的燃料
 */
public class FuelItem extends Item {
    private int burnTime=0;

    public FuelItem(Properties pProperties,int buenTime) {
        super(pProperties);
        this.burnTime = buenTime;
    }

    @Override
    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?>recipeType) {
        return this.burnTime;
    }

}

package com.bslx.XueXiMod.item.custom;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import javax.annotation.Nullable;

public class fuelItem extends Item {
    private int burnTime=0;

    public fuelItem(Properties pProperties,int buenTime) {
        super(pProperties);
        this.burnTime = buenTime;
    }

    public int getBurnTime(ItemStack itemStack, @Nullable RecipeType<?>recipeType) {
        return this.burnTime;
    }

}

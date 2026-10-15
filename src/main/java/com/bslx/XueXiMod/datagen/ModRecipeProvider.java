package com.bslx.XueXiMod.datagen;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.*;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;
import net.minecraftforge.common.crafting.conditions.IConditionBuilder;

import java.util.List;
import java.util.function.Consumer;

/**
 * 合成配方数据生成器 - 自动生成模组合成配方 JSON 文件
 *
 * 核心职责：
 * - 定义矿物熔炼/烧炼配方（术矿石 → 术原胚，支持4种矿石变体）
 * - 定义有序合成配方（9个术原胚 → 术方块）
 * - 定义无序分解配方（术方块 → 9个术原胚）
 *
 * 配方类型：熔炼（smelting）、烧炼（blasting）、有序合成（shaped）、无序合成（shapeless）
 * 输出目录：src/generated/resources/
 */
public class ModRecipeProvider extends RecipeProvider implements IConditionBuilder {
    private static final List<ItemLike> shu_SMEllTABLES =List.of(ModItems.shu_raw.get(),
            ModBlocks.ars_ore.get(),ModBlocks.deeplate_ars_ore.get(),ModBlocks.nether_ars_ore.get(),ModBlocks.end_ars_ore.get());

    public ModRecipeProvider(PackOutput pOutput) {
        super(pOutput);
    }

    @Override
    protected void buildRecipes(Consumer<FinishedRecipe> consumer) {
        oreBlasting(consumer,shu_SMEllTABLES,RecipeCategory.MISC,ModItems.shu_embryo.get(),1f,100,"shu");
        oreSmelting(consumer,shu_SMEllTABLES,RecipeCategory.MISC,ModItems.shu_embryo.get(),1f,200,"shu");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC,ModBlocks.shu_block.get())
                .pattern("SSS")
                .pattern("SSS")
                .pattern("SSS")
                .define('S',ModItems.shu_embryo.get())
                .unlockedBy(getHasName(ModItems.shu_embryo.get()),has(ModItems.shu_embryo.get()))
                .save(consumer);

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC,ModItems.shu_embryo.get(),9)
                .requires(ModBlocks.shu_block.get())
                .unlockedBy(getHasName(ModBlocks.shu_block.get()),has(ModBlocks.shu_block.get()))
                .save(consumer);

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.skill_storage.get())
                .pattern("E")
                .pattern("C")
                .define('E', Items.ENDER_EYE)
                .define('C', Items.CHEST)
                .unlockedBy(getHasName(Items.CHEST), has(Items.CHEST))
                .save(consumer, "skill_storage");

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ModItems.basic_staff.get())
                .pattern(" S ")
                .pattern("DW ")
                .pattern(" S ")
                .define('S', Items.STICK)
                .define('W', ModItems.ore_sousuo.get())
                .define('D', Items.DIAMOND)
                .unlockedBy(getHasName(ModItems.ore_sousuo.get()), has(ModItems.ore_sousuo.get()))
                .save(consumer, "basic_staff");
    }
    protected static void oreSmelting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTIme, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.SMELTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTIme, pGroup, "_from_smelting");
    }

    protected static void oreBlasting(Consumer<FinishedRecipe> pFinishedRecipeConsumer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup) {
        oreCooking(pFinishedRecipeConsumer, RecipeSerializer.BLASTING_RECIPE, pIngredients, pCategory, pResult, pExperience, pCookingTime, pGroup, "_from_blasting");
    }

    protected static void oreCooking(Consumer<FinishedRecipe> pFinishedRecipeConsumer, RecipeSerializer<? extends AbstractCookingRecipe> pCookingSerializer, List<ItemLike> pIngredients, RecipeCategory pCategory, ItemLike pResult, float pExperience, int pCookingTime, String pGroup, String pRecipeName) {
        for(ItemLike itemlike : pIngredients) {
            SimpleCookingRecipeBuilder.generic(Ingredient.of(new ItemLike[]{itemlike}), pCategory, pResult,
                    pExperience, pCookingTime, pCookingSerializer)
                    .group(pGroup).unlockedBy(getHasName(itemlike), has(itemlike))
                    .save(pFinishedRecipeConsumer, XueXiMod.MOD_ID +":" + getItemName(pResult) + pRecipeName + "_" + getItemName(itemlike));
        }

    }
}

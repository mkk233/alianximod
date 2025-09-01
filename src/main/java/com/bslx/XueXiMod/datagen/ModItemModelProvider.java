package com.bslx.XueXiMod.datagen;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.ModItems;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.client.model.generators.ItemModelBuilder;
import net.minecraftforge.client.model.generators.ItemModelProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

// 物品模型生成器 - 自动创建物品的JSON模型文件
public class ModItemModelProvider extends ItemModelProvider {
    // 构造函数：设置数据生成路径
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        // 参数说明：
        // output - 数据输出目录（默认生成到src/generated）
        // MOD_ID - 当前模组的命名空间
        // existingFileHelper - 资源文件验证助手
        super(output, XueXiMod.MOD_ID, existingFileHelper);
    }
    @Override
    protected void registerModels() {
        // 为每个自定义物品生成标准模型
        simpleItem(ModItems.ore_sousuo);  // 矿石搜索工具
        simpleItem(ModItems.shu_embryo); // 胚胎物品
        simpleItem(ModItems.shu_canzha); // 残渣物品
        simpleItem(ModItems.shu_raw);    // 原始材料
        simpleItem(ModItems.shu_food);   // 食物类物品
    }
    /**
     * 创建标准物品模型的核心方法
     * @param item 注册表中的物品对象
     * @return 模型构建器
     */
    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        // withExistingParent: 基于原版生成的父模型
        // 参数1: 新模型的注册路径（使用物品ID的路径名）
        // 参数2: 父模型资源路径 - 使用原版"item/generated"模型（适用于2D物品）

        // texture: 设置模型使用的纹理
        // "layer0": 标准物品模型的纹理层标识
        // 纹理路径: "modid:item/item_id" 对应 resources/assets/modid/textures/item/ 下的PNG文件
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(XueXiMod.MOD_ID, "item/" + item.getId().getPath()));
    }
}

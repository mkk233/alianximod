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

/**
 * 物品模型数据生成器 - 自动生成物品模型 JSON 文件
 *
 * 核心职责：
 * - 为所有注册物品生成 item/generated 类型的模型 JSON
 * - 模型引用 textures/item/{id}.png 作为 layer0 纹理
 *
 * 生成物品：ore_sousuo、shu_embryo、shu_wood_imprint_embryo、shu_canzha、shu_raw、shu_food、skill_sword、skill_magic
 * 输出目录：src/generated/resources/
 */
public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, XueXiMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(ModItems.ore_sousuo);
        simpleItem(ModItems.shu_canzha);
        simpleItem(ModItems.shu_raw);
        simpleItem(ModItems.shu_food);
    }

    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                ResourceLocation.parse("item/generated")).texture("layer0",
                ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, "item/" + item.getId().getPath()));
    }
}

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

public class ModItemModelProvider extends ItemModelProvider {
    public ModItemModelProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, XueXiMod.MOD_ID, existingFileHelper);
    }

    @Override
    protected void registerModels() {
        simpleItem(ModItems.ore_sousuo);
        simpleItem(ModItems.shu_embryo);
        simpleItem(ModItems.shu_canzha);
        simpleItem(ModItems.shu_raw);
        simpleItem(ModItems.shu_food);
    }

    @SuppressWarnings("removal")
    private ItemModelBuilder simpleItem(RegistryObject<Item> item) {
        return withExistingParent(item.getId().getPath(),
                new ResourceLocation("item/generated")).texture("layer0",
                new ResourceLocation(XueXiMod.MOD_ID, "item/" + item.getId().getPath()));
    }
}

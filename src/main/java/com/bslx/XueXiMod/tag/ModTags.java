package com.bslx.XueXiMod.tag;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

public class ModTags {
    public static class Blocks {
        //为方块添加tag
        public static final TagKey<Block> METAL = tag("metal");
        private static TagKey<Block> tag(String name){
             return BlockTags.create(new ResourceLocation(XueXiMod.MOD_ID, name));
        }
    }
    public static class Items {
        private static TagKey<Item> tag(String name){
            return ItemTags.create(new ResourceLocation(XueXiMod.MOD_ID, name));
        }
    }
}

package com.bslx.XueXiMod.tag;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * 自定义标签定义 - 模组 TagKey 注册中心
 *
 * Blocks 内部类：
 * - METAL：金属/矿石类方块标签，用于 teShuItem 矿物探测判断
 *
 * Items 内部类：
 * - 预留物品标签框架（当前未定义具体标签）
 *
 * 使用方式：ModTags.Blocks.METAL 用于方块标签检查
 */
public class ModTags {
    public static class Blocks {
        //为方块添加tag
        public static final TagKey<Block> METAL = tag("metal");
        private static TagKey<Block> tag(String name){
             return BlockTags.create(ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, name));
        }
    }
    public static class Items {
        private static TagKey<Item> tag(String name){
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath(XueXiMod.MOD_ID, name));
        }
    }
}

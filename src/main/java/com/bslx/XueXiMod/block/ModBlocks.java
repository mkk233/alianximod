package com.bslx.XueXiMod.block;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.item.ModItems;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, XueXiMod.MOD_ID);
    //创建方块，设定方块性质,可以从已有方块复制或者自己创建,json文件名需与name一致
    public static final RegistryObject<Block> ars_ore=registerBlock("ars_ore", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)));

    public static final RegistryObject<Block> shu_block=registerBlock("shu_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
//    public static final RegistryObject<Block> ars_ore=registerBlock("ars_ore",
//    上半为固定写法下半为复制写法
//    () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
//    以下用于单独修改某项属性
//    .sound(SoundType.AMETHYST)));

    //注册方块本身
    private static <T extends  Block>RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> blockObj = BLOCKS.register(name, block);
        registerBlockItem(name, blockObj);
        return blockObj;
    }

    //方块对应物品，这段代码是Minecraft模组开发中用于自动注册方块物品的工厂方法，主要实现自动化绑定为每个注册的方块自动创建对应的手持物品
    private static <T extends  Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name,() -> new BlockItem(block.get(),new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

package com.bslx.XueXiMod.block;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.custom.teShuBlocks;
import com.bslx.XueXiMod.item.ModItems;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
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
//    public static final RegistryObject<Block> ars_ore=registerBlock("ars_ore", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)));
    public static final RegistryObject<Block> shu_block=registerBlock("shu_block", () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> zhu_block=registerBlock("zhu_block", () -> new teShuBlocks(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    //.noLootTable()这个属性声明了无需配置战利品列表
//    public static final RegistryObject<Block> ars_ore=registerBlock("ars_ore",
//    上半为固定写法下半为复制写法
//    () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_ORE)
//    以下用于单独修改某项属性
//    .sound(SoundType.AMETHYST)));

    //UniformInt.of(x,y)用于获取经验x为下限y为上限
    public static final RegistryObject<Block> ars_ore=registerBlock("ars_ore",() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2F)
            .requiresCorrectToolForDrops(), UniformInt.of(3,6)));
    public static final RegistryObject<Block> deeplate_ars_ore=registerBlock("deeplate_ars_ore",() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(3F)
            .requiresCorrectToolForDrops(), UniformInt.of(3,7)));
    public static final RegistryObject<Block> nether_ars_ore=registerBlock("nether_ars_ore",() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHERRACK).strength(1F)
            .requiresCorrectToolForDrops(), UniformInt.of(3,7)));
    public static final RegistryObject<Block> end_ars_ore=registerBlock("end_ars_ore",() -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.END_STONE).strength(4F)
            .requiresCorrectToolForDrops(), UniformInt.of(3,7)));
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

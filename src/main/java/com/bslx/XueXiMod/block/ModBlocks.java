package com.bslx.XueXiMod.block;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.custom.TeShuBlocks;
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

    public static final RegistryObject<Block> shu_block = registerBlock("shu_block",
            () -> new Block(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK).sound(SoundType.AMETHYST)));
    public static final RegistryObject<Block> zhu_block = registerBlock("zhu_block",
            () -> new TeShuBlocks(BlockBehaviour.Properties.copy(Blocks.IRON_BLOCK)));
    public static final RegistryObject<Block> ars_ore = registerBlock("ars_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.STONE).strength(2F)
                    .requiresCorrectToolForDrops(), UniformInt.of(3, 6)));
    public static final RegistryObject<Block> deeplate_ars_ore = registerBlock("deeplate_ars_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.DEEPSLATE).strength(3F)
                    .requiresCorrectToolForDrops(), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> nether_ars_ore = registerBlock("nether_ars_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.NETHERRACK).strength(1F)
                    .requiresCorrectToolForDrops(), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> end_ars_ore = registerBlock("end_ars_ore",
            () -> new DropExperienceBlock(BlockBehaviour.Properties.copy(Blocks.END_STONE).strength(4F)
                    .requiresCorrectToolForDrops(), UniformInt.of(3, 7)));
    public static final RegistryObject<Block> IMPRINT_DESK = registerBlock("imprint_desk",
            () -> new ImprintDeskBlock(BlockBehaviour.Properties.copy(Blocks.OAK_PLANKS).sound(SoundType.WOOD)));
    public static final RegistryObject<Block> METEORITE = registerBlock("meteorite", MeteoriteBlock::new);

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> block) {
        RegistryObject<T> registeredBlock = BLOCKS.register(name, block);
        registerBlockItem(name, registeredBlock);
        return registeredBlock;
    }

    private static <T extends Block> RegistryObject<Item> registerBlockItem(String name, RegistryObject<T> block) {
        return ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}

package com.bslx.XueXiMod.datagen;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.tag.ModTags;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.tags.BlockTags;
import net.minecraftforge.common.Tags;
import net.minecraftforge.common.data.BlockTagsProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/**
 * 方块标签数据生成器 - 自动生成方块标签 JSON 文件
 *
 * 核心职责：
 * - 为 ars_ore 添加 METAL 和 ORES 标签
 * - 指定方块可挖掘工具（镐、斧）
 * - 指定方块挖掘等级（石/铁/钻石/下界合金）
 *
 * 标签映射：
 * - 镐挖掘：所有矿石 + shu_block + zhu_block
 * - 斧挖掘：writing_desk、imprint_desk
 * - 挖掘等级：ars_ore(铁)、deeplate_ars_ore(铁)、nether_ars_ore(钻石)、end_ars_ore(下界合金)
 * 输出目录：src/generated/resources/
 */
public class ModBlockTagGenerator extends BlockTagsProvider {
    public ModBlockTagGenerator(PackOutput output, CompletableFuture<HolderLookup.Provider> lookupProvider, @Nullable ExistingFileHelper existingFileHelper) {
        super(output, lookupProvider, XueXiMod.MOD_ID, existingFileHelper);
    }
    //给指定物品添加标签
    //需与配置文件对应
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        //为方块添加tag
        this.tag(ModTags.Blocks.METAL)
                .add(ModBlocks.ars_ore.get(),
                    ModBlocks.deeplate_ars_ore.get(),
                    ModBlocks.nether_ars_ore.get(),
                    ModBlocks.end_ars_ore.get());
        //指定挖掘工具
        this.tag(BlockTags.MINEABLE_WITH_PICKAXE)
                .add(ModBlocks.ars_ore.get(),
                    ModBlocks.shu_block.get(),
                    ModBlocks.zhu_block.get(),
                    ModBlocks.end_ars_ore.get(),
                    ModBlocks.nether_ars_ore.get(),
                    ModBlocks.deeplate_ars_ore.get());

        // 木制方块用斧头挖掘
        this.tag(BlockTags.MINEABLE_WITH_AXE)
                .add(ModBlocks.IMPRINT_DESK.get());

        //添加挖掘等级
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.ars_ore.get());
        this.tag(BlockTags.NEEDS_IRON_TOOL).add(ModBlocks.deeplate_ars_ore.get());
        this.tag(BlockTags.NEEDS_DIAMOND_TOOL).add(ModBlocks.nether_ars_ore.get());
        this.tag(BlockTags.NEEDS_STONE_TOOL).add(ModBlocks.shu_block.get());

        this.tag(Tags.Blocks.NEEDS_NETHERITE_TOOL).add(ModBlocks.end_ars_ore.get());
    }
}

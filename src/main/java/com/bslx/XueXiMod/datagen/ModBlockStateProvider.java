package com.bslx.XueXiMod.datagen;

//import net.minecraft.data.DataGenerator;
import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.common.data.ExistingFileHelper;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockStateProvider extends BlockStateProvider {
    // 构造方法参数说明：
    // output - 数据生成输出目录（默认生成到src/generated）
    // existingFileHelper - 用于验证资源路径的辅助工具
    public ModBlockStateProvider(PackOutput output,  ExistingFileHelper existingFileHelper) {
        super(output, XueXiMod.MOD_ID,existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        // 为每个注册的方块创建标准模型
            blockWithItem(ModBlocks.shu_block);
            blockWithItem(ModBlocks.zhu_block);
            blockWithItem(ModBlocks.ars_ore);
            blockWithItem(ModBlocks.deeplate_ars_ore);
            blockWithItem(ModBlocks.end_ars_ore);
            blockWithItem(ModBlocks.nether_ars_ore);

    }
    // 自定义辅助方法：同时生成方块状态和物品模型
    private void blockWithItem(RegistryObject<Block> block) {
        // simpleBlockWithItem 会生成：
        // - 方块状态JSON
        // - 方块模型JSON
        // - 对应的物品模型JSON
        // cubeAll 创建基于texture的简单立方体模型
        simpleBlockWithItem(block.get(),cubeAll(block.get()));
    }
}

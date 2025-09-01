package com.bslx.XueXiMod.datagen;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.ItemTagsProvider;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.data.ExistingFileHelper;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;
// 物品标签生成器 - 用于创建自定义物品分类标签
public class ModItemTagGenerator extends ItemTagsProvider {
    /**
     * 构造函数 - 初始化标签生成器
     * @param p_275343_ 数据包输出路径
     * @param p_275729_ 注册表提供器的异步接口
     * @param p_275322_ 方块标签查询器的异步接口
     * @param existingFileHelper 资源文件验证助手
     */
    public ModItemTagGenerator(PackOutput p_275343_,
                               CompletableFuture<HolderLookup.Provider> p_275729_,
                               CompletableFuture<TagLookup<Block>> p_275322_,
                               @Nullable ExistingFileHelper existingFileHelper) {
        // 参数说明:
        // p_275343_ : 数据输出目录 (通常由事件处理器传入)
        // p_275729_ : 异步获取游戏注册表内容
        // p_275322_ : 异步获取方块标签数据 (用于自动关联方块对应的物品标签)
        // XueXiMod.MOD_ID : 当前模组的命名空间
        super(p_275343_, p_275729_, p_275322_, XueXiMod.MOD_ID, existingFileHelper);
    }
    @Override
    protected void addTags(HolderLookup.Provider provider) {
        // 在此方法中添加自定义物品标签
        // 示例:
        // tag(ModTags.Items.SHU_MATERIALS) // 创建自定义标签
        //    .add(ModItems.shu_raw.get())   // 添加物品到标签
        //    .add(ModItems.shu_canzha.get());

        // tag(Tags.Items.ORES)             // 使用原版标签
        //    .add(ModItems.ore_sousuo.get());
    }
}

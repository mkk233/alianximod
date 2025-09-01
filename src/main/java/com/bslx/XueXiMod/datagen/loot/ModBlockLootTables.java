package com.bslx.XueXiMod.datagen.loot;

import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.item.ModItems;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

// 模组方块战利品表生成器
public class ModBlockLootTables extends BlockLootSubProvider {
    protected ModBlockLootTables() {
        // 参数1: 空的特征标志集合
        // 参数2: 所有注册的特征标志
        super(Set.of(), FeatureFlags.REGISTRY.allFlags());
    }
    @Override
    protected void generate() {
        // 普通方块掉落自身
        this.dropSelf(ModBlocks.shu_block.get()); // shu方块掉落自身
        this.dropSelf(ModBlocks.zhu_block.get()); // zhu方块掉落自身
        // 为矿石设置特殊掉落规则
        this.add(ModBlocks.ars_ore.get(), // 地表矿石
                block -> createCopperLikeOreDrops(block, ModItems.shu_raw.get()));

        this.add(ModBlocks.deeplate_ars_ore.get(), // 深层矿石
                block -> createCopperLikeOreDrops(block, ModItems.shu_raw.get()));

        this.add(ModBlocks.nether_ars_ore.get(), // 下界矿石
                block -> createCopperLikeOreDrops(block, ModItems.shu_raw.get()));

        this.add(ModBlocks.end_ars_ore.get(), // 末地矿石
                block -> createCopperLikeOreDrops(block, ModItems.shu_raw.get()));
    }

    /**
     * 创建类似铜矿的掉落规则
     * @param pBlock 目标方块
     * @param item 掉落的物品
     * @return 配置好的战利品表
     */
    protected LootTable.Builder createCopperLikeOreDrops(Block pBlock, Item item) {
        return createSilkTouchDispatchTable(pBlock, // 处理精准采集的情况
                (LootPoolEntryContainer.Builder)this.applyExplosionDecay(
                        pBlock,
                        LootItem.lootTableItem(item) // 掉落物品
                                // 设置掉落数量范围：2-5个
                                .apply(SetItemCountFunction.setCount(
                                        UniformGenerator.between(2.0F, 5.0F)))
                                // 应用时运附魔加成
                                .apply(ApplyBonusCount.addOreBonusCount(
                                        Enchantments.BLOCK_FORTUNE))
                ));
    }
    @Override
    protected Iterable<Block> getKnownBlocks(){
        // 返回所有已注册的模组方块
        return ModBlocks.BLOCKS.getEntries()
                .stream()
                .map(RegistryObject::get)
                ::iterator;
    }
}
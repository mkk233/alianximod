package com.bslx.XueXiMod.datagen;

import com.bslx.XueXiMod.datagen.loot.ModBlockLootTables;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.LootTableProvider;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;

import java.util.List;
import java.util.Set;

// 模组战利品表提供器 - 负责创建和注册战利品表
public class ModLootTableProvider {
    /**
     * 创建战利品表生成器实例
     * @param output 数据输出目录
     * @return 配置好的战利品表提供器
     */
    public static LootTableProvider create(PackOutput output) {
        return new LootTableProvider(output, Set.of(),
                // 注册子提供器列表：
                List.of(
                        // 方块战利品表子提供器
                        new LootTableProvider.SubProviderEntry(
                                ModBlockLootTables::new, // 使用ModBlockLootTables生成方块战利品
                                LootContextParamSets.BLOCK // 指定为方块战利品类型
                        )
                ));
    }
}

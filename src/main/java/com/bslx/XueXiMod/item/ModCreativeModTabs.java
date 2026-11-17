package com.bslx.XueXiMod.item;

import com.bslx.XueXiMod.XueXiMod;
import com.bslx.XueXiMod.block.ModBlocks;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

/**
 * 创造模式标签页注册 - 自定义 "术" 物品栏
 *
 * 核心职责：
 * - 注册 shu_tab 创造模式标签页（图标：shu_embryo）
 * - 定义标签页内物品排列顺序（决定游戏内显示顺序）
 *
 * 物品排序：术原胚 → 术矿石 → 各类矿石 → 特殊物品 → 技物品 → 功能方块
 * 依赖：通过 XueXiMod 构造函数注册到 modEventBus
 */
public class ModCreativeModTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS=
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, XueXiMod.MOD_ID);
    //创建选项卡
    public static final RegistryObject<CreativeModeTab>  shu_tab = CREATIVE_MODE_TABS.register("shu_tab",
            //图标
            () -> CreativeModeTab.builder().icon(() -> new ItemStack(ModItems.shu_embryo.get()))
                    //名称
            .title(Component.translatable("creativetab.shu_tab"))
            .displayItems((pParameters, pOutput) -> {
                //代码顺序将决定游戏内物品排序
                pOutput.accept(ShuEmbryoItem.create(com.bslx.XueXiMod.shu.ShuAttribute.NONE));
                pOutput.accept(ShuEmbryoItem.create(com.bslx.XueXiMod.shu.ShuAttribute.WOOD));
                pOutput.accept(ShuEmbryoItem.create(com.bslx.XueXiMod.shu.ShuAttribute.FIRE));
                pOutput.accept(ShuEmbryoItem.create(com.bslx.XueXiMod.shu.ShuAttribute.WATER));
                pOutput.accept(ShuEmbryoItem.create(com.bslx.XueXiMod.shu.ShuAttribute.EARTH));
                pOutput.accept(ShuEmbryoItem.create(com.bslx.XueXiMod.shu.ShuAttribute.METAL));
                pOutput.accept(ModItems.skill_stone.get());
                pOutput.accept(ModItems.skill_storage.get());
                pOutput.accept(ModItems.shu_raw.get());
                pOutput.accept(ModBlocks.ars_ore.get());
                pOutput.accept(ModBlocks.shu_block.get());
                pOutput.accept(ModBlocks.deeplate_ars_ore.get());
                pOutput.accept(ModBlocks.end_ars_ore.get());
                pOutput.accept(ModBlocks.nether_ars_ore.get());
                pOutput.accept(ModItems.ore_sousuo.get());
                pOutput.accept(ModItems.basic_staff.get());
                pOutput.accept(ModItems.test_staff.get());
                pOutput.accept(ModBlocks.zhu_block.get());
                pOutput.accept(ModItems.shu_canzha.get());
                pOutput.accept(ModBlocks.IMPRINT_DESK.get());
                pOutput.accept(ModItems.shu_food.get());
                pOutput.accept(ModItems.SKILL_BOOK_SWORD.get());
                pOutput.accept(ModItems.SKILL_BOOK_MAGIC.get());
                pOutput.accept(ModItems.STUDY_SLIP_MORTAL.get());
                pOutput.accept(ModItems.STUDY_SLIP_MYSTIC.get());
                pOutput.accept(ModItems.STUDY_SLIP_TRANSCENDENT.get());
                pOutput.accept(ModItems.STUDY_SCROLL_SWORD.get());
                pOutput.accept(ModItems.STUDY_SCROLL_WATER.get());
                pOutput.accept(ModItems.STUDY_SCROLL_FIRE.get());
                pOutput.accept(ModItems.DAOZHANG_SPAWN_EGG.get());
                pOutput.accept(ModBlocks.METEORITE.get());
            })
            .build());
    //存入bus
    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

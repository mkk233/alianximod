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

//用于添加属于模组的新创造栏
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
                pOutput.accept(ModItems.shu_embryo.get());
                pOutput.accept(ModItems.shu_raw.get());
                pOutput.accept(ModBlocks.ars_ore.get());
                pOutput.accept(ModBlocks.shu_block.get());
                pOutput.accept(ModBlocks.deeplate_ars_ore.get());
                pOutput.accept(ModBlocks.end_ars_ore.get());
                pOutput.accept(ModBlocks.nether_ars_ore.get());
                pOutput.accept(ModItems.ore_sousuo.get());
                pOutput.accept(ModBlocks.zhu_block.get());
                pOutput.accept(ModItems.shu_canzha.get());
            })
            .build());
    //存入bus
    public static void register(IEventBus eventBus){
        CREATIVE_MODE_TABS.register(eventBus);
    }
}

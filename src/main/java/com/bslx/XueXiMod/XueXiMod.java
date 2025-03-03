package com.bslx.XueXiMod;

import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.item.ModCreativeModTabs;
import com.bslx.XueXiMod.item.ModItems;
import com.mojang.logging.LogUtils;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(XueXiMod.MOD_ID)
public class XueXiMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "xueximod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public XueXiMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();
        //通过EventBus添加物品
        ModItems.register(modEventBus);
        //通过EventBus添加方块
        ModBlocks.register(modEventBus);
        //通过EventBus添加创造物品选项卡
        ModCreativeModTabs.register(modEventBus);
        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {

    }
    //添加内容给创造模式
    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        //将物品添加到创造选项栏中
        if (event.getTabKey()== CreativeModeTabs.INGREDIENTS){
//            event.accept(ModItems.shu_embryo);
//            event.accept(ModItems.shu_raw);
        }
    }

    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    // You can use EventBusSubscriber to automatically register all static methods in the class annotated with @SubscribeEvent
    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {

        }
    }
}

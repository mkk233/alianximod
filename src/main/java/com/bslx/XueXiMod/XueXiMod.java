package com.bslx.XueXiMod;

import com.bslx.XueXiMod.block.ModBlockEntities;
import com.bslx.XueXiMod.block.ModBlocks;
import com.bslx.XueXiMod.gui.ModMenuTypes;
import com.bslx.XueXiMod.gui.ImprintDeskScreen;
import com.bslx.XueXiMod.gui.ShuManagementScreen;
import com.bslx.XueXiMod.gui.SkillAssemblyScreen;
import com.bslx.XueXiMod.gui.SpellAssemblyScreen;
import com.bslx.XueXiMod.gui.SpellStorageScreen;
import com.bslx.XueXiMod.item.ModItems;
import com.bslx.XueXiMod.item.ShuEmbryoItem;
import com.bslx.XueXiMod.item.SkillStoneItem;
import com.bslx.XueXiMod.item.ModCreativeModTabs;
import com.bslx.XueXiMod.client.MagicHudOverlay;
import com.bslx.XueXiMod.keybind.ModKeyBindings;
import com.bslx.XueXiMod.network.ModNetwork;
import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.technique.release.entity.ReleaseEntities;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
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

/**
 * 模组主入口类 - XueXiMod（术模组）
 *
 * 核心职责：
 * - 注册所有 DeferredRegister（物品、方块、方块实体、菜单、创造模式标签页）
 * - 初始化网络通道（ModNetwork）和术系统（ModTechniques）
 * - 注册客户端 GUI 屏幕（撰写台、刻印桌）
 * - 注册客户端按键映射（C/V/R/M）
 * - 配置 Forge Config
 *
 * 生命周期：构造函数 → commonSetup → 服务器启动
 * 客户端事件通过内部类 ClientModEvents 处理
 */
// The value here should match an entry in the META-INF/mods.toml file
@Mod(XueXiMod.MOD_ID)
public class XueXiMod {
    // Define mod id in a common place for everything to reference
    public static final String MOD_ID = "xueximod";
    // Directly reference a slf4j logger
    private static final Logger LOGGER = LogUtils.getLogger();

    public XueXiMod(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        registerContent(modEventBus);

        modEventBus.addListener(this::commonSetup);

        MinecraftForge.EVENT_BUS.register(this);

        modEventBus.addListener(this::addCreative);

        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void registerContent(IEventBus modEventBus) {
        //通过EventBus添加物品
        ModItems.register(modEventBus);
        //通过EventBus添加方块
        ModBlocks.register(modEventBus);
        //通过EventBus添加方块实体
        ModBlockEntities.register(modEventBus);
        //通过EventBus添加菜单类型
        ModMenuTypes.register(modEventBus);
        //通过EventBus添加创造物品选项卡
        ModCreativeModTabs.register(modEventBus);
        //通过EventBus添加释放实体
        ReleaseEntities.register(modEventBus);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 注册网络
        ModNetwork.register();
        // 注册所有术
        ModTechniques.registerAll();
        LOGGER.info("术系统初始化完成，共注册 {} 个术", com.bslx.XueXiMod.shu.TechniqueManager.getAll().size());
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
            // 注册GUI屏幕
            MenuScreens.register(ModMenuTypes.IMPRINT_DESK.get(), ImprintDeskScreen::new);
            MenuScreens.register(ModMenuTypes.SHU_MANAGEMENT.get(), ShuManagementScreen::new);
            MenuScreens.register(ModMenuTypes.SKILL_ASSEMBLY.get(), SkillAssemblyScreen::new);
            MenuScreens.register(ModMenuTypes.SPELL_STORAGE.get(), SpellStorageScreen::new);
            MenuScreens.register(ModMenuTypes.SPELL_ASSEMBLY.get(), SpellAssemblyScreen::new);
            event.enqueueWork(() -> {
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.shu_embryo.get(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "shu_attribute"),
                        (stack, level, entity, seed) -> ShuEmbryoItem.getAttribute(stack).getPredicateValue());
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.skill_stone.get(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "shu_attribute"),
                        (stack, level, entity, seed) -> {
                            var data = SkillStoneItem.getData(stack);
                            return data == null ? 0f : data.getEmbryoAttribute().getPredicateValue();
                        });
                net.minecraft.client.renderer.item.ItemProperties.register(
                        ModItems.skill_stone.get(),
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(MOD_ID, "skill_attack"),
                        (stack, level, entity, seed) -> {
                            var data = SkillStoneItem.getData(stack);
                            if (data == null) return 0f;
                            var technique = data.getTechnique();
                            return (technique != null && technique.getType() == com.bslx.XueXiMod.shu.TechniqueType.SWORD) ? 1f : 0f;
                        });
            });
        }

        @SubscribeEvent
        public static void onKeyRegister(RegisterKeyMappingsEvent event) {
            ModKeyBindings.registerAll(event);
        }

        @SubscribeEvent
        public static void onRegisterGuiOverlays(RegisterGuiOverlaysEvent event) {
            event.registerAbove(VanillaGuiOverlay.HOTBAR.id(), "magic", MagicHudOverlay::render);
        }
    }
}

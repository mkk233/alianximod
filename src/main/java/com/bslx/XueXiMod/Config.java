package com.bslx.XueXiMod;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import com.bslx.XueXiMod.status.WetnessFlightExempt;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 模组配置类 - Forge Config 规范定义
 *
 * 核心职责：
 * - 定义 ForgeConfigSpec 配置规范（COMMON 类型）
 * - 提供配置值的静态缓存（logDirtBlock、magicNumber 等）
 * - 在 ModConfigEvent 加载时刷新缓存值
 *
 * 当前为示例配置，后续可扩展为术系统相关配置（如伤害倍率、冷却倍率等）
 */
// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Forge's config APIs
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    private static final ForgeConfigSpec.BooleanValue LOG_DIRT_BLOCK = BUILDER
            .comment("Whether to log the dirt block on common setup")
            .define("logDirtBlock", true);

    private static final ForgeConfigSpec.IntValue MAGIC_NUMBER = BUILDER
            .comment("A magic number")
            .defineInRange("magicNumber", 42, 0, Integer.MAX_VALUE);

    public static final ForgeConfigSpec.ConfigValue<String> MAGIC_NUMBER_INTRODUCTION = BUILDER
            .comment("What you want the introduction message to be for the magic number")
            .define("magicNumberIntroduction", "The magic number is... ");

    // a list of strings that are treated as resource locations for items
    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> ITEM_STRINGS = BUILDER
            .comment("A list of items to log on common setup.")
            .defineListAllowEmpty("items", List.of("minecraft:iron_ingot"), Config::validateItemName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> FLIGHT_EXEMPT_ENTITY_IDS = defineFlightExempt();

    static final ForgeConfigSpec SPEC = BUILDER.build();

    static {
        WetnessFlightExempt.ensureDefault();
    }

    public static boolean logDirtBlock;
    public static int magicNumber;
    public static String magicNumberIntroduction;
    public static Set<Item> items;

    private static boolean validateItemName(final Object candidate)
    {
        return candidate instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(ResourceLocation.parse(itemName));
    }

    private static ForgeConfigSpec.ConfigValue<List<? extends String>> defineFlightExempt()
    {
        BUILDER.push("wetness");
        ForgeConfigSpec.ConfigValue<List<? extends String>> defined = BUILDER
                .comment("Registry ids whose entities skip wetness descent only. Other wetness effects still apply. An empty list exempts nobody and does not restore the default.")
                .defineListAllowEmpty("flightExemptEntityIds", List.of("minecraft:ender_dragon"), WetnessFlightExempt::validSyntax);
        BUILDER.pop();
        return defined;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        logDirtBlock = LOG_DIRT_BLOCK.get();
        magicNumber = MAGIC_NUMBER.get();
        magicNumberIntroduction = MAGIC_NUMBER_INTRODUCTION.get();

        // convert the list of strings into a set of items
        items = ITEM_STRINGS.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(ResourceLocation.parse(itemName)))
                .collect(Collectors.toSet());
        WetnessFlightExempt.replace(FLIGHT_EXEMPT_ENTITY_IDS.get());
    }
}

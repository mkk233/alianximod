package com.bslx.XueXiMod.terrain;

import com.bslx.XueXiMod.XueXiMod;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * 本批「破坏已有方块」开关。默认开启。
 * 只被崩地读取。陨石安放不读它。生成水、分海和其他旧世界修改也不读它。
 * 配置文件本身要由公共注册把 {@link #SPEC} 挂上；没挂上时保持默认开启。
 */
@Mod.EventBusSubscriber(modid = XueXiMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class BatchTerrainBreakSwitch {
    public static final String CONFIG_FILE = "xueximod-batch-terrain.toml";
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    private static final ForgeConfigSpec.BooleanValue BREAK_EXISTING = BUILDER
            .comment("Whether this batch may break existing blocks. Placement of meteorite residue ignores this. Water generation and sea parting are not controlled here.")
            .define("breakExistingBlocks", true);
    public static final ForgeConfigSpec SPEC = BUILDER.build();
    private static volatile boolean breakExistingBlocks = true;

    private BatchTerrainBreakSwitch() {
    }

    public static boolean allowsBreakingExisting() {
        return breakExistingBlocks;
    }

    public static void applyConfigured(boolean enabled) {
        breakExistingBlocks = enabled;
    }

    @SubscribeEvent
    public static void onLoad(ModConfigEvent event) {
        if (event.getConfig().getSpec() != SPEC) {
            return;
        }
        breakExistingBlocks = BREAK_EXISTING.get();
    }
}

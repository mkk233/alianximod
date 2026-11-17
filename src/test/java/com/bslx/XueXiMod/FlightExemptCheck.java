package com.bslx.XueXiMod;

import com.bslx.XueXiMod.status.WetnessFlight;
import com.bslx.XueXiMod.status.WetnessFlightExempt;
import com.electronwill.nightconfig.core.UnmodifiableConfig;
import net.minecraft.resources.ResourceLocation;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Set;

/**
 * 调用生产的下降黑名单。不构造生物或世界，也不另写一套注册名规则。
 */
public final class FlightExemptCheck {
    private static int checks;
    private static int failures;

    private FlightExemptCheck() {}

    public static void main(String[] arguments) throws Exception {
        WetnessFlightExempt.ensureDefault();
        Set<ResourceLocation> initial = readIds();
        ResourceLocation dragon = ResourceLocation.tryParse("minecraft:ender_dragon");
        check(dragon != null && initial.contains(dragon) && initial.size() == 1,
                "配置加载前的默认豁免不是只有末影龙，实际 " + initial);
        check(!WetnessFlightExempt.isExempt(null) && !WetnessFlight.isDescentExempt(null),
                "空生物被当成了豁免");

        WetnessFlightExempt.replace(List.of());
        check(readIds().isEmpty(), "空列表之后仍有豁免 " + readIds());
        WetnessFlightExempt.ensureDefault();
        check(readIds().isEmpty(), "空列表之后 ensureDefault 又恢复了默认末影龙");
        WetnessFlightExempt.replace(null);
        check(readIds().isEmpty(), "空引用没有变成零豁免");

        WetnessFlightExempt.replace(List.of("minecraft:ender_dragon"));
        check(readIds().equals(Set.of(dragon)), "精确名单没有只留下末影龙，实际 " + readIds());
        WetnessFlightExempt.replace(List.of("minecraft:ender_dragons", "ender_dragon", "minecraft:ender_dragon_extra"));
        check(!readIds().contains(dragon), "子串或邻近名字匹配了末影龙，实际 " + readIds());

        ResourceLocation zombie = ResourceLocation.tryParse("minecraft:zombie");
        ResourceLocation custom = ResourceLocation.tryParse("examplemod:custom_mob");
        WetnessFlightExempt.replace(List.of("minecraft:zombie", "examplemod:custom_mob"));
        Set<ResourceLocation> pair = readIds();
        check(zombie != null && custom != null && pair.equals(Set.of(zombie, custom)),
                "原版和模组名字没有互相独立保留，实际 " + pair);

        check(!WetnessFlightExempt.validSyntax("not an id"), "空格名字被当成合法注册名");
        check(!WetnessFlightExempt.validSyntax(":ender_dragon"), "缺少命名空间的名字被当成合法");
        check(!WetnessFlightExempt.validSyntax(""), "空字符串被当成合法");
        check(!WetnessFlightExempt.validSyntax("Minecraft:Ender_Dragon"), "大写名字被当成合法");
        check(!WetnessFlightExempt.validSyntax("ender_dragon"), "没有命名空间的名字被当成合法");
        check(WetnessFlightExempt.validSyntax("examplemod:not_real"), "合法但未注册的名字被拒绝");
        WetnessFlightExempt.replace(List.of("not an id", "examplemod:not_real"));
        ResourceLocation unknown = ResourceLocation.tryParse("examplemod:not_real");
        check(unknown != null && readIds().equals(Set.of(unknown)),
                "非法条目没有丢掉，或未知合法名字没有保留，实际 " + readIds());

        WetnessFlightExempt.replace(List.of("minecraft:ender_dragon"));
        WetnessFlightExempt.replace(List.of("minecraft:pig"));
        ResourceLocation pig = ResourceLocation.tryParse("minecraft:pig");
        check(pig != null && readIds().equals(Set.of(pig)) && !readIds().contains(dragon),
                "后一次替换没有去掉之前的末影龙，实际 " + readIds());

        printConfigPath();
        System.out.println("未执行 真实生物的 isExempt：没有构造末影龙、玩家或普通生物");
        System.out.println("未执行 配置文件重载事件：没有启动 Forge，也没有改 run/config");
        System.out.println("断言 " + checks + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void printConfigPath() {
        try {
            UnmodifiableConfig values = Config.SPEC.getValues();
            Object section = values.get("wetness");
            Object listed = section instanceof UnmodifiableConfig wetness ? wetness.get("flightExemptEntityIds") : null;
            System.out.println("配置文件 config/xueximod-common.toml 键 [wetness].flightExemptEntityIds 已写进 COMMON 规格");
            check(listed != null, "COMMON 配置里没有 wetness.flightExemptEntityIds");
        } catch (RuntimeException failure) {
            failures++;
            checks++;
            System.out.println("失败 读取 COMMON 配置规格抛出 " + failure);
        }
    }

    @SuppressWarnings("unchecked")
    private static Set<ResourceLocation> readIds() throws ReflectiveOperationException {
        Field cache = WetnessFlightExempt.class.getDeclaredField("exemptIds");
        cache.setAccessible(true);
        return (Set<ResourceLocation>) cache.get(null);
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("失败[" + checks + "] " + detail);
        }
    }
}

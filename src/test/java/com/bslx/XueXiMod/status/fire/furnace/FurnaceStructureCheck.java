package com.bslx.XueXiMod.status.fire.furnace;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 只读源码结构。不运行 Mixin，也不编译 Forge。 */
public final class FurnaceStructureCheck {
    private static int assertionCount;
    private static int failures;

    private FurnaceStructureCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        Path root = locateRoot();
        String service = read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java"));
        String events = read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceEvents.java"));
        String runtime = read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceRuntime.java"));
        String effect = read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceMobEffect.java"));
        String resistance = read(root.resolve("src/main/java/com/bslx/XueXiMod/mixin/FurnaceResistanceMixin.java"));
        String registry = read(root.resolve("src/main/java/com/bslx/XueXiMod/registry/ModMobEffects.java"));
        String mixins = read(root.resolve("src/main/resources/xueximod.mixins.json"));
        String chinese = read(root.resolve("src/main/resources/assets/xueximod/lang/zh_cn.json"));
        String english = read(root.resolve("src/main/resources/assets/xueximod/lang/en_us.json"));
        String furnaceTree = service + events + runtime + effect
                + read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceStore.java"))
                + read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceAttributes.java"))
                + read(root.resolve("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceMath.java"));

        check(service.contains("fireBonus") && service.contains("seedMultiplier")
                        && service.contains("waterIceMultiplier") && service.contains("boolean active"),
                "查询方法还在");
        check(service.contains("enterNormal") && service.contains("enterOverdrive") && service.contains("endPassive")
                        && service.contains("FurnaceChange cool"),
                "进入、升级、被动结束和主动冷却都有入口");
        String coolBody = between(service, "FurnaceChange cool", "private static FurnaceChange enter");
        String passiveBody = between(service, "FurnaceChange endPassive", "FurnaceChange cool");
        check(coolBody.contains("SteamBurst.release"), "主动冷却调用蒸汽");
        check(!passiveBody.contains("SteamBurst"), "被动结束不调用蒸汽");
        check(!events.contains("SteamBurst") && !runtime.contains("SteamBurst"), "生命周期和爬坡不调用蒸汽");
        check(runtime.contains("advanceOnline"), "爬坡走单拍在线推进");
        check(!runtime.contains("elapsedTicks *"), "不用离线间隔乘热量");
        check(events.contains("Phase.START") && events.contains("Phase.END"), "隐藏效果清理在 tick 开始，爬坡在 tick 结束");
        check(events.contains("LivingHealEvent") && events.contains("isWasDeath") && events.contains("PlayerLoggedInEvent"),
                "治疗、克隆和登录有订阅");
        check(events.contains("MobEffectEvent.Remove") && events.contains("getCurativeItems") == false,
                "移除事件清运行态，牛奶不靠取消全部移除");
        check(effect.contains("getCurativeItems") && effect.contains("new ArrayList"), "治愈物列表为空，牛奶对不上");
        check(effect.contains("xuexiSetHiddenEffect") && effect.contains("INFINITE_DURATION"), "规范化会拆隐藏效果并保持无限");
        check(resistance.contains("getDamageAfterMagicAbsorb") && resistance.contains("DAMAGE_RESISTANCE")
                        && resistance.contains("suppressesOwnResistance"),
                "抗性钩子在魔法吸收方法里");
        check(!resistance.contains("PROTECTION") && !resistance.contains("LivingHurtEvent")
                        && !resistance.contains("LivingDamageEvent"),
                "不在受伤事件里改保护附魔或全局伤害");
        check(registry.contains("furnace_core") && registry.contains("FurnaceMobEffect"), "效果注册名是 furnace_core");
        check(mixins.contains("\"FurnaceResistanceMixin\"") && mixins.contains("\"FurnaceEffectAccess\""),
                "两个炉心 mixin 都登记了");
        check(chinese.contains("\"effect.xueximod.furnace_core\"") && english.contains("\"effect.xueximod.furnace_core\""),
                "中英文都有炉心效果名");
        check(!furnaceTree.contains("AttributeStateService") && !furnaceTree.contains("Wetness"),
                "炉心不用潮湿的有限时长替换");
        check(!furnaceTree.contains("ModTechniques") && !furnaceTree.contains("CastCooldown")
                        && !furnaceTree.contains("MagicPool"),
                "炉心包不接术式、冷却或魔力");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未运行：Mixin 转换、奶桶实喝、命令清除、保护附魔结算、跨维存档。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static Path locateRoot() {
        Path direct = Path.of("src/main/java/com/bslx/XueXiMod/status/fire/furnace/FurnaceService.java");
        if (Files.isRegularFile(direct)) {
            return Path.of("").toAbsolutePath();
        }
        return Path.of("D:/alianximod");
    }

    private static String read(Path path) throws Exception {
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static String between(String source, String start, String end) {
        int from = source.indexOf(start);
        int stop = source.indexOf(end, from + start.length());
        if (from < 0 || stop < 0) {
            return "";
        }
        return source.substring(from, stop);
    }

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            failures++;
            System.out.println("失败: " + label);
        }
    }
}

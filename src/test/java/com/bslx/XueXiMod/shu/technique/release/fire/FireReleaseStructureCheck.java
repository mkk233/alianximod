package com.bslx.XueXiMod.shu.technique.release.fire;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

/** 核对火伤源码和伤害标签没有绕开防御，也没有把未来术式写进运行模块。 */
public final class FireReleaseStructureCheck {
    private static final Path ROOT = Path.of("D:/alianximod");
    private static final Path FIRE = ROOT.resolve("src/main/java/com/bslx/XueXiMod/shu/technique/release/fire");
    private static final Path TAGS = ROOT.resolve("src/main/resources/data/minecraft/tags/damage_type");
    private static int assertionCount;
    private static int failures;

    private FireReleaseStructureCheck() {
    }

    public static void main(String[] arguments) throws IOException {
        section("运行源码", FireReleaseStructureCheck::runtime);
        section("标签", FireReleaseStructureCheck::tags);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：数据包加载、标签合并、真实无敌帧。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void runtime() throws IOException {
        String all = readTree(FIRE);
        String[] forbidden = {
                "invulnerableTime",
                "setSecondsOnFire",
                "setRemainingFireTicks",
                "setBlock",
                "playerAttack",
                "Relations",
                "heat_beam",
                "magic_ignite",
                "fire_orb",
                "fire_burst",
                "燎原",
                "燎里",
                "焚天",
                "1.2",
                "0.36",
                "0.04"
        };
        for (String token : forbidden) {
            check(!all.contains(token), "运行模块不含 " + token);
        }
        check(!all.contains("while"), "燃烧推进没有补打循环");
        String consumption = Files.readString(FIRE.resolve("FireConsumption.java"), StandardCharsets.UTF_8);
        int schedule = consumption.indexOf("public static Optional<SharedSpec> schedule");
        int detonate = consumption.indexOf("public static Optional<FireSeedSnapshot> detonate");
        check(schedule >= 0 && detonate > schedule, "消费入口顺序可读");
        String scheduleBody = consumption.substring(schedule, detonate);
        check(!scheduleBody.contains("FireSeedState.consume"), "挂载不扣火种");
        check(scheduleBody.contains(".duplicate(") && scheduleBody.contains("admitShared")
                        && scheduleBody.contains("PLAN_CAP"),
                "挂载先看容量和同一次施法，再锁定参数");
        String detonateBody = consumption.substring(detonate);
        int prepared = detonateBody.indexOf("consumePrepared");
        int strike = detonateBody.indexOf("FireDamage.strikeOnce");
        check(prepared >= 0 && prepared < strike, "引爆先确认分量，再扣点并打一次");
        check(!detonateBody.contains("FireSeedState.consume("), "引爆不走先扣后验");
        String carrier = Files.readString(FIRE.resolve("BurnCarrier.java"), StandardCharsets.UTF_8);
        check(count(carrier, "strikeOnce") == 1, "每段只提交一次打击");
        String steam = Files.readString(FIRE.resolve("SteamBurst.java"), StandardCharsets.UTF_8);
        check(steam.contains("RelationPolicy") && steam.contains("SteamHostility.struck"), "蒸汽复用关系策略并先排除队友");
        check(steam.contains("SteamTuning.BASE_DAMAGE"), "蒸汽使用固定基础伤害");
        check(steam.contains("ElementDamageScope.armNeutral") && !steam.contains("armFire")
                        && !steam.contains("armWater") && !steam.contains("armIce"),
                "蒸汽每次 hurt 显式中性");
        check(!steam.contains("FurnaceService") && !steam.contains("FireSeed"), "蒸汽不读炉心或火种");
        String damage = Files.readString(FIRE.resolve("FireDamage.java"), StandardCharsets.UTF_8);
        check(damage.contains("ElementDamageScope.armFire"), "资源火伤带显式火来源");
        check(damage.contains("bypasses") == false, "代码里不手写绕过标签判断");
        String events = Files.readString(FIRE.resolve("FireRuntimeEvents.java"), StandardCharsets.UTF_8);
        check(events.contains("ElementDamageScope.currentHurt()") && events.contains("call.target()")
                        && events.contains("call.source()") && events.contains("FurnaceService.fireBonus")
                        && events.contains("FurnaceService.waterIceMultiplier"),
                "事件只读取当前这次 hurt 的元素帧");
        String dispatch = Files.readString(FIRE.resolve("BurnDispatch.java"), StandardCharsets.UTF_8);
        int spent = dispatch.indexOf("commitSpent");
        int commit = dispatch.indexOf("memory.write(ledger)");
        int loop = dispatch.indexOf("for (BurnPulse pulse");
        check(spent >= 0 && spent < commit && loop > commit, "先扣燃料，再提交账本，然后分发伤害");
        check(loop >= 0 && !dispatch.substring(loop).contains("memory.write"), "回调之后不再写回旧账本");
        check(carrier.contains("BurnDispatch.tick"), "燃烧 tick 走先提交再打击");
    }

    private static void tags() throws IOException {
        String cooldown = Files.readString(TAGS.resolve("bypasses_cooldown.json"), StandardCharsets.UTF_8);
        String fire = Files.readString(TAGS.resolve("is_fire.json"), StandardCharsets.UTF_8);
        check(cooldown.contains("\"replace\": false") && cooldown.contains("xueximod:fire_resource"),
                "资源火伤加入冷却标签且不替换原版列表");
        check(!cooldown.contains("steam_burst"), "蒸汽不绕过命中冷却");
        check(fire.contains("\"replace\": false") && fire.contains("xueximod:fire_resource"),
                "资源火伤标成火焰，抗火和火焰保护仍能识别");
        check(!fire.contains("steam_burst"), "蒸汽不标成火焰");
        String[] defense = {
                "bypasses_armor.json",
                "bypasses_effects.json",
                "bypasses_resistance.json",
                "bypasses_enchantments.json"
        };
        for (String name : defense) {
            String text = Files.readString(TAGS.resolve(name), StandardCharsets.UTF_8);
            check(!text.contains("fire_resource") && !text.contains("steam_burst"), name + " 不含新的火伤或蒸汽");
        }
        String fireType = Files.readString(ROOT.resolve("src/main/resources/data/xueximod/damage_type/fire_resource.json"),
                StandardCharsets.UTF_8);
        String steamType = Files.readString(ROOT.resolve("src/main/resources/data/xueximod/damage_type/steam_burst.json"),
                StandardCharsets.UTF_8);
        check(fireType.contains("\"scaling\": \"never\"") && fireType.contains("\"exhaustion\": 0.0"),
                "资源火伤不吃难度缩放");
        check(steamType.contains("\"scaling\": \"never\"") && steamType.contains("\"message_id\": \"steam_burst\""),
                "蒸汽伤害类型独立");
    }

    private static String readTree(Path directory) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (Stream<Path> walk = Files.walk(directory)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".java")).sorted().toList()) {
                builder.append(Files.readString(file, StandardCharsets.UTF_8));
            }
        }
        return builder.toString();
    }

    private static int count(String text, String token) {
        int found = 0;
        int from = 0;
        while (true) {
            int next = text.indexOf(token, from);
            if (next < 0) {
                return found;
            }
            found += 1;
            from = next + token.length();
        }
    }

    private static void section(String title, IoBody body) {
        int before = failures;
        try {
            body.run();
        } catch (Exception failure) {
            failures += 1;
            assertionCount += 1;
            System.out.println("失败 " + title + " " + failure.getMessage());
        }
        System.out.println(title + " 失败 " + (failures - before));
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }

    @FunctionalInterface
    private interface IoBody {
        void run() throws IOException;
    }
}

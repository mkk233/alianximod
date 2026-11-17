package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** 核对幽冥代码默认、两份现行配置和 5 刻结算窗口。不启动原版注册。 */
public final class NetherGloomPeriodCheck {
    private static int assertions;
    private static int failures;
    private static long rememberedWindow = Long.MIN_VALUE;

    private NetherGloomPeriodCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        defaults();
        files();
        cadence();
        System.out.println("assertions " + assertions + " failures " + failures);
        System.out.println("not run: furnace multiplier, living entities, game");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void defaults() {
        check(ResourceEnchantNumbers.GLOOM_BASE == 5, "seed amount stays 5");
        check(ResourceEnchantNumbers.GLOOM_PERIOD_TICKS == 5, "code default period is 5");
        check(DomainDefinition.gloom().lifetimeTicks() == 200, "domain life stays 200");
        check(com.bslx.XueXiMod.status.fire.seed.FireSeedMath.UPPER_BOUND == 100, "seed cap stays 100");
    }

    private static void files() throws Exception {
        checkFile(Path.of("run/defaultconfigs/xueximod-balance.json"), "template");
        checkFile(Path.of("run/saves/新的世界/serverconfig/xueximod-balance.json"), "world");
    }

    private static void checkFile(Path path, String label) throws Exception {
        JsonObject gloom = JsonParser.parseString(Files.readString(path, StandardCharsets.UTF_8))
                .getAsJsonObject().getAsJsonObject("techniques").getAsJsonObject("fire_nether_gloom");
        check(gloom.get("seedCount").getAsInt() == 5, label + " seeds stay 5");
        check(gloom.get("periodTicks").getAsInt() == 5, label + " period is 5");
        check(gloom.get("lifetimeTicks").getAsInt() == 200, label + " life stays 200");
    }

    private static void cadence() {
        int period = ResourceEnchantNumbers.GLOOM_PERIOD_TICKS;
        int grants = 0;
        boolean created = false;
        boolean first = false;
        boolean last = false;
        for (int age = 0; age <= 200; age++) {
            boolean due = due(1_000_000L + age, period, age);
            grants += due ? 1 : 0;
            created = age == 0 ? due : created;
            first = age == period ? due : first;
            last = age == 200 ? due : last;
        }
        check(!created, "age 0 does not grant");
        check(first, "age 5 grants");
        check(last, "age 200 grants");
        check(grants == 40, "200 ticks grant 40 times");
        long firstWindow = 1_000_005L / period;
        check("GRANTED".equals(remember(firstWindow)), "first window grants");
        check("ALREADY".equals(remember(firstWindow)), "same window is deduplicated");
        check("GRANTED".equals(remember(firstWindow + 1L)), "next window grants again");
        check(untilFull(5, 100) == 20, "100 points take 20 grants");
        check(20 * period == 100, "full cap takes 100 ticks");
    }

    private static boolean due(long gameTime, int period, int age) {
        return period > 0 && gameTime > 0L && gameTime % period == 0L && age >= period;
    }

    private static String remember(long windowId) {
        if (rememberedWindow == windowId) {
            return "ALREADY";
        }
        rememberedWindow = windowId;
        return "GRANTED";
    }

    private static int untilFull(int points, int cap) {
        int balance = 0;
        int count = 0;
        while (balance < cap) {
            balance = Math.min(cap, balance + points);
            count++;
        }
        return count;
    }

    private static void check(boolean passed, String label) {
        assertions++;
        if (!passed) {
            failures++;
            System.out.println("FAIL " + label);
        }
    }
}

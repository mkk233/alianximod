package com.bslx.XueXiMod.shu.technique.release.fire;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** 按原版翻译规则格式化死亡句。不加载语言，也不启动游戏。 */
public final class FireDeathMessageCheck {
    private static final Path ROOT = Path.of("D:/alianximod");
    private static final Pattern TOKEN = Pattern.compile("%(?:(\\d+)\\$)?([A-Za-z%]|$)");
    private static final String[] BASES = {
            "death.attack.fire_resource",
            "death.attack.steam_burst"
    };
    private static int assertionCount;
    private static int failures;

    private FireDeathMessageCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        String chinese = Files.readString(ROOT.resolve("src/main/resources/assets/xueximod/lang/zh_cn.json"),
                StandardCharsets.UTF_8);
        String english = Files.readString(ROOT.resolve("src/main/resources/assets/xueximod/lang/en_us.json"),
                StandardCharsets.UTF_8);
        check(chinese.contains("\"death.attack.water_prison_pressure\": \"%1$s被水牢的压力压垮了\""), "原有中文水牢死亡句还在");
        check(english.contains("\"death.attack.water_prison_pressure\": \"%1$s was crushed by water prison pressure\""),
                "原有英文水牢死亡句还在");
        formatLanguage("中文", chinese);
        formatLanguage("英文", english);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：游戏内死亡句选择。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void formatLanguage(String language, String document) {
        for (String key : BASES) {
            String template = value(document, key);
            String alone = format(template, "VictimOne");
            String extraAttacker = format(template, "VictimOne", "AttackerTwo");
            check(alone != null && alone.equals(extraAttacker), language + " " + key + " 多传攻击者时显示与单参相同");
            check(alone != null && alone.contains("VictimOne") && !alone.contains("AttackerTwo") && !alone.contains("%"),
                    language + " " + key + " 单参只留下受害者");
            String playerLine = format(value(document, key + ".player"), "VictimOne", "AttackerTwo");
            check(playerLine != null && playerLine.contains("VictimOne") && playerLine.contains("AttackerTwo")
                            && !playerLine.contains("%"),
                    language + " " + key + ".player 两参都能代入");
            String itemLine = format(value(document, key + ".item"), "VictimOne", "AttackerTwo", "WeaponThree");
            check(itemLine != null && itemLine.contains("VictimOne") && itemLine.contains("AttackerTwo")
                            && itemLine.contains("WeaponThree") && !itemLine.contains("%"),
                    language + " " + key + ".item 三参都能代入");
        }
    }

    private static String format(String template, String... arguments) {
        if (template == null) {
            return null;
        }
        Matcher matcher = TOKEN.matcher(template);
        StringBuilder rendered = new StringBuilder();
        int sequential = 0;
        int scanned = 0;
        while (matcher.find(scanned)) {
            int start = matcher.start();
            int stop = matcher.end();
            if (stop == scanned) {
                return null;
            }
            if (start > scanned) {
                String plain = template.substring(scanned, start);
                if (plain.indexOf('%') >= 0) {
                    return null;
                }
                rendered.append(plain);
            }
            String kind = matcher.group(2);
            String token = template.substring(start, stop);
            if ("%".equals(kind) && "%%".equals(token)) {
                rendered.append('%');
            } else if (!"s".equals(kind)) {
                return null;
            } else {
                String indexText = matcher.group(1);
                int argumentIndex = indexText == null ? sequential++ : Integer.parseInt(indexText) - 1;
                if (argumentIndex < 0 || argumentIndex >= arguments.length || arguments[argumentIndex] == null) {
                    return null;
                }
                rendered.append(arguments[argumentIndex]);
            }
            scanned = stop;
        }
        if (scanned < template.length()) {
            String tail = template.substring(scanned);
            if (tail.indexOf('%') >= 0) {
                return null;
            }
            rendered.append(tail);
        }
        return rendered.toString();
    }

    private static String value(String document, String key) {
        String marker = "\"" + key + "\": \"";
        int start = document.indexOf(marker);
        if (start < 0) {
            return null;
        }
        int from = start + marker.length();
        int end = document.indexOf('"', from);
        if (end < 0) {
            return null;
        }
        return document.substring(from, end);
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}

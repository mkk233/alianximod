import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 核对水牢伤害标签、死亡文案和伤害类型 JSON。不启动游戏，也不做 Mixin 变换。
 */
public final class PrisonResourceGate {
    private static final String PRESSURE_ID = "xueximod:water_prison_pressure";
    private static final String MESSAGE_ID = "water_prison_pressure";
    private static final String DEATH_KEY = "death.attack.water_prison_pressure";

    private final Path resources;
    private int checks;
    private int failures;

    private PrisonResourceGate(Path resources) {
        this.resources = resources;
    }

    public static void main(String[] arguments) throws Exception {
        PrisonResourceGate gate = new PrisonResourceGate(Path.of("D:/alianximod/src/main/resources"));
        gate.run();
        System.out.println("断言 " + gate.checks + " 失败 " + gate.failures);
        System.out.println("启动证明 否");
        System.out.println("资源检查 读的是源码 JSON，不是游戏语言加载");
        if (gate.failures != 0) {
            System.exit(1);
        }
    }

    private void run() throws IOException {
        checkDamageType();
        checkTags();
        checkLanguage();
    }

    private void checkDamageType() throws IOException {
        Path file = resources.resolve("data/xueximod/damage_type/water_prison_pressure.json");
        check(Files.isRegularFile(file), "缺少伤害类型 " + file);
        if (!Files.isRegularFile(file)) {
            return;
        }
        String body = Files.readString(file, StandardCharsets.UTF_8);
        String messageId = jsonString(body, "message_id");
        check(MESSAGE_ID.equals(messageId), "message_id 不是 " + MESSAGE_ID + " 实际 " + messageId);
        String lowered = body.toLowerCase();
        check(!lowered.contains("void") && !body.contains("虚空") && !lowered.contains("out_of_world")
                        && !lowered.contains("generickill"),
                "伤害类型 JSON 带了虚空或原版击杀来源");
    }

    private void checkTags() throws IOException {
        Path folder = resources.resolve("data/minecraft/tags/damage_type");
        check(Files.isDirectory(folder), "缺少伤害标签目录 " + folder);
        if (!Files.isDirectory(folder)) {
            return;
        }
        String[] required = {
                "bypasses_armor.json",
                "bypasses_effects.json",
                "bypasses_resistance.json",
                "bypasses_enchantments.json",
                "always_hurts_ender_dragons.json"
        };
        for (String fileName : required) {
            Path file = folder.resolve(fileName);
            String body = Files.isRegularFile(file) ? Files.readString(file, StandardCharsets.UTF_8) : "";
            check(body.contains(PRESSURE_ID), fileName + " 没有 " + PRESSURE_ID);
        }
        try (Stream<Path> walk = Files.list(folder)) {
            for (Path file : walk.filter(path -> path.toString().endsWith(".json")).toList()) {
                String fileName = file.getFileName().toString().toLowerCase();
                boolean forbidden = fileName.contains("invulnerability") || fileName.contains("cooldown");
                if (!forbidden) {
                    continue;
                }
                String body = Files.readString(file, StandardCharsets.UTF_8);
                check(!body.contains(PRESSURE_ID), file.getFileName() + " 不应包含 " + PRESSURE_ID);
            }
        }
        Path missingInvulnerability = folder.resolve("bypasses_invulnerability.json");
        Path missingCooldown = folder.resolve("bypasses_cooldown.json");
        if (!Files.isRegularFile(missingInvulnerability)) {
            System.out.println("记录 没有 bypasses_invulnerability.json，水牢不在该标签中");
        }
        if (!Files.isRegularFile(missingCooldown)) {
            System.out.println("记录 没有 bypasses_cooldown.json，水牢不在该标签中");
        }
    }

    private void checkLanguage() throws IOException {
        Path chinese = resources.resolve("assets/xueximod/lang/zh_cn.json");
        Path english = resources.resolve("assets/xueximod/lang/en_us.json");
        check(Files.isRegularFile(chinese), "缺少 zh_cn.json");
        if (!Files.isRegularFile(english)) {
            System.out.println("未覆盖 没有 en_us.json，英文客户端是否显示死亡文案未检查");
        }
        checkLanguageFile(chinese);
        checkLanguageFile(english);
    }

    private void checkLanguageFile(Path file) throws IOException {
        if (!Files.isRegularFile(file)) {
            return;
        }
        String body = Files.readString(file, StandardCharsets.UTF_8);
        List<String> keys = new ArrayList<>();
        keys.add(DEATH_KEY);
        String playerKey = DEATH_KEY + ".player";
        if (body.contains("\"" + playerKey + "\"")) {
            keys.add(playerKey);
        }
        for (String key : keys) {
            String text = jsonString(body, key);
            check(text != null && !text.isBlank(), file.getFileName() + " 缺少 " + key);
            if (text == null) {
                continue;
            }
            String lowered = text.toLowerCase();
            check(!lowered.contains("void") && !text.contains("虚空"), key + " 把水牢压力写成了虚空：" + text);
            check(!text.equals(key), key + " 文案仍是原始翻译键");
        }
    }

    private static String jsonString(String body, String key) {
        String token = "\"" + key + "\"";
        int from = 0;
        while (from < body.length()) {
            int at = body.indexOf(token, from);
            if (at < 0) {
                return null;
            }
            int colon = body.indexOf(':', at + token.length());
            int open = colon < 0 ? -1 : body.indexOf('"', colon + 1);
            if (open < 0) {
                return null;
            }
            StringBuilder text = new StringBuilder();
            boolean escaped = false;
            for (int cursor = open + 1; cursor < body.length(); cursor++) {
                char current = body.charAt(cursor);
                if (escaped) {
                    text.append(current);
                    escaped = false;
                    continue;
                }
                if (current == '\\') {
                    escaped = true;
                    continue;
                }
                if (current == '"') {
                    return text.toString();
                }
                text.append(current);
            }
            from = open + 1;
        }
        return null;
    }

    private void check(boolean passed, String message) {
        checks++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}

package com.bslx.XueXiMod.shu.balance;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 模板和世界文件都只在缺失时生成。已有世界文件只读，不回写玩家的值，也不删未知键。
 */
public final class BalanceDocuments {
    static final String FILE_NAME = "xueximod-balance.json";

    private BalanceDocuments() {
    }

    /** 默认配置目录里的平衡模板。 */
    public static Path templatePath() {
        return FMLPaths.GAMEDIR.get().resolve("defaultconfigs").resolve(FILE_NAME);
    }

    /** 这个世界自己的平衡文件。 */
    public static Path worldPath(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("serverconfig").resolve(FILE_NAME);
    }

    static void writeTemplateIfAbsent() throws IOException {
        Path template = templatePath();
        if (Files.exists(template)) {
            return;
        }
        Files.createDirectories(template.getParent());
        try {
            Files.writeString(template, renderDefaults(), StandardCharsets.UTF_8, StandardOpenOption.CREATE_NEW);
        } catch (java.nio.file.FileAlreadyExistsException ignored) {
            // 另一个启动已经写好模板，保留那一份。
        }
    }

    static String renderDefaults() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", BalanceSchema.CURRENT_VERSION);
        JsonObject global = new JsonObject();
        global.addProperty(BalanceKeys.DAMAGE_MULTIPLIER, 1.0d);
        global.addProperty(BalanceKeys.MANA_MULTIPLIER, 1.0d);
        global.addProperty(BalanceKeys.COOLDOWN_MULTIPLIER, 1.0d);
        root.add("global", global);
        JsonObject techniques = new JsonObject();
        for (TechniqueBalanceSpec spec : BalanceRegistry.specs()) {
            JsonObject row = new JsonObject();
            for (BalanceParameter parameter : spec.parameters().values()) {
                if (!BalanceRegistry.hasFrozenDefault(spec.techniqueId(), parameter.parameterKey())) {
                    continue;
                }
                double amount = BalanceRegistry.frozenDefault(spec.techniqueId(), parameter.parameterKey());
                if (parameter.kind() == BalanceValueKind.INTEGER && BalanceMath.isIntegral(amount)) {
                    row.addProperty(parameter.parameterKey(), (long) amount);
                } else {
                    row.addProperty(parameter.parameterKey(), amount);
                }
            }
            techniques.add(spec.techniqueId(), row);
        }
        root.add("techniques", techniques);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }

    static byte[] encodeAccepted(BalanceSnapshot snapshot) {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", snapshot.schemaVersion());
        root.add("global", numbers(snapshot.acceptedGlobalOverrides()));
        JsonObject techniques = new JsonObject();
        for (Map.Entry<String, Map<String, Double>> techniqueEntry : snapshot.acceptedTechniqueOverrides().entrySet()) {
            techniques.add(techniqueEntry.getKey(), numbers(techniqueEntry.getValue()));
        }
        root.add("techniques", techniques);
        return new GsonBuilder().create().toJson(root).getBytes(StandardCharsets.UTF_8);
    }

    static Parsed parse(String text) {
        JsonElement rootElement = JsonParser.parseString(text);
        if (!rootElement.isJsonObject()) {
            throw new BalanceDocumentException("平衡文件必须是对象");
        }
        JsonObject root = rootElement.getAsJsonObject();
        List<BalanceWarning> warnings = new ArrayList<>();
        if (!root.has("schemaVersion") || !root.get("schemaVersion").isJsonPrimitive()
                || !root.get("schemaVersion").getAsJsonPrimitive().isNumber()) {
            throw new BalanceDocumentException("平衡文件缺少 schemaVersion");
        }
        BalanceSchema.requireSupported(root.get("schemaVersion").getAsInt());
        for (Map.Entry<String, JsonElement> entry : root.entrySet()) {
            String fieldName = entry.getKey();
            if (!"schemaVersion".equals(fieldName) && !"global".equals(fieldName) && !"techniques".equals(fieldName)) {
                warnings.add(BalanceWarning.of(BalanceWarning.Kind.UNKNOWN_PARAMETER, "", fieldName, "未知根字段已忽略"));
            }
        }
        Map<String, Double> globalOverrides = readObject(root.get("global"), "", warnings);
        Map<String, Map<String, Double>> techniqueOverrides = new LinkedHashMap<>();
        if (!root.has("techniques") || !root.get("techniques").isJsonObject()) {
            throw new BalanceDocumentException("平衡文件的 techniques 必须是对象");
        }
        for (Map.Entry<String, JsonElement> techniqueEntry : root.getAsJsonObject("techniques").entrySet()) {
            if (!techniqueEntry.getValue().isJsonObject()) {
                warnings.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, techniqueEntry.getKey(), "",
                        "术式字段不是对象，已忽略"));
                continue;
            }
            techniqueOverrides.put(techniqueEntry.getKey(), readObject(techniqueEntry.getValue(), techniqueEntry.getKey(), warnings));
        }
        return new Parsed(globalOverrides, techniqueOverrides, warnings);
    }

    private static Map<String, Double> readObject(JsonElement element, String techniqueId, List<BalanceWarning> warnings) {
        LinkedHashMap<String, Double> amounts = new LinkedHashMap<>();
        if (element == null || element.isJsonNull()) {
            return amounts;
        }
        if (!element.isJsonObject()) {
            throw new BalanceDocumentException("平衡文件的 global 必须是对象");
        }
        for (Map.Entry<String, JsonElement> entry : element.getAsJsonObject().entrySet()) {
            Double amount = readNumber(entry.getValue());
            if (amount == null) {
                warnings.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, techniqueId, entry.getKey(),
                        "字段不是有限数字，已忽略"));
                continue;
            }
            amounts.put(entry.getKey(), amount);
        }
        return amounts;
    }

    private static Double readNumber(JsonElement element) {
        if (element == null || !element.isJsonPrimitive()) {
            return null;
        }
        JsonPrimitive primitive = element.getAsJsonPrimitive();
        if (!primitive.isNumber()) {
            return null;
        }
        double amount = primitive.getAsDouble();
        if (!Double.isFinite(amount)) {
            return null;
        }
        return amount;
    }

    private static JsonObject numbers(Map<String, Double> amounts) {
        JsonObject row = new JsonObject();
        for (Map.Entry<String, Double> entry : amounts.entrySet()) {
            double amount = entry.getValue();
            if (BalanceMath.isIntegral(amount)) {
                row.addProperty(entry.getKey(), (long) amount);
            } else {
                row.addProperty(entry.getKey(), amount);
            }
        }
        return row;
    }

    static final class Parsed {
        final Map<String, Double> globalOverrides;
        final Map<String, Map<String, Double>> techniqueOverrides;
        final List<BalanceWarning> warnings;

        Parsed(Map<String, Double> globalOverrides, Map<String, Map<String, Double>> techniqueOverrides,
               List<BalanceWarning> warnings) {
            this.globalOverrides = globalOverrides;
            this.techniqueOverrides = techniqueOverrides;
            this.warnings = warnings;
        }
    }
}

package com.bslx.XueXiMod.shu.balance;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 某一次世界加载后的不可变有效值。
 * 术式字段保存全局倍率之前的技术数值；三个全局倍率另存在 {@link #globals()}。
 * 魔力、冷却和伤害倍率都只是配置数值，不扣玩家魔力，也不改水势。
 * 玩家文件里的单个非法字段回退到声明默认，未知编号和未知键只记录警告。
 */
public final class BalanceSnapshot {
    private static final Set<String> GLOBAL_KEYS = Set.of(
            BalanceKeys.DAMAGE_MULTIPLIER,
            BalanceKeys.MANA_MULTIPLIER,
            BalanceKeys.COOLDOWN_MULTIPLIER);

    private static volatile BalanceSnapshot cachedDefaults;
    private static volatile int cachedGeneration = -1;

    private final int schemaVersion;
    private final int registryGeneration;
    private final boolean fromSealedCatalog;
    private final GlobalBalance globals;
    private final Map<String, Map<String, ResolvedRow>> rows;
    private final Map<String, Double> acceptedGlobalOverrides;
    private final Map<String, Map<String, Double>> acceptedTechniqueOverrides;
    private final List<BalanceWarning> warnings;

    private BalanceSnapshot(int registryGeneration, boolean fromSealedCatalog, GlobalBalance globals,
                            Map<String, Map<String, ResolvedRow>> rows,
                            Map<String, Double> acceptedGlobalOverrides,
                            Map<String, Map<String, Double>> acceptedTechniqueOverrides,
                            List<BalanceWarning> warnings) {
        this.schemaVersion = BalanceSchema.CURRENT_VERSION;
        this.registryGeneration = registryGeneration;
        this.fromSealedCatalog = fromSealedCatalog;
        this.globals = globals;
        this.rows = rows;
        this.acceptedGlobalOverrides = acceptedGlobalOverrides;
        this.acceptedTechniqueOverrides = acceptedTechniqueOverrides;
        this.warnings = warnings;
    }

    /** 不带世界覆盖的代码默认快照。封存后按声明代数缓存。 */
    public static BalanceSnapshot defaults() {
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        if (BalanceRegistry.isSealed()) {
            int generation = BalanceRegistry.generation();
            BalanceSnapshot cached = cachedDefaults;
            if (cached != null && cachedGeneration == generation) {
                return cached;
            }
            BalanceSnapshot created = resolve(null, null, null);
            cachedDefaults = created;
            cachedGeneration = generation;
            return created;
        }
        return resolve(null, null, null);
    }

    /** 用全局和术式覆盖做出一份快照。非法字段回退，未知编号和未知键只进警告。 */
    public static BalanceSnapshot resolve(@Nullable Map<String, Double> globalOverrides,
                                          @Nullable Map<String, Map<String, Double>> techniqueOverrides,
                                          @Nullable List<BalanceWarning> warnings) {
        List<BalanceWarning> collected = new ArrayList<>();
        BalanceRegistry.Capture capture = BalanceRegistry.capture();
        LinkedHashMap<String, Double> acceptedGlobals = new LinkedHashMap<>();
        GlobalBalance globals = readGlobals(globalOverrides, acceptedGlobals, collected);
        Map<String, Map<String, Double>> normalized = normalizeTechniques(techniqueOverrides, collected);
        LinkedHashMap<String, Map<String, ResolvedRow>> builtRows = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, Double>> acceptedTechniques = new LinkedHashMap<>();
        for (TechniqueBalanceSpec spec : capture.specs.values()) {
            builtRows.put(spec.techniqueId(), buildRows(capture, spec, normalized.get(spec.techniqueId()),
                    acceptedTechniques, collected));
        }
        for (String techniqueId : normalized.keySet()) {
            if (!builtRows.containsKey(techniqueId)) {
                collected.add(BalanceWarning.of(BalanceWarning.Kind.UNKNOWN_TECHNIQUE, techniqueId, "",
                        "未知术式已忽略，原文件保留"));
            }
        }
        if (warnings != null) {
            warnings.addAll(collected);
        }
        Map<String, Map<String, ResolvedRow>> stableRows = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, ResolvedRow>> entry : builtRows.entrySet()) {
            stableRows.put(entry.getKey(), Collections.unmodifiableMap(entry.getValue()));
        }
        return new BalanceSnapshot(capture.generation, capture.sealed, globals,
                Collections.unmodifiableMap(stableRows),
                Collections.unmodifiableMap(acceptedGlobals),
                deepUnmodifiable(acceptedTechniques),
                List.copyOf(collected));
    }

    /** 这份快照使用的结构版本。 */
    public int schemaVersion() {
        return schemaVersion;
    }

    /** 构建时的本进程声明代数。 */
    public int registryGeneration() {
        return registryGeneration;
    }

    /** 构建时目录是不是已经封存。 */
    public boolean fromSealedCatalog() {
        return fromSealedCatalog;
    }

    /** 三个全局倍率。术式行里的数值还没乘它们。 */
    public GlobalBalance globals() {
        return globals;
    }

    /** 解析时记下的警告。 */
    public List<BalanceWarning> warnings() {
        return warnings;
    }

    /** 实际收下的全局覆盖。回退和未知键不在这里。 */
    public Map<String, Double> acceptedGlobalOverrides() {
        return acceptedGlobalOverrides;
    }

    /** 实际收下的术式覆盖。 */
    public Map<String, Map<String, Double>> acceptedTechniqueOverrides() {
        return acceptedTechniqueOverrides;
    }

    /**
     * 已能确定的技术数值，不含全局倍率。调用方默认值尚未提供的键不会出现。
     */
    public Map<String, Map<String, Double>> effectiveValues() {
        LinkedHashMap<String, Map<String, Double>> exported = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, ResolvedRow>> techniqueEntry : rows.entrySet()) {
            LinkedHashMap<String, Double> amounts = new LinkedHashMap<>();
            for (ResolvedRow row : techniqueEntry.getValue().values()) {
                Double effective = row.acceptedOverride != null ? row.acceptedOverride : row.frozenDefault;
                if (effective != null) {
                    amounts.put(row.parameter.parameterKey(), effective);
                }
            }
            exported.put(techniqueEntry.getKey(), Collections.unmodifiableMap(amounts));
        }
        return Collections.unmodifiableMap(exported);
    }

    @Nullable
    public BalanceParameter parameter(String techniqueId, String parameterKey) {
        ResolvedRow row = find(techniqueId, parameterKey);
        if (row == null) {
            return null;
        }
        return row.parameter;
    }

    /** 这把键是不是用了收下的覆盖值。 */
    public boolean overrideApplied(String techniqueId, String parameterKey) {
        ResolvedRow row = find(techniqueId, parameterKey);
        return row != null && row.acceptedOverride != null;
    }

    double configuredBase(String techniqueId, String parameterKey, boolean callSiteProvided, double callSiteRaw) {
        BalanceKeys.checkDeclarable(parameterKey);
        String canonical = BalanceRegistry.canonicalize(techniqueId);
        ResolvedRow row = find(canonical, parameterKey);
        if (row == null) {
            throw new BalanceDeclarationException("术式 " + canonical + " 未声明参数 " + parameterKey);
        }
        if (row.acceptedOverride != null) {
            if (row.frozenDefault != null) {
                return row.acceptedOverride;
            }
            if (callSiteProvided && !row.parameter.bound().accepts(row.acceptedOverride, callSiteRaw)) {
                return checkedCallSite(row, callSiteRaw);
            }
            if (!callSiteProvided
                    && row.parameter.bound().zeroRequiresMatchingDefault()
                    && Double.compare(row.acceptedOverride, 0.0d) == 0) {
                throw new BalanceValueException("术式 " + canonical + " 的参数 " + parameterKey
                        + " 需要调用方原始默认值复核");
            }
            return row.acceptedOverride;
        }
        if (row.frozenDefault != null) {
            return row.frozenDefault;
        }
        if (!callSiteProvided) {
            throw new BalanceValueException("术式 " + canonical + " 的参数 " + parameterKey
                    + " 使用调用方原始默认值，请改用带 rawDefault 的读取入口");
        }
        return checkedCallSite(row, callSiteRaw);
    }

    private static Map<String, ResolvedRow> buildRows(BalanceRegistry.Capture capture, TechniqueBalanceSpec spec,
                                                      @Nullable Map<String, Double> overrides,
                                                      Map<String, Map<String, Double>> acceptedTechniques,
                                                      List<BalanceWarning> collected) {
        Map<String, Double> frozen = capture.frozenDefaults.getOrDefault(spec.techniqueId(), Map.of());
        LinkedHashMap<String, ResolvedRow> techniqueRows = new LinkedHashMap<>();
        LinkedHashMap<String, Double> accepted = new LinkedHashMap<>();
        for (BalanceParameter parameter : spec.parameters().values()) {
            Double frozenAmount = freezeOne(capture, spec.techniqueId(), parameter, frozen);
            Double acceptedOverride = null;
            if (overrides != null && overrides.containsKey(parameter.parameterKey())) {
                double candidate = overrides.get(parameter.parameterKey());
                if (usableOverride(parameter, candidate, frozenAmount)) {
                    acceptedOverride = candidate;
                    accepted.put(parameter.parameterKey(), candidate);
                    if (frozenAmount == null
                            && parameter.bound().zeroRequiresMatchingDefault()
                            && Double.compare(candidate, 0.0d) == 0) {
                        collected.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, spec.techniqueId(),
                                parameter.parameterKey(),
                                "配置 0 将在读取时对照调用方原始默认值，原始值不是 0 时回退"));
                    }
                } else {
                    collected.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, spec.techniqueId(),
                            parameter.parameterKey(), "非法值 " + candidate + " 已回退该字段默认"));
                }
            }
            techniqueRows.put(parameter.parameterKey(), new ResolvedRow(parameter, frozenAmount, acceptedOverride));
        }
        if (overrides != null) {
            for (String parameterKey : overrides.keySet()) {
                if (techniqueRows.containsKey(parameterKey)) {
                    continue;
                }
                BalanceWarning.Kind kind = BalanceWarning.Kind.UNKNOWN_PARAMETER;
                String detail = "未声明的参数已忽略";
                try {
                    BalanceKeys.checkDeclarable(parameterKey);
                } catch (BalanceDeclarationException exception) {
                    kind = BalanceWarning.Kind.INVALID_VALUE;
                    detail = "参数键不合法，已忽略";
                }
                collected.add(BalanceWarning.of(kind, spec.techniqueId(), parameterKey, detail));
            }
        }
        if (!accepted.isEmpty()) {
            acceptedTechniques.put(spec.techniqueId(), accepted);
        }
        return techniqueRows;
    }

    @Nullable
    private static Double freezeOne(BalanceRegistry.Capture capture, String techniqueId, BalanceParameter parameter,
                                    Map<String, Double> frozen) {
        RawDefault.Mode mode = parameter.rawDefault().mode();
        if (mode == RawDefault.Mode.CALL_SITE) {
            return null;
        }
        if (capture.sealed) {
            Double frozenAmount = frozen.get(parameter.parameterKey());
            if (frozenAmount == null) {
                throw new BalanceDeclarationException("封存目录缺少冻结默认值: " + techniqueId
                        + " " + parameter.parameterKey());
            }
            return frozenAmount;
        }
        if (mode == RawDefault.Mode.LITERAL) {
            return parameter.rawDefault().literalAmount();
        }
        return BalanceRegistry.invokeCalculator(techniqueId, parameter);
    }

    private static boolean usableOverride(BalanceParameter parameter, double candidate, @Nullable Double frozenAmount) {
        if (parameter.kind() == BalanceValueKind.INTEGER && !BalanceMath.isIntegral(candidate)) {
            return false;
        }
        if (frozenAmount != null) {
            return parameter.bound().accepts(candidate, frozenAmount);
        }
        return parameter.bound().containsRange(candidate);
    }

    private static GlobalBalance readGlobals(@Nullable Map<String, Double> globalOverrides,
                                             Map<String, Double> acceptedGlobals,
                                             List<BalanceWarning> collected) {
        double damageMultiplier = GlobalBalance.IDENTITY_MULTIPLIER;
        double manaMultiplier = GlobalBalance.IDENTITY_MULTIPLIER;
        double cooldownMultiplier = GlobalBalance.IDENTITY_MULTIPLIER;
        if (globalOverrides != null) {
            for (Map.Entry<String, Double> entry : globalOverrides.entrySet()) {
                String parameterKey = entry.getKey() == null ? "" : entry.getKey();
                Double amount = entry.getValue();
                if (!GLOBAL_KEYS.contains(parameterKey)) {
                    collected.add(BalanceWarning.of(BalanceWarning.Kind.UNKNOWN_PARAMETER, "", parameterKey,
                            "未知全局参数已忽略"));
                    continue;
                }
                if (amount == null || !BalanceBound.multiplier().containsRange(amount)) {
                    collected.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, "", parameterKey,
                            "非法全局倍率已回退 1"));
                    continue;
                }
                acceptedGlobals.put(parameterKey, amount);
                if (BalanceKeys.DAMAGE_MULTIPLIER.equals(parameterKey)) {
                    damageMultiplier = amount;
                } else if (BalanceKeys.MANA_MULTIPLIER.equals(parameterKey)) {
                    manaMultiplier = amount;
                } else {
                    cooldownMultiplier = amount;
                }
            }
        }
        if (BalanceMath.isOne(damageMultiplier) && BalanceMath.isOne(manaMultiplier)
                && BalanceMath.isOne(cooldownMultiplier)) {
            return GlobalBalance.identity();
        }
        return GlobalBalance.checked(damageMultiplier, manaMultiplier, cooldownMultiplier);
    }

    private static Map<String, Map<String, Double>> normalizeTechniques(
            @Nullable Map<String, Map<String, Double>> techniqueOverrides,
            List<BalanceWarning> collected) {
        LinkedHashMap<String, Map<String, Double>> canonicalEntries = new LinkedHashMap<>();
        LinkedHashMap<String, Map<String, Double>> aliasEntries = new LinkedHashMap<>();
        if (techniqueOverrides == null) {
            return canonicalEntries;
        }
        for (Map.Entry<String, Map<String, Double>> entry : techniqueOverrides.entrySet()) {
            String rawId = entry.getKey();
            if (entry.getValue() == null) {
                collected.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, rawId == null ? "" : rawId, "",
                        "术式对象为空，已忽略"));
                continue;
            }
            String canonical;
            try {
                canonical = BalanceRegistry.canonicalize(rawId);
            } catch (BalanceDeclarationException exception) {
                collected.add(BalanceWarning.of(BalanceWarning.Kind.UNKNOWN_TECHNIQUE, rawId == null ? "" : rawId, "",
                        "术式编号无法识别，已忽略"));
                continue;
            }
            Map<String, Double> copied = copyNumbers(entry.getValue(), canonical, collected);
            if (!canonical.equals(rawId)) {
                if (aliasEntries.containsKey(canonical)) {
                    collected.add(BalanceWarning.of(BalanceWarning.Kind.ALIAS_IGNORED, canonical, "",
                            "重复别名已忽略"));
                    continue;
                }
                aliasEntries.put(canonical, copied);
                continue;
            }
            if (canonicalEntries.containsKey(canonical)) {
                collected.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, canonical, "",
                        "重复的术式对象已忽略"));
                continue;
            }
            canonicalEntries.put(canonical, copied);
        }
        for (Map.Entry<String, Map<String, Double>> entry : aliasEntries.entrySet()) {
            if (canonicalEntries.containsKey(entry.getKey())) {
                collected.add(BalanceWarning.of(BalanceWarning.Kind.ALIAS_IGNORED, entry.getKey(), "",
                        "正式编号优先，已忽略别名对象"));
                continue;
            }
            collected.add(BalanceWarning.of(BalanceWarning.Kind.ALIAS_CANONICALIZED, entry.getKey(), "",
                    "已按正式编号收录别名"));
            canonicalEntries.put(entry.getKey(), entry.getValue());
        }
        return canonicalEntries;
    }

    private static Map<String, Double> copyNumbers(Map<String, Double> source, String techniqueId,
                                                   List<BalanceWarning> collected) {
        LinkedHashMap<String, Double> copied = new LinkedHashMap<>();
        for (Map.Entry<String, Double> entry : source.entrySet()) {
            String parameterKey = entry.getKey() == null ? "" : entry.getKey();
            Double amount = entry.getValue();
            if (entry.getKey() == null || amount == null || !Double.isFinite(amount)) {
                collected.add(BalanceWarning.of(BalanceWarning.Kind.INVALID_VALUE, techniqueId, parameterKey,
                        "参数值不是有限数，已忽略"));
                continue;
            }
            copied.put(parameterKey, amount);
        }
        return copied;
    }

    private static Map<String, Map<String, Double>> deepUnmodifiable(Map<String, Map<String, Double>> source) {
        LinkedHashMap<String, Map<String, Double>> copied = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, Double>> entry : source.entrySet()) {
            copied.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
        }
        return Collections.unmodifiableMap(copied);
    }

    @Nullable
    private ResolvedRow find(String techniqueId, String parameterKey) {
        String canonical;
        try {
            canonical = BalanceRegistry.canonicalize(techniqueId);
        } catch (BalanceDeclarationException exception) {
            return null;
        }
        Map<String, ResolvedRow> techniqueRows = rows.get(canonical);
        if (techniqueRows == null || parameterKey == null) {
            return null;
        }
        return techniqueRows.get(parameterKey);
    }

    private static double checkedCallSite(ResolvedRow row, double callSiteRaw) {
        if (row.parameter.kind() == BalanceValueKind.INTEGER && !BalanceMath.isIntegral(callSiteRaw)) {
            throw new BalanceValueException("调用方原始默认值必须是整数: " + row.parameter.parameterKey());
        }
        if (!row.parameter.bound().accepts(callSiteRaw, callSiteRaw)) {
            throw new BalanceValueException("调用方原始默认值超出边界: " + row.parameter.parameterKey());
        }
        return callSiteRaw;
    }

    private static final class ResolvedRow {
        private final BalanceParameter parameter;
        private final Double frozenDefault;
        private final Double acceptedOverride;

        private ResolvedRow(BalanceParameter parameter, @Nullable Double frozenDefault,
                            @Nullable Double acceptedOverride) {
            this.parameter = parameter;
            this.frozenDefault = frozenDefault;
            this.acceptedOverride = acceptedOverride;
        }
    }
}

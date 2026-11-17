package com.bslx.XueXiMod.shu.balance;

import com.bslx.XueXiMod.shu.TechniqueIds;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * 按正式术式编号登记平衡参数。注册期只累积声明；封存时冻结代码默认值。
 * 世界配置不进这个目录。不同键合并；同一键再次声明直接拒绝，即使默认值相同。
 */
public final class BalanceRegistry {
    private static final BalanceRegistry HANDLE = new BalanceRegistry();
    private static final Object GATE = new Object();
    private static final Pattern TECHNIQUE_ID_PATTERN = Pattern.compile("[a-z0-9_]{3,128}");
    private static final ThreadLocal<Integer> CALCULATOR_DEPTH = ThreadLocal.withInitial(() -> 0);
    private static final Map<String, TechniqueBalanceSpec> SPECS = new LinkedHashMap<>();
    private static final Map<String, Map<String, Double>> FROZEN = new LinkedHashMap<>();

    private static boolean sealed;
    private static int generation;

    private BalanceRegistry() {
    }

    /** 唯一目录。静态声明和 {@link #acceptSpecial} 都写进这一份。 */
    public static BalanceRegistry current() {
        return HANDLE;
    }

    /** 把一个专项参数写进唯一目录。 */
    public void acceptSpecial(String techniqueId, BalanceParameter parameter) {
        declareSpecial(techniqueId, parameter);
    }

    /** 把一份专项声明写进唯一目录。 */
    public void acceptSpecial(TechniqueBalanceSpec spec) {
        declareSpecial(spec);
    }

    /** 收成正式术式编号。长度或字符不符时拒绝。 */
    public static String canonicalize(String techniqueId) {
        if (techniqueId == null || !TECHNIQUE_ID_PATTERN.matcher(techniqueId).matches()) {
            throw new BalanceDeclarationException("术式编号只能包含 3 到 128 位小写字母、数字和下划线");
        }
        String canonical = TechniqueIds.canonicalId(techniqueId);
        if (canonical == null || !TECHNIQUE_ID_PATTERN.matcher(canonical).matches()) {
            throw new BalanceDeclarationException("术式编号无法收成正式编号");
        }
        return canonical;
    }

    /** 声明一个专项参数。同一键再次出现时拒绝。 */
    public static void declareSpecial(String techniqueId, BalanceParameter parameter) {
        declareSpecial(TechniqueBalanceSpec.builder(techniqueId).parameter(parameter).build());
    }

    /** 合并一份专项声明。 */
    public static void declareSpecial(TechniqueBalanceSpec spec) {
        merge(spec, BalanceParameter.Origin.SPECIAL);
    }

    /** 补标准键。已有同一键时由合并规则拒绝，不覆盖。 */
    public static void completeStandard(TechniqueBalanceSpec spec) {
        merge(spec, BalanceParameter.Origin.STANDARD);
    }

    /**
     * 在内置和外部术式都登记完成之后补齐标准键。
     * {@code manaDefault} 为空表示不补 {@code manaCost}，改由已经声明的其他魔力键承担。
     * {@code includeDamageMultiplier} 只给真正会形成伤害贡献的术式打开。
     */
    public static void fillStandard(String techniqueId, RawDefault cooldownDefault,
                                    @Nullable RawDefault manaDefault, boolean includeDamageMultiplier) {
        Objects.requireNonNull(cooldownDefault, "cooldownDefault");
        TechniqueBalanceSpec.Builder builder = TechniqueBalanceSpec.builder(techniqueId)
                .cooldownTicks(cooldownDefault);
        if (manaDefault != null) {
            builder.manaCost(manaDefault);
        }
        if (includeDamageMultiplier) {
            builder.damageMultiplier();
        }
        completeStandard(builder.build());
    }

    public static void seal(Collection<String> registeredTechniqueIds) {
        Objects.requireNonNull(registeredTechniqueIds, "registeredTechniqueIds");
        ensureOutsideDefaultEvaluation();
        synchronized (GATE) {
            if (sealed) {
                throw new BalanceDeclarationException("参数目录已经封存");
            }
            if (registeredTechniqueIds.isEmpty()) {
                throw new BalanceDeclarationException("登记术式集合不能为空");
            }
            LinkedHashSet<String> required = new LinkedHashSet<>();
            for (String techniqueId : registeredTechniqueIds) {
                required.add(canonicalize(techniqueId));
            }
            List<String> problems = new ArrayList<>();
            for (String techniqueId : required) {
                TechniqueBalanceSpec spec = SPECS.get(techniqueId);
                if (spec == null) {
                    problems.add("缺少平衡声明: " + techniqueId);
                    continue;
                }
                boolean cooldownPresent = false;
                boolean manaPresent = false;
                for (BalanceParameter parameter : spec.parameters().values()) {
                    if (BalanceKeys.COOLDOWN_TICKS.equals(parameter.parameterKey())) {
                        cooldownPresent = true;
                    }
                    if (parameter.unit() == BalanceUnit.MANA) {
                        manaPresent = true;
                    }
                    collectProblems(techniqueId, parameter, problems);
                }
                if (!cooldownPresent) {
                    problems.add("缺少 cooldownTicks: " + techniqueId);
                }
                if (!manaPresent) {
                    problems.add("缺少魔力费用键: " + techniqueId);
                }
            }
            for (String techniqueId : SPECS.keySet()) {
                if (!required.contains(techniqueId)) {
                    problems.add("声明了未登记术式: " + techniqueId);
                }
            }
            if (!problems.isEmpty()) {
                throw new BalanceDeclarationException(String.join("；", problems));
            }
            LinkedHashMap<String, Map<String, Double>> frozen = freezeDefaults();
            FROZEN.clear();
            FROZEN.putAll(frozen);
            sealed = true;
            generation++;
        }
    }

    /** 目录是不是已经封存。 */
    public static boolean isSealed() {
        synchronized (GATE) {
            return sealed;
        }
    }

    /** 本进程里的声明代数。不能拿去和另一侧比较合同。 */
    public static int generation() {
        synchronized (GATE) {
            return generation;
        }
    }

    /** 这门术式有没有声明这把键。 */
    public static boolean isDeclared(String techniqueId, String parameterKey) {
        BalanceKeys.checkDeclarable(parameterKey);
        String canonical = canonicalize(techniqueId);
        synchronized (GATE) {
            TechniqueBalanceSpec spec = SPECS.get(canonical);
            return spec != null && spec.parameters().containsKey(parameterKey);
        }
    }

    /** 取一门术式已合并的声明。 */
    @Nullable
    public static TechniqueBalanceSpec spec(String techniqueId) {
        String canonical = canonicalize(techniqueId);
        synchronized (GATE) {
            return SPECS.get(canonical);
        }
    }

    /** 当前全部声明。 */
    public static List<TechniqueBalanceSpec> specs() {
        synchronized (GATE) {
            return List.copyOf(SPECS.values());
        }
    }

    /** 已有声明的正式术式编号。 */
    public static List<String> declaredTechniqueIds() {
        synchronized (GATE) {
            return List.copyOf(SPECS.keySet());
        }
    }

    /** 封存后这把键有没有冻结数字。调用方默认值不算。 */
    public static boolean hasFrozenDefault(String techniqueId, String parameterKey) {
        BalanceKeys.checkDeclarable(parameterKey);
        String canonical = canonicalize(techniqueId);
        synchronized (GATE) {
            if (!sealed) {
                return false;
            }
            Map<String, Double> amounts = FROZEN.get(canonical);
            return amounts != null && amounts.containsKey(parameterKey);
        }
    }

    /** 取封存时冻住的默认数字。未封存或这把键没有数字时拒绝。 */
    public static double frozenDefault(String techniqueId, String parameterKey) {
        BalanceKeys.checkDeclarable(parameterKey);
        String canonical = canonicalize(techniqueId);
        synchronized (GATE) {
            if (!sealed) {
                throw new BalanceDeclarationException("封存后才能导出冻结默认值");
            }
            Map<String, Double> amounts = FROZEN.get(canonical);
            if (amounts == null || !amounts.containsKey(parameterKey)) {
                throw new BalanceValueException("术式 " + canonical + " 的参数 " + parameterKey
                        + " 使用调用方原始默认值，没有冻结数字");
            }
            return amounts.get(parameterKey);
        }
    }

    /**
     * 封存后的声明文本。服务端和客户端比较这段文本，不一致就不要安装对方的快照。
     * 代数 {@link #generation()} 只在本进程内使用，不能当合同。
     */
    public static String contractText() {
        synchronized (GATE) {
            if (!sealed) {
                throw new BalanceDeclarationException("封存后才能导出声明合同");
            }
            StringBuilder builder = new StringBuilder();
            for (TechniqueBalanceSpec spec : SPECS.values()) {
                builder.append(spec.techniqueId()).append('\n');
                Map<String, Double> amounts = FROZEN.getOrDefault(spec.techniqueId(), Map.of());
                for (BalanceParameter parameter : spec.parameters().values()) {
                    BalanceBound bound = parameter.bound();
                    builder.append(parameter.parameterKey())
                            .append('|').append(parameter.kind())
                            .append('|').append(parameter.unit())
                            .append('|').append(bound.lowerExclusive() ? "open" : "closed")
                            .append('|').append(Double.toString(bound.lowerInclusive()))
                            .append('|').append(Double.toString(bound.upperInclusive()))
                            .append('|').append(bound.zeroRequiresMatchingDefault())
                            .append('|').append(parameter.rawDefault().mode());
                    if (amounts.containsKey(parameter.parameterKey())) {
                        builder.append('|').append(Double.toString(amounts.get(parameter.parameterKey())));
                    }
                    builder.append('\n');
                }
            }
            return builder.toString();
        }
    }

    static Capture capture() {
        ensureOutsideDefaultEvaluation();
        synchronized (GATE) {
            Map<String, TechniqueBalanceSpec> specCopy = new LinkedHashMap<>(SPECS);
            Map<String, Map<String, Double>> frozenCopy = new LinkedHashMap<>();
            for (Map.Entry<String, Map<String, Double>> entry : FROZEN.entrySet()) {
                frozenCopy.put(entry.getKey(), Collections.unmodifiableMap(new LinkedHashMap<>(entry.getValue())));
            }
            return new Capture(sealed, generation, Collections.unmodifiableMap(specCopy),
                    Collections.unmodifiableMap(frozenCopy));
        }
    }

    static void ensureOutsideDefaultEvaluation() {
        if (CALCULATOR_DEPTH.get() > 0) {
            throw new BalanceDeclarationException("原始默认计算器不得读取有效配置或改写参数目录");
        }
    }

    static double invokeCalculator(String techniqueId, BalanceParameter parameter) {
        if (parameter.rawDefault().mode() != RawDefault.Mode.CALCULATOR) {
            throw new BalanceDeclarationException("参数没有原始默认计算器: " + parameter.parameterKey());
        }
        if (CALCULATOR_DEPTH.get() > 0) {
            throw new BalanceDeclarationException("原始默认计算器不得嵌套求值");
        }
        CALCULATOR_DEPTH.set(1);
        try {
            double amount = parameter.rawDefault().calculator().rawDefault(techniqueId);
            validateFrozenAmount(techniqueId, parameter, amount);
            return amount;
        } catch (BalanceException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new BalanceDeclarationException("术式 " + techniqueId + " 的参数 " + parameter.parameterKey()
                    + " 原始默认计算器失败: " + exception.getMessage(), exception);
        } finally {
            CALCULATOR_DEPTH.set(0);
        }
    }

    private static void merge(TechniqueBalanceSpec spec, BalanceParameter.Origin origin) {
        Objects.requireNonNull(spec, "spec");
        ensureOutsideDefaultEvaluation();
        synchronized (GATE) {
            if (sealed) {
                throw new BalanceDeclarationException("参数目录已封存，不能再声明");
            }
            String canonical = spec.techniqueId();
            LinkedHashMap<String, BalanceParameter> updated = new LinkedHashMap<>();
            TechniqueBalanceSpec existing = SPECS.get(canonical);
            if (existing != null) {
                updated.putAll(existing.parameters());
            }
            for (BalanceParameter incoming : spec.parameters().values()) {
                BalanceParameter stamped = incoming.withOrigin(origin);
                BalanceParameter previous = updated.get(stamped.parameterKey());
                if (previous == null) {
                    updated.put(stamped.parameterKey(), stamped);
                    continue;
                }
                throw new BalanceConflictException(canonical, stamped.parameterKey(), "同一参数不能重复声明");
            }
            SPECS.put(canonical, new TechniqueBalanceSpec(canonical, updated));
            generation++;
        }
    }

    private static void collectProblems(String techniqueId, BalanceParameter parameter, List<String> problems) {
        if (BalanceKeys.DAMAGE_MULTIPLIER.equals(parameter.parameterKey())
                && parameter.rawDefault().mode() == RawDefault.Mode.CALL_SITE) {
            problems.add(techniqueId + " 的 damageMultiplier 不能使用调用方默认值");
        }
        if (parameter.rawDefault().mode() == RawDefault.Mode.CALL_SITE
                && parameter.unit() != BalanceUnit.MANA
                && !BalanceKeys.COOLDOWN_TICKS.equals(parameter.parameterKey())) {
            problems.add(techniqueId + " 的参数 " + parameter.parameterKey() + " 不能使用调用方默认值");
        }
    }

    private static LinkedHashMap<String, Map<String, Double>> freezeDefaults() {
        LinkedHashMap<String, Map<String, Double>> frozen = new LinkedHashMap<>();
        for (TechniqueBalanceSpec spec : SPECS.values()) {
            LinkedHashMap<String, Double> amounts = new LinkedHashMap<>();
            for (BalanceParameter parameter : spec.parameters().values()) {
                RawDefault.Mode mode = parameter.rawDefault().mode();
                if (mode == RawDefault.Mode.CALL_SITE) {
                    continue;
                }
                double amount = mode == RawDefault.Mode.LITERAL
                        ? parameter.rawDefault().literalAmount()
                        : invokeCalculator(spec.techniqueId(), parameter);
                if (mode == RawDefault.Mode.LITERAL) {
                    validateFrozenAmount(spec.techniqueId(), parameter, amount);
                }
                amounts.put(parameter.parameterKey(), amount);
            }
            frozen.put(spec.techniqueId(), amounts);
        }
        return frozen;
    }

    private static void validateFrozenAmount(String techniqueId, BalanceParameter parameter, double amount) {
        if (parameter.kind() == BalanceValueKind.INTEGER && !BalanceMath.isIntegral(amount)) {
            throw new BalanceDeclarationException("术式 " + techniqueId + " 的参数 " + parameter.parameterKey()
                    + " 冻结默认值必须是整数");
        }
        if (!parameter.bound().accepts(amount, amount)) {
            throw new BalanceDeclarationException("术式 " + techniqueId + " 的参数 " + parameter.parameterKey()
                    + " 冻结默认值超出边界: " + amount);
        }
    }

    static final class Capture {
        final boolean sealed;
        final int generation;
        final Map<String, TechniqueBalanceSpec> specs;
        final Map<String, Map<String, Double>> frozenDefaults;

        private Capture(boolean sealed, int generation, Map<String, TechniqueBalanceSpec> specs,
                        Map<String, Map<String, Double>> frozenDefaults) {
            this.sealed = sealed;
            this.generation = generation;
            this.specs = specs;
            this.frozenDefaults = frozenDefaults;
        }
    }
}

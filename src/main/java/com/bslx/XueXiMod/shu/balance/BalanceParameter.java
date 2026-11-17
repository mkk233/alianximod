package com.bslx.XueXiMod.shu.balance;

import java.util.Objects;

/**
 * 一个术式参数的声明。注册期只保存原始默认来源和边界，不保存世界配置。
 */
public final class BalanceParameter {
    public enum Origin {
        SPECIAL,
        STANDARD
    }

    private final String parameterKey;
    private final BalanceValueKind kind;
    private final RawDefault rawDefault;
    private final BalanceBound bound;
    private final BalanceUnit unit;
    private final String description;
    private final Origin origin;

    private BalanceParameter(String parameterKey, BalanceValueKind kind, RawDefault rawDefault,
                             BalanceBound bound, BalanceUnit unit, String description, Origin origin) {
        this.parameterKey = parameterKey;
        this.kind = kind;
        this.rawDefault = rawDefault;
        this.bound = bound;
        this.unit = unit;
        this.description = description;
        this.origin = origin;
    }

    /** 声明一个专项参数。 */
    public static BalanceParameter of(BalanceValueKind kind, String parameterKey, RawDefault rawDefault,
                                      BalanceBound bound, BalanceUnit unit, String description) {
        return build(kind, parameterKey, rawDefault, bound, unit, description, Origin.SPECIAL);
    }

    /** 伤害倍率。代码默认值固定是 1。 */
    public static BalanceParameter damageMultiplier() {
        return build(BalanceValueKind.NUMBER, BalanceKeys.DAMAGE_MULTIPLIER, RawDefault.literal(1.0d),
                BalanceBound.multiplier(), BalanceUnit.MULTIPLIER,
                "伤害贡献倍率。默认 1，不替换术式结构底伤。", Origin.SPECIAL);
    }

    /** 标准魔力费用。 */
    public static BalanceParameter manaCost(RawDefault rawDefault) {
        return manaCost(BalanceKeys.MANA_COST, rawDefault, "基础或单位魔力。全局魔力倍率在读取时再乘一次。");
    }

    /** 另一把整数魔力键，说明由调用方给出。 */
    public static BalanceParameter manaCost(String parameterKey, RawDefault rawDefault, String description) {
        return build(BalanceValueKind.INTEGER, parameterKey, rawDefault, BalanceBound.mana(),
                BalanceUnit.MANA, description, Origin.SPECIAL);
    }

    /** 基础冷却，单位是刻。 */
    public static BalanceParameter cooldownTicks(RawDefault rawDefault) {
        return build(BalanceValueKind.INTEGER, BalanceKeys.COOLDOWN_TICKS, rawDefault, BalanceBound.cooldownTicks(),
                BalanceUnit.TICKS, "基础冷却，单位为刻。全局冷却倍率在读取时再乘一次。", Origin.SPECIAL);
    }

    private static BalanceParameter build(BalanceValueKind kind, String parameterKey, RawDefault rawDefault,
                                          BalanceBound bound, BalanceUnit unit, String description, Origin origin) {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(rawDefault, "rawDefault");
        Objects.requireNonNull(bound, "bound");
        Objects.requireNonNull(unit, "unit");
        Objects.requireNonNull(origin, "origin");
        BalanceKeys.checkDeclarable(parameterKey);
        if (description == null || description.isBlank() || description.trim().length() > 200) {
            throw new BalanceDeclarationException("参数说明不能为空，且不超过 200 字: " + parameterKey);
        }
        if (rawDefault.mode() == RawDefault.Mode.LITERAL) {
            double amount = rawDefault.literalAmount();
            if (kind == BalanceValueKind.INTEGER && !BalanceMath.isIntegral(amount)) {
                throw new BalanceDeclarationException("整数参数的原始默认值必须是整数: " + parameterKey);
            }
            if (!bound.accepts(amount, amount)) {
                throw new BalanceDeclarationException("原始默认值超出边界: " + parameterKey + " = " + amount);
            }
        }
        if (rawDefault.mode() == RawDefault.Mode.CALL_SITE
                && unit != BalanceUnit.MANA
                && !BalanceKeys.COOLDOWN_TICKS.equals(parameterKey)) {
            throw new BalanceDeclarationException("只有魔力键和 cooldownTicks 可以使用调用方原始默认值: " + parameterKey);
        }
        if (BalanceKeys.DAMAGE_MULTIPLIER.equals(parameterKey)
                && (kind != BalanceValueKind.NUMBER || unit != BalanceUnit.MULTIPLIER
                || rawDefault.mode() != RawDefault.Mode.LITERAL
                || Double.compare(rawDefault.literalAmount(), 1.0d) != 0)) {
            throw new BalanceDeclarationException("damageMultiplier 的代码默认值必须是字面量 1");
        }
        if (BalanceKeys.COOLDOWN_TICKS.equals(parameterKey)
                && (kind != BalanceValueKind.INTEGER || unit != BalanceUnit.TICKS)) {
            throw new BalanceDeclarationException("cooldownTicks 必须是整数刻");
        }
        if (unit == BalanceUnit.MANA && kind != BalanceValueKind.INTEGER) {
            throw new BalanceDeclarationException("魔力参数必须是整数: " + parameterKey);
        }
        return new BalanceParameter(parameterKey, kind, rawDefault, bound, unit, description.trim(), origin);
    }

    BalanceParameter withOrigin(Origin origin) {
        Objects.requireNonNull(origin, "origin");
        if (this.origin == origin) {
            return this;
        }
        return new BalanceParameter(parameterKey, kind, rawDefault, bound, unit, description, origin);
    }

    public String parameterKey() {
        return parameterKey;
    }

    public BalanceValueKind kind() {
        return kind;
    }

    public RawDefault rawDefault() {
        return rawDefault;
    }

    public BalanceBound bound() {
        return bound;
    }

    public BalanceUnit unit() {
        return unit;
    }

    public String description() {
        return description;
    }

    public Origin origin() {
        return origin;
    }

    /** 键、种类、单位和边界相同。 */
    boolean sameShape(BalanceParameter other) {
        return parameterKey.equals(other.parameterKey)
                && kind == other.kind
                && unit == other.unit
                && bound.sameRange(other.bound);
    }

    /** 形状相同，原始默认来源也相同。 */
    boolean sameContract(BalanceParameter other) {
        return sameShape(other) && rawDefault.sameSource(other.rawDefault);
    }

    /** 来源和形状相同，而且两边都用计算器提供默认值。 */
    boolean calculatorRefill(BalanceParameter incoming) {
        return origin == incoming.origin
                && rawDefault.mode() == RawDefault.Mode.CALCULATOR
                && incoming.rawDefault.mode() == RawDefault.Mode.CALCULATOR
                && sameShape(incoming);
    }

    /** 把已有声明和新声明的来源、种类、单位、边界和默认来源写成一段差异。 */
    String describeDifference(BalanceParameter incoming) {
        return "已有 " + origin + " " + kind + " " + unit + " " + bound + " " + rawDefault
                + "；新的 " + incoming.origin + " " + incoming.kind + " " + incoming.unit + " "
                + incoming.bound + " " + incoming.rawDefault;
    }
}

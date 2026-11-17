package com.bslx.XueXiMod.shu.balance;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 一个术式已经合并的平衡声明。不含连锁角色、复制份数或其他结构属性。
 * 术式编号会收成 {@code TechniqueIds.canonicalId}。
 */
public final class TechniqueBalanceSpec {
    private final String techniqueId;
    private final Map<String, BalanceParameter> parameters;

    TechniqueBalanceSpec(String techniqueId, Map<String, BalanceParameter> parameters) {
        this.techniqueId = techniqueId;
        this.parameters = Collections.unmodifiableMap(new LinkedHashMap<>(parameters));
    }

    /** 开始为一门术式收集参数。 */
    public static Builder builder(String techniqueId) {
        return new Builder(techniqueId);
    }

    /** 这门术式的正式编号。 */
    public String techniqueId() {
        return techniqueId;
    }

    /** 已合并的参数。 */
    public Map<String, BalanceParameter> parameters() {
        return parameters;
    }

    /** 按键取一个参数。没有时为空。 */
    public BalanceParameter parameter(String parameterKey) {
        return parameters.get(parameterKey);
    }

    public static final class Builder {
        private final String techniqueId;
        private final LinkedHashMap<String, BalanceParameter> parameters = new LinkedHashMap<>();

        private Builder(String techniqueId) {
            this.techniqueId = BalanceRegistry.canonicalize(techniqueId);
        }

        /** 放进一个参数。同一把键出现两次直接拒绝。 */
        public Builder parameter(BalanceParameter parameter) {
            Objects.requireNonNull(parameter, "parameter");
            if (parameters.containsKey(parameter.parameterKey())) {
                throw new BalanceConflictException(techniqueId, parameter.parameterKey(), "同一声明里出现了两次");
            }
            parameters.put(parameter.parameterKey(), parameter);
            return this;
        }

        /** 加伤害倍率。 */
        public Builder damageMultiplier() {
            return parameter(BalanceParameter.damageMultiplier());
        }

        /** 加标准魔力费用。 */
        public Builder manaCost(RawDefault rawDefault) {
            return parameter(BalanceParameter.manaCost(rawDefault));
        }

        /** 加另一把整数魔力费用。 */
        public Builder manaFee(String parameterKey, RawDefault rawDefault, String description) {
            return parameter(BalanceParameter.manaCost(parameterKey, rawDefault, description));
        }

        /** 加基础冷却。 */
        public Builder cooldownTicks(RawDefault rawDefault) {
            return parameter(BalanceParameter.cooldownTicks(rawDefault));
        }

        /** 加一个小数专项参数。 */
        public Builder numberParameter(String parameterKey, RawDefault rawDefault, BalanceBound bound,
                                       BalanceUnit unit, String description) {
            return parameter(BalanceParameter.of(BalanceValueKind.NUMBER, parameterKey, rawDefault, bound, unit, description));
        }

        /** 加一个整数专项参数。 */
        public Builder integerParameter(String parameterKey, RawDefault rawDefault, BalanceBound bound,
                                        BalanceUnit unit, String description) {
            return parameter(BalanceParameter.of(BalanceValueKind.INTEGER, parameterKey, rawDefault, bound, unit, description));
        }

        /** 收成声明。一个参数都没有时拒绝。 */
        public TechniqueBalanceSpec build() {
            if (parameters.isEmpty()) {
                throw new BalanceDeclarationException("术式 " + techniqueId + " 的平衡声明至少包含一个参数");
            }
            return new TechniqueBalanceSpec(techniqueId, parameters);
        }
    }
}

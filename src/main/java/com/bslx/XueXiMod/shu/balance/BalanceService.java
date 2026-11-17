package com.bslx.XueXiMod.shu.balance;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 当前侧的平衡读取入口。
 * 类加载不读取配置，也不把快照写进静态常量。
 * 尚未安装时，已封存目录返回代码默认快照；两侧都已安装时，未绑定线程不能猜测。
 * 集成服务器断线只 {@link #clear(BalanceSide)} 客户端，并保持该线程绑定在客户端。
 */
public final class BalanceService {
    private static final AtomicReference<BalanceSnapshot> SERVER_SNAPSHOT = new AtomicReference<>();
    private static final AtomicReference<BalanceSnapshot> CLIENT_SNAPSHOT = new AtomicReference<>();
    private static final ThreadLocal<BalanceSide> BOUND_SIDE = new ThreadLocal<>();

    private BalanceService() {
    }

    /** 装入某一侧的快照。目录未封存、快照不是封存后构建，或代数不同，都拒绝。 */
    public static void install(BalanceSide side, BalanceSnapshot snapshot) {
        Objects.requireNonNull(side, "side");
        Objects.requireNonNull(snapshot, "snapshot");
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        if (!BalanceRegistry.isSealed() || !snapshot.fromSealedCatalog()
                || snapshot.registryGeneration() != BalanceRegistry.generation()) {
            throw new BalanceDeclarationException("快照必须在参数目录封存之后、用同一代声明构建");
        }
        reference(side).set(snapshot);
    }

    /** 去掉这一侧已安装的快照。 */
    public static void clear(BalanceSide side) {
        Objects.requireNonNull(side, "side");
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        reference(side).set(null);
    }

    /** 这一侧已经装入的快照。还没装时为空。 */
    @Nullable
    public static BalanceSnapshot installed(BalanceSide side) {
        Objects.requireNonNull(side, "side");
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        return reference(side).get();
    }

    /** 已安装的快照；这一侧还没装时退回代码默认快照。 */
    public static BalanceSnapshot view(BalanceSide side) {
        Objects.requireNonNull(side, "side");
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        BalanceSnapshot installed = reference(side).get();
        if (installed != null) {
            return installed;
        }
        return BalanceSnapshot.defaults();
    }

    /** 把当前线程绑到某一侧。 */
    public static void bind(BalanceSide side) {
        Objects.requireNonNull(side, "side");
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        BOUND_SIDE.set(side);
    }

    /** 解开当前线程的侧绑定。 */
    public static void unbind() {
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        BOUND_SIDE.remove();
    }

    /** 当前线程绑的是哪一侧。没绑时为空。 */
    @Nullable
    public static BalanceSide boundSide() {
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        return BOUND_SIDE.get();
    }

    /** 当前线程所属侧的快照。两侧都已安装但线程没绑定时拒绝猜测。 */
    public static BalanceSnapshot current() {
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        BalanceSide side = BOUND_SIDE.get();
        if (side != null) {
            return view(side);
        }
        if (SERVER_SNAPSHOT.get() == null && CLIENT_SNAPSHOT.get() == null) {
            return BalanceSnapshot.defaults();
        }
        throw new BalanceSideException("当前线程未绑定平衡侧，不能在已安装的服务端与客户端快照之间猜测");
    }

    /** 目录里有没有声明这把键。 */
    public static boolean isDeclared(String techniqueId, String parameterKey) {
        BalanceRegistry.ensureOutsideDefaultEvaluation();
        return BalanceRegistry.isDeclared(techniqueId, parameterKey);
    }

    /** 原始伤害先乘术式倍率，再乘一次全局伤害倍率。 */
    public static ConfiguredDamage damageContribution(String techniqueId, double rawAmount) {
        return damageContribution(current(), techniqueId, rawAmount);
    }

    /** 指定侧上的术式伤害贡献。 */
    public static ConfiguredDamage damageContribution(BalanceSide side, String techniqueId, double rawAmount) {
        return damageContribution(view(side), techniqueId, rawAmount);
    }

    /** 用原始伤害对象计算当前侧的术式伤害贡献。 */
    public static ConfiguredDamage damageContribution(String techniqueId, RawDamage rawDamage) {
        Objects.requireNonNull(rawDamage, "rawDamage");
        return damageContribution(current(), techniqueId, rawDamage.amount());
    }

    /** 指定侧上、来自原始伤害对象的术式伤害贡献。 */
    public static ConfiguredDamage damageContribution(BalanceSide side, String techniqueId, RawDamage rawDamage) {
        Objects.requireNonNull(rawDamage, "rawDamage");
        return damageContribution(view(side), techniqueId, rawDamage.amount());
    }

    /** 只乘全局伤害一次。给没有术式倍率的新贡献用，例如无潮印武器印记。 */
    public static ConfiguredDamage globalDamageContribution(double rawAmount) {
        return globalDamageContribution(current(), rawAmount);
    }

    public static ConfiguredDamage globalDamageContribution(BalanceSide side, double rawAmount) {
        return globalDamageContribution(view(side), rawAmount);
    }

    /** 当前侧的整数魔力。先取技术值，再乘一次全局魔力倍率。 */
    public static int manaCost(String techniqueId, String parameterKey, int rawDefault) {
        return manaCost(current(), techniqueId, parameterKey, rawDefault);
    }

    /** 指定侧的整数魔力。 */
    public static int manaCost(BalanceSide side, String techniqueId, String parameterKey, int rawDefault) {
        return manaCost(view(side), techniqueId, parameterKey, rawDefault);
    }

    /** 当前侧的冷却刻数。先取技术值，再乘一次全局冷却倍率。 */
    public static int cooldownTicks(String techniqueId, int rawDefault) {
        return cooldownTicks(current(), techniqueId, rawDefault);
    }

    /** 指定侧的冷却刻数。 */
    public static int cooldownTicks(BalanceSide side, String techniqueId, int rawDefault) {
        return cooldownTicks(view(side), techniqueId, rawDefault);
    }

    /** 当前侧的小数技术值，不乘全局倍率。 */
    public static double number(String techniqueId, String parameterKey) {
        return number(current(), techniqueId, parameterKey);
    }

    /** 指定侧的小数技术值。 */
    public static double number(BalanceSide side, String techniqueId, String parameterKey) {
        return number(view(side), techniqueId, parameterKey);
    }

    /** 当前侧的整数技术值，不乘全局倍率。 */
    public static int intValue(String techniqueId, String parameterKey) {
        return intValue(current(), techniqueId, parameterKey);
    }

    /** 指定侧的整数技术值。 */
    public static int intValue(BalanceSide side, String techniqueId, String parameterKey) {
        return intValue(view(side), techniqueId, parameterKey);
    }

    private static ConfiguredDamage damageContribution(BalanceSnapshot snapshot, String techniqueId, double rawAmount) {
        String canonical = BalanceRegistry.canonicalize(techniqueId);
        BalanceParameter parameter = requireParameter(snapshot, canonical, BalanceKeys.DAMAGE_MULTIPLIER);
        if (parameter.kind() != BalanceValueKind.NUMBER || parameter.unit() != BalanceUnit.MULTIPLIER) {
            throw new BalanceDeclarationException("damageMultiplier 必须是倍率数值");
        }
        double techniqueMultiplier = snapshot.configuredBase(canonical, BalanceKeys.DAMAGE_MULTIPLIER, false, 0.0d);
        double globalMultiplier = snapshot.globals().damageMultiplier();
        float narrowed = BalanceMath.scaleDamage(rawAmount, techniqueMultiplier, globalMultiplier);
        if (BalanceMath.isOne(techniqueMultiplier) && BalanceMath.isOne(globalMultiplier)) {
            return ConfiguredDamage.preserve(narrowed);
        }
        return ConfiguredDamage.scaled(narrowed);
    }

    private static ConfiguredDamage globalDamageContribution(BalanceSnapshot snapshot, double rawAmount) {
        double globalMultiplier = snapshot.globals().damageMultiplier();
        float narrowed = BalanceMath.scaleDamage(rawAmount, 1.0d, globalMultiplier);
        if (BalanceMath.isOne(globalMultiplier)) {
            return ConfiguredDamage.preserve(narrowed);
        }
        return ConfiguredDamage.scaled(narrowed);
    }

    private static int manaCost(BalanceSnapshot snapshot, String techniqueId, String parameterKey, int rawDefault) {
        if (rawDefault < 0) {
            throw new BalanceValueException("原始魔力不能为负");
        }
        String canonical = BalanceRegistry.canonicalize(techniqueId);
        BalanceKeys.checkDeclarable(parameterKey);
        BalanceParameter parameter = requireParameter(snapshot, canonical, parameterKey);
        if (parameter.kind() != BalanceValueKind.INTEGER || parameter.unit() != BalanceUnit.MANA) {
            throw new BalanceDeclarationException("魔力键必须是整数魔力: " + parameterKey);
        }
        double base = snapshot.configuredBase(canonical, parameterKey, true, rawDefault);
        return BalanceMath.scaleMana(BalanceMath.toIntExact(base), snapshot.globals().manaMultiplier());
    }

    private static int cooldownTicks(BalanceSnapshot snapshot, String techniqueId, int rawDefault) {
        if (rawDefault < 0) {
            throw new BalanceValueException("原始冷却不能为负");
        }
        String canonical = BalanceRegistry.canonicalize(techniqueId);
        BalanceParameter parameter = requireParameter(snapshot, canonical, BalanceKeys.COOLDOWN_TICKS);
        if (parameter.kind() != BalanceValueKind.INTEGER || parameter.unit() != BalanceUnit.TICKS) {
            throw new BalanceDeclarationException("cooldownTicks 必须是整数刻");
        }
        double base = snapshot.configuredBase(canonical, BalanceKeys.COOLDOWN_TICKS, true, rawDefault);
        return BalanceMath.scaleCooldown(BalanceMath.toIntExact(base), snapshot.globals().cooldownMultiplier());
    }

    private static double number(BalanceSnapshot snapshot, String techniqueId, String parameterKey) {
        String canonical = BalanceRegistry.canonicalize(techniqueId);
        BalanceKeys.checkDeclarable(parameterKey);
        BalanceParameter parameter = requireParameter(snapshot, canonical, parameterKey);
        rejectScaledKey(parameter);
        if (parameter.kind() != BalanceValueKind.NUMBER) {
            throw new BalanceValueException("键 " + parameterKey + " 是整数，请使用 intValue");
        }
        return snapshot.configuredBase(canonical, parameterKey, false, 0.0d);
    }

    private static int intValue(BalanceSnapshot snapshot, String techniqueId, String parameterKey) {
        String canonical = BalanceRegistry.canonicalize(techniqueId);
        BalanceKeys.checkDeclarable(parameterKey);
        BalanceParameter parameter = requireParameter(snapshot, canonical, parameterKey);
        rejectScaledKey(parameter);
        if (parameter.kind() != BalanceValueKind.INTEGER) {
            throw new BalanceValueException("键 " + parameterKey + " 是小数，请使用 number");
        }
        return BalanceMath.toIntExact(snapshot.configuredBase(canonical, parameterKey, false, 0.0d));
    }

    private static void rejectScaledKey(BalanceParameter parameter) {
        if (BalanceKeys.COOLDOWN_TICKS.equals(parameter.parameterKey()) || parameter.unit() == BalanceUnit.MANA) {
            throw new BalanceValueException("费用和冷却请使用 manaCost 或 cooldownTicks，避免漏乘全局倍率");
        }
    }

    private static BalanceParameter requireParameter(BalanceSnapshot snapshot, String techniqueId, String parameterKey) {
        BalanceParameter parameter = snapshot.parameter(techniqueId, parameterKey);
        if (parameter == null) {
            throw new BalanceDeclarationException("术式 " + techniqueId + " 未声明参数 " + parameterKey);
        }
        return parameter;
    }

    private static AtomicReference<BalanceSnapshot> reference(BalanceSide side) {
        if (side == BalanceSide.SERVER) {
            return SERVER_SNAPSHOT;
        }
        return CLIENT_SNAPSHOT;
    }
}

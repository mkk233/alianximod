package com.bslx.XueXiMod.shu.technique.release.cooperative;

import com.bslx.XueXiMod.shu.technique.release.cooperative.api.ReleaseGateKind;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * 术式执行能力。旧路径使用 {@link #none()}，不改变原来的释放。
 * 一项术式只能声明一种能力。横劈即使会登记演员适配，定义上仍是 none，无新前缀时走原路径。
 * 协同规则在链角色落地前靠 {@link Kind#COOPERATIVE_RULE} 识别，整合时再同时登记新的链角色。
 */
public final class TechniqueExecutionCapabilities {
    public enum Kind {
        TIMED_RELEASE,
        STANDALONE_DEPLOYMENT,
        EXPLICIT_ATTACK_ADAPTER,
        COOPERATIVE_RULE
    }

    private static final TechniqueExecutionCapabilities NONE = new TechniqueExecutionCapabilities(Set.of(), null);

    private final Set<Kind> kinds;
    private final @Nullable ReleaseGateKind actionGate;

    private TechniqueExecutionCapabilities(Set<Kind> kinds, @Nullable ReleaseGateKind actionGate) {
        this.kinds = kinds;
        this.actionGate = actionGate;
    }

    public static TechniqueExecutionCapabilities none() {
        return NONE;
    }

    public static TechniqueExecutionCapabilities timedRelease() {
        return of(Set.of(Kind.TIMED_RELEASE), ReleaseGateKind.TIMED);
    }

    public static TechniqueExecutionCapabilities standaloneDeployment() {
        return of(Set.of(Kind.STANDALONE_DEPLOYMENT), null);
    }

    public static TechniqueExecutionCapabilities explicitImmediateAttack() {
        return of(Set.of(Kind.EXPLICIT_ATTACK_ADAPTER), ReleaseGateKind.IMMEDIATE);
    }

    public static TechniqueExecutionCapabilities cooperativeRule() {
        return of(Set.of(Kind.COOPERATIVE_RULE), null);
    }

    public static TechniqueExecutionCapabilities of(Set<Kind> kinds, @Nullable ReleaseGateKind actionGate) {
        Set<Kind> copied = kinds == null || kinds.isEmpty() ? Set.of() : Set.copyOf(kinds);
        if (copied.isEmpty()) {
            if (actionGate != null) {
                throw new IllegalArgumentException("没有能力时不能声明动作门");
            }
            return NONE;
        }
        if (copied.size() != 1) {
            throw new IllegalArgumentException("一项术式只能声明一种执行能力");
        }
        Kind onlyKind = copied.iterator().next();
        if (onlyKind == Kind.TIMED_RELEASE) {
            if (actionGate != ReleaseGateKind.TIMED) {
                throw new IllegalArgumentException("定时释放的动作门必须是 TIMED");
            }
        } else if (onlyKind == Kind.EXPLICIT_ATTACK_ADAPTER) {
            if (actionGate != ReleaseGateKind.IMMEDIATE) {
                throw new IllegalArgumentException("显式攻击适配的动作门必须是 IMMEDIATE");
            }
        } else if (actionGate != null) {
            throw new IllegalArgumentException("这项能力不声明动作门");
        }
        return new TechniqueExecutionCapabilities(copied, actionGate);
    }

    public Set<Kind> kinds() {
        return kinds;
    }

    public boolean has(Kind kind) {
        return kind != null && kinds.contains(kind);
    }

    public @Nullable ReleaseGateKind actionGate() {
        return actionGate;
    }

    public boolean isTimedRelease() {
        return has(Kind.TIMED_RELEASE);
    }

    public boolean isStandaloneDeployment() {
        return has(Kind.STANDALONE_DEPLOYMENT);
    }

    public boolean isExplicitAttackAdapter() {
        return has(Kind.EXPLICIT_ATTACK_ADAPTER);
    }

    public boolean isCooperativeRule() {
        return has(Kind.COOPERATIVE_RULE);
    }
}

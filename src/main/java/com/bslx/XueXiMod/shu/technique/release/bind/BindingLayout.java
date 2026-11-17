package com.bslx.XueXiMod.shu.technique.release.bind;

import com.bslx.XueXiMod.skill.Skill;

import java.util.List;
import java.util.Map;

/**
 * 切步前的不可变捕获结果。编译不做世界操作，也不扣费。
 * 结构编译器由网络包安装，本类不反向引用网络类型。
 */
public final class BindingLayout {
    public interface Compiler {
        /** 把原始技能收成切步前的布局。 */
        BindingLayout compile(Skill rawSkill);
    }

    private static Compiler installedCompiler;

    private final Skill projectedSkill;
    private final int[] originalIndexes;
    private final Map<Integer, BoundReleasePlan> plansByDomain;
    private final List<StructureFault> faults;
    private final boolean failed;

    private BindingLayout(Skill projectedSkill, int[] originalIndexes, Map<Integer, BoundReleasePlan> plansByDomain,
                          List<StructureFault> faults, boolean failed) {
        this.projectedSkill = projectedSkill;
        this.originalIndexes = originalIndexes;
        this.plansByDomain = plansByDomain;
        this.faults = faults;
        this.failed = failed;
    }

    /** 安装切步前使用的结构编译器。再次安装会换成新的。 */
    public static void installCompiler(Compiler compiler) {
        if (compiler == null) {
            throw new IllegalArgumentException("结构编译器不能为空");
        }
        installedCompiler = compiler;
    }

    /** 用已安装的编译器捕获这条技能。编译器缺失或没返回布局时失败。 */
    public static BindingLayout compile(Skill rawSkill) {
        Compiler compiler = installedCompiler;
        if (compiler == null) {
            throw new IllegalStateException("领域绑定结构编译器还没有安装");
        }
        BindingLayout layout = compiler.compile(rawSkill);
        if (layout == null) {
            throw new IllegalStateException("领域绑定结构编译器没有返回布局");
        }
        return layout;
    }

    /** 复制投影下标和各领域计划，作为一次成功布局。 */
    public static BindingLayout ready(Skill projectedSkill, int[] originalIndexes, Map<Integer, BoundReleasePlan> plansByDomain) {
        int[] copiedIndexes = originalIndexes == null ? new int[0] : originalIndexes.clone();
        Map<Integer, BoundReleasePlan> copiedPlans = plansByDomain == null || plansByDomain.isEmpty()
                ? Map.of()
                : Map.copyOf(plansByDomain);
        return new BindingLayout(projectedSkill, copiedIndexes, copiedPlans, List.of(), false);
    }

    /** 记下结构失败。没有具体故障时补一条默认说明。 */
    public static BindingLayout failed(Skill projectedSkill, List<StructureFault> faults) {
        List<StructureFault> copiedFaults = faults == null || faults.isEmpty()
                ? List.of(new StructureFault(-1, "领域绑定后面没有可绑定的攻击"))
                : List.copyOf(faults);
        return new BindingLayout(projectedSkill, new int[0], Map.of(), copiedFaults, true);
    }

    public Skill projectedSkill() {
        return projectedSkill;
    }

    /** 投影下标越界时返回 -1。 */
    public int originalIndex(int projectedIndex) {
        if (projectedIndex < 0 || projectedIndex >= originalIndexes.length) {
            return -1;
        }
        return originalIndexes[projectedIndex];
    }

    /** 从这一步的投影下标找回原始装配位置。 */
    public int originalFromStep(List<Integer> projectedStepIndexes, int localActionIndex) {
        if (projectedStepIndexes == null || localActionIndex < 0 || localActionIndex >= projectedStepIndexes.size()) {
            return -1;
        }
        Integer projectedIndex = projectedStepIndexes.get(localActionIndex);
        if (projectedIndex == null) {
            return -1;
        }
        return originalIndex(projectedIndex);
    }

    /** 取这个原始领域下标对应的冻结计划。没有则为空。 */
    public BoundReleasePlan planAtDomain(int originalDomainIndex) {
        return plansByDomain.get(originalDomainIndex);
    }

    public boolean failed() {
        return failed;
    }

    public List<StructureFault> faults() {
        return faults;
    }
}

package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinitions;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseBatch;
import com.bslx.XueXiMod.shu.technique.release.selfstatus.SelfStatusCommit;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.lang.reflect.Constructor;
import java.util.List;

/**
 * 调用混链拒绝、释放批次和自身状态提交。不启动世界。
 */
public final class SixFireChainSupplementCheck {
    private static final String SELF_REFUSAL = "自身状态不能和其他术式装配";
    private static int failures;

    private SixFireChainSupplementCheck() {
    }

    public static void main(String[] programArguments) {
        String group = programArguments.length == 0 ? "all" : programArguments[0];
        if ("mixed".equals(group) || "all".equals(group)) {
            checkMixed();
        }
        if ("batch".equals(group) || "all".equals(group)) {
            checkBatch();
        }
        if ("commit".equals(group) || "all".equals(group)) {
            checkCommit();
        }
        if (failures != 0) {
            System.err.println("supplement failures=" + failures);
            System.exit(1);
        }
        System.out.println("supplement group " + group + " finished");
    }

    private static void checkMixed() {
        ReleaseDefinitions.registerAll();
        Technique heat = technique("fire_heat_up", 0f);
        Technique burn = technique("fire_burn", 0f);
        Technique cool = technique("fire_cool", 0f);
        Technique spread = technique("fire_liaoyuan", 2f);
        Technique triple = technique("rule_triple_cast", 0f);
        Technique dual = technique("rule_dual_release", 0f);
        expect(SkillSegmentMath.mixedChainRefusal(List.of(heat)) == null, "升温单式允许");
        expect(SkillSegmentMath.mixedChainRefusal(List.of(burn)) == null, "燃烧单式允许");
        expect(SkillSegmentMath.mixedChainRefusal(List.of(cool)) == null, "冷却单式允许");
        expect(SELF_REFUSAL.equals(SkillSegmentMath.mixedChainRefusal(List.of(heat, spread))), "升温加燎原拒绝");
        expect(SELF_REFUSAL.equals(SkillSegmentMath.mixedChainRefusal(List.of(burn, spread))), "燃烧加燎原拒绝");
        expect(SELF_REFUSAL.equals(SkillSegmentMath.mixedChainRefusal(List.of(cool, spread))), "冷却加燎原拒绝");
        expect(SELF_REFUSAL.equals(SkillSegmentMath.mixedChainRefusal(List.of(triple, heat))), "升温加一分为三拒绝");
        expect(SELF_REFUSAL.equals(SkillSegmentMath.mixedChainRefusal(List.of(dual, burn))), "燃烧加双重释放拒绝");
        expect(SELF_REFUSAL.equals(SkillSegmentMath.mixedChainRefusal(List.of(cool, triple))), "冷却加一分为三拒绝");
        expect(SkillSegmentMath.mixedChainRefusal(List.of(triple, spread)) == null, "规则加攻击不是自身状态拒绝");
    }

    private static void checkBatch() {
        ReleaseDefinitions.registerAll();
        ReleaseBatch opened = ReleaseBatch.open(null, null);
        System.out.println("batch_opened=" + (opened != null) + " current=" + (ReleaseBatch.current() == opened));
        try {
            opened.fireContext();
            System.out.println("fire_context=created");
        } catch (Throwable failure) {
            System.out.println("fire_context_exception=" + failure.getClass().getName() + " message=" + failure.getMessage());
        } finally {
            ReleaseBatch.unbind();
        }
        printConstructors("net.minecraft.server.level.ServerPlayer");
        printConstructors("net.minecraftforge.common.util.FakePlayer");
        Technique triple = technique("rule_triple_cast", 0f);
        Technique dual = technique("rule_dual_release", 0f);
        Technique spread = technique("fire_liaoyuan", 2f);
        Technique sky = technique("fire_fentian", 2f);
        SkillChainPlan.ModifierPlan triplePlan = SkillChainPlan.collectModifiers(
                new Skill("triple", SkillType.MAGIC, List.of(triple, spread)),
                List.of(new ChainSegment(0, List.of()), new ChainSegment(1, List.of())),
                List.of(triple, spread), 0);
        System.out.println("triple_copies=" + triplePlan.copies + " attack=" + triplePlan.attackSegment);
        expect(triplePlan.copies == 3 && triplePlan.attackSegment == 1, "一分为三把份数乘三");
        List<Technique> dualTechniques = List.of(dual, spread, sky);
        List<ChainSegment> dualSegments = List.of(new ChainSegment(0, List.of()), new ChainSegment(1, List.of()),
                new ChainSegment(2, List.of()));
        SkillChainPlan.ModifierPlan dualPlan = SkillChainPlan.collectModifiers(
                new Skill("dual", SkillType.MAGIC, dualTechniques), dualSegments, dualTechniques, 0);
        int releasedCount = SkillChainPlan.releasedSegments(dualSegments, dualPlan.dualRuleSegment, dualTechniques).size();
        System.out.println("dual_segment=" + dualPlan.dualRuleSegment + " copies=" + dualPlan.copies
                + " released=" + releasedCount);
        expect(dualPlan.dualRuleSegment == 0 && dualPlan.copies == 1 && releasedCount == 2, "双重释放拉出两段且份数仍是一");
        System.out.println("batch_note=份数来自SkillChainPlan，不是ReleaseBatch按份执行");
    }

    private static void checkCommit() {
        ReleaseDefinitions.registerAll();
        SelfStatusCommit.Result nullPlayer = SelfStatusCommit.attempt(null, ReleaseRegistry.get("fire_heat_up"));
        SelfStatusCommit.Result wrongKind = SelfStatusCommit.attempt(null, ReleaseRegistry.get("fire_liaoyuan"));
        System.out.println("null_player=" + nullPlayer + " wrong_kind=" + wrongKind);
        expect(nullPlayer == SelfStatusCommit.Result.REJECTED, "空玩家拒绝");
        expect(wrongKind == SelfStatusCommit.Result.REJECTED, "非自身状态拒绝");
        System.out.println("commit_note=未进入预检成功、扣费、安装和退款");
    }

    private static void printConstructors(String typeName) {
        try {
            Class<?> loaded = Class.forName(typeName);
            Constructor<?>[] constructors = loaded.getDeclaredConstructors();
            System.out.println("constructors " + typeName + " count=" + constructors.length);
            for (Constructor<?> constructor : constructors) {
                System.out.println("constructor " + constructor);
            }
        } catch (Throwable failure) {
            System.out.println("constructor_load_failed " + typeName);
            printFailure(failure);
        }
    }

    private static void printFailure(Throwable failure) {
        Throwable current = failure;
        int depth = 0;
        while (current != null && depth < 8) {
            System.out.println("cause" + depth + "=" + current.getClass().getName() + " " + current.getMessage());
            StackTraceElement[] frames = current.getStackTrace();
            if (frames.length > 0) {
                System.out.println("at " + frames[0]);
            }
            current = current.getCause();
            depth += 1;
        }
    }

    private static Technique technique(String techniqueId, float baseDamage) {
        return new Technique(techniqueId, techniqueId, TechniqueType.MAGIC, baseDamage, 0, 20, techniqueId,
                0, ShuAttribute.FIRE, 0f);
    }

    private static void expect(boolean condition, String label) {
        if (!condition) {
            failures += 1;
            System.err.println("fail " + label);
        }
    }
}

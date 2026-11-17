package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.release.ChainRole;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.util.ArrayList;
import java.util.List;

/**
 * 检查真实计划冻结。期望值是契约字面量。本阶段不跑 Gradle，所以这个 main 还未执行。
 */
public final class RouteReleaseInputCheck {
    private static int assertions;
    private static int failures;

    private RouteReleaseInputCheck() {
    }

    public static void main(String[] programArguments) {
        ProbeDefinition gather = register("route_check_gather", ChainRole.ATTACK, 1f, 2f, 0);
        ProbeDefinition secondGather = register("route_check_gather_b", ChainRole.ATTACK, 1f, 2f, 0);
        ProbeDefinition dualRule = register("route_check_dual", ChainRole.RULE_DUAL, 1f, 1f, 1);
        ProbeDefinition tripleRule = register("route_check_triple", ChainRole.RULE_TRIPLE, 1f, 1f, 0);
        ProbeDefinition magicAttack = register("route_check_magic", ChainRole.ATTACK, 1f, 1f, 0);
        ProbeDefinition secondMagic = register("route_check_magic_b", ChainRole.ATTACK, 1f, 1f, 0);
        ProbeDefinition noneAttack = register("route_check_none", ChainRole.ATTACK, 1f, 1f, 0);
        ProbeDefinition crescent = register("route_check_crescent", ChainRole.ATTACK, 1.35f, 1f, 0);

        checkBaseTwoAndFour(gather, secondGather, magicAttack);
        checkIndependentMembers(gather, magicAttack, secondMagic);
        checkDualSnapshot(gather, dualRule, magicAttack, secondMagic);
        checkTripleCopiesShareOneInput(gather, tripleRule, magicAttack);
        checkSwordExclusion(gather, magicAttack, crescent);
        checkOwnEnergyStaysSeparate(crescent);
        checkGatherSlotPredicate(gather, magicAttack);
        checkPendingNoneStillUsesExistingScan(gather, noneAttack);

        System.out.println("assertions=" + assertions);
        System.out.println("failures=" + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void checkBaseTwoAndFour(ProbeDefinition gather, ProbeDefinition secondGather, ProbeDefinition magicAttack) {
        Skill magicSkill = skill(SkillType.MAGIC, technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        ReleaseInput baseInput = SkillChainPlan.frozenChainInput(magicSkill, magicSkill.getTechniques(), 0);
        checkFloat("base component", baseInput.chainEnergyMultiplier(), 1f);
        check("base is BASE", baseInput == ReleaseInput.BASE, "expected the BASE instance");

        Skill doubled = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        SkillChainPlan.ModifierPlan doubledPlan = SkillChainPlan.collectModifiers(doubled, segments(2), doubled.getTechniques(), 0);
        checkFloat("x2 frozen", doubledPlan.attackInput().chainEnergyMultiplier(), 2f);
        check("x2 same snapshot", doubledPlan.attackInput() == doubledPlan.memberInputs.get(0), "attack input was recopied");
        gather.chainEnergy = 9f;
        checkFloat("x2 stays frozen", doubledPlan.attackInput().chainEnergyMultiplier(), 2f);
        gather.chainEnergy = 2f;

        Skill quadrupled = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(secondGather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        SkillChainPlan.ModifierPlan quadrupledPlan = SkillChainPlan.collectModifiers(quadrupled, segments(3), quadrupled.getTechniques(), 0);
        checkFloat("x4 frozen", quadrupledPlan.attackInput().chainEnergyMultiplier(), 4f);
    }

    private static void checkIndependentMembers(ProbeDefinition gather, ProbeDefinition firstAttack, ProbeDefinition secondAttack) {
        Skill magicSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(firstAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f),
                technique(secondAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        List<ChainSegment> planned = segments(3);
        SkillChainPlan.ModifierPlan firstPlan = SkillChainPlan.collectModifiers(magicSkill, planned, magicSkill.getTechniques(), 0);
        SkillChainPlan.ModifierPlan secondPlan = SkillChainPlan.collectModifiers(magicSkill, planned, magicSkill.getTechniques(), firstPlan.resume);
        checkFloat("first member", firstPlan.attackInput().chainEnergyMultiplier(), 2f);
        check("second member baseline", secondPlan.attackInput() == ReleaseInput.BASE, "later member reused the earlier gather");
    }

    private static void checkDualSnapshot(ProbeDefinition gather, ProbeDefinition dualRule, ProbeDefinition firstAttack, ProbeDefinition secondAttack) {
        Skill magicSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(dualRule, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(firstAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f),
                technique(secondAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        List<ChainSegment> planned = segments(4);
        SkillChainPlan.ModifierPlan dualPlan = SkillChainPlan.collectModifiers(magicSkill, planned, magicSkill.getTechniques(), 0);
        checkFloat("dual first", dualPlan.dualInput(0).chainEnergyMultiplier(), 2f);
        checkFloat("dual second", dualPlan.dualInput(1).chainEnergyMultiplier(), 2f);
        check("dual first stored", dualPlan.dualInput(0) == dualPlan.memberInputs.get(0), "dual accessor recopied");
        check("dual second stored", dualPlan.dualInput(1) == dualPlan.memberInputs.get(1), "dual accessor recopied");
        CompiledReleaseStep compiled = CompiledReleaseStep.compile(magicSkill, magicSkill.getTechniques(), planned, 0);
        check("compiled dual", compiled != null && compiled.memberInputs.size() == 2, "dual plan did not freeze two members");
        ReleaseInput firstFrozen = compiled.memberInputs.get(0);
        ReleaseInput secondFrozen = compiled.memberInputs.get(1);
        gather.chainEnergy = 9f;
        checkFloat("compiled first stays", firstFrozen.chainEnergyMultiplier(), 2f);
        checkFloat("compiled second stays", secondFrozen.chainEnergyMultiplier(), 2f);
        check("compiled accessor", compiled.memberInput(1) == secondFrozen, "session accessor recopied");
        gather.chainEnergy = 2f;
    }

    private static void checkTripleCopiesShareOneInput(ProbeDefinition gather, ProbeDefinition tripleRule, ProbeDefinition magicAttack) {
        Skill magicSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(tripleRule, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        SkillChainPlan.ModifierPlan triplePlan = SkillChainPlan.collectModifiers(magicSkill, segments(3), magicSkill.getTechniques(), 0);
        check("triple copies", triplePlan.copies == 3, "actual=" + triplePlan.copies);
        check("triple one snapshot", triplePlan.memberInputs.size() == 1, "actual=" + triplePlan.memberInputs.size());
        checkFloat("triple scale", triplePlan.attackInput().chainEnergyMultiplier(), 2f);
        check("triple shared", triplePlan.attackInput() == triplePlan.memberInputs.get(0), "copies would not share the frozen input");
    }

    private static void checkSwordExclusion(ProbeDefinition gather, ProbeDefinition magicAttack, ProbeDefinition crescent) {
        Skill swordSkill = skill(SkillType.SWORD,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        SkillChainPlan.ModifierPlan swordPlan = SkillChainPlan.collectModifiers(swordSkill, segments(2), swordSkill.getTechniques(), 0);
        check("sword skill baseline", swordPlan.attackInput() == ReleaseInput.BASE, "old sword chain still gathered energy");

        Skill staffSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(crescent, TechniqueType.SWORD, ShuAttribute.WATER, 8f));
        ReleaseInput swordMember = SkillChainPlan.frozenChainInput(staffSkill, staffSkill.getTechniques(), 1);
        check("element sword baseline", swordMember == ReleaseInput.BASE, "element sword received chain energy");
        checkFloat("crescent own stays", SkillChainPlan.geometryEnergy(1.35f, swordMember), 1.35f);
    }

    private static void checkOwnEnergyStaysSeparate(ProbeDefinition crescent) {
        checkFloat("own times two", SkillChainPlan.geometryEnergy(1.5f, new ReleaseInput(2f)), 3f);
        checkFloat("crescent own times base", SkillChainPlan.geometryEnergy(crescent.energyScale(), ReleaseInput.BASE), 1.35f);
    }

    private static void checkGatherSlotPredicate(ProbeDefinition gather, ProbeDefinition magicAttack) {
        check("gather supplies", SkillChainPlan.suppliesChainEnergy(technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f)),
                "gather was not recognized");
        check("attack does not supply", !SkillChainPlan.suppliesChainEnergy(technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f)),
                "ordinary attack looked like gather");
    }

    private static void checkPendingNoneStillUsesExistingScan(ProbeDefinition gather, ProbeDefinition noneAttack) {
        Skill magicSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(noneAttack, TechniqueType.MAGIC, ShuAttribute.NONE, 4f));
        ReleaseInput pendingInput = SkillChainPlan.frozenChainInput(magicSkill, magicSkill.getTechniques(), 1);
        checkFloat("pending none keeps scan", pendingInput.chainEnergyMultiplier(), 2f);
    }

    private static ProbeDefinition register(String identifier, ChainRole chainRole, float ownEnergy,
                                            float chainEnergy, int pullCount) {
        ProbeDefinition definition = new ProbeDefinition(identifier, chainRole, ownEnergy, chainEnergy, pullCount);
        ReleaseRegistry.register(definition);
        return definition;
    }

    private static Technique technique(ProbeDefinition definition, TechniqueType techniqueType, ShuAttribute attribute, float damage) {
        return new Technique(definition.id(), definition.id(), techniqueType, damage, 20, 1, "probe", 0, attribute, 1f);
    }

    private static Skill skill(SkillType skillType, Technique... members) {
        return new Skill("probe", skillType, List.of(members));
    }

    private static List<ChainSegment> segments(int memberCount) {
        List<ChainSegment> planned = new ArrayList<>();
        for (int segmentIndex = 0; segmentIndex < memberCount; segmentIndex++) {
            planned.add(new ChainSegment(segmentIndex, List.of()));
        }
        return planned;
    }

    private static void check(String label, boolean passed, String detail) {
        assertions++;
        if (passed) {
            System.out.println("PASS " + label);
            return;
        }
        failures++;
        System.out.println("FAIL " + label + " " + detail);
    }

    private static void checkFloat(String label, float actual, float expected) {
        check(label, Float.compare(actual, expected) == 0, "actual=" + actual + " expected=" + expected);
    }

    private static final class ProbeDefinition implements ReleaseDefinition {
        private final String identifier;
        private final ChainRole chainRole;
        private final float ownEnergy;
        private float chainEnergy;
        private final int pullCount;

        private ProbeDefinition(String identifier, ChainRole chainRole, float ownEnergy, float chainEnergy, int pullCount) {
            this.identifier = identifier;
            this.chainRole = chainRole;
            this.ownEnergy = ownEnergy;
            this.chainEnergy = chainEnergy;
            this.pullCount = pullCount;
        }

        @Override
        public String id() {
            return identifier;
        }

        @Override
        public ReleaseKind kind() {
            return chainRole == ChainRole.ATTACK && ownEnergy >= 1f && chainEnergy == 1f ? ReleaseKind.HIT_VOLUME : ReleaseKind.RULE;
        }

        @Override
        public ReleaseTag tag() {
            return ReleaseTag.SUSTAINED;
        }

        @Override
        public Trajectory trajectory() {
            return Trajectory.STRAIGHT;
        }

        @Override
        public boolean moving() {
            return false;
        }

        @Override
        public int lifetimeTicks() {
            return 20;
        }

        @Override
        public float speed() {
            return 1f;
        }

        @Override
        public float thickness() {
            return 0f;
        }

        @Override
        public ChainRole role() {
            return chainRole;
        }

        @Override
        public float energyScale() {
            return ownEnergy;
        }

        @Override
        public float chainEnergyScale() {
            return chainEnergy;
        }

        @Override
        public int pullAfterA() {
            return pullCount;
        }
    }
}

package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.ModTechniques;
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
 * 检查真实计划冻结。期望值是倍率字面量，不要求基准单例。
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
        ProbeDefinition container = register("route_check_container", ChainRole.ATTACK, 1f, 1f, 0);
        ProbeDefinition ruleVolume = registerKind("route_check_rule", ChainRole.ATTACK, 1f, 1f, 0, ReleaseKind.RULE);
        ProbeDefinition actionVolume = register("route_check_action", ChainRole.ACTION, 1f, 1f, 0);

        checkBaseTwoAndFour(gather, secondGather, magicAttack);
        checkIndependentMembers(gather, magicAttack, secondMagic);
        checkDualSnapshot(gather, dualRule, magicAttack, secondMagic);
        checkTripleCopiesShareOneInput(gather, tripleRule, magicAttack);
        checkSwordExclusion(gather, magicAttack, crescent);
        checkOwnEnergyStaysSeparate(crescent);
        checkGatherSlotPredicate(gather, magicAttack);
        checkElementConsumers(gather, secondGather, magicAttack, container);
        checkRejectedMembers(gather, noneAttack, crescent, ruleVolume, actionVolume);
        checkMissingTargets(magicAttack);
        checkGatherRegistration();

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
        checkFloat("base size", baseInput.sizeMultiplier(), 1f);

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
        checkFloat("second member size", secondPlan.attackInput().sizeMultiplier(), 1f);
        checkFloat("second member not reused", secondPlan.attackInput().chainEnergyMultiplier(), 1f);
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
        checkFloat("sword skill baseline", swordPlan.attackInput().sizeMultiplier(), 1f);

        Skill staffSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(crescent, TechniqueType.SWORD, ShuAttribute.WATER, 8f));
        ReleaseInput swordMember = SkillChainPlan.frozenChainInput(staffSkill, staffSkill.getTechniques(), 1);
        checkFloat("element sword baseline", swordMember.sizeMultiplier(), 1f);
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

    private static void checkElementConsumers(ProbeDefinition gather, ProbeDefinition secondGather,
                                               ProbeDefinition magicAttack, ProbeDefinition container) {
        Skill positive = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        checkFloat("element positive two",
                SkillChainPlan.frozenChainInput(positive, positive.getTechniques(), 1).sizeMultiplier(), 2f);
        Skill positiveFour = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(secondGather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        checkFloat("element positive four",
                SkillChainPlan.frozenChainInput(positiveFour, positiveFour.getTechniques(), 2).sizeMultiplier(), 4f);

        Skill zeroDamage = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.MAGIC, ShuAttribute.WOOD, 0f),
                technique(container, TechniqueType.MAGIC, ShuAttribute.WATER, 0f));
        checkFloat("zero container two",
                SkillChainPlan.frozenChainInput(zeroDamage, zeroDamage.getTechniques(), 1).sizeMultiplier(), 2f);
        Skill zeroFour = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(secondGather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(container, TechniqueType.MAGIC, ShuAttribute.WATER, 0f));
        checkFloat("zero container four",
                SkillChainPlan.frozenChainInput(zeroFour, zeroFour.getTechniques(), 2).sizeMultiplier(), 4f);
    }

    private static void checkRejectedMembers(ProbeDefinition gather, ProbeDefinition noneAttack, ProbeDefinition crescent,
                                              ProbeDefinition ruleVolume, ProbeDefinition actionVolume) {
        Skill noneSkill = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.MAGIC, ShuAttribute.WATER, 0f),
                technique(noneAttack, TechniqueType.MAGIC, ShuAttribute.NONE, 4f));
        checkFloat("none stays base",
                SkillChainPlan.frozenChainInput(noneSkill, noneSkill.getTechniques(), 1).sizeMultiplier(), 1f);

        Skill forgedSword = skill(SkillType.SWORD,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(noneAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        checkFloat("forged sword slot",
                SkillChainPlan.frozenChainInput(forgedSword, forgedSword.getTechniques(), 1).sizeMultiplier(), 1f);

        Skill movement = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(actionVolume, TechniqueType.MOVEMENT, ShuAttribute.WATER, 4f));
        checkFloat("movement stays base",
                SkillChainPlan.frozenChainInput(movement, movement.getTechniques(), 1).sizeMultiplier(), 1f);

        Skill universal = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(crescent, TechniqueType.UNIVERSAL, ShuAttribute.WATER, 4f));
        checkFloat("universal stays base",
                SkillChainPlan.frozenChainInput(universal, universal.getTechniques(), 1).sizeMultiplier(), 1f);

        Skill rule = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                technique(ruleVolume, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        checkFloat("rule kind stays base",
                SkillChainPlan.frozenChainInput(rule, rule.getTechniques(), 1).sizeMultiplier(), 1f);

        Skill enchanted = skill(SkillType.MAGIC,
                technique(gather, TechniqueType.UNIVERSAL, ShuAttribute.NONE, 0f),
                enchanted(crescent, TechniqueType.MAGIC, ShuAttribute.WATER, 4f));
        checkFloat("enchantment stays base",
                SkillChainPlan.frozenChainInput(enchanted, enchanted.getTechniques(), 1).sizeMultiplier(), 1f);
    }

    private static void checkMissingTargets(ProbeDefinition magicAttack) {
        Technique eligible = technique(magicAttack, TechniqueType.MAGIC, ShuAttribute.WATER, 4f);
        Skill magicSkill = skill(SkillType.MAGIC, eligible);
        checkFloat("null skill", SkillChainPlan.frozenChainInput(null, magicSkill.getTechniques(), 0).sizeMultiplier(), 1f);
        checkFloat("empty list", SkillChainPlan.frozenChainInput(magicSkill, List.of(), 0).sizeMultiplier(), 1f);
        checkFloat("negative index", SkillChainPlan.frozenChainInput(magicSkill, magicSkill.getTechniques(), -1).sizeMultiplier(), 1f);
        checkFloat("past end", SkillChainPlan.frozenChainInput(magicSkill, magicSkill.getTechniques(), 1).sizeMultiplier(), 1f);
        List<Technique> nullMember = new ArrayList<>();
        nullMember.add(null);
        checkFloat("null member", SkillChainPlan.frozenChainInput(magicSkill, nullMember, 0).sizeMultiplier(), 1f);
        Technique missing = new Technique("route_check_missing", "route_check_missing", TechniqueType.MAGIC,
                4f, 20, 1, "probe", 0, ShuAttribute.WATER, 1f);
        Skill missingSkill = skill(SkillType.MAGIC, missing);
        checkFloat("null definition",
                SkillChainPlan.frozenChainInput(missingSkill, missingSkill.getTechniques(), 0).sizeMultiplier(), 1f);
    }

    private static void checkGatherRegistration() {
        Technique gather = ModTechniques.RULE_GATHER_ENERGY;
        check("gather registered magic", gather.getType() == TechniqueType.MAGIC, "actual=" + gather.getType());
        check("gather blocked from sword type", !gather.getType().canUseForSwordSkill(), "sword slots still accept the type");
    }

    private static ProbeDefinition register(String identifier, ChainRole chainRole, float ownEnergy,
                                            float chainEnergy, int pullCount) {
        return registerKind(identifier, chainRole, ownEnergy, chainEnergy, pullCount, null);
    }

    private static ProbeDefinition registerKind(String identifier, ChainRole chainRole, float ownEnergy,
                                                 float chainEnergy, int pullCount, ReleaseKind releaseKind) {
        ProbeDefinition definition = new ProbeDefinition(identifier, chainRole, ownEnergy, chainEnergy, pullCount, releaseKind);
        ReleaseRegistry.register(definition);
        return definition;
    }

    private static Technique technique(ProbeDefinition definition, TechniqueType techniqueType, ShuAttribute attribute, float damage) {
        return new Technique(definition.id(), definition.id(), techniqueType, damage, 20, 1, "probe", 0, attribute, 1f);
    }

    private static Technique enchanted(ProbeDefinition definition, TechniqueType techniqueType, ShuAttribute attribute, float damage) {
        return new Technique(definition.id(), definition.id(), techniqueType, damage, 20, 1, "probe", 0, attribute, 1f, true);
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
        private final ReleaseKind forcedKind;

        private ProbeDefinition(String identifier, ChainRole chainRole, float ownEnergy, float chainEnergy,
                                int pullCount, ReleaseKind forcedKind) {
            this.identifier = identifier;
            this.chainRole = chainRole;
            this.ownEnergy = ownEnergy;
            this.chainEnergy = chainEnergy;
            this.pullCount = pullCount;
            this.forcedKind = forcedKind;
        }

        @Override
        public String id() {
            return identifier;
        }

        @Override
        public ReleaseKind kind() {
            if (forcedKind != null) {
                return forcedKind;
            }
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

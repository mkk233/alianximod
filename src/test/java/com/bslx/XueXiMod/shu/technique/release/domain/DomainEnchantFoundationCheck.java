package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.network.BatchReleaseCompiler;
import com.bslx.XueXiMod.network.BatchReleasePlan;
import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.BindingLayout;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.lang.reflect.Method;
import java.util.List;

/** 只检查领域附魔的分类、绑定和报价入口。不启动世界，也不收取周期费用。 */
public final class DomainEnchantFoundationCheck {
    private static int assertions;
    private static int failures;

    private DomainEnchantFoundationCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        registerOnce();
        installBindingCompiler();
        classification();
        binding();
        quotes();
        System.out.println("assertions " + assertions + " failures " + failures);
        System.out.println("not run: periodic payment, entity damage, status application, game");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void registerOnce() {
        if (ReleaseRegistry.get("water_mist") != null && ReleaseRegistry.get("fire_nether_gloom") != null) {
            return;
        }
        ModTechniques.registerAll();
        EnchantmentDefinitions.registerAll();
        declareDamageDefaults();
    }

    /** 只补正伤害术式的伤害倍率，避免离线检查引导完整原版注册。 */
    private static void declareDamageDefaults() {
        for (Technique technique : TechniqueManager.getAll()) {
            if (technique == null || technique.getId() == null || technique.getBaseDamage() <= 0f) {
                continue;
            }
            if (com.bslx.XueXiMod.shu.balance.BalanceRegistry.isDeclared(technique.getId(), BalanceKeys.DAMAGE_MULTIPLIER)) {
                continue;
            }
            com.bslx.XueXiMod.shu.balance.BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder(technique.getId())
                    .damageMultiplier()
                    .build());
        }
    }

    private static void classification() throws Exception {
        Technique gloom = ModTechniques.FIRE_NETHER_GLOOM;
        Technique mist = TechniqueManager.getById("water_mist");
        Technique ignite = ModTechniques.MAGIC_IGNITE;
        check(gloom.isEnchantment() && ReleaseRegistry.get(gloom.getId()).domain() != null, "gloom keeps enchantment and domain");
        check(ReleaseRegistry.get(gloom.getId()).explicitDomainHost(), "gloom is a domain host");
        check(ReleaseRegistry.get(mist.getId()).domain() != null, "mist remains a domain");
        List<ChainSegment> stolen = TechniqueChain.split(skill(gloom, mist));
        check(stolen.size() == 2 && stolen.get(0).enchantIndexes().isEmpty(), "gloom is not absorbed by mist");
        List<ChainSegment> hosted = TechniqueChain.split(skill(ignite, gloom));
        check(hosted.size() == 1 && hosted.get(0).actionIndex() == 1
                && hosted.get(0).enchantIndexes().equals(List.of(0)), "gloom carries ordinary ignite");
        check(stepCount(skill(gloom, mist)) == 2, "steps keep gloom separate from mist");
        check(stepCount(skill(ignite, gloom)) == 1, "steps keep ignite with gloom");
    }

    private static void binding() {
        Skill bound = skill(ModTechniques.FIRE_NETHER_GLOOM, ModTechniques.RULE_DOMAIN_BIND, ModTechniques.FIRE_SPARK);
        BindingLayout layout = BindingLayout.compile(bound);
        check(!layout.failed(), "gloom bind spark compiles");
        check(layout.projectedSkill().getTechniques().size() == 1
                && "fire_nether_gloom".equals(layout.projectedSkill().getTechniques().get(0).getId()),
                "projection keeps gloom");
        check(layout.planAtDomain(0) != null
                && "fire_spark".equals(layout.planAtDomain(0).members().get(0).technique().getId()),
                "bound member is spark");
        Skill reversed = skill(ModTechniques.RULE_DOMAIN_BIND, ModTechniques.FIRE_NETHER_GLOOM, ModTechniques.FIRE_SPARK);
        check(BindingLayout.compile(reversed).failed(), "gloom cannot be bound onto another domain");
        Skill ordinary = skill(ModTechniques.MAGIC_IGNITE, ModTechniques.RULE_DOMAIN_BIND, ModTechniques.FIRE_SPARK);
        check(BindingLayout.compile(ordinary).failed(), "ordinary enchant remains unable to host a bind");
        BatchReleasePlan mixed = BatchReleaseCompiler.compile(skill(ModTechniques.FIRE_NETHER_GLOOM,
                TechniqueManager.getById("water_mist")), 0.0d);
        check(mixed == null || mixed.refusalKey() == null, "gloom plus mist is not refused as unsupported host");
        check(BatchReleaseCompiler.compile(skill(ModTechniques.MAGIC_IGNITE, ModTechniques.FIRE_SPARK), 0.0d) != null,
                "ordinary enchant plus spark stays on the batch path");
    }

    private static void quotes() throws Exception {
        Skill gloomOnly = skill(ModTechniques.FIRE_NETHER_GLOOM);
        Integer opening = pressCost(gloomOnly.getTechniques(), TechniqueChain.split(gloomOnly));
        check(opening != null && opening == ModTechniques.FIRE_NETHER_GLOOM.manaCost(),
                "gloom opening uses its registered enchantment price");
        Skill carried = skill(ModTechniques.MAGIC_IGNITE, ModTechniques.FIRE_NETHER_GLOOM);
        Integer carriedOpening = pressCost(carried.getTechniques(), TechniqueChain.split(carried));
        check(carriedOpening != null && carriedOpening == ModTechniques.FIRE_NETHER_GLOOM.manaCost(),
                "carried ignite is not prepaid with the domain opening");
        Skill mist = skill(ModTechniques.MAGIC_IGNITE, TechniqueManager.getById("water_mist"));
        Integer mistOpening = pressCost(mist.getTechniques(), TechniqueChain.split(mist));
        check(mistOpening != null && mistOpening == 0, "ordinary domain does not prepay its carried enchantment");
        check(DomainDefinition.mistRain().initialCost() == 40, "ordinary domain body price stays on the domain");
    }

    private static Skill skill(Technique... techniques) {
        java.util.ArrayList<ShuAttribute> embryos = new java.util.ArrayList<>();
        for (int index = 0; index < techniques.length; index++) {
            embryos.add(ShuAttribute.NONE);
        }
        return new Skill("check", SkillType.MAGIC, List.of(techniques), embryos);
    }

    private static Integer pressCost(List<Technique> techniques, List<ChainSegment> segments) throws Exception {
        Method cost = Class.forName("com.bslx.XueXiMod.network.SkillChainPlan")
                .getDeclaredMethod("pressCommitCost", List.class, List.class, int.class);
        cost.setAccessible(true);
        return (Integer) cost.invoke(null, techniques, segments, 0);
    }

    private static void installBindingCompiler() throws Exception {
        Class<?> compilerType = Class.forName("com.bslx.XueXiMod.network.BindingStructureCompiler");
        Method compiler = compilerType.getDeclaredMethod("compiler");
        compiler.setAccessible(true);
        BindingLayout.installCompiler((BindingLayout.Compiler) compiler.invoke(null));
    }

    private static int stepCount(Skill skill) throws Exception {
        Method list = Class.forName("com.bslx.XueXiMod.network.ChainSteps").getDeclaredMethod("list", Skill.class);
        list.setAccessible(true);
        return ((List<?>) list.invoke(null, skill)).size();
    }

    private static void check(boolean passed, String label) {
        assertions++;
        if (!passed) {
            failures++;
            System.out.println("FAIL " + label);
        }
    }
}

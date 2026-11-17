package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.domain.DomainSpawnInheritance;
import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.TechniqueType;
import com.bslx.XueXiMod.shu.balance.BalanceCatalog;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseKind;
import com.bslx.XueXiMod.shu.technique.release.ReleaseTag;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainAuraSpec;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainNativePolicy;
import com.bslx.XueXiMod.shu.technique.EnchantmentAccumulator;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinition;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
import com.bslx.XueXiMod.shu.technique.EnchantmentRegistry;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantBonus;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantCatalog;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantLoad;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.bind.BindingLayout;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundMember;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;
import com.bslx.XueXiMod.status.fire.seed.FireSeedMath;

import net.minecraft.server.level.ServerPlayer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Calls the current split, binding, bonus, inheritance, and seed-merge symbols.
 * It does not create a world, player, or entity, so live payment and damage stay unverified.
 */
public final class DomainSpawnInheritanceCheck {
    private static int assertions;
    private static int failures;

    private DomainSpawnInheritanceCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        registerOnce();
        installBindingCompiler();
        exactCombination();
        ordinaryChain();
        attributeValues();
        nativeAndBilling();
        sharedFoundation();
        System.out.println("assertions " + assertions + " failures " + failures);
        System.out.println("sealed balance follows; burn, control, and contact numbers are not claimed if it does not return");
        sealedBalance();
        System.out.println("not run: world, player, entity damage, live mana payment, game");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void exactCombination() throws Exception {
        Skill assembled = skill(ShuAttribute.NONE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_NETHER_GLOOM,
                ModTechniques.RULE_DOMAIN_BIND, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_SPARK);
        BindingLayout layout = BindingLayout.compile(assembled);
        check(!layout.failed(), "requested assembly compiles");
        BoundReleasePlan plan = layout.planAtDomain(1);
        check(plan != null && plan.members().size() == 1, "one frozen attack");
        BoundMember spark = plan.members().get(0);
        check("fire_spark".equals(spark.technique().getId()), "frozen host is spark");
        check(spark.enchantLocalIndexes().equals(List.of(0)), "binding keeps the sow after the marker");

        Skill domainSkill = skill(ShuAttribute.NONE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_NETHER_GLOOM);
        List<ChainSegment> domainSegments = TechniqueChain.split(domainSkill);
        check(domainSegments.size() == 1 && domainSegments.get(0).enchantIndexes().equals(List.of(0)),
                "domain split keeps only sow before gloom");
        EnchantmentSnapshot carried = snapshot(List.of(ResourceEffectKeys.SOW));
        check(DomainSpawnInheritance.inheritable(carried).equals(List.of(ResourceEffectKeys.SOW)),
                "sow transfers");
        check(DomainSpawnInheritance.transferable(ResourceEffectKeys.GLOOM), "declared gloom attack payload transfers");
        check(!DomainSpawnInheritance.repeatable(ResourceEffectKeys.GLOOM), "gloom payload stays outside the paid pulse");

        Skill frozen = plan.frozenSkill();
        EnchantBonus own = EnchantBonus.resolve(frozen, spark.enchantLocalIndexes(),
                spark.technique().getBaseDamage(), 1);
        ResourceEnchantLoad merged = DomainSpawnInheritance.combine(own.resourceLoad,
                DomainSpawnInheritance.inheritable(carried));
        int body = ReleaseRegistry.get(spark.technique().getId()).bodyScaledSeedBase();
        int requested = FireSeedMath.mergedRequest(body + merged.scaledSeedBase(), own.seedPoints, 1.0d);
        check(body == 2, "spark body base is 2");
        check(merged.scaledSeedBase() == 20, "own sow and carried sow each keep 10");
        check(own.seedPoints == 0, "resource sow does not add legacy seed points");
        check(requested == 22, "accepted hit requests 22 before furnace scaling");
        check(merged.slices().size() == 2, "same enchantment id is retained twice");
        check(merged.manaPrice() == own.resourceLoad.manaPrice(),
                "combining inheritance does not add another mana price");
    }

    private static void ordinaryChain() {
        Skill plain = skill(ShuAttribute.FIRE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_SPARK);
        List<ChainSegment> segments = TechniqueChain.split(plain);
        check(segments.size() == 1 && segments.get(0).enchantIndexes().equals(List.of(0)), "plain split unchanged");
        int gap = TechniqueChain.gapBefore(new ChainSegment(0, List.of()), segments.get(0), plain);
        check(gap == 5, "plain chain keeps the sow enchantment cooldown at 5");
        check(DomainSpawnInheritance.displayedAttackCooldown(ModTechniques.FIRE_SPARK, true, null) == 4,
                "plain same-attribute skill cooldown rounds 3.5 up to 4");
        EnchantBonus bonus = EnchantBonus.resolve(plain, List.of(0), ModTechniques.FIRE_SPARK.getBaseDamage(), 1);
        EnchantBonus stacked = DomainSpawnInheritance.stackBonus(bonus, ShuAttribute.FIRE, true, null);
        check(stacked.attackDamage == bonus.attackDamage && stacked.resourceLoad.scaledSeedBase() == 10,
                "no carried enchantment leaves the plain bonus unchanged");
    }

    private static void attributeValues() throws Exception {
        Skill fire = skill(ShuAttribute.FIRE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_SPARK);
        EnchantBonus own = EnchantBonus.resolve(fire, List.of(0), ModTechniques.FIRE_SPARK.getBaseDamage(), 1);
        EnchantmentSnapshot carried = snapshot(List.of(ResourceEffectKeys.SOW));
        EnchantmentSnapshot inherited = carried;
        boolean matched = DomainSpawnInheritance.matchedByOwn(ShuAttribute.FIRE, fire, List.of(0));
        EnchantBonus once = DomainSpawnInheritance.stackBonus(own, ShuAttribute.FIRE, matched, inherited);
        check(matched, "own fire enchantment already matches fire spark");
        check(once.attackDamage == own.attackDamage, "carried same attribute does not multiply damage again");
        int ownGap = DomainSpawnInheritance.displayedAttackCooldown(ModTechniques.FIRE_SPARK, matched, null);
        int inheritedGap = DomainSpawnInheritance.displayedAttackCooldown(ModTechniques.FIRE_SPARK, matched, inherited);
        check(ownGap == 4 && inheritedGap == 4, "own and carried same-attribute skill cooldown each round once to 4");

        Skill neutral = skill(ShuAttribute.NONE, ModTechniques.FIRE_SPARK);
        EnchantBonus bare = EnchantBonus.resolve(neutral, List.of(), ModTechniques.FIRE_SPARK.getBaseDamage(), 1);
        EnchantBonus inheritedOnly = DomainSpawnInheritance.stackBonus(bare, ShuAttribute.FIRE, false, inherited);
        check(inheritedOnly.attackDamage == bare.attackDamage, "carried enchantment without a stored embryo does not invent same-attribute damage");
        check(DomainSpawnInheritance.displayedAttackCooldown(ModTechniques.FIRE_SPARK, false, inherited) == 5,
                "a carried enchantment without its embryo does not invent the attribute reaction");
        check(inheritedOnly.resourceLoad.scaledSeedBase() == bare.resourceLoad.scaledSeedBase(),
                "damage stacking does not multiply seed points");
    }

    private static void nativeAndBilling() throws Exception {
        EnchantmentSnapshot gloom = snapshot(List.of(ResourceEffectKeys.GLOOM));
        check(DomainSpawnInheritance.inheritable(gloom).equals(List.of(ResourceEffectKeys.GLOOM)),
                "declared gloom payload contributes one attack enchantment");
        ResourceEnchantLoad empty = DomainSpawnInheritance.combine(ResourceEnchantLoad.EMPTY,
                DomainSpawnInheritance.inheritable(gloom));
        check(empty.gloom() && empty.scaledSeedBase() == 5 && empty.manaPrice() == 0,
                "gloom payload keeps one configured seed slice without another mana price");
        EnchantmentSnapshot offered = DomainSpawnInheritance.offer(null, DomainDefinition.gloom());
        check(offered != null && offered.basicTypeIds().equals(List.of(ResourceEffectKeys.GLOOM)),
                "gloom domain offers its declared payload without a carried enchantment");
        check(DomainSpawnInheritance.offer(null, DomainDefinition.mistRain()) == null,
                "a plain domain without a declared payload offers nothing");

        Skill frozen = skill(ShuAttribute.NONE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_SPARK);
        int inheritedPrice = DomainSpawnInheritance.automaticEnchantPrice(frozen, List.of(0), true);
        check(inheritedPrice == 0, "an already priced own enchantment is not billed again for inheritance");
        int missingOwnPrice = DomainSpawnInheritance.automaticEnchantPrice(frozen, List.of(), false);
        check(missingOwnPrice == 0, "no held inherited enchantment adds no automatic price");

        Skill opening = skill(ShuAttribute.NONE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_NETHER_GLOOM);
        Integer domainOpening = pressCost(opening.getTechniques(), TechniqueChain.split(opening));
        check(domainOpening != null && domainOpening == ModTechniques.FIRE_NETHER_GLOOM.manaCost(),
                "opening still bills only the domain body");
        EnchantmentSnapshot carried = snapshot(List.of(ResourceEffectKeys.SOW));
        check(DomainEnchantPulse.price(carried) == ResourceEnchantNumbers.MANA_PRICE,
                "domain pulse still prices carried sow once");
        check(ResourceEnchantNumbers.GLOOM_PERIOD_TICKS == 5 && ResourceEnchantNumbers.GLOOM_BASE == 5,
                "gloom production remains 5 points every 5 ticks");
        check(DomainEnchantPulse.PERIOD_TICKS == 10, "domain enchantment pulse remains 10");
        check(DomainBindingRuntime.PERIOD_TICKS == 20, "binding beat remains 20");
        check(DomainBindingRuntime.PERIOD_TICKS == 20 && DomainSpawnInheritance.displayedAttackCooldown(ModTechniques.FIRE_SPARK, false, null) == 5, "binding period stays 20 while plain attack cooldown stays 5");
        check(DomainDefinition.gloom().lifetimeTicks() == 200, "domain life remains 200");

    }


    private static void sharedFoundation() throws Exception {
        Skill spark = skill(ShuAttribute.NONE, ModTechniques.FIRE_SPARK);
        EnchantBonus own = EnchantBonus.resolve(spark, List.of(), ModTechniques.FIRE_SPARK.getBaseDamage(), 1);
        EnchantmentSnapshot water = effectSnapshot("tide_mark", ShuAttribute.WATER, 2f, 0, 0, 0f, 1, 0);
        check(DomainSpawnInheritance.merge(own, water, List.of("tide_mark")).tideMarks == 1,
                "legacy tide mark transfers");
        EnchantmentSnapshot earth = effectSnapshot("sinking_edge", ShuAttribute.EARTH, 2f, 0, 0, 0.35f, 0, 0);
        check(DomainSpawnInheritance.merge(own, earth, List.of("sinking_edge")).knockback == 0.35f,
                "legacy knockback transfers");

        Skill armed = skill(ShuAttribute.NONE, ModTechniques.FIRE_SEED_SOW, ModTechniques.FIRE_SPARK);
        EnchantBonus armedBonus = EnchantBonus.resolve(armed, List.of(0), ModTechniques.FIRE_SPARK.getBaseDamage(), 1);
        EnchantmentSnapshot carriedSow = snapshot(List.of(ResourceEffectKeys.SOW));
        ResourceEnchantLoad retained = DomainSpawnInheritance.combine(armedBonus.resourceLoad,
                DomainSpawnInheritance.inheritable(carriedSow));
        int body = ReleaseRegistry.get(ModTechniques.FIRE_SPARK.getId()).bodyScaledSeedBase();
        check(body == 2 && armedBonus.resourceLoad.scaledSeedBase() == 10 && retained.scaledSeedBase() == 20
                        && retained.slices().size() == 2,
                "own sow and carried sow each remain");
        check(FireSeedMath.mergedRequest(body + retained.scaledSeedBase(), armedBonus.seedPoints, 1.0d) == 22,
                "shared resource merge still requests 22");

        EnchantmentSnapshot trail = snapshot(List.of(ResourceEffectKeys.TRAIL, ResourceEffectKeys.BREAK_GROUND));
        ResourceEnchantLoad spatial = DomainSpawnInheritance.combine(ResourceEnchantLoad.EMPTY,
                DomainSpawnInheritance.inheritable(trail));
        check(spatial.trail() && spatial.breakGround(), "trail and landing effects stay on the attack payload");
        check(!DomainSpawnInheritance.repeatable(ResourceEffectKeys.TRAIL)
                        && !DomainSpawnInheritance.repeatable(ResourceEffectKeys.BREAK_GROUND),
                "trail and landing effects do not become domain pulses");

        check(DomainSpawnInheritance.transferable("check_future_enchant")
                        && DomainSpawnInheritance.repeatable("check_future_enchant"),
                "a newly registered enchantment uses the shared foundation");
        EnchantmentSnapshot future = effectSnapshot("check_future_enchant", ShuAttribute.NONE, 0f, 0, 0, 0f, 0, 0);
        check(DomainSpawnInheritance.inheritable(future).contains("check_future_enchant"),
                "a new registered enchantment is accepted by the shared foundation");
        EnchantBonus futureMerged = DomainSpawnInheritance.merge(own, future, List.of("check_future_enchant"));
        check(futureMerged.attackDamage == own.attackDamage && futureMerged.addedDamage == 2f,
                "the new metal effect reuses EnchantBonus.resolve without changing the foundation");
        check(!DomainSpawnInheritance.transferable("check_future_domain"), "a new domain definition stays native");
        EnchantmentSnapshot futureDomain = effectSnapshot("check_future_domain", ShuAttribute.NONE, 0f, 0, 0, 0f, 0, 0);
        check(DomainSpawnInheritance.inheritable(futureDomain).isEmpty(),
                "the new domain definition is excluded by the existing domain capability");
        registerFuture("check_future_payload", ShuAttribute.METAL, true);
        EnchantmentRegistry.register(new EnchantmentDefinition() {
            public String id() { return "check_future_payload"; }
            public ShuAttribute element() { return ShuAttribute.METAL; }
            public EnchantmentAccumulator createAccumulator() {
                return new EnchantmentAccumulator("metal", 0, 0f, 0f, 1f, 1f, 1f, 1f);
            }
            public EnchantmentAccumulator fold(ElementReaction reaction, EnchantmentAccumulator current) {
                return createAccumulator();
            }
            public String attackPayloadId() { return "check_future_payload"; }
        });
        check(DomainSpawnInheritance.transferable("check_future_payload")
                        && !DomainSpawnInheritance.repeatable("check_future_payload"),
                "a newly declared domain payload transfers without entering the paid pulse");
        for (String domainId : List.of("water_mist", "water_deep_pressure", "water_vortex", "water_bubble_column",
                "fire_nether_gloom")) {
            check(ReleaseRegistry.get(domainId) != null && ReleaseRegistry.get(domainId).domain() != null,
                    domainId + " remains a shared domain definition");
        }
    }

    private static void sealedBalance() throws Exception {
        net.minecraft.SharedConstants.tryDetectVersion();
        net.minecraft.server.Bootstrap.bootStrap();
        BalanceCatalog.sealAfterContent();
        BalanceService.install(BalanceSide.SERVER, com.bslx.XueXiMod.shu.balance.BalanceSnapshot.defaults());
        BalanceService.bind(BalanceSide.SERVER);
        Skill spark = skill(ShuAttribute.NONE, ModTechniques.FIRE_SPARK);
        EnchantBonus own = EnchantBonus.resolve(spark, List.of(), ModTechniques.FIRE_SPARK.getBaseDamage(), 1);
        EnchantmentSnapshot fire = effectSnapshot("magic_ignite", ShuAttribute.NONE, 0f, 0, 0, 0f, 0, 0);
        EnchantmentSnapshot wood = effectSnapshot("vine_mark", ShuAttribute.NONE, 0f, 0, 0, 0f, 0, 0);
        EnchantBonus fireMerged = DomainSpawnInheritance.merge(own, fire, List.of("magic_ignite"));
        EnchantBonus woodMerged = DomainSpawnInheritance.merge(own, wood, List.of("vine_mark"));
        check(fireMerged.burnSeconds == own.burnSeconds + 5 && fireMerged.seedPoints == own.seedPoints + 2
                        && fireMerged.addedDamage == 2f && fireMerged.attackDamage == own.attackDamage,
                "legacy fire resolve adds configured burn, legacy seeds, and added damage");
        EnchantmentSnapshot bare = EnchantmentSnapshot.bare(UUID.randomUUID());
        EnchantmentSnapshot fireContact = bare.inherit(fire, List.of("magic_ignite"));
        check(fireContact.contactDamage == EnchantmentSnapshot.damageOf(
                        skill(ShuAttribute.NONE, TechniqueManager.getById("magic_ignite")), List.of(0))
                        && fireContact.contactDamage > 0f && fireContact.burnSeconds == 5,
                "legacy fire contact damage and configured burn both transfer");
        check(woodMerged.controlTicks == 0, "legacy control resolve transfers the no legacy wood control");
        EnchantmentSnapshot woodContact = bare.inherit(wood, List.of("vine_mark"));
        check(woodContact.controlTicks == 0 && woodContact.contactDamage == EnchantmentSnapshot.damageOf(
                        skill(ShuAttribute.NONE, TechniqueManager.getById("vine_mark")), List.of(0))
                        && woodContact.contactDamage > 0f,
                "legacy control and contact damage both follow the shared snapshot read");
        EnchantmentSnapshot future = effectSnapshot("check_future_enchant", ShuAttribute.NONE, 0f, 0, 0, 0f, 0, 0);
        EnchantmentSnapshot futureContact = bare.inherit(future, List.of("check_future_enchant"));
        check(futureContact.contactDamage == EnchantmentSnapshot.damageOf(
                        skill(ShuAttribute.NONE, TechniqueManager.getById("check_future_enchant")), List.of(0))
                        && futureContact.contactDamage > 0f,
                "the new enchantment reuses EnchantmentSnapshot contact damage");
        System.out.println("assertions " + assertions + " failures " + failures);
    }

    private static void registerFuture(String typeId, ShuAttribute element, boolean domain) throws Exception {
        EnchantmentRegistry.register(new EnchantmentDefinition() {
            public String id() { return typeId; }
            public ShuAttribute element() { return element; }
            public EnchantmentAccumulator createAccumulator() {
                return new EnchantmentAccumulator("metal", 0, 0f, 0f, 1f, 1f, 1f, 1f);
            }
            public EnchantmentAccumulator fold(ElementReaction reaction, EnchantmentAccumulator current) {
                return createAccumulator();
            }
            public int magicDamage(ElementReaction reaction) { return 3; }
        });
        TechniqueManager.register(new Technique(typeId, typeId, TechniqueType.MAGIC,
                domain ? 0f : 1f, 0, 0, "", 0, element, 1f, true));
        if (!domain) {
            ReleaseRegistry.register(com.bslx.XueXiMod.shu.technique.release.batch.ListedRelease.enchant(
                    typeId, com.bslx.XueXiMod.shu.technique.release.ReleaseTag.TRIGGER));
            return;
        }
        DomainDefinition.Placer placer = (ServerPlayer player) -> null;
        DomainDefinition domainDefinition = new DomainDefinition(placer, 1, 1,
                new DomainNativePolicy(typeId, 0, 0, 0, 0, 0, 0), new DomainAuraSpec(typeId, 0f), false);
        ReleaseRegistry.register(new ReleaseDefinition() {
            public String id() { return typeId; }
            public ReleaseKind kind() { return ReleaseKind.RULE; }
            public ReleaseTag tag() { return ReleaseTag.SUSTAINED; }
            public Trajectory trajectory() { return Trajectory.STRAIGHT; }
            public boolean moving() { return false; }
            public int lifetimeTicks() { return 1; }
            public float speed() { return 0f; }
            public float thickness() { return 0f; }
            public DomainDefinition domain() { return domainDefinition; }
        });
    }
    private static EnchantmentSnapshot effectSnapshot(String typeId, ShuAttribute embryo, float damage, int burn,
                                                      int control, float knockback, int tide, int seeds) throws Exception {
        Constructor<EnchantmentSnapshot> constructor = EnchantmentSnapshot.class.getDeclaredConstructor(
                UUID.class, float.class, int.class, int.class, boolean.class, float.class, int.class, int.class, List.class, List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(UUID.randomUUID(), damage, burn, control, false, knockback, tide, seeds,
                List.of(typeId), List.of(embryo));
    }

    private static void registerOnce() throws Exception {
        com.bslx.XueXiMod.shu.ModTechniques.registerAll();
        EnchantmentDefinitions.registerAll();
        if (ReleaseRegistry.get("fire_spark") == null || ReleaseRegistry.get("fire_nether_gloom") == null) {
        ReleaseRegistry.register(com.bslx.XueXiMod.shu.technique.release.batch.ListedRelease.spark());
        ReleaseRegistry.register(com.bslx.XueXiMod.shu.technique.release.batch.ListedRelease.enchant("fire_seed_sow", com.bslx.XueXiMod.shu.technique.release.ReleaseTag.TRIGGER));
        ReleaseRegistry.register(com.bslx.XueXiMod.shu.technique.release.batch.ListedRelease.gloomDomain(DomainDefinition.gloom()));
        registerBind();
        }
        BalanceCatalog.noteBuiltins(TechniqueManager.getAll());
        registerFuture("check_future_enchant", ShuAttribute.METAL, false);
        registerFuture("check_future_domain", ShuAttribute.WATER, true);
    }


    private static void registerBind() throws Exception {
        Class<?> type = Class.forName("com.bslx.XueXiMod.shu.technique.release.batch.ListedRelease");
        java.lang.reflect.Constructor<?> constructor = type.getDeclaredConstructor(String.class,
                com.bslx.XueXiMod.shu.technique.release.ReleaseKind.class,
                com.bslx.XueXiMod.shu.technique.release.ReleaseTag.class,
                com.bslx.XueXiMod.shu.technique.release.Trajectory.class,
                com.bslx.XueXiMod.shu.technique.release.ChainRole.class,
                boolean.class, int.class, float.class, float.class, double.class, double.class,
                boolean.class, boolean.class, boolean.class, boolean.class, int.class, int.class, boolean.class);
        constructor.setAccessible(true);
        Object definition = constructor.newInstance("rule_domain_bind",
                com.bslx.XueXiMod.shu.technique.release.ReleaseKind.RULE,
                com.bslx.XueXiMod.shu.technique.release.ReleaseTag.TRIGGER,
                com.bslx.XueXiMod.shu.technique.release.Trajectory.STRAIGHT,
                com.bslx.XueXiMod.shu.technique.release.ChainRole.RULE_DOMAIN_BIND,
                false, 1, 0f, 0f, 0d, 0d, false, false, false, true, 0, -1, false);
        ReleaseRegistry.register((com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition) definition);
    }

    private static void installBindingCompiler() throws Exception {
        Method compiler = Class.forName("com.bslx.XueXiMod.network.BindingStructureCompiler").getDeclaredMethod("compiler");
        compiler.setAccessible(true);
        BindingLayout.installCompiler((BindingLayout.Compiler) compiler.invoke(null));
    }

    private static Skill skill(ShuAttribute embryo, Technique... techniques) {
        ArrayList<ShuAttribute> embryos = new ArrayList<>();
        for (int index = 0; index < techniques.length; index++) {
            embryos.add(embryo);
        }
        return new Skill("check", SkillType.MAGIC, List.of(techniques), embryos);
    }

    private static EnchantmentSnapshot snapshot(List<String> typeIds) throws Exception {
        Constructor<EnchantmentSnapshot> constructor = EnchantmentSnapshot.class.getDeclaredConstructor(
                UUID.class, float.class, int.class, int.class, boolean.class, float.class, int.class, int.class, List.class, List.class);
        constructor.setAccessible(true);
        return constructor.newInstance(UUID.randomUUID(), 0f, 0, 0, false, 0f, 0, 0, typeIds, java.util.Collections.nCopies(typeIds.size(), ShuAttribute.NONE));
    }

    private static Integer pressCost(List<Technique> techniques, List<ChainSegment> segments) throws Exception {
        Method cost = Class.forName("com.bslx.XueXiMod.network.SkillChainPlan")
                .getDeclaredMethod("pressCommitCost", List.class, List.class, int.class);
        cost.setAccessible(true);
        return (Integer) cost.invoke(null, techniques, segments, 0);
    }

    private static void check(boolean passed, String label) {
        assertions++;
        if (!passed) {
            failures++;
            System.out.println("FAIL " + label);
        }
    }
}

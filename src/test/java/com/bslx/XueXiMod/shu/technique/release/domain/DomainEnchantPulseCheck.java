package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.ModTechniques;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.Technique;
import com.bslx.XueXiMod.shu.TechniqueManager;
import com.bslx.XueXiMod.shu.technique.EnchantmentDefinitions;
import com.bslx.XueXiMod.shu.technique.chain.ChainSegment;
import com.bslx.XueXiMod.shu.technique.chain.TechniqueChain;
import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.skill.Skill;
import com.bslx.XueXiMod.skill.SkillType;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

/**
 * 领域额外附魔的周期报价和节拍。
 * 不创建世界、玩家或实体，因此不声称已经验证扣款、伤害、燃烧或队伍过滤。
 */
public final class DomainEnchantPulseCheck {
    private static int assertions;
    private static int failures;

    private DomainEnchantPulseCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        registerOnce();
        prices();
        beats();
        unsupported();
        System.out.println("assertions " + assertions + " failures " + failures);
        System.out.println("not run: world entities, mana payment, damage, burning, team filtering, game");
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

    private static void declareDamageDefaults() {
        for (String techniqueId : new String[] {"magic_ignite", "tide_mark", "vine_mark", "sinking_edge", "keen_mark"}) {
            if (com.bslx.XueXiMod.shu.balance.BalanceRegistry.isDeclared(techniqueId,
                    com.bslx.XueXiMod.shu.balance.BalanceKeys.DAMAGE_MULTIPLIER)) {
                continue;
            }
            com.bslx.XueXiMod.shu.balance.BalanceRegistry.declareSpecial(
                    com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec.builder(techniqueId).damageMultiplier().build());
        }
    }

    private static void prices() throws Exception {
        Skill bare = skill(TechniqueManager.getById("water_mist"));
        check(price(bare) == 0, "bare domain has no pulse price");
        Skill ignite = skill(ModTechniques.MAGIC_IGNITE, TechniqueManager.getById("water_mist"));
        check(price(ignite) == ModTechniques.MAGIC_IGNITE.manaCost(), "ignite uses its configured price once");
        Skill pair = skill(ModTechniques.MAGIC_IGNITE, ModTechniques.TIDE_MARK, TechniqueManager.getById("water_mist"));
        check(price(pair) == ModTechniques.MAGIC_IGNITE.manaCost() + ModTechniques.TIDE_MARK.manaCost(),
                "two extras sum once");
        Skill kindle = skill(ModTechniques.FIRE_KINDLE, TechniqueManager.getById("water_mist"));
        check(price(kindle) == ModTechniques.FIRE_KINDLE.manaCost(), "zero-damage kindle keeps its price");
        Skill gloom = skill(ModTechniques.FIRE_NETHER_GLOOM);
        check(price(gloom) == 0, "gloom native effect is not an extra price");
        Skill trail = skill(ModTechniques.FIRE_FLAME_TRAIL, TechniqueManager.getById("water_mist"));
        check(price(trail) == 0, "trail has no domain pulse price");
        Skill broken = skill(ModTechniques.EARTH_BREAK_GROUND, TechniqueManager.getById("water_mist"));
        check(price(broken) == 0, "break-ground has no domain pulse price");
        check(com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot.rawDamageOf(ignite,
                com.bslx.XueXiMod.shu.technique.chain.TechniqueChain.split(ignite).get(0).enchantIndexes()) > 0f,
                "ignite has configured raw damage");
        check(com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot.rawDamageOf(kindle,
                com.bslx.XueXiMod.shu.technique.chain.TechniqueChain.split(kindle).get(0).enchantIndexes()) == 0f,
                "kindle does not invent raw damage");
    }

    private static void beats() {
        DomainDefinition mist = DomainDefinition.mistRain();
        DomainInstance domain = domain(mist, 1000, mist.lifetimeTicks());
        check(!DomainEnchantPulse.due(domain, 1000), "creation tick is not charged");
        check(DomainEnchantPulse.due(domain, 1010), "tick 10 is due");
        check(DomainEnchantPulse.due(domain, 1020), "tick 20 is due");
        check(!DomainEnchantPulse.due(domain, 1015), "middle tick is not due");
        check(domain.nextBindingBeat() == 0, "unbound domain has no binding beat");
        domain.close();
        check(!DomainEnchantPulse.due(domain, 1010), "closed domain stops");
        DomainInstance expired = domain(mist, 1000, 10);
        check(!DomainEnchantPulse.due(expired, 1010), "expired domain stops");
        check(DomainBindingRuntime.PERIOD_TICKS == 20, "binding period stays 20");
        check(com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers.GLOOM_PERIOD_TICKS == 5,
                "gloom seed period is 5 ticks");
    }

    private static void unsupported() throws Exception {
        Skill mixed = skill(ModTechniques.FIRE_FLAME_TRAIL, ModTechniques.MAGIC_IGNITE,
                TechniqueManager.getById("water_mist"));
        check(price(mixed) == ModTechniques.MAGIC_IGNITE.manaCost(), "trail does not add to a supported enchant");
        check(typeIds(mixed).contains(ModTechniques.FIRE_FLAME_TRAIL.getId()),
                "unsupported trail remains listed");
    }

    private static List<String> typeIds(Skill skill) throws Exception {
        ChainSegment segment = TechniqueChain.split(skill).get(0);
        Method types = EnchantmentSnapshot.class.getDeclaredMethod("distinctBasicTypeIds", Skill.class, List.class);
        types.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> listed = (List<String>) types.invoke(null, skill, segment.enchantIndexes());
        return listed;
    }
    private static int price(Skill skill) throws Exception {
        return DomainEnchantPulse.price(namesOnly(skill));
    }

    private static EnchantmentSnapshot namesOnly(Skill skill) throws Exception {
        Constructor<?> constructor = EnchantmentSnapshot.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        return (EnchantmentSnapshot) constructor.newInstance(UUID.randomUUID(), 0f, 0, 0, false, 0f, 0, 0, typeIds(skill));
    }

    private static EnchantmentSnapshot snapshot(Skill skill) throws Exception {
        ChainSegment segment = TechniqueChain.split(skill).get(0);
        Method read = EnchantmentSnapshot.class.getDeclaredMethod("read", Skill.class, List.class);
        read.setAccessible(true);
        Object loaded = read.invoke(null, skill, segment.enchantIndexes());
        Method types = EnchantmentSnapshot.class.getDeclaredMethod("distinctBasicTypeIds", Skill.class, List.class);
        types.setAccessible(true);
        @SuppressWarnings("unchecked")
        List<String> typeIds = (List<String>) types.invoke(null, skill, segment.enchantIndexes());
        Constructor<?> constructor = EnchantmentSnapshot.class.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Class<?> readType = loaded.getClass();
        return (EnchantmentSnapshot) constructor.newInstance(UUID.randomUUID(), fieldFloat(readType, loaded, "damage"),
                fieldInt(readType, loaded, "burnSeconds"), fieldInt(readType, loaded, "controlTicks"),
                fieldBoolean(readType, loaded, "extinguish"), fieldFloat(readType, loaded, "knockback"),
                fieldInt(readType, loaded, "tideMarks"), fieldInt(readType, loaded, "seedPoints"), typeIds);
    }

    private static DomainInstance domain(DomainDefinition definition, int startedAt, int life) {
        return new DomainInstance(UUID.randomUUID(), null,
                DomainDefinition.feetBox(new net.minecraft.world.phys.Vec3(0, 64, 0), 7, 7, 8),
                definition, startedAt, life, null);
    }

    private static Skill skill(Technique... techniques) {
        java.util.ArrayList<ShuAttribute> embryos = new java.util.ArrayList<>();
        for (int index = 0; index < techniques.length; index++) {
            embryos.add(ShuAttribute.NONE);
        }
        return new Skill("pulse-check", SkillType.MAGIC, List.of(techniques), embryos);
    }

    private static int fieldInt(Class<?> type, Object value, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getInt(value);
    }

    private static float fieldFloat(Class<?> type, Object value, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getFloat(value);
    }

    private static boolean fieldBoolean(Class<?> type, Object value, String name) throws Exception {
        Field field = type.getDeclaredField(name);
        field.setAccessible(true);
        return field.getBoolean(value);
    }

    private static void check(boolean passed, String label) {
        assertions++;
        if (!passed) {
            failures++;
            System.out.println("FAIL " + label);
        }
    }
}

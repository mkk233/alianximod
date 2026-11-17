package com.bslx.XueXiMod.shu;

import com.bslx.XueXiMod.shu.technique.TechniqueEffectRegistry;
import com.bslx.XueXiMod.shu.technique.effects.WaterFormEffect;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.conditional.WaterPrisonEffect;
import com.bslx.XueXiMod.shu.technique.release.water.WaterBeatBridge;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;

/** 留下的水系成品：缺月、水幕、水雾、水环、深海重压、旋流、气泡、水牢。 */
public final class WaterTechniques {
    private static final WaterFormEffect FORM = new WaterFormEffect();

    private WaterTechniques() {
    }

    public static void register() {
        WaterBeatBridge.install();
        WaterReleaseDefinition blade = WaterReleaseDefinition.shot("water_blade", Trajectory.STRAIGHT, 10, 1.3f, 0.35f)
                .along(3.2f).span(0.7f).energy(1.35f).wetness(6)
                .swordForm();
        Technique bladeTechnique = new Technique("water_blade", "缺月", TechniqueType.SWORD, 7f, 14, 20,
                "随挥剑扫出月牙形水流弧斩，可连续衔接", 0, ShuAttribute.WATER, blade.speed());
        TechniqueManager.register(bladeTechnique);
        ReleaseRegistry.register(blade);
        TechniqueEffectRegistry.register("water_blade", FORM);
        barrier("water_curtain", "水幕", 40, 0, "身前立起一道固定的薄水墙，穿过它的箭伤害减半",
                BarrierDefinition.curtain());
        domain("water_mist", "雾雨", 80, 10, "固定的雨雾领域。完整进入的敌人出不去，并会变潮湿",
                DomainDefinition.mistRain());
        guard("water_ring", "水环", 40, 0, "环绕自身的水环，受到的伤害削去两成",
                GuardDefinition.personalRing());
        domain("water_deep_pressure", "深海重压", 80, 10, "低矮的重压领域。范围内暂时积起不流动的水，里面的人变慢、变潮湿，氧气消耗加快",
                DomainDefinition.deepPressure());
        domain("water_vortex", "水流旋涡", 80, 10, "干地水平牵引。入卷收取水势；与上升气泡和真实水组成龙卷后才按湿度扣返",
                DomainDefinition.vortex());
        domain("water_bubble_column", "气泡水柱", 60, 10, "真实水里的上升水柱。底部与旋流重合时挂上去，父旋流关闭后不再独立续挂",
                DomainDefinition.bubble());
        prison();
    }

    /**
     * 不登记水系命中效果。那条生成会打出命中体，水牢的伤害必须保持为 0。
     */
    private static void prison() {
        WaterReleaseDefinition definition = WaterReleaseDefinition.conditional("water_prison", 40, WaterPrisonEffect.INSTANCE);
        Technique technique = new Technique("water_prison", "水牢", TechniqueType.MAGIC, 0f, 400, 20,
                "以自身为中心十二格，把周围潮湿的敌对目标关进水牢。每座成功消耗四十法力，关不进去不耗法力。",
                0, ShuAttribute.WATER, 0f);
        TechniqueManager.register(technique);
        ReleaseRegistry.register(definition);
    }

    private static void shot(String releaseId, String name, float damage, int cooldown, int complexity,
                             String description, WaterReleaseDefinition definition) {
        Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, damage, cooldown, 20,
                description, complexity, ShuAttribute.WATER, definition.speed());
        TechniqueManager.register(technique);
        ReleaseRegistry.register(definition);
        TechniqueEffectRegistry.register(releaseId, FORM);
    }

    private static void zone(String releaseId, String name, int complexity, String description,
                             WaterReleaseDefinition definition) {
        zone(releaseId, name, 40, complexity, description, definition);
    }

    private static void zone(String releaseId, String name, int cooldown, int complexity, String description,
                             WaterReleaseDefinition definition) {
        Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
                description, complexity, ShuAttribute.WATER, 0f);
        TechniqueManager.register(technique);
        ReleaseRegistry.register(definition);
        TechniqueEffectRegistry.register(releaseId, FORM);
    }

    private static void domain(String releaseId, String name, int cooldown, int complexity, String description,
                               DomainDefinition domain) {
        Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
                description, complexity, ShuAttribute.WATER, 0f);
        TechniqueManager.register(technique);
        ReleaseRegistry.register(WaterReleaseDefinition.domain(releaseId, domain));
        TechniqueEffectRegistry.register(releaseId, FORM);
    }

    private static void barrier(String releaseId, String name, int cooldown, int complexity, String description,
                                BarrierDefinition barrier) {
        Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
                description, complexity, ShuAttribute.WATER, 0f);
        TechniqueManager.register(technique);
        ReleaseRegistry.register(WaterReleaseDefinition.barrier(releaseId, barrier));
        TechniqueEffectRegistry.register(releaseId, FORM);
    }

    private static void guard(String releaseId, String name, int cooldown, int complexity, String description,
                              GuardDefinition guard) {
        Technique technique = new Technique(releaseId, name, TechniqueType.MAGIC, 0f, cooldown, 20,
                description, complexity, ShuAttribute.WATER, 0f);
        TechniqueManager.register(technique);
        ReleaseRegistry.register(WaterReleaseDefinition.bodyGuard(releaseId, guard));
        TechniqueEffectRegistry.register(releaseId, FORM);
    }

    /** 外部注册完成、封存之前调用。只登记水系专项，不登记标准伤害、耗魔和冷却。 */
    public static void declareSpecialBalances() {
        ReleaseDefinition blade = ReleaseRegistry.get("water_blade");
        if (blade != null) {
            SpecialBalance.publish(SpecialBalance.thickness(
                    SpecialBalance.speed(
                            SpecialBalance.life(SpecialBalance.begin("water_blade"), blade.lifetimeTicks(), "缺月寿命"),
                            blade.speed(), "缺月速度"),
                    blade.thickness(), "缺月厚度"));
        }
        BarrierDefinition.declareSpecialBalances();
        GuardDefinition.declareSpecialBalances();
        DomainDefinition.declareSpecialBalances();
        WaterPrisonEffect.declareBalance();
    }
}

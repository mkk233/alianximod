package com.bslx.XueXiMod.shu.technique;

import com.bslx.XueXiMod.shu.ElementReaction;
import com.bslx.XueXiMod.shu.ShuAttribute;
import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceDeclarationException;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.enchant.SpecialBalance;
import com.bslx.XueXiMod.status.wood.vine.VineMarkBalance;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantDefinition;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantNumbers;
import com.bslx.XueXiMod.shu.technique.enchant.resource.ResourceEnchantSlice;
import com.bslx.XueXiMod.shu.technique.release.fire.FireLimits;
import com.bslx.XueXiMod.shu.technique.release.fire.ResourceEffectKeys;
import com.bslx.XueXiMod.shu.technique.release.fire.SteamTuning;
import com.bslx.XueXiMod.shu.technique.release.firecast.GroundFireFields;
import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceContactField;
import com.bslx.XueXiMod.shu.technique.release.firecast.SurfaceFireHosts;

public final class EnchantmentDefinitions {
    private EnchantmentDefinitions() {}

    public static void registerAll() {
        register("magic_ignite", ShuAttribute.FIRE, "fire");
        register("vine_mark", ShuAttribute.WOOD, "wood");
        register("tide_mark", ShuAttribute.WATER, "water");
        register("sinking_edge", ShuAttribute.EARTH, "earth");
        register("keen_mark", ShuAttribute.METAL, "metal");
        registerResource(ResourceEffectKeys.TRAIL, ShuAttribute.FIRE, ResourceEnchantSlice.trail());
        registerResource(ResourceEffectKeys.SOW, ShuAttribute.FIRE, ResourceEnchantSlice.sow());
        registerResource(ResourceEffectKeys.KINDLE, ShuAttribute.FIRE, ResourceEnchantSlice.kindle());
        registerResource(ResourceEffectKeys.DETONATE, ShuAttribute.FIRE, ResourceEnchantSlice.detonate());
        registerResource(ResourceEffectKeys.GLOOM, ShuAttribute.FIRE, ResourceEnchantSlice.gloom());
        registerResource(ResourceEffectKeys.BREAK_GROUND, ShuAttribute.EARTH, ResourceEnchantSlice.breakGround());
    }

    private static void registerResource(String enchantmentId, ShuAttribute element, ResourceEnchantSlice slice) {
        EnchantmentRegistry.register(new ResourceEnchantDefinition(enchantmentId, element, slice));
    }

    private static void register(String enchantmentId, ShuAttribute element, String effectId) {
        EnchantmentRegistry.register(new EnchantmentDefinition() {
            @Override
            public String id() {
                return enchantmentId;
            }

            @Override
            public ShuAttribute element() {
                return element;
            }

            @Override
            public EnchantmentAccumulator createAccumulator() {
                return new EnchantmentAccumulator(effectId, 0, 0.5f, 5f, 1f, 1f, 1f, 1f);
            }

            @Override
            public EnchantmentAccumulator fold(ElementReaction reaction, EnchantmentAccumulator current) {
                EnchantmentAccumulator base = current != null ? current : createAccumulator();
                return new EnchantmentAccumulator(
                        base.effectId(),
                        base.layers() + 1,
                        base.radiusBase(),
                        base.durationBase(),
                        base.radiusMultiplier() * reaction.radiusMultiplier(),
                        base.durationMultiplier() * reaction.durationMultiplier(),
                        base.damageMultiplier() * reaction.damageMultiplier(),
                        base.speedMultiplier() * reaction.speedMultiplier());
            }

            @Override
            public int magicDamage(ElementReaction reaction) {
                if ("vine_mark".equals(enchantmentId)) {
                    return 0;
                }
                if ("tide_mark".equals(enchantmentId)) {
                    return 2;
                }
                return (int) Math.ceil(2.0 * reaction.damageMultiplier() - 1.0E-6);
            }
        });
    }

    /**
     * 只声明 C 名下专项参数。标准费用、冷却和伤害倍率由 A 登记。
     * 句柄必须是 A 传出的当前目录，声明经它写入，不另建一份。
     */
    public static void declareBalance(BalanceRegistry registry) {
        if (registry == null) {
            throw new BalanceDeclarationException("附魔平衡声明需要当前参数目录");
        }
        registry.acceptSpecial(TechniqueBalanceSpec.builder(SpecialBalance.LIAOYUAN)
                .numberParameter(BalanceKeys.RADIUS, RawDefault.literal(GroundFireFields.BASE_RADIUS),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS,
                        "燎原半径。调用方乘体积倍率后传入火场，火场不再读一次。")
                .build());
        registry.acceptSpecial(TechniqueBalanceSpec.builder(SpecialBalance.SCORCH_FIELD)
                .integerParameter(BalanceKeys.LIFETIME_TICKS, RawDefault.literal(SurfaceFireHosts.FIELD_LIFE),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "灼地寿命。只在灼地宿主出生时读取。")
                .numberParameter(SpecialBalance.HALF_EXTENT, RawDefault.literal(SurfaceContactField.HALF_EXTENT),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "灼地水平半幅。前进步长和竖直松弛不开放。")
                .build());
        registry.acceptSpecial(TechniqueBalanceSpec.builder(ResourceEffectKeys.GLOOM)
                .integerParameter(BalanceKeys.SEED_COUNT, RawDefault.literal(ResourceEnchantNumbers.GLOOM_BASE),
                        BalanceBound.count(0.0d, FireLimits.SEED_CAP), BalanceUnit.COUNT,
                        "幽冥产种基数。炉心产种倍率仍在其后乘一次。")
                .integerParameter(SpecialBalance.PERIOD_TICKS, RawDefault.literal(ResourceEnchantNumbers.GLOOM_PERIOD_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "幽冥产种周期。不乘全局冷却倍率。")
                .build());
        registry.acceptSpecial(TechniqueBalanceSpec.builder(ResourceEffectKeys.SOW)
                .integerParameter(BalanceKeys.SEED_COUNT, RawDefault.literal(ResourceEnchantNumbers.SOW_BASE),
                        BalanceBound.count(0.0d, FireLimits.SEED_CAP), BalanceUnit.COUNT,
                        "种火附魔基数。只替换附魔这一截，不改本体基数。")
                .build());
        registry.acceptSpecial(TechniqueBalanceSpec.builder(ResourceEffectKeys.KINDLE)
                .numberParameter(BalanceKeys.SEGMENT_DAMAGE,
                        RawDefault.literal((double) ResourceEnchantNumbers.KINDLE_SEGMENT_DAMAGE),
                        BalanceBound.flatDamage(), BalanceUnit.DAMAGE, "引燃段伤基数。先代入原公式，再做一次伤害缩放。")
                .integerParameter(SpecialBalance.INTERVAL_TICKS, RawDefault.literal(ResourceEnchantNumbers.KINDLE_INTERVAL_TICKS),
                        BalanceBound.inclusive(FireLimits.INTERVAL_FLOOR, FireLimits.INTERVAL_CAP), BalanceUnit.TICKS,
                        "引燃段间隔。每段燃料需求不开放。")
                .integerParameter(SpecialBalance.FLAT_TICKS, RawDefault.literal(ResourceEnchantNumbers.KINDLE_FLAT_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "引燃基础持续刻数。")
                .integerParameter(SpecialBalance.TIER_TICKS, RawDefault.literal(ResourceEnchantNumbers.KINDLE_TIER_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "引燃每档追加刻数。")
                .build());
        registry.acceptSpecial(TechniqueBalanceSpec.builder(SpecialBalance.COOL)
                .numberParameter(BalanceKeys.RADIUS, RawDefault.literal(SteamTuning.RADIUS),
                        BalanceBound.positiveSize(), BalanceUnit.BLOCKS, "主动冷却蒸汽半径。击退强度不开放。")
                .build());
        VineMarkBalance.declare(registry);
        registry.acceptSpecial(TechniqueBalanceSpec.builder(SpecialBalance.MAGIC_IGNITE)
                .integerParameter(SpecialBalance.BURN_SECONDS, RawDefault.literal(SpecialBalance.IGNITE_BURN_SECONDS),
                        BalanceBound.count(1.0d, BalanceBound.TICK_UPPER), BalanceUnit.COUNT,
                        "灼印着火秒数基数。不控制原版燃烧伤害。")
                .build());
    }
}

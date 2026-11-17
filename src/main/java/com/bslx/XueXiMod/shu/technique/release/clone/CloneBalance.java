package com.bslx.XueXiMod.shu.technique.release.clone;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.balance.RawDamage;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.release.clone.core.CloneKind;

/**
 * 分身底座的数值单一来源。专项只声明真实消费的寿命、生命、诱敌、固定伤害和射程。
 * 标准魔力、冷却和伤害倍率由封存补齐，这里不重复声明，读取时也不再乘第二遍。
 */
public final class CloneBalance {
    public static final String PHANTOM_ID = "clone_phantom";
    public static final String BODY_ID = "clone_body";
    public static final String PULSE_ID = "clone_response_pulse";
    public static final String MANA_KEY = "manaCost";
    public static final String LIFETIME_KEY = "lifetimeTicks";
    public static final String HEALTH_KEY = "maxHealth";
    public static final String DECOY_KEY = "decoyRadius";
    public static final String FIXED_DAMAGE_KEY = "fixedDamage";
    public static final String RANGE_KEY = "range";

    public static final int SPAWN_MANA_COST = 10;
    public static final int SPAWN_COOLDOWN_TICKS = 20;
    public static final int LIFETIME_TICKS = 0;
    public static final double BODY_HEALTH = 20.0d;
    public static final double DECOY_RADIUS = 8.0d;
    public static final int PULSE_MANA_COST = 2;
    public static final int PULSE_COOLDOWN_TICKS = 20;
    /** 标准声明的代码默认是 1。伤害读取只走一次 damageContribution，不再乘这个数。 */
    public static final double PULSE_DAMAGE_MULTIPLIER = 1.0d;
    public static final double FIXED_DAMAGE = 2.0d;
    public static final double PULSE_RANGE = 8.0d;
    public static final float RAW_SPAWN_DAMAGE = 0.0f;
    public static final float RAW_PULSE_DAMAGE = 2.0f;
    public static final int LEVEL_CAP = 1;
    public static final int COMPLEXITY = 0;
    public static final float PROJECTILE_SPEED = 0.0f;

    private CloneBalance() {
    }

    public static void declareSpecials() {
        declareSpawn(PHANTOM_ID);
        declareSpawn(BODY_ID);
        BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder(PULSE_ID)
                .numberParameter(FIXED_DAMAGE_KEY, RawDefault.literal(FIXED_DAMAGE),
                        BalanceBound.inclusive(0, 1_000_000), BalanceUnit.COUNT, "生命伤害")
                .numberParameter(RANGE_KEY, RawDefault.literal(PULSE_RANGE),
                        BalanceBound.inclusive(0.1, 32), BalanceUnit.BLOCKS, "脉冲射程")
                .build());
    }

    public static CloneSpawnQuote readSpawn(BalanceSide side, CloneKind kind) {
        requireServer(side);
        if (kind == null) {
            throw new IllegalArgumentException("分身类型不能为空");
        }
        String techniqueId = switch (kind) {
            case PHANTOM -> PHANTOM_ID;
            case NORMAL -> BODY_ID;
        };
        int manaCost = BalanceService.manaCost(BalanceSide.SERVER, techniqueId, MANA_KEY, SPAWN_MANA_COST);
        int cooldownTicks = BalanceService.cooldownTicks(BalanceSide.SERVER, techniqueId, SPAWN_COOLDOWN_TICKS);
        int lifetimeTicks = BalanceService.intValue(BalanceSide.SERVER, techniqueId, LIFETIME_KEY);
        double bodyHealth = BalanceService.number(BalanceSide.SERVER, techniqueId, HEALTH_KEY);
        double decoyRadiusBlocks = BalanceService.number(BalanceSide.SERVER, techniqueId, DECOY_KEY);
        return new CloneSpawnQuote(techniqueId, kind, manaCost, cooldownTicks, lifetimeTicks, bodyHealth,
                decoyRadiusBlocks);
    }

    public static ClonePulseQuote readPulse(BalanceSide side) {
        requireServer(side);
        int manaCost = BalanceService.manaCost(BalanceSide.SERVER, PULSE_ID, MANA_KEY, PULSE_MANA_COST);
        int cooldownTicks = BalanceService.cooldownTicks(BalanceSide.SERVER, PULSE_ID, PULSE_COOLDOWN_TICKS);
        double rawFixedDamage = BalanceService.number(BalanceSide.SERVER, PULSE_ID, FIXED_DAMAGE_KEY);
        ConfiguredDamage configuredDamage = BalanceService.damageContribution(
                BalanceSide.SERVER, PULSE_ID, RawDamage.of(rawFixedDamage));
        double rangeBlocks = BalanceService.number(BalanceSide.SERVER, PULSE_ID, RANGE_KEY);
        return new ClonePulseQuote(PULSE_ID, configuredDamage, manaCost, cooldownTicks, rangeBlocks, rawFixedDamage);
    }

    private static void declareSpawn(String techniqueId) {
        BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder(techniqueId)
                .integerParameter(LIFETIME_KEY, RawDefault.literal(LIFETIME_TICKS),
                        BalanceBound.count(0, 1_200_000), BalanceUnit.TICKS,
                        "没有自然到期写 0，正数从生成提交的服务器单调 tick 起算")
                .numberParameter(HEALTH_KEY, RawDefault.literal(BODY_HEALTH),
                        BalanceBound.inclusive(1, 1024), BalanceUnit.COUNT, "生命值（非实体个数）")
                .numberParameter(DECOY_KEY, RawDefault.literal(DECOY_RADIUS),
                        BalanceBound.inclusive(0, 16), BalanceUnit.BLOCKS, "诱敌半径，0 关闭")
                .build());
    }

    private static void requireServer(BalanceSide side) {
        if (side != BalanceSide.SERVER) {
            throw new IllegalArgumentException("分身平衡只能在服务端读取");
        }
    }

    public record CloneSpawnQuote(String techniqueId, CloneKind kind, int manaCost,
                                  int cooldownTicks, int lifetimeTicks, double bodyHealth,
                                  double decoyRadiusBlocks) {
    }

    public record ClonePulseQuote(String techniqueId, ConfiguredDamage configuredDamage,
                                  int manaCost, int cooldownTicks, double rangeBlocks,
                                  double rawFixedDamage) {
    }
}

package com.bslx.XueXiMod.shu.technique.enchant;

import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import com.bslx.XueXiMod.shu.balance.RawDamage;

/**
 * C 名下专项键拼写，以及火伤贡献的唯一转换。
 * 两个伤害倍率都是 1 时保留原公式的 double，避免提前收成 float 改变燃料比例。
 */
public final class SpecialBalance {
    public static final String LIAOYUAN = "fire_liaoyuan";
    public static final String FENTIAN = "fire_fentian";
    public static final String LIAOLI = "fire_liaoli";
    public static final String SCORCH_FIELD = "fire_scorch_field";
    public static final String COOL = "fire_cool";
    public static final String TIDE_MARK = "tide_mark";
    public static final String VINE_MARK = "vine_mark";
    public static final String MAGIC_IGNITE = "magic_ignite";

    public static final String CONTROL_TICKS = "controlTicks";
    public static final String BURN_SECONDS = "burnSeconds";
    public static final String PERIOD_TICKS = "periodTicks";
    public static final String INTERVAL_TICKS = "intervalTicks";
    public static final String FLAT_TICKS = "flatTicks";
    public static final String TIER_TICKS = "tierTicks";
    public static final String HALF_EXTENT = "halfExtent";

    public static final int VINE_CONTROL_TICKS = 40;
    public static final int IGNITE_BURN_SECONDS = 5;

    private SpecialBalance() {
    }

    public static double settle(String techniqueId, double rawAmount) {
        ConfiguredDamage configured = BalanceService.damageContribution(techniqueId, RawDamage.of(rawAmount));
        float configuredAmount = configured.amount();
        if (Float.compare(configuredAmount, (float) rawAmount) == 0) {
            return rawAmount;
        }
        return configuredAmount;
    }

    public static int controlTicks(float durationMultiplier) {
        int controlBase = BalanceService.intValue(VINE_MARK, CONTROL_TICKS);
        return Math.max(1, Math.round(controlBase * durationMultiplier));
    }

    public static int burnSeconds(float durationScale) {
        int burnBase = BalanceService.intValue(MAGIC_IGNITE, BURN_SECONDS);
        return Math.max(1, Math.round(burnBase * durationScale));
    }
}

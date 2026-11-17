package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import com.bslx.XueXiMod.shu.balance.ConfiguredDamage;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * 一次载荷的冻结报价。拒绝时没有伤害对象，尺寸和费用都是 0。
 * 已接受的伤害、魔力和冷却必须落在 0 到 100 万的闭区间。
 */
public record CaptureQuote(
        boolean accepted,
        @Nullable PayloadQuoteFault fault,
        String playerText,
        String techniqueId,
        int adapterVersion,
        @Nullable ConfiguredDamage configuredDamage,
        int configuredMana,
        int configuredCooldownTicks,
        double depthBlocks,
        double heightBlocks,
        double widthBlocks,
        int lifetimeTicks) {

    public static final int AMOUNT_LIMIT = 1_000_000;

    public CaptureQuote {
        Objects.requireNonNull(playerText, "playerText");
        if (techniqueId == null || techniqueId.isBlank()) {
            throw new IllegalArgumentException("报价缺少术式编号");
        }
        if (accepted) {
            if (fault != null || configuredDamage == null) {
                throw new IllegalArgumentException("已接受的报价缺少伤害");
            }
            if (adapterVersion < 1 || lifetimeTicks <= 0
                    || !finitePositive(depthBlocks)
                    || !finitePositive(heightBlocks)
                    || !finitePositive(widthBlocks)
                    || !withinAmount(configuredDamage.amount())
                    || !withinAmount(configuredMana)
                    || !withinAmount(configuredCooldownTicks)) {
                throw new IllegalArgumentException("已接受的报价超出合同范围");
            }
        } else if (fault == null || configuredDamage != null || playerText.isBlank()) {
            throw new IllegalArgumentException("被拒绝的报价必须给出原因和玩家文字，且不能带伤害");
        }
    }

    public static CaptureQuote accepted(String techniqueId, int adapterVersion, ConfiguredDamage configuredDamage,
                                        int configuredMana, int configuredCooldownTicks,
                                        double depthBlocks, double heightBlocks, double widthBlocks,
                                        int lifetimeTicks) {
        return new CaptureQuote(true, null, "", techniqueId, adapterVersion, configuredDamage,
                configuredMana, configuredCooldownTicks, depthBlocks, heightBlocks, widthBlocks, lifetimeTicks);
    }

    public static CaptureQuote refused(PayloadQuoteFault fault, String playerText, String techniqueId,
                                       int adapterVersion) {
        return new CaptureQuote(false, fault, playerText, techniqueId, adapterVersion, null,
                0, 0, 0.0d, 0.0d, 0.0d, 0);
    }

    private static boolean finitePositive(double sample) {
        return Double.isFinite(sample) && sample > 0.0d;
    }

    private static boolean withinAmount(float amount) {
        return Float.isFinite(amount) && amount >= 0.0f && amount <= AMOUNT_LIMIT;
    }

    private static boolean withinAmount(int amount) {
        return amount >= 0 && amount <= AMOUNT_LIMIT;
    }
}

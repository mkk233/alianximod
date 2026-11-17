package com.bslx.XueXiMod.shu.balance;

/**
 * 已声明的键才读取快照。没有伤害键时保留调用方原来的金额，避免给剑来一类术式补出倍率。
 */
public final class BalanceDisplay {
    private BalanceDisplay() {
    }

    public static float damage(String techniqueId, float rawAmount) {
        if (rawAmount == 0f || techniqueId == null || techniqueId.isEmpty()) {
            return rawAmount;
        }
        if (!BalanceService.isDeclared(techniqueId, BalanceKeys.DAMAGE_MULTIPLIER)) {
            return rawAmount;
        }
        return BalanceService.damageContribution(techniqueId, rawAmount).amount();
    }

    public static int cooldown(String techniqueId, int rawTicks) {
        if (rawTicks < 0) {
            return 0;
        }
        if (techniqueId == null || techniqueId.isEmpty()
                || !BalanceService.isDeclared(techniqueId, BalanceKeys.COOLDOWN_TICKS)) {
            return rawTicks;
        }
        return BalanceService.cooldownTicks(techniqueId, rawTicks);
    }

    /**
     * 原始值小于 0 时不读取。原始值是 0 仍会读取，这样覆盖值 0 可以生效。
     * 报价若必须保持 0、不能改读冻结默认值，调用方要在进来之前返回。
     */
    public static int mana(String techniqueId, String parameterKey, int rawAmount) {
        if (rawAmount < 0) {
            return 0;
        }
        if (techniqueId == null || techniqueId.isEmpty() || !BalanceService.isDeclared(techniqueId, parameterKey)) {
            return rawAmount;
        }
        return BalanceService.manaCost(techniqueId, parameterKey, rawAmount);
    }
}

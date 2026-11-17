package com.bslx.XueXiMod.shu.technique.release.clone.water;

import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceRegistry;
import com.bslx.XueXiMod.shu.balance.BalanceService;
import com.bslx.XueXiMod.shu.balance.BalanceSide;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.balance.TechniqueBalanceSpec;
import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;

/**
 * 水幻形自己的平衡。标准魔力和冷却等术式登记后由封存补上，这里只声明寿命和诱敌半径。
 * declareSpecials 由 ModTechniques.declareSpecialBalances 在封存前调用。
 * 键还没封存时 read 直接拒绝，不返回一套假的生效数字。
 */
public final class WaterPhantomBalance {
    public static final int MANA_COST = 10;
    public static final int COOLDOWN_TICKS = 20;
    public static final int LIFETIME_TICKS = 0;
    public static final double DECOY_RADIUS = 8.0D;

    private WaterPhantomBalance() {
    }

    public static void declareSpecials() {
        BalanceRegistry.declareSpecial(TechniqueBalanceSpec.builder(WaterPhantomDefinition.TECHNIQUE_ID)
                .integerParameter(BalanceKeys.LIFETIME_TICKS, RawDefault.literal(LIFETIME_TICKS),
                        BalanceBound.count(0, 1_200_000), BalanceUnit.TICKS,
                        "没有自然到期写 0，正数从生成提交的服务器单调 tick 起算")
                .numberParameter(CloneBalance.DECOY_KEY, RawDefault.literal(DECOY_RADIUS),
                        BalanceBound.inclusive(0, 16), BalanceUnit.BLOCKS, "诱敌半径，0 关闭")
                .build());
    }

    public static WaterPhantomQuote read(BalanceSide side) {
        if (!BalanceRegistry.isSealed()) {
            throw new IllegalStateException("水幻形平衡尚未封存，不能读取生效值");
        }
        if (side != BalanceSide.SERVER) {
            throw new IllegalArgumentException("水幻形平衡只能在服务端读取");
        }
        int manaCost = BalanceService.manaCost(BalanceSide.SERVER, WaterPhantomDefinition.TECHNIQUE_ID,
                BalanceKeys.MANA_COST, MANA_COST);
        int cooldownTicks = BalanceService.cooldownTicks(BalanceSide.SERVER, WaterPhantomDefinition.TECHNIQUE_ID,
                COOLDOWN_TICKS);
        int lifetimeTicks = BalanceService.intValue(BalanceSide.SERVER, WaterPhantomDefinition.TECHNIQUE_ID,
                BalanceKeys.LIFETIME_TICKS);
        double decoyRadius = BalanceService.number(BalanceSide.SERVER, WaterPhantomDefinition.TECHNIQUE_ID,
                CloneBalance.DECOY_KEY);
        return new WaterPhantomQuote(manaCost, cooldownTicks, lifetimeTicks, decoyRadius);
    }

    public record WaterPhantomQuote(int manaCost, int cooldownTicks, int lifetimeTicks, double decoyRadius) {
    }
}

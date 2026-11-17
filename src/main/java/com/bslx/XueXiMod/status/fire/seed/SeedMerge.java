package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import net.minecraft.world.entity.LivingEntity;

/**
 * 同一次确认命中的产种合并。新基数先相加，再乘当前产种倍率并向下取整一次；
 * 旧固定点数按原语义加在取整之后。然后进入同一余额，上限 100。
 * 不新建施法上下文。满额时增加量可以是 0，这次仍占掉该目标的产种机会。
 * 例：本体 2 与种火 10 合成 12，倍率 1.12 时请求为 13。旧点火的 2 不参与这次乘法。
 */
public final class SeedMerge {
    private SeedMerge() {
    }

    /** 只算出合并请求，不入账。 */
    public static int request(int scaledBase, int legacyPoints, double multiplier) {
        return FireSeedMath.mergedRequest(scaledBase, legacyPoints, multiplier);
    }

    /** 读施法者当前炉心倍率，把合并请求记进调用方给出的同一本施法账。 */
    public static SeedMergeResult commit(LivingEntity target, FireCastContext cast, LivingEntity caster,
                                         int scaledBase, int legacyPoints) {
        if (scaledBase < 0 || legacyPoints < 0) {
            return SeedMergeResult.rejected();
        }
        double multiplier = FurnaceService.seedMultiplier(caster);
        int requested = FireSeedMath.mergedRequest(scaledBase, legacyPoints, multiplier);
        return FireSeedState.mergePrepared(target, cast, requested);
    }
}

package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.status.fire.furnace.FurnaceService;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 调用方已经去重的一次产种。进入目标上的共享火种余额。
 * 每次新建独立施法上下文，所以不会占用、也不会改写调用方持有的那本一次性生产账。
 * 倍率只读施法者当前炉心。不造成伤害，不发命中，不种火，不引燃，不引爆。
 * 返回值是实际新增点数，供后续视觉在大于 0 时播放。
 * 同一命中的本体基数和种火不要分两次调用这里，应交给 SeedMerge。
 */
public final class FireSeedProduction {
    private FireSeedProduction() {
    }

    /** 用施法者当前炉心倍率产一次火种。倍率只读炉心，不改炉心热量。 */
    public static int produce(LivingEntity target, LivingEntity caster, int baseAmount) {
        if (baseAmount <= 0) {
            return 0;
        }
        double multiplier = FurnaceService.seedMultiplier(caster);
        UUID casterId = caster == null ? null : caster.getUUID();
        FireCastContext productionCast = new FireCastContext(UUID.randomUUID(), casterId);
        return FireSeedState.grant(target, productionCast, baseAmount, multiplier);
    }
}

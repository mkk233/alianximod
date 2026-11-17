package com.bslx.XueXiMod.status.fire.seed;

import com.bslx.XueXiMod.shu.technique.release.firecast.DamagePermission;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/**
 * 一次已经接受的攻击伤害挂上固定火种。
 * 每次调用新建施法上下文，不进入消费者共用的批次账，也不改生产和消费的去重。
 * 同一命中里的新基数和种火不要分两次调用这里，应交给 SeedMerge。
 */
public final class FireSeedGrant {
    private FireSeedGrant() {
    }

    /** 伤害许可通过后，用一份新的施法上下文把固定点数产进目标火种。 */
    public static int deposit(LivingEntity target, ServerPlayer caster, int points) {
        if (points <= 0 || !DamagePermission.allows(caster, target)) {
            return 0;
        }
        FireCastContext cast = new FireCastContext(UUID.randomUUID(), caster.getUUID());
        return FireSeedState.grant(target, cast, points, 1.0D);
    }
}

package com.bslx.XueXiMod.carrier;

import com.bslx.XueXiMod.shu.technique.release.batch.HitVerdict;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * 给 A 的陨岩接触入口。签名不变。
 * 实体候选返回未处理时，由 A 分类这一下。取消不会在这里扩圈或记成已处理。
 */
public final class BatchCarrierHooks {
    private BatchCarrierHooks() {
    }

    public static MeteorContactResult intercept(TechniqueHitEntity body, ServerLevel level, Vec3 point,
                                                LivingEntity struck, MeteorContactKind kind) {
        return BatchFlightBook.intercept(body, level, point, struck, kind);
    }

    public static boolean handled(MeteorContactResult result) {
        return result != null && result.handled();
    }

    public static HitVerdict verdict(MeteorContactResult result) {
        return result == null || result.verdict() == null ? HitVerdict.ESTABLISHED_NO_DAMAGE : result.verdict();
    }
}

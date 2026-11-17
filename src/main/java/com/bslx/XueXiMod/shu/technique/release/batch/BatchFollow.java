package com.bslx.XueXiMod.shu.technique.release.batch;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/** 父实例结算后的一次回调。子段由会话自己决定是否再等账本延迟。 */
public interface BatchFollow {
    void onVerdict(HitVerdict verdict, Vec3 point, Vec3 direction, LivingEntity target);
}

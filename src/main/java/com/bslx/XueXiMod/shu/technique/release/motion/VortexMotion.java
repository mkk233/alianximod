package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * 涡旋采样。不接受锁定落点，只按原点、前向、速度和年龄取样。
 * 不改无敌帧或账本。
 */
public final class VortexMotion implements TechniqueMotion {
    @Override
    /** 位置和朝向都用同一次公共采样结果。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        TrajectorySampler.Sample sample = TrajectorySampler.sample(
                Trajectory.VORTEX, entity.origin(), entity.forward(), entity.speed(),
                entity.lifetime(), entity.age(), null, false, entity.forward(), null);
        entity.placeAt(sample.position(), sample.direction());
    }
}

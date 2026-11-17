package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 按运动参数做抛物线。没有参数时用首版常数，不退回直线。 */
public final class ParabolaMotion implements TechniqueMotion {
    private final MotionProfile profile;

    public ParabolaMotion(MotionProfile profile) {
        MotionProfile resolved = profile == null ? MotionProfile.NONE : profile;
        this.profile = resolved.kind() == MotionProfile.Kind.PARABOLA
                ? resolved
                : MotionProfile.approvedParabola();
    }

    @Override
    /** 初速度先加上俯仰，再按年龄做弹道采样。不处理水平距离上限。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 velocity = TrajectorySampler.pitchedVelocity(entity.forward(), profile.speed(), profile.pitchDegrees());
        TrajectorySampler.Sample sample = TrajectorySampler.sampleBallistic(
                entity.origin(), velocity, profile.gravity(), entity.age(), profile.motionTicks(), 0.0D);
        entity.placeAt(sample.position(), sample.direction());
    }
}

package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

/** 单份竖直，多份绕冻结方向的锥面。水平距离到上限后停在边界上。 */
public final class UpwellMotion implements TechniqueMotion {
    private final MotionProfile profile;
    private final int spreadCount;
    private final int copyOrdinal;

    public UpwellMotion(MotionProfile profile, int spreadCount, int copyOrdinal) {
        MotionProfile resolved = profile == null ? MotionProfile.NONE : profile;
        this.profile = resolved.kind() == MotionProfile.Kind.UPWELL
                ? resolved
                : MotionProfile.approvedUpwell();
        this.spreadCount = Math.max(1, spreadCount);
        this.copyOrdinal = Math.max(0, copyOrdinal);
    }

    @Override
    /** 份数至少为 1，序号小于 0 时按第 0 份。重力、拍数和水平上限都来自运动参数。 */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 velocity = TrajectorySampler.upwellVelocity(entity.forward(), profile.speed(), profile.coneDegrees(),
                copyOrdinal, spreadCount);
        TrajectorySampler.Sample sample = TrajectorySampler.sampleBallistic(
                entity.origin(), velocity, profile.gravity(), entity.age(),
                profile.motionTicks(), profile.horizontalLimit());
        entity.placeAt(sample.position(), sample.direction());
    }
}

package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitSpec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 追踪。只借用原弹道每拍走过的距离，获得目标后不再回到原路径。
 * 直线且锁定时，初始方向先朝标记目标。不改无敌帧或账本。
 */
public final class TrackingMotion implements TechniqueMotion {
    private final Trajectory trajectory;
    private Vec3 baseForward;
    private Vec3 previousBase;
    private Vec3 direction;
    private UUID lockedTarget;
    private boolean acquired;

    public TrackingMotion(TechniqueHitSpec.Path path) {
        this.trajectory = switch (path) {
            case DROP -> Trajectory.DROP;
            case VORTEX -> Trajectory.VORTEX;
            default -> Trajectory.STRAIGHT;
        };
    }

    @Override
    /**
     * 第 0 拍仍放在原采样位置。之后没有目标就继续原路径；
     * 有目标后按转向限制前进，步长仍是原路径这一拍的距离。
     */
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (this.baseForward == null) {
            this.baseForward = entity.forward();
            if (this.trajectory == Trajectory.STRAIGHT && entity.lockThisCast()) {
                LivingEntity marked = LockTargetService.getMarkedTarget(level, entity.casterUuid());
                if (marked != null) {
                    Vec3 toward = marked.getBoundingBox().getCenter().subtract(entity.origin());
                    if (toward.lengthSqr() > 1.0E-8) {
                        this.baseForward = toward.normalize();
                    }
                }
            }
            TrajectorySampler.Sample initial = sample(entity, 0);
            this.previousBase = initial.position();
            this.direction = initial.direction();
        }
        TrajectorySampler.Sample base = sample(entity, entity.age());
        double distance = base.position().distanceTo(this.previousBase);
        this.previousBase = base.position();
        if (entity.age() == 0) {
            this.direction = DropMotion.slantedHeading(entity, base.direction());
            entity.setForward(this.direction);
            entity.placeAt(DropMotion.slantedPosition(entity, base.position()), this.direction);
            return;
        }
        LivingEntity target = TrackingSupport.resolveTarget(level, entity.position(), this.lockedTarget, entity::canTrack);
        this.lockedTarget = target == null ? null : target.getUUID();
        if (target != null) {
            this.acquired = true;
            this.direction = TrackingSupport.turnToward(this.direction,
                    target.getBoundingBox().getCenter().subtract(entity.position()));
        }
        if (!this.acquired) {
            this.direction = DropMotion.slantedHeading(entity, base.direction());
            entity.setForward(this.direction);
            entity.placeAt(DropMotion.slantedPosition(entity, base.position()), this.direction);
        } else {
            entity.setForward(this.direction);
            entity.placeAt(entity.position().add(this.direction.scale(distance)), this.direction);
        }
    }

    private TrajectorySampler.Sample sample(TechniqueHitEntity entity, int age) {
        return TrajectorySampler.sample(this.trajectory, entity.origin(), this.baseForward, entity.speed(),
                entity.lifetime(), age, null, false, this.baseForward, null);
    }
}

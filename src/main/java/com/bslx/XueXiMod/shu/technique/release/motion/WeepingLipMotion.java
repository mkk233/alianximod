package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.technique.effects.LockTargetService;
import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.TrajectorySampler;
import com.bslx.XueXiMod.shu.technique.release.WeepingCurtain;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 只移动垂丝上沿。帘身仍由 {@link WeepingCurtain#fit} 从上沿竖直挂下。
 * 没有追踪时，上沿沿出手方向伸出后略微下沉并停住。
 * 有追踪时，步长仍取直线采样，转向仍走现有追踪，不再把上沿拉回函数原点。
 */
public final class WeepingLipMotion implements TechniqueMotion {
    private static final double REACH_DISTANCE = 3.0;
    private static final int REACH_TICKS = 8;
    private static final double SAG_DISTANCE = 1.0;
    private static final int SAG_TICKS = 8;
    /** 垂丝登记速度是 0。上沿改走追踪时，用这个速度向直线采样要步长。 */
    private static final double TRACK_SPEED = 0.45;

    private final boolean tracking;
    private Vec3 lip;
    private Vec3 travelDirection;
    private Vec3 baseFacing;
    private Vec3 previousSample;
    private UUID trackedIdentity;
    private boolean acquiredTarget;
    private int reachTicks;
    private int sagTicks;

    public WeepingLipMotion(boolean tracking) {
        this.tracking = tracking;
    }

    @Override
    public void tick(TechniqueHitEntity entity, ServerLevel level) {
        if (this.lip == null) {
            this.lip = birthLip(entity);
            this.captureFacing(entity, level);
            if (this.tracking) {
                this.previousSample = sample(entity, 0).position();
            }
        }
        if (entity.age() > 0) {
            if (this.tracking) {
                this.advanceTracking(entity, level);
            } else {
                this.advanceNative();
            }
        }
        WeepingCurtain.moveLip(entity, this.lip);
        entity.setForward(this.travelDirection);
        TrajectorySampler.fitVolume(entity.functionId(), entity, level, null);
    }

    private void captureFacing(TechniqueHitEntity entity, ServerLevel level) {
        Vec3 facing = entity.forward();
        if (facing == null || facing.lengthSqr() < 1.0E-8) {
            facing = new Vec3(0.0, 0.0, 1.0);
        } else {
            facing = facing.normalize();
        }
        if (entity.lockThisCast()) {
            LivingEntity marked = LockTargetService.getMarkedTarget(level, entity.casterUuid());
            if (marked != null) {
                Vec3 toward = marked.getBoundingBox().getCenter().subtract(this.lip);
                if (toward.lengthSqr() > 1.0E-8) {
                    facing = toward.normalize();
                }
            }
        }
        this.travelDirection = facing;
        this.baseFacing = facing;
    }

    private void advanceNative() {
        if (this.reachTicks < REACH_TICKS) {
            this.lip = this.lip.add(this.travelDirection.scale(REACH_DISTANCE / REACH_TICKS));
            this.reachTicks++;
            return;
        }
        if (this.sagTicks < SAG_TICKS) {
            this.lip = this.lip.add(0.0, -SAG_DISTANCE / SAG_TICKS, 0.0);
            this.sagTicks++;
        }
    }

    private void advanceTracking(TechniqueHitEntity entity, ServerLevel level) {
        TrajectorySampler.Sample base = sample(entity, entity.age());
        double distance = base.position().distanceTo(this.previousSample);
        this.previousSample = base.position();
        LivingEntity target = TrackingSupport.resolveTarget(level, this.lip, this.trackedIdentity, entity::canTrack);
        this.trackedIdentity = target == null ? null : target.getUUID();
        if (target != null) {
            this.acquiredTarget = true;
            this.travelDirection = TrackingSupport.turnToward(this.travelDirection,
                    target.getBoundingBox().getCenter().subtract(this.lip));
        } else if (!this.acquiredTarget) {
            this.travelDirection = this.baseFacing;
        }
        if (this.travelDirection == null || this.travelDirection.lengthSqr() < 1.0E-8) {
            this.travelDirection = new Vec3(0.0, 0.0, 1.0);
        } else {
            this.travelDirection = this.travelDirection.normalize();
        }
        this.lip = this.lip.add(this.travelDirection.scale(distance));
    }

    private TrajectorySampler.Sample sample(TechniqueHitEntity entity, int lived) {
        Vec3 origin = entity.origin() == null ? Vec3.ZERO : entity.origin();
        return TrajectorySampler.sample(Trajectory.STRAIGHT, origin, this.baseFacing, TRACK_SPEED,
                entity.lifetime(), lived, null, false, this.baseFacing, null);
    }

    private static Vec3 birthLip(TechniqueHitEntity entity) {
        Vec3 origin = entity.origin();
        if (origin == null || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
            origin = Vec3.ZERO;
        }
        if (entity.automaticLifetime()) {
            return origin;
        }
        return WeepingCurtain.lipOf(origin);
    }
}

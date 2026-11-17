package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import net.minecraft.world.phys.Vec3;

/**
 * 水环绕行六拍。出口角和绕行方向在构造时定死，两条路线不共享这段进度。
 * 环心和法线每次取样读当前主人与当前姿态；法线变化时基底只做最小旋转，角度不另抽。
 * {@link #emergencyRelease(long)} 在正常走完时给出径向朝外的单位方向；主人失效时给出当时位置和方向并封存。
 * 同一 tick 再问不会重抽随机，也不会把时钟再推一拍。速度幅值由弹体批保存。
 */
public final class RingOrbitRoute implements CaptureRoute {
    private final RingFollow follow;
    private final Vec3 contact;
    private final long captureTick;
    private final double radius;
    private final double entryAngle;
    private final double exitAngle;
    private final int windingSign;
    private final double signedTravel;
    private final RingBandGeometry.PlaneBasis captureBasis;
    private final Vec3 captureCenter;
    private boolean sealed;
    private boolean lost;
    private Vec3 sealedPosition;
    private Vec3 sealedDirection;

    public RingOrbitRoute(RingFollow follow, Vec3 contact, Vec3 incomingVelocity, long captureTick,
                          double randomUnit, double radius) {
        this.follow = follow;
        this.contact = contact == null ? Vec3.ZERO : contact;
        this.captureTick = captureTick;
        this.radius = radius > 1.0E-6 ? radius : 0.9;
        Vec3 center = this.contact;
        Vec3 normal = new Vec3(0.0, 1.0, 0.0);
        if (follow != null && follow.live() && finite(follow.center()) && finite(follow.planeNormal())) {
            center = follow.center();
            normal = follow.planeNormal();
        }
        this.captureCenter = center;
        this.captureBasis = RingBandGeometry.PlaneBasis.of(normal);
        this.entryAngle = RingBandGeometry.entryAngle(this.contact, center, captureBasis);
        Vec3 anchor = follow == null || follow.ownerAnchor() == null ? center : follow.ownerAnchor();
        this.exitAngle = RingBandGeometry.chooseExitAngle(entryAngle, randomUnit, center, captureBasis, this.radius, anchor);
        this.windingSign = RingBandGeometry.winding(entryAngle, incomingVelocity, captureBasis);
        this.signedTravel = RingBandGeometry.signedTravel(entryAngle, exitAngle, windingSign);
        if (follow == null || !follow.live()) {
            seal(this.contact, outward(captureBasis, exitAngle), true);
        }
    }

    /** 当前环上的位置。正常走完时样本结束。 */
    @Override
    public RouteSample sample(long gameTick) {
        Pose pose = pose(gameTick);
        return new RouteSample(pose.position, pose.finished && !pose.lost);
    }

    /** 正常结束给径向朝外方向。主人失效时封存当时位置和方向。 */
    @Override
    public ReleaseSample emergencyRelease(long gameTick) {
        Pose pose = pose(gameTick);
        if (pose.finished || pose.lost) {
            seal(pose.position, pose.unitDirection, pose.lost && !pose.finished);
        }
        Pose stable = pose(gameTick);
        return new ReleaseSample(stable.position, stable.unitDirection);
    }

    /** 主人失效并封存后路线不再活动。 */
    @Override
    public boolean active() {
        return !lost;
    }

    private Pose pose(long gameTick) {
        if (sealed) {
            return new Pose(sealedPosition, sealedDirection, !lost, lost);
        }
        long elapsed = gameTick < captureTick ? 0L : gameTick - captureTick;
        boolean timeDone = elapsed >= RingBandGeometry.ORBIT_TICKS;
        boolean living = follow != null && follow.live();
        if (!living) {
            Pose lostPose = placed(elapsed, false);
            seal(lostPose.position, lostPose.unitDirection, true);
            return new Pose(sealedPosition, sealedDirection, false, true);
        }
        if (timeDone) {
            Pose done = placed(RingBandGeometry.ORBIT_TICKS, true);
            seal(done.position, done.unitDirection, false);
            return new Pose(sealedPosition, sealedDirection, true, false);
        }
        return placed(elapsed, false);
    }

    private Pose placed(long elapsed, boolean finished) {
        double alpha = RingBandGeometry.ORBIT_TICKS <= 0
                ? 1.0
                : Math.min(1.0, elapsed / (double) RingBandGeometry.ORBIT_TICKS);
        Vec3 center = captureCenter;
        RingBandGeometry.PlaneBasis basis = captureBasis;
        Vec3 anchor = captureCenter;
        if (follow != null && follow.live() && finite(follow.center()) && finite(follow.planeNormal())) {
            center = follow.center();
            basis = captureBasis.rotatedTo(follow.planeNormal());
            if (finite(follow.ownerAnchor())) {
                anchor = follow.ownerAnchor();
            }
        }
        double angle = entryAngle + signedTravel * alpha;
        Vec3 circle = center.add(basis.radial(angle, radius));
        Vec3 entryCircle = captureCenter.add(captureBasis.radial(entryAngle, radius));
        Vec3 position = circle.add(contact.subtract(entryCircle).scale(1.0 - alpha));
        Vec3 direction = finished ? releaseDirection(basis, anchor, position) : travelDirection(alpha, center, basis);
        return new Pose(position, direction, finished, false);
    }

    private Vec3 releaseDirection(RingBandGeometry.PlaneBasis basis, Vec3 anchor, Vec3 exitPoint) {
        Vec3 outward = outward(basis, exitAngle);
        if (finite(anchor) && outward.dot(anchor.subtract(exitPoint)) > 0.0) {
            outward = outward.scale(-1.0);
        }
        return outward;
    }

    private Vec3 travelDirection(double alpha, Vec3 center, RingBandGeometry.PlaneBasis basis) {
        double ahead = Math.min(1.0, alpha + 1.0 / RingBandGeometry.ORBIT_TICKS);
        double angle = entryAngle + signedTravel * ahead;
        Vec3 next = center.add(basis.radial(angle, radius));
        double currentAngle = entryAngle + signedTravel * alpha;
        Vec3 current = center.add(basis.radial(currentAngle, radius));
        Vec3 step = next.subtract(current);
        if (step.lengthSqr() < 1.0E-12) {
            return outward(basis, exitAngle);
        }
        return step.normalize();
    }

    private static Vec3 outward(RingBandGeometry.PlaneBasis basis, double angle) {
        Vec3 radial = basis.radial(angle, 1.0);
        if (radial.lengthSqr() < 1.0E-16) {
            return new Vec3(0.0, 1.0, 0.0);
        }
        return radial.normalize();
    }

    private void seal(Vec3 position, Vec3 direction, boolean markLost) {
        if (sealed) {
            return;
        }
        sealed = true;
        lost = markLost;
        sealedPosition = position == null ? contact : position;
        sealedDirection = direction == null || direction.lengthSqr() < 1.0E-16
                ? new Vec3(0.0, 1.0, 0.0)
                : direction.normalize();
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private record Pose(Vec3 position, Vec3 unitDirection, boolean finished, boolean lost) {
    }
}

package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import net.minecraft.world.phys.Vec3;

/**
 * 把弹体从接触点运到当前外沿，再沿剩余活动进度走到 1。不延长到拖尾，也不在沿途另开伤害。
 * 捕获时若本拍已经是活动终点，立刻在外沿按末端切线放出。
 * {@link #emergencyRelease(long)} 在正常 finished 时返回末端切线；宿主提前没了时返回当时位置并封存。
 * 切线是 sign(sweep) * (-forward sin(end) + cross cos(end))，不是径向。
 * 每条路线自带进度，不和另一支箭共用可变游标。速度幅值由弹体批保存。
 */
public final class ArcTransportRoute implements CaptureRoute {
    private final ArcBandSnapshot snapshot;
    private final ArcPresence presence;
    private final Vec3 contact;
    private final double contactProgress;
    private final long captureTick;
    private final long remaining;
    private final Vec3 endTangent;
    private boolean sealed;
    private boolean lost;
    private Vec3 sealedPosition;
    private Vec3 sealedDirection;

    public ArcTransportRoute(ArcBandSnapshot snapshot, ArcPresence presence, Vec3 contact, double contactProgress,
                             long captureTick) {
        this.snapshot = snapshot;
        this.presence = presence;
        this.contact = contact == null ? Vec3.ZERO : contact;
        this.contactProgress = Double.isFinite(contactProgress) ? contactProgress : 0.0;
        this.captureTick = captureTick;
        this.remaining = snapshot == null
                ? 0L
                : ArcSliceGeometry.remainingTicks(captureTick, snapshot.birthTick(), snapshot.activeTicks());
        Vec3 tangent = snapshot == null ? null : ArcSliceGeometry.endTangent(snapshot);
        this.endTangent = tangent == null ? new Vec3(0.0, 0.0, 1.0) : tangent;
        if (presence == null || !presence.present(captureTick)) {
            seal(this.contact, endTangent, true);
        }
    }

    /** 当前运送位置。正常走到终点时样本结束。 */
    @Override
    public RouteSample sample(long gameTick) {
        View view = view(gameTick);
        return new RouteSample(view.position, view.finished && !view.lost);
    }

    /** 正常结束给末端切线。宿主提前消失时封存当时位置。 */
    @Override
    public ReleaseSample emergencyRelease(long gameTick) {
        View view = view(gameTick);
        if (view.finished || view.lost) {
            seal(view.position, view.unitDirection, view.lost && !view.finished);
        }
        View stable = view(gameTick);
        return new ReleaseSample(stable.position, stable.unitDirection);
    }

    /** 宿主提前消失并封存后路线不再活动。 */
    @Override
    public boolean active() {
        return !lost;
    }

    private View view(long gameTick) {
        if (sealed) {
            return new View(sealedPosition, sealedDirection, !lost, lost);
        }
        long elapsed = gameTick < captureTick ? 0L : gameTick - captureTick;
        boolean timeDone = remaining <= 0L || elapsed >= remaining;
        boolean living = presence != null && presence.present(gameTick);
        if (!living) {
            View lostView = placed(Math.min(elapsed, Math.max(remaining, 0L)), false);
            seal(lostView.position, lostView.unitDirection, true);
            return new View(sealedPosition, sealedDirection, false, true);
        }
        if (timeDone) {
            View done = placed(remaining, true);
            seal(done.position, endTangent, false);
            return new View(sealedPosition, sealedDirection, true, false);
        }
        return placed(elapsed, false);
    }

    private View placed(long elapsed, boolean finished) {
        double alpha = remaining <= 0L ? 1.0 : Math.min(1.0, elapsed / (double) remaining);
        Vec3 position = snapshot == null
                ? contact
                : ArcSliceGeometry.transportPosition(snapshot, contact, contactProgress, alpha);
        Vec3 direction = finished ? endTangent : travelDirection(alpha, position);
        return new View(position, direction, finished, false);
    }

    private Vec3 travelDirection(double alpha, Vec3 position) {
        if (snapshot == null) {
            return endTangent;
        }
        double ahead = Math.min(1.0, alpha + (remaining <= 0L ? 1.0 : 1.0 / remaining));
        Vec3 next = ArcSliceGeometry.transportPosition(snapshot, contact, contactProgress, ahead);
        Vec3 step = next.subtract(position);
        if (step.lengthSqr() < 1.0E-12) {
            return endTangent;
        }
        return step.normalize();
    }

    private void seal(Vec3 position, Vec3 direction, boolean markLost) {
        if (sealed) {
            return;
        }
        sealed = true;
        lost = markLost;
        sealedPosition = position == null ? contact : position;
        sealedDirection = direction == null || direction.lengthSqr() < 1.0E-16
                ? endTangent
                : direction.normalize();
    }

    private record View(Vec3 position, Vec3 unitDirection, boolean finished, boolean lost) {
    }
}

package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.WaterSegment;
import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import net.minecraft.world.phys.Vec3;

/**
 * 箭从真实接触点沿当前瀑布向下，最多六拍。水平出口在构造时冻住，不跟着射手，也不先跳到水脚。
 * 水段没了、上沿追过箭、宿主撤掉时，停在已经走到的位置并给出冻结出口。
 * 方块挡路仍由捕获簿原来的检测处理，这里不销毁箭，也不另找绕路。
 */
public final class WeepingDownflowRoute implements CaptureRoute {
    public static final int FLOW_TICKS = 6;
    private static final double FOOT_EPSILON = 1.0E-6;

    private final Vec3 contact;
    private final Vec3 frozenAhead;
    private final long captureTick;
    private final SegmentSupply supply;
    private final FlowWindow flowWindow;
    private final double captureBottom;
    private final double captureTop;
    private final double fallDistance;
    private boolean sealed;
    private boolean lost;
    private Vec3 sealedPosition;
    private Vec3 sealedDirection;
    private Vec3 accepted;

    WeepingDownflowRoute(Vec3 contact, Vec3 frozenAhead, long captureTick, SegmentSupply supply, FlowWindow flowWindow) {
        this.contact = finite(contact) ? contact : Vec3.ZERO;
        Vec3 horizontal = WeepingDeflectionHost.horizontalAhead(frozenAhead);
        this.frozenAhead = horizontal == null ? new Vec3(0.0, 0.0, 1.0) : horizontal;
        this.captureTick = captureTick;
        this.supply = supply;
        this.flowWindow = flowWindow;
        this.accepted = this.contact;
        WaterSegment initial = supply == null ? null : supply.current();
        boolean open = flowWindow != null && flowWindow.open(captureTick) && WeepingDeflectionHost.usable(initial);
        this.captureBottom = open ? WeepingDeflectionHost.boxBottom(initial) : this.contact.y;
        this.captureTop = open ? WeepingDeflectionHost.boxTop(initial) : this.contact.y;
        double fall = this.contact.y - this.captureBottom;
        this.fallDistance = fall > 0.0 ? fall : 0.0;
        if (!open) {
            seal(this.contact, this.frozenAhead, true);
        } else if (this.fallDistance <= FOOT_EPSILON) {
            seal(new Vec3(this.contact.x, this.captureBottom, this.contact.z), this.frozenAhead, false);
        }
    }

    /** 当前下落位置。正常落到出口时样本结束。 */
    @Override
    public RouteSample sample(long gameTick) {
        FlowView placed = view(gameTick);
        return new RouteSample(placed.position, placed.finished && !placed.lost);
    }

    /** 提前放出时停在已经走到的位置，并使用冻结出口。 */
    @Override
    public ReleaseSample emergencyRelease(long gameTick) {
        FlowView placed = view(gameTick);
        if (!sealed) {
            seal(placed.position, frozenAhead, true);
            placed = view(gameTick);
        }
        return new ReleaseSample(placed.position, placed.unitDirection);
    }

    /** 水段失效并封存后路线不再活动。 */
    @Override
    public boolean active() {
        return !lost;
    }

    private FlowView view(long gameTick) {
        if (sealed) {
            return new FlowView(sealedPosition, sealedDirection, !lost, lost);
        }
        long elapsed = gameTick < captureTick ? 0L : gameTick - captureTick;
        if (elapsed > FLOW_TICKS) {
            elapsed = FLOW_TICKS;
        }
        Vec3 desired = descend(elapsed);
        if (flowWindow == null || supply == null || !flowWindow.open(gameTick)) {
            seal(accepted, frozenAhead, true);
            return new FlowView(sealedPosition, sealedDirection, false, true);
        }
        WaterSegment segment = supply.current();
        if (!WeepingDeflectionHost.usable(segment)) {
            seal(accepted, frozenAhead, true);
            return new FlowView(sealedPosition, sealedDirection, false, true);
        }
        if (overtaken(segment, desired)) {
            seal(accepted, frozenAhead, true);
            return new FlowView(sealedPosition, sealedDirection, false, true);
        }
        double currentBottom = WeepingDeflectionHost.boxBottom(segment);
        boolean reachedFoot = desired.y <= currentBottom + FOOT_EPSILON;
        if (reachedFoot || elapsed >= FLOW_TICKS) {
            double exitHeight = desired.y < currentBottom ? currentBottom : desired.y;
            seal(new Vec3(contact.x, exitHeight, contact.z), frozenAhead, false);
            return new FlowView(sealedPosition, sealedDirection, true, false);
        }
        accepted = desired;
        return new FlowView(desired, frozenAhead, false, false);
    }

    private Vec3 descend(long elapsed) {
        double traveled = FLOW_TICKS <= 0 ? fallDistance : fallDistance * Math.min(1.0, elapsed / (double) FLOW_TICKS);
        return new Vec3(contact.x, contact.y - traveled, contact.z);
    }

    private boolean overtaken(WaterSegment segment, Vec3 desired) {
        double excess = contact.y - captureTop;
        if (excess < 0.0) {
            excess = 0.0;
        }
        double ceiling = WeepingDeflectionHost.boxTop(segment) + excess;
        return desired.y > ceiling + FOOT_EPSILON;
    }

    private void seal(Vec3 position, Vec3 direction, boolean markLost) {
        if (sealed) {
            return;
        }
        sealed = true;
        lost = markLost;
        sealedPosition = finite(position) ? position : contact;
        if (!finite(direction) || direction.lengthSqr() < 1.0E-16) {
            sealedDirection = frozenAhead;
            return;
        }
        sealedDirection = direction.normalize();
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    interface SegmentSupply {
        WaterSegment current();
    }

    interface FlowWindow {
        boolean open(long gameTick);
    }

    private record FlowView(Vec3 position, Vec3 unitDirection, boolean finished, boolean lost) {
    }
}

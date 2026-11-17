package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.WaterSegment;
import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.lang.ref.WeakReference;
import java.util.Optional;
import java.util.UUID;

/**
 * 一面垂丝瀑布。命中用当前水段的竖直薄盒，宽厚和上下落差跟精确体积一致，不用实体的粗立方。
 * 好友、湿折扣、三次上限和箭的归属仍走原来的弹体批，这里不覆盖资格，也不做全身盾。
 * 同一实体的 {@link HostKey} 在构造时定死，之后只更换水段。
 */
public final class WeepingDeflectionHost implements DeflectionHost {
    static final long STABLE_GENERATION = 0L;
    private static final double SURFACE_EPSILON = 1.0E-8;

    private WeakReference<TechniqueHitEntity> bodyRef;
    private WeakReference<ServerLevel> levelRef;
    private final HostKey hostKey;
    private UUID ownerIdentity;
    private WaterSegment segment;
    private boolean withdrawn;

    private WeepingDeflectionHost(TechniqueHitEntity entity, ServerLevel serverLevel) {
        this.bodyRef = new WeakReference<>(entity);
        this.levelRef = new WeakReference<>(serverLevel);
        this.hostKey = new HostKey(entity.getUUID(), STABLE_GENERATION);
        this.ownerIdentity = entity.casterUuid();
    }

    static WeepingDeflectionHost open(TechniqueHitEntity entity, ServerLevel serverLevel) {
        return new WeepingDeflectionHost(entity, serverLevel);
    }

    void attach(TechniqueHitEntity entity, ServerLevel serverLevel) {
        if (withdrawn || entity == null || serverLevel == null) {
            return;
        }
        this.bodyRef = new WeakReference<>(entity);
        this.levelRef = new WeakReference<>(serverLevel);
        UUID liveOwner = entity.casterUuid();
        if (liveOwner != null) {
            this.ownerIdentity = liveOwner;
        }
    }

    void replaceSegment(WaterSegment segment) {
        if (withdrawn) {
            return;
        }
        this.segment = segment;
    }

    void withdraw() {
        this.withdrawn = true;
        this.segment = null;
    }

    boolean isWithdrawn() {
        return withdrawn;
    }

    WaterSegment segmentNow() {
        return segment;
    }

    /** 优先返回活体上的施术者，否则返回最后记下的主人。 */
    @Override
    public UUID ownerId() {
        TechniqueHitEntity body = bodyRef.get();
        if (body != null && body.casterUuid() != null) {
            return body.casterUuid();
        }
        return ownerIdentity;
    }

    /** 构造时定死的宿主身份。 */
    @Override
    public HostKey key() {
        return hostKey;
    }

    /** 当前弱引用还指着的服务端世界。 */
    @Override
    public ServerLevel level() {
        return levelRef.get();
    }

    /** 水段、实体和主人都仍有效。 */
    @Override
    public boolean active() {
        if (withdrawn || !usable(segment)) {
            return false;
        }
        TechniqueHitEntity body = bodyRef.get();
        ServerLevel serverLevel = levelRef.get();
        if (body == null || serverLevel == null || body.isRemoved() || !body.isAlive() || !body.deflectsProjectiles()) {
            return false;
        }
        if (body.level() != serverLevel || serverLevel.getServer() == null) {
            return false;
        }
        UUID owner = ownerId();
        if (owner == null) {
            return false;
        }
        ServerPlayer caster = serverLevel.getServer().getPlayerList().getPlayer(owner);
        return caster != null && caster.isAlive() && caster.level() == serverLevel;
    }

    boolean routeOpen(long gameTick) {
        return gameTick != Long.MIN_VALUE && active();
    }

    /** 当前竖直薄盒上的最早接触。 */
    @Override
    public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        if (!routeOpen(gameTick)) {
            return Optional.empty();
        }
        Optional<SlabHit> hit = intersectSegment(segment, from, toward, projectileRadius);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        SlabHit found = hit.get();
        return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
    }

    /** 点是否还在当前垂丝薄盒里。 */
    @Override
    public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
        if (!routeOpen(gameTick)) {
            return false;
        }
        return coversPoint(segment, point, projectileRadius);
    }

    /** 沿瀑布向下的路线。出口水平方向在构造时冻住。 */
    @Override
    public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
        Vec3 point = contact == null ? Vec3.ZERO : contact.point();
        // 入射速度不参与出口，避免顺着射手改方向。
        Vec3 exit = horizontalAhead(segment == null ? null : segment.ahead());
        if (exit == null) {
            exit = new Vec3(0.0, 0.0, 1.0);
        }
        return new WeepingDownflowRoute(point, exit, gameTick, this::segmentNow, this::routeOpen);
    }

    static Optional<SlabHit> intersectSegment(WaterSegment segment, Vec3 from, Vec3 toward, double projectileRadius) {
        Column column = Column.fit(segment, projectileRadius);
        if (column == null || !finite(from) || !finite(toward)) {
            return Optional.empty();
        }
        double[] fromLocal = column.local(from);
        double[] towardLocal = column.local(toward);
        double[] travel = new double[] {
                towardLocal[0] - fromLocal[0],
                towardLocal[1] - fromLocal[1],
                towardLocal[2] - fromLocal[2]
        };
        double[] window = new double[] {0.0, 1.0};
        int[] entryFace = new int[] {-1, 1};
        double[] halves = new double[] {column.halfThick, column.halfHeight, column.halfAcross};
        for (int axisIndex = 0; axisIndex < halves.length; axisIndex++) {
            if (!clipAxis(fromLocal[axisIndex], travel[axisIndex], -halves[axisIndex], halves[axisIndex], window, axisIndex, entryFace)) {
                return Optional.empty();
            }
        }
        if (window[0] > 1.0 + SURFACE_EPSILON || window[1] < -SURFACE_EPSILON) {
            return Optional.empty();
        }
        double segmentT = window[0];
        if (segmentT < 0.0) {
            segmentT = 0.0;
        } else if (segmentT > 1.0) {
            segmentT = 1.0;
        }
        Vec3 point = from.add(toward.subtract(from).scale(segmentT));
        Vec3 unitNormal = column.entryNormal(fromLocal, entryFace[0], entryFace[1]);
        Vec3 worldTravel = toward.subtract(from);
        if (worldTravel.lengthSqr() > 1.0E-16 && worldTravel.dot(unitNormal) > 0.0) {
            unitNormal = unitNormal.scale(-1.0);
        }
        return Optional.of(new SlabHit(segmentT, point, unitNormal));
    }

    static boolean coversPoint(WaterSegment segment, Vec3 point, double projectileRadius) {
        if (!usable(segment) || !finite(point) || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
            return false;
        }
        double padding = projectileRadius * 2.0;
        return SizedBoxHitbox.holdsPoint(segment.center(), segment.ahead(),
                segment.thick() + padding, segment.across() + padding, segment.activeHeight() + padding, point);
    }

    static boolean usable(WaterSegment segment) {
        if (segment == null || !finite(segment.center()) || !finite(segment.ahead())) {
            return false;
        }
        return segment.activeHeight() > 0.0
                && Double.isFinite(segment.across()) && segment.across() > 0.0
                && Double.isFinite(segment.thick()) && segment.thick() > 0.0;
    }

    static double boxTop(WaterSegment segment) {
        return segment.center().y + segment.activeHeight() * 0.5;
    }

    static double boxBottom(WaterSegment segment) {
        return segment.center().y - segment.activeHeight() * 0.5;
    }

    static Vec3 horizontalAhead(Vec3 ahead) {
        return WaterSegment.horizontalAhead(ahead);
    }

    private static boolean clipAxis(double origin, double travelAlong, double lower, double upper, double[] window,
                                    int axisIndex, int[] entryFace) {
        if (Math.abs(travelAlong) <= SURFACE_EPSILON) {
            return origin >= lower - SURFACE_EPSILON && origin <= upper + SURFACE_EPSILON;
        }
        double near = (lower - origin) / travelAlong;
        double far = (upper - origin) / travelAlong;
        int nearSign = -1;
        if (near > far) {
            double swap = near;
            near = far;
            far = swap;
            nearSign = 1;
        }
        if (near > window[0]) {
            window[0] = near;
            entryFace[0] = axisIndex;
            entryFace[1] = nearSign;
        } else if (entryFace[0] < 0 && near >= window[0] - SURFACE_EPSILON) {
            entryFace[0] = axisIndex;
            entryFace[1] = nearSign;
        }
        if (far < window[1]) {
            window[1] = far;
        }
        return window[1] >= window[0] - SURFACE_EPSILON;
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    record SlabHit(double segmentT, Vec3 point, Vec3 unitNormal) {
    }

    private static final class Column {
        private final Vec3 center;
        private final Vec3 ahead;
        private final Vec3 right;
        private final double halfThick;
        private final double halfHeight;
        private final double halfAcross;

        private Column(Vec3 center, Vec3 ahead, Vec3 right, double halfThick, double halfHeight, double halfAcross) {
            this.center = center;
            this.ahead = ahead;
            this.right = right;
            this.halfThick = halfThick;
            this.halfHeight = halfHeight;
            this.halfAcross = halfAcross;
        }

        private static Column fit(WaterSegment segment, double projectileRadius) {
            if (!usable(segment) || !Double.isFinite(projectileRadius) || projectileRadius < 0.0) {
                return null;
            }
            Vec3 ahead = horizontalAhead(segment.ahead());
            if (!finite(ahead)) {
                return null;
            }
            double span = segment.activeHeight();
            return new Column(
                    segment.center(),
                    ahead,
                    new Vec3(ahead.z, 0.0, -ahead.x),
                    segment.thick() * 0.5 + projectileRadius,
                    span * 0.5 + projectileRadius,
                    segment.across() * 0.5 + projectileRadius);
        }

        private double[] local(Vec3 point) {
            Vec3 offset = point.subtract(center);
            return new double[] {offset.dot(ahead), point.y - center.y, offset.dot(right)};
        }

        private Vec3 entryNormal(double[] local, int axisIndex, int sign) {
            if (axisIndex < 0) {
                return nearestNormal(local);
            }
            return axisNormal(axisIndex, sign);
        }

        private Vec3 nearestNormal(double[] local) {
            double[] gaps = new double[] {
                    halfThick - Math.abs(local[0]),
                    halfHeight - Math.abs(local[1]),
                    halfAcross - Math.abs(local[2])
            };
            int chosen = 0;
            for (int axisIndex = 1; axisIndex < gaps.length; axisIndex++) {
                if (gaps[axisIndex] < gaps[chosen]) {
                    chosen = axisIndex;
                }
            }
            int sign = local[chosen] >= 0.0 ? 1 : -1;
            return axisNormal(chosen, sign);
        }

        private Vec3 axisNormal(int axisIndex, int sign) {
            Vec3 basis = ahead;
            if (axisIndex == 1) {
                basis = new Vec3(0.0, 1.0, 0.0);
            } else if (axisIndex >= 2) {
                basis = right;
            }
            if (sign < 0) {
                return basis.scale(-1.0);
            }
            return basis;
        }
    }
}

package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 三拍水弧的一面。三份复制各自登记自己的快照，公共 contacts 只留最早的一面。
 * 查询用出生 tick 推本拍切片，不看这实体本拍是否已经推进伤害游标，也不看视觉 cohort。
 * 活动窗结束或实体移除后不再接弹。本宿主不扣水势，也不按份送偏转次数。
 * gameTick 必须是 {@link ServerLevel#getGameTime()}，与弧的 birthTick 同一时钟。
 */
public final class ArcDeflectionHost implements DeflectionHost {
    private final TechniqueHitEntity body;
    private final HostKey key;
    private final ServerLevel serverLevel;

    private ArcDeflectionHost(TechniqueHitEntity body, ServerLevel serverLevel, long birthTick) {
        this.body = body;
        this.serverLevel = serverLevel;
        this.key = new HostKey(body.getUUID(), birthTick);
    }

    public static ArcDeflectionHost bind(TechniqueHitEntity body) {
        if (body == null || !(body.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        ArcBandSnapshot snapshot = body.arcBand();
        if (snapshot == null || snapshot.birthTick() < 0L) {
            return null;
        }
        return new ArcDeflectionHost(body, serverLevel, snapshot.birthTick());
    }

    /** 弧的施术者。 */
    @Override
    public UUID ownerId() {
        return body.casterUuid();
    }

    /** 用实体编号和出生时刻区分这一面弧。 */
    @Override
    public HostKey key() {
        return key;
    }

    /** 弧所在世界。 */
    @Override
    public ServerLevel level() {
        return serverLevel;
    }

    /** 实体仍在，并且当前游戏时刻落在活动窗里。 */
    @Override
    public boolean active() {
        return open(serverLevel == null ? Long.MIN_VALUE : serverLevel.getGameTime());
    }

    /** 本拍活动径向切片上的最早接触。 */
    @Override
    public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        if (!open(gameTick)) {
            return Optional.empty();
        }
        ArcBandSnapshot snapshot = body.arcBand();
        Optional<ArcSliceGeometry.ArcHit> hit = ArcSliceGeometry.earliest(snapshot, from, toward, projectileRadius, gameTick);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        ArcSliceGeometry.ArcHit found = hit.get();
        return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
    }

    /** 点是否还在本拍活动切片里。 */
    @Override
    public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
        if (!open(gameTick)) {
            return false;
        }
        return ArcSliceGeometry.covers(body.arcBand(), point, projectileRadius, gameTick);
    }

    /** 从接触进度运到弧外沿。本宿主不扣水势。 */
    @Override
    public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
        ArcBandSnapshot snapshot = body.arcBand();
        double progress = 0.0;
        if (snapshot != null) {
            Optional<ArcSliceGeometry.ArcHit> hit = ArcSliceGeometry.earliest(
                    snapshot, contact.point(), contact.point(), 0.0, gameTick);
            if (hit.isPresent()) {
                progress = hit.get().progress();
            } else {
                progress = progressNear(snapshot, contact.point());
            }
        }
        ArcBandSnapshot frozen = snapshot;
        return new ArcTransportRoute(frozen, this::stillThere, contact.point(), progress, gameTick);
    }

    private boolean open(long gameTick) {
        if (serverLevel == null || body.isRemoved() || !body.isAlive() || !body.deflectsProjectiles()) {
            return false;
        }
        ArcBandSnapshot snapshot = body.arcBand();
        if (snapshot == null || snapshot.mode() != ArcBandMode.RADIAL_BLADE || snapshot.birthTick() < 0L) {
            return false;
        }
        if (gameTick == Long.MIN_VALUE) {
            return false;
        }
        long ageTicks = gameTick - snapshot.birthTick();
        return ageTicks >= 0L && ageTicks <= snapshot.activeTicks();
    }

    private boolean stillThere(long gameTick) {
        return open(gameTick);
    }

    private static double progressNear(ArcBandSnapshot snapshot, Vec3 point) {
        Vec3 origin = ArcSliceGeometry.vec(snapshot.origin());
        Vec3 forward = ArcSliceGeometry.vec(snapshot.forwardAxis());
        Vec3 cross = ArcSliceGeometry.vec(snapshot.crossAxis());
        Vec3 offset = point.subtract(origin);
        if (Math.abs(snapshot.sweepAngle()) < 1.0E-8) {
            return 0.0;
        }
        double angle = Math.atan2(offset.dot(cross), offset.dot(forward));
        double turned = Math.atan2(Math.sin(angle - snapshot.startAngle()), Math.cos(angle - snapshot.startAngle()));
        double progress = turned / snapshot.sweepAngle();
        if (progress < 0.0) {
            return 0.0;
        }
        if (progress > 1.0) {
            return 1.0;
        }
        return progress;
    }
}

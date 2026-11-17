package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;
import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 水幕宿主。只在定义显式打开偏转时由墙实体登记。
 * 几何是墙上的有限盒子。本宿主不扣水势，也不按波次另给次数。
 */
public final class WallDeflectionHost implements DeflectionHost {
    private final BarrierBody body;
    private final HostKey key;
    private final ServerLevel serverLevel;

    public WallDeflectionHost(BarrierBody body, long generation) {
        this.body = body;
        this.key = new HostKey(body.getUUID(), generation);
        this.serverLevel = body.level() instanceof ServerLevel level ? level : null;
    }

    @Override
    public UUID ownerId() {
        return body.ownerId();
    }

    @Override
    public HostKey key() {
        return key;
    }

    @Override
    public ServerLevel level() {
        return serverLevel;
    }

    @Override
    public boolean active() {
        if (serverLevel == null || !body.deflectsProjectiles() || !body.isAlive() || body.isRemoved()) {
            return false;
        }
        if (serverLevel.getServer() == null) {
            return false;
        }
        if (serverLevel.getServer().getTickCount() >= body.expireTick()) {
            return false;
        }
        UUID owner = body.ownerId();
        if (owner == null) {
            return false;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
        return player != null && player.isAlive() && player.level() == serverLevel;
    }

    @Override
    public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        if (!active() || body.wall() == null) {
            return Optional.empty();
        }
        Optional<WallBandGeometry.WallHit> hit = WallBandGeometry.earliest(body.wall().box(), from, toward, projectileRadius);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        WallBandGeometry.WallHit found = hit.get();
        return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
    }

    @Override
    public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
        if (!active() || body.wall() == null) {
            return false;
        }
        return WallBandGeometry.covers(body.wall().box(), point, projectileRadius);
    }

    @Override
    public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
        Vec3 incoming = incomingVelocity == null ? Vec3.ZERO : incomingVelocity;
        Vec3 reflected = WallBandGeometry.reflect(incoming, contact.unitNormal());
        Vec3 unit = reflected.lengthSqr() < 1.0E-16 ? contact.unitNormal().scale(-1.0) : reflected.normalize();
        Vec3 exit = outside(contact.point(), unit);
        return new WallReflectRoute(exit, unit);
    }

    private Vec3 outside(Vec3 contact, Vec3 unitDirection) {
        Vec3 nudged = WallBandGeometry.nudgeClear(contact, unitDirection, WallBandGeometry.SURFACE_NUDGE);
        if (serverLevel == null) {
            return nudged;
        }
        BlockHitResult hit = serverLevel.clip(new ClipContext(
                contact, nudged, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, body));
        if (hit.getType() != HitResult.Type.BLOCK) {
            return nudged;
        }
        Vec3 pulled = hit.getLocation().subtract(unitDirection.scale(0.01));
        if (pulled.subtract(contact).dot(unitDirection) <= 0.0) {
            return contact;
        }
        return pulled;
    }
}

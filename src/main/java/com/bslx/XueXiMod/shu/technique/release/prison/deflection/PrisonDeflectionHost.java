package com.bslx.XueXiMod.shu.technique.release.prison.deflection;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.prison.PrisonView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;
import java.util.UUID;

/**
 * 水牢偏转宿主。主人是施术者，所以水势从施术者扣，箭也归施术者。
 * 只接受被困者从壳内穿出的接触；壳内其他人和外打内都没有交点或身份不符。
 * 几何用创建时的 bounds，不再按术式编号改旧水幕、水环或缺月。
 */
public final class PrisonDeflectionHost implements DeflectionHost {
    private final PrisonView view;
    private final HostKey hostKey;
    private final ServerLevel serverLevel;
    /** 同一服务器线程里，intersect 记下的弹体半径，route 用来把箭身推离壳。 */
    private double bodyRadius;

    public PrisonDeflectionHost(PrisonView view) {
        this.view = view;
        this.hostKey = new HostKey(view.prisonId(), view.revision());
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        this.serverLevel = server == null || view.dimension() == null ? null : server.getLevel(view.dimension());
    }

    /** 水势和箭的主人是施术者，不是被困者。 */
    @Override
    public UUID ownerId() {
        return view.casterId();
    }

    /** 牢编号和创建修订组成的稳定宿主身份。 */
    @Override
    public HostKey key() {
        return hostKey;
    }

    /** 创建快照所在维度的服务端世界。 */
    @Override
    public ServerLevel level() {
        return serverLevel;
    }

    /** 这个攻击者是不是被困者本人。 */
    public boolean confines(UUID actorId) {
        return actorId != null && view.targetId() != null && actorId.equals(view.targetId());
    }

    /** 只接受被困者本人的攻击。 */
    @Override
    public boolean eligible(DeflectionAttack context) {
        if (context == null || context.actor() == null) {
            return false;
        }
        return confines(context.actor().getUUID());
    }

    /** 壳、施术者和到期时间都有效时才接接触。 */
    @Override
    public boolean active() {
        if (serverLevel == null || serverLevel.getServer() == null || view.bounds() == null || view.casterId() == null) {
            return false;
        }
        if (view.dimension() != null && !view.dimension().equals(serverLevel.dimension())) {
            return false;
        }
        if (serverLevel.getGameTime() >= view.endTick()) {
            return false;
        }
        ServerPlayer caster = serverLevel.getServer().getPlayerList().getPlayer(view.casterId());
        return caster != null && caster.isAlive() && caster.level() == serverLevel;
    }

    /** 查询被困者从壳内穿出的接触。外打内没有交点。 */
    @Override
    public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        this.bodyRadius = projectileRadius;
        if (!active()) {
            return Optional.empty();
        }
        Optional<PrisonShellGeometry.ShellHit> hit = PrisonShellGeometry.leaving(view.bounds(), from, toward, projectileRadius);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        PrisonShellGeometry.ShellHit found = hit.get();
        return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.inwardNormal()));
    }

    /** 箭身是否还贴着壳。完全退进壳内不算。 */
    @Override
    public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
        if (!active()) {
            return false;
        }
        return PrisonShellGeometry.touches(view.bounds(), point, projectileRadius);
    }

    /** 给出当拍向内反射的路线。本宿主不扣水势。 */
    @Override
    public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
        Vec3 incoming = incomingVelocity == null ? Vec3.ZERO : incomingVelocity;
        Vec3 inward = contact.unitNormal();
        Vec3 reflected = PrisonShellGeometry.reflectInward(incoming, inward);
        Vec3 unit = reflected.lengthSqr() < 1.0E-16 ? safeNormal(inward) : reflected.normalize();
        AABB shell = view.bounds();
        Vec3 release = PrisonShellGeometry.releaseInside(shell, contact.point(), inward, unit, bodyRadius);
        return new PrisonReflectRoute(release, unit);
    }

    private static Vec3 safeNormal(Vec3 inward) {
        if (inward == null || inward.lengthSqr() < 1.0E-16) {
            return new Vec3(0.0D, 1.0D, 0.0D);
        }
        return inward.normalize();
    }
}

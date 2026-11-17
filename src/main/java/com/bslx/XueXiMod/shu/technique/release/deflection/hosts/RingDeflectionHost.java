package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.event.ResponseWindowService;
import com.bslx.XueXiMod.shu.technique.effects.SwordGuardEffect;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 水环宿主。环心是当前主人身体高度上的点，法线是护体 from→toward 的确定插值，
 * 和 {@link com.bslx.XueXiMod.shu.technique.release.guard.GuardStates} 登记时用的是同一公式。
 * 附魔接触仍读目标法线，这里不改那条旧卸力。
 * 管半径是带宽的一半，不加附魔容差。环带几何仍不把圆盘当盾。
 * 弹体另外用主人碰撞盒的外壳提前接住，所以穿心、打头和打脚也会在命中之前被接走。
 * 近战另用主人包围盒的水平外扩包络，只护当前这一代的主人，不把环心圆盘当成无敌。
 * 换代时用新的 generation 重新登记，旧路线看到 live 为假就退出。
 * 本宿主不扣水势。出口随机只在每条路线构造时向服务端要一次。
 */
public final class RingDeflectionHost implements DeflectionHost {
    private final RingFollow follow;
    private final UUID owner;
    private final HostKey key;
    private final ServerLevel serverLevel;
    private final double majorRadius;
    private final double tubeRadius;

    public RingDeflectionHost(RingFollow follow, UUID owner, UUID hostId, long generation, ServerLevel serverLevel,
                              double majorRadius, double tubeRadius) {
        this.follow = follow;
        this.owner = owner;
        this.key = new HostKey(hostId, generation);
        this.serverLevel = serverLevel;
        this.majorRadius = majorRadius;
        this.tubeRadius = tubeRadius;
    }

    /** 水环主人。 */
    @Override
    public UUID ownerId() {
        return owner;
    }

    /** 这一代水环的宿主身份。 */
    @Override
    public HostKey key() {
        return key;
    }

    /** 水环所在世界。 */
    @Override
    public ServerLevel level() {
        return serverLevel;
    }

    /** 跟随姿态仍有效时才接弹。 */
    @Override
    public boolean active() {
        return serverLevel != null && follow != null && follow.live() && !guardWindowOpen();
    }

    /** 环管和主人身体外壳里更早的接触。 */
    @Override
    public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
        if (!active() || !finite(follow.center()) || !finite(follow.planeNormal())) {
            return Optional.empty();
        }
        Optional<RingBandGeometry.RingHit> ringHit = RingBandGeometry.earliest(
                follow.center(), follow.planeNormal(), majorRadius, tubeRadius, from, toward, projectileRadius);
        Optional<RingBodyShell.ShellHit> bodyHit = RingBodyShell.earliest(readOwnerBody(), from, toward);
        if (bodyHit.isPresent() && (ringHit.isEmpty() || bodyHit.get().segmentT() < ringHit.get().segmentT())) {
            RingBodyShell.ShellHit found = bodyHit.get();
            return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
        }
        if (ringHit.isEmpty()) {
            return Optional.empty();
        }
        RingBandGeometry.RingHit found = ringHit.get();
        return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
    }

    /** 近战只走主人包络。没有合法受击者时为空。 */
    @Override
    public Optional<HostContact> intersectMelee(DeflectionAttack attack, Vec3 from, Vec3 toward, long gameTick) {
        if (attack == null || attack.kind() != DeflectionAttack.Kind.MELEE || attack.victim() == null) {
            return Optional.empty();
        }
        return openMelee(active(), attack.victim().getUUID(), attack.victim().getBoundingBox(), from, toward);
    }

    /**
     * 活动代和主人身份由调用方传入的代标志、受击者决定。几何只认包络，不读环带。
     */
    Optional<HostContact> openMelee(boolean generationLive, UUID victimId, AABB ownerBody, Vec3 from, Vec3 toward) {
        Optional<RingMeleeGeometry.MeleeHit> hit = RingMeleeGeometry.protect(
                generationLive, owner, victimId, readCenter(), majorRadius, tubeRadius, ownerBody, from, toward);
        if (hit.isEmpty()) {
            return Optional.empty();
        }
        RingMeleeGeometry.MeleeHit found = hit.get();
        return Optional.of(new HostContact(this, found.segmentT(), found.point(), found.unitNormal()));
    }

    /** 箭心是否还在环管里。身体外壳不在这里延续。 */
    @Override
    public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
        if (!active() || !finite(follow.center()) || !finite(follow.planeNormal())) {
            return false;
        }
        return RingBandGeometry.covers(follow.center(), follow.planeNormal(), majorRadius, tubeRadius,
                point, projectileRadius);
    }

    /** 构造一条六拍绕行路线。随机只在这里取一次。 */
    @Override
    public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
        double randomUnit = 0.0;
        if (serverLevel != null && serverLevel.getRandom() != null) {
            randomUnit = serverLevel.getRandom().nextDouble();
        }
        return new RingOrbitRoute(follow, contact.point(), incomingVelocity, gameTick, randomUnit, majorRadius);
    }

    private boolean guardWindowOpen() {
        if (owner == null || serverLevel == null) {
            return false;
        }
        return ResponseWindowService.open(owner, SwordGuardEffect.TECHNIQUE_ID, serverLevel.getGameTime());
    }

    private AABB readOwnerBody() {
        if (serverLevel == null || owner == null || serverLevel.getServer() == null) {
            return null;
        }
        ServerPlayer player = serverLevel.getServer().getPlayerList().getPlayer(owner);
        if (player == null || !player.isAlive() || player.level() != serverLevel) {
            return null;
        }
        return player.getBoundingBox();
    }

    private Vec3 readCenter() {
        if (follow == null) {
            return null;
        }
        try {
            return follow.center();
        } catch (RuntimeException failure) {
            return null;
        }
    }

    private static boolean finite(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }
}

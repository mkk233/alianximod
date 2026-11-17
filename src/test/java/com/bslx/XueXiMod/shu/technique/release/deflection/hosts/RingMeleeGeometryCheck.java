package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.deflection.CaptureRoute;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionAttack;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionHosts;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostContact;
import com.bslx.XueXiMod.shu.technique.release.deflection.HostKey;
import com.bslx.XueXiMod.shu.technique.release.deflection.melee.MeleeDeflection;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 水环近战包络。直接调用生产几何和宿主方法，不重写公式，也不启动世界。
 */
public final class RingMeleeGeometryCheck {
    private static final UUID OWNER = new UUID(7L, 11L);
    private static final UUID OTHER = new UUID(7L, 12L);
    private static final Vec3 CENTER = new Vec3(0.0, 0.99, 0.0);
    private static final double MAJOR = 0.9;
    private static final double TUBE = 0.06;
    private static final AABB PLAYER_BODY = new AABB(-0.3, 0.0, -0.3, 0.3, 1.8, 0.3);
    private static final AABB MOB_BODY = new AABB(-0.3, 0.0, -0.3, 0.3, 1.95, 0.3);
    private static final Vec3 EYE_FROM = new Vec3(2.0, 1.62, 0.0);
    private static final Vec3 EYE_TOWARD = new Vec3(0.3, 1.62, 0.0);

    private static int assertionCount;

    private RingMeleeGeometryCheck() {
    }

    public static void main(String[] arguments) {
        eyeHeightMissesTorusAndHitsEnvelope();
        standingHeights();
        lowAttacker();
        closeAndOverlap();
        rejectsStrangersAndMalformed();
        hostGates();
        otherHostsKeepOldSegment();
        arrowTorusUnchanged();
        bodyShellPrecedesVanillaHit();
        poseOnlyAfterRingCommit();
        meleeQueryRejectsMissingContext();
        System.out.println("水环近战包络检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void eyeHeightMissesTorusAndHitsEnvelope() {
        check(RingBandGeometry.earliest(CENTER, worldUp(), MAJOR, TUBE, EYE_FROM, EYE_TOWARD, 0.0).isEmpty(),
                "眼睛高度穿过环心圆盘时，旧环带仍然不命中");
        Optional<RingMeleeGeometry.MeleeHit> hit = shield(OWNER, PLAYER_BODY, EYE_FROM, EYE_TOWARD);
        check(hit.isPresent(), "同一条眼睛高度线段进入水环近战包络");
        RingMeleeGeometry.MeleeHit found = hit.get();
        check(found.segmentT() > 0.5 && found.segmentT() < 0.8, "包络接触早于身体终点");
        check(Math.abs(found.point().y - 1.62) < 1.0E-9, "接触保持眼睛高度");
        double riseX = found.point().x - CENTER.x;
        double riseZ = found.point().z - CENTER.z;
        double horizontal = Math.sqrt(riseX * riseX + riseZ * riseZ);
        check(Math.abs(horizontal - (MAJOR + TUBE)) < 1.0E-6, "外侧来袭落在环外半径上");
        check(Math.abs(found.unitNormal().length() - 1.0) < 1.0E-9, "近战法线是单位向量");
        check(found.unitNormal().x > 0.9, "法线朝向来袭的一侧");
    }

    private static void standingHeights() {
        Vec3 playerEyeFrom = new Vec3(2.0, 1.62, 0.0);
        Vec3 playerEyeToward = new Vec3(0.3, 1.62, 0.0);
        check(shield(OWNER, PLAYER_BODY, playerEyeFrom, playerEyeToward).isPresent(), "站立玩家眼睛高度能护住");
        Vec3 zombieFrom = new Vec3(2.0, 1.74, 0.0);
        Vec3 zombieToward = new Vec3(0.3, 1.74, 0.0);
        check(RingBandGeometry.earliest(CENTER, worldUp(), MAJOR, TUBE, zombieFrom, zombieToward, 0.0).isEmpty(),
                "僵尸眼睛高度打不中腰部环带");
        check(shield(OWNER, PLAYER_BODY, zombieFrom, zombieToward).isPresent(), "僵尸眼睛高度能打进玩家包络");
        Vec3 tallFrom = new Vec3(2.0, 1.9, 0.0);
        Vec3 tallToward = new Vec3(0.3, 1.9, 0.0);
        check(shield(OWNER, PLAYER_BODY, tallFrom, tallToward).isEmpty(), "高出玩家头顶的线段不扩竖直包络");
        check(shield(OWNER, MOB_BODY, tallFrom, tallToward).isPresent(), "同一高度能打进更高的身体");
        check(shield(OWNER, PLAYER_BODY, new Vec3(2.0, 1.8, 0.0), new Vec3(0.3, 1.8, 0.0)).isPresent(),
                "贴着玩家头顶的线段仍然相交");
        check(shield(OWNER, PLAYER_BODY, new Vec3(2.0, 2.0, 0.0), new Vec3(0.3, 2.0, 0.0)).isEmpty(),
                "离开头顶的水平线段不相交");
    }

    private static void lowAttacker() {
        Vec3 lowFrom = new Vec3(2.0, 0.2, 0.0);
        Vec3 lowToward = new Vec3(0.3, 0.2, 0.0);
        check(RingBandGeometry.earliest(CENTER, worldUp(), MAJOR, TUBE, lowFrom, lowToward, 0.0).isEmpty(),
                "低处来袭打不中腰部环带");
        Optional<RingMeleeGeometry.MeleeHit> hit = shield(OWNER, PLAYER_BODY, lowFrom, lowToward);
        check(hit.isPresent(), "低处来袭能打进玩家身体高度");
        check(Math.abs(hit.get().point().y - 0.2) < 1.0E-9, "低处接触保持来袭高度");
    }

    private static void closeAndOverlap() {
        Vec3 inside = new Vec3(0.0, 1.0, 0.0);
        Optional<RingMeleeGeometry.MeleeHit> overlap = shield(OWNER, PLAYER_BODY, inside, inside);
        check(overlap.isPresent() && overlap.get().segmentT() == 0.0, "身体内的零长度重合可以接住");
        check(Math.abs(overlap.get().unitNormal().length() - 1.0) < 1.0E-9, "重合接触仍有单位法线");
        Vec3 nudged = new Vec3(0.0, 1.0, 1.0E-6);
        check(shield(OWNER, PLAYER_BODY, inside, nudged).isPresent(), "短于旧阈值的贴身线段水环仍接受");
        Vec3 onFace = new Vec3(0.3, 1.2, 0.0);
        check(shield(OWNER, PLAYER_BODY, onFace, onFace).isPresent(), "贴在身体表面上的零长度可以接住");
        Vec3 inRingOnly = new Vec3(0.6, 1.2, 0.0);
        check(shield(OWNER, PLAYER_BODY, inRingOnly, inRingOnly).isEmpty(), "只压在外扩圈里、没有碰到身体的零长度不接");
        Vec3 outside = new Vec3(3.0, 1.2, 0.0);
        check(shield(OWNER, PLAYER_BODY, outside, outside).isEmpty(), "身体外的零长度不接");
        Vec3 closeFrom = new Vec3(0.31, 1.62, 0.0);
        Vec3 closeToward = new Vec3(0.3, 1.62, 0.0);
        check(RingBandGeometry.earliest(CENTER, worldUp(), MAJOR, TUBE, closeFrom, closeToward, 0.0).isEmpty(),
                "贴身短线段打不中环带");
        Optional<RingMeleeGeometry.MeleeHit> closeHit = shield(OWNER, PLAYER_BODY, closeFrom, closeToward);
        check(closeHit.isPresent() && closeHit.get().segmentT() == 0.0, "贴身短线段从包络内部开始并到达身体");
        Vec3 wideFrom = new Vec3(1.5, 1.0, 0.0);
        Vec3 wideToward = new Vec3(1.2, 1.0, 0.0);
        AABB wideBody = new AABB(-2.0, 0.0, -2.0, 2.0, 1.8, 2.0);
        check(shield(OWNER, wideBody, wideFrom, wideToward).isPresent(), "身体已经宽过外半径时不把包络缩回去");
    }

    private static void rejectsStrangersAndMalformed() {
        check(shield(OTHER, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(), "其他受击者不能借用主人的包络");
        check(RingMeleeGeometry.protect(false, OWNER, OWNER, CENTER, MAJOR, TUBE, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "非当前代直接拒绝同一条可命中线段");
        check(shield(OWNER, PLAYER_BODY, new Vec3(2.0, 1.62, 2.0), new Vec3(-2.0, 1.62, 2.0)).isEmpty(),
                "侧面错开的线段碰不到包络");
        check(shield(OWNER, PLAYER_BODY, new Vec3(2.0, 4.0, 0.0), new Vec3(-2.0, 4.0, 0.0)).isEmpty(),
                "完全高于身体的线段不相交");
        check(shield(OWNER, PLAYER_BODY, new Vec3(2.0, 1.62, 0.0), new Vec3(0.5, 1.62, 0.0)).isEmpty(),
                "只扫过外扩圈、终点没到身体的线段不接");
        check(RingMeleeGeometry.protect(true, null, OWNER, CENTER, MAJOR, TUBE, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "没有主人身份不接");
        check(RingMeleeGeometry.protect(true, OWNER, null, CENTER, MAJOR, TUBE, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "没有受击者身份不接");
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, null, MAJOR, TUBE, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "环心缺失不接");
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, CENTER, MAJOR, TUBE, null, EYE_FROM, EYE_TOWARD).isEmpty(),
                "身体缺失不接");
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, CENTER, Double.NaN, TUBE, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "主半径不是有限数时不接");
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, CENTER, -1.0, TUBE, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "负的主半径不接");
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, CENTER, MAJOR, -0.01, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "负的管半径不接");
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, CENTER, MAJOR, TUBE, PLAYER_BODY,
                new Vec3(Double.NaN, 1.62, 0.0), EYE_TOWARD).isEmpty(), "起点不是有限点时不接");
        AABB broken = new AABB(Double.NaN, 0.0, 0.0, 1.0, 1.0, 1.0);
        check(RingMeleeGeometry.protect(true, OWNER, OWNER, CENTER, MAJOR, TUBE, broken, EYE_FROM, EYE_TOWARD).isEmpty(),
                "身体边界不是有限数时不接");
    }

    private static void hostGates() {
        StillRing current = new StillRing(true, CENTER);
        RingDeflectionHost ring = new RingDeflectionHost(current, OWNER, UUID.randomUUID(), 3L, null, MAJOR, TUBE);
        check(!ring.active(), "没有服务器关卡时水环不算活动");
        check(ring.intersect(EYE_FROM, EYE_TOWARD, 0.0, 3L).isEmpty(), "未激活时箭交点仍为空");
        check(!ring.covers(CENTER, 0.0, 3L), "未激活时箭重叠仍为假");
        check(ring.intersectMelee(null, EYE_FROM, EYE_TOWARD, 3L).isEmpty(), "没有近战身份时不接");
        DeflectionAttack blank = new DeflectionAttack(null, null, null, DeflectionAttack.Kind.MELEE);
        DeflectionAttack arrowKind = new DeflectionAttack(null, null, null, DeflectionAttack.Kind.PROJECTILE);
        check(ring.intersectMelee(blank, EYE_FROM, EYE_TOWARD, 3L).isEmpty(), "近战缺少受击者时不接");
        check(ring.intersectMelee(arrowKind, EYE_FROM, EYE_TOWARD, 3L).isEmpty(), "弹体种类不走水环近战");
        check(ring.openMelee(false, OWNER, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(), "关掉当前代后同一条线段不接");
        check(ring.openMelee(true, OTHER, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(), "宿主不替其他受击者接招");
        Optional<HostContact> accepted = ring.openMelee(true, OWNER, PLAYER_BODY, EYE_FROM, EYE_TOWARD);
        check(accepted.isPresent() && accepted.get().host() == ring, "当前代把包络接触记在这面水环上");
        check(accepted.get().segmentT() >= 0.0 && accepted.get().segmentT() <= 1.0, "宿主接触的线段参数落在 0 到 1");
        Vec3 normal = accepted.get().unitNormal();
        check(Double.isFinite(normal.x) && Double.isFinite(normal.y) && Double.isFinite(normal.z), "宿主法线是有限数");
        StillRing stale = new StillRing(false, CENTER);
        RingDeflectionHost retired = new RingDeflectionHost(stale, OWNER, UUID.randomUUID(), 4L, null, MAJOR, TUBE);
        check(!retired.active() && retired.openMelee(false, OWNER, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(),
                "跟随已经结束的一代不接");
        RingDeflectionHost broken = new RingDeflectionHost(new BrokenRing(), OWNER, UUID.randomUUID(), 5L, null, MAJOR, TUBE);
        check(broken.openMelee(true, OWNER, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isEmpty(), "环心读取失败时不接");
        check(shield(OWNER, PLAYER_BODY, EYE_FROM, EYE_TOWARD).isPresent(), "几何本身仍接受这条眼睛高度线段");
    }

    private static void otherHostsKeepOldSegment() {
        PlainHost plain = new PlainHost();
        Vec3 overlap = new Vec3(0.0, 1.0, 0.0);
        check(plain.intersectMelee(null, overlap, overlap, 17L).isEmpty(), "其他宿主拒绝零长度");
        check(plain.intersectCalls == 0, "零长度不会掉进旧交点");
        Vec3 nudged = new Vec3(0.0, 1.0, 1.0E-6);
        check(plain.intersectMelee(null, overlap, nudged, 17L).isEmpty(), "其他宿主拒绝低于旧阈值的贴身线段");
        check(plain.intersectCalls == 0, "贴身短线段也不会掉进旧交点");
        Optional<HostContact> delegated = plain.intersectMelee(null, EYE_FROM, EYE_TOWARD, 17L);
        check(delegated.isPresent() && delegated.get().segmentT() == 0.25, "可用线段仍返回旧交点的结果");
        check(plain.intersectCalls == 1 && plain.seenRadius == 0.0 && plain.seenTick == 17L, "旧交点半径仍是 0，并收到原来的时刻");
        check(plain.seenFrom == EYE_FROM && plain.seenToward == EYE_TOWARD, "旧交点拿到原来的两端");
        Vec3 highFrom = new Vec3(2.0, 4.0, 0.0);
        Vec3 highToward = new Vec3(0.3, 4.0, 0.0);
        check(plain.intersectMelee(null, highFrom, highToward, 18L).isEmpty(), "旧交点拒绝时近战缺省也拒绝");
        check(plain.intersectCalls == 2, "拒绝来自旧交点，不是查询前的新几何");
    }

    private static void arrowTorusUnchanged() {
        Vec3 worldUp = worldUp();
        check(RingBandGeometry.earliest(CENTER, worldUp, MAJOR, TUBE,
                new Vec3(0.2, 0.99, 0.0), new Vec3(-0.2, 0.99, 0.0), 0.0).isEmpty(),
                "环心圆盘仍然不是箭的盾");
        check(RingBandGeometry.earliest(CENTER, worldUp, MAJOR, TUBE,
                new Vec3(0.0, -2.0, 0.0), new Vec3(0.0, 2.0, 0.0), 0.0).isEmpty(),
                "沿轴穿过环心的箭仍然不命中");
        check(!RingBandGeometry.covers(CENTER, worldUp, MAJOR, TUBE, CENTER, 0.0), "环心圆盘仍然不算重叠");
        Vec3 tubePoint = new Vec3(MAJOR, 0.99, 0.0);
        check(RingBandGeometry.covers(CENTER, worldUp, MAJOR, TUBE, tubePoint, 0.0), "管心上的箭心仍然算在环带里");
        check(RingBandGeometry.earliest(CENTER, worldUp, MAJOR, TUBE,
                new Vec3(2.0, 0.99, 0.0), new Vec3(0.3, 0.99, 0.0), 0.0).isPresent(),
                "腰部高度穿过管壁的箭仍然命中");
    }

    private static void bodyShellPrecedesVanillaHit() {
        Optional<RingBodyShell.ShellHit> chest = RingBodyShell.earliest(PLAYER_BODY, EYE_FROM, EYE_TOWARD);
        check(chest.isPresent(), "穿过环心打向身体的箭被外壳接住");
        AABB vanilla = PLAYER_BODY.inflate(RingBodyShell.PICK_INFLATE);
        Optional<Vec3> vanillaHit = vanilla.clip(EYE_FROM, EYE_TOWARD);
        check(vanillaHit.isPresent(), "原版外扩盒也应被这条线段碰到");
        double vanillaT = segmentParameter(EYE_FROM, EYE_TOWARD, vanillaHit.get());
        check(chest.get().segmentT() < vanillaT - 1.0E-4, "外壳接触早于原版命中");
        Vec3 headFrom = new Vec3(0.0, 3.2, 0.0);
        Vec3 headToward = new Vec3(0.0, 0.4, 0.0);
        check(RingBandGeometry.earliest(CENTER, worldUp(), MAJOR, TUBE, headFrom, headToward, 0.0).isEmpty(),
                "头顶来箭仍然不碰环带");
        check(RingBodyShell.earliest(PLAYER_BODY, headFrom, headToward).isPresent(), "头顶来箭被外壳接住");
        check(RingBodyShell.earliest(null, EYE_FROM, EYE_TOWARD).isEmpty(), "没有身体时外壳不接");
    }

    private static double segmentParameter(Vec3 from, Vec3 toward, Vec3 point) {
        double spanX = toward.x - from.x;
        double spanY = toward.y - from.y;
        double spanZ = toward.z - from.z;
        double scale = spanX * spanX + spanY * spanY + spanZ * spanZ;
        return ((point.x - from.x) * spanX + (point.y - from.y) * spanY + (point.z - from.z) * spanZ) / scale;
    }

    private static void poseOnlyAfterRingCommit() {
        RingDeflectionHost ring = new RingDeflectionHost(new StillRing(true, CENTER), OWNER, UUID.randomUUID(), 6L,
                null, MAJOR, TUBE);
        PlainHost plain = new PlainHost();
        check(MeleeDeflection.horizontalAfterCommit(ring, true), "水环提交成功后才允许水平姿态");
        check(!MeleeDeflection.horizontalAfterCommit(ring, false), "水环只预留或提交失败时不摆姿态");
        check(!MeleeDeflection.horizontalAfterCommit(plain, true), "其他宿主提交成功也不摆水环姿态");
        check(!MeleeDeflection.horizontalAfterCommit(null, true), "没有宿主时不摆姿态");
    }

    private static void meleeQueryRejectsMissingContext() {
        DeflectionAttack attack = new DeflectionAttack(null, null, null, DeflectionAttack.Kind.MELEE);
        check(DeflectionHosts.meleeOrdered(null, attack, EYE_FROM, EYE_TOWARD, 1L).isEmpty(), "没有关卡时近战查询为空");
        check(DeflectionHosts.meleeOrdered(null, null, null, null, 1L).isEmpty(), "缺身份或缺端点时近战查询为空");
    }

    private static Optional<RingMeleeGeometry.MeleeHit> shield(UUID victimId, AABB ownerBody, Vec3 from, Vec3 toward) {
        return RingMeleeGeometry.protect(true, OWNER, victimId, CENTER, MAJOR, TUBE, ownerBody, from, toward);
    }

    private static Vec3 worldUp() {
        return new Vec3(0.0, 1.0, 0.0);
    }

    private static void check(boolean condition, String detail) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(detail);
        }
    }

    private static final class StillRing implements RingFollow {
        private final boolean living;
        private final Vec3 center;

        private StillRing(boolean living, Vec3 center) {
            this.living = living;
            this.center = center;
        }

        @Override
        public boolean live() {
            return living;
        }

        @Override
        public Vec3 center() {
            return center;
        }

        @Override
        public Vec3 planeNormal() {
            return new Vec3(0.0, 1.0, 0.0);
        }

        @Override
        public Vec3 ownerAnchor() {
            return new Vec3(center.x, 0.0, center.z);
        }
    }

    private static final class BrokenRing implements RingFollow {
        @Override
        public boolean live() {
            return true;
        }

        @Override
        public Vec3 center() {
            throw new IllegalStateException("环心不可用");
        }

        @Override
        public Vec3 planeNormal() {
            return new Vec3(0.0, 1.0, 0.0);
        }

        @Override
        public Vec3 ownerAnchor() {
            return Vec3.ZERO;
        }
    }

    /**
     * 不覆盖近战方法，用来确认缺省仍调用原来的交点。
     */
    private static final class PlainHost implements DeflectionHost {
        private final HostKey hostKey = new HostKey(UUID.randomUUID(), 1L);
        private final UUID owner = UUID.randomUUID();
        private int intersectCalls;
        private Vec3 seenFrom;
        private Vec3 seenToward;
        private double seenRadius = Double.NaN;
        private long seenTick = Long.MIN_VALUE;

        @Override
        public UUID ownerId() {
            return owner;
        }

        @Override
        public HostKey key() {
            return hostKey;
        }

        @Override
        public ServerLevel level() {
            return null;
        }

        @Override
        public boolean active() {
            return true;
        }

        @Override
        public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
            intersectCalls++;
            seenFrom = from;
            seenToward = toward;
            seenRadius = projectileRadius;
            seenTick = gameTick;
            if (from.y > 3.0 && toward.y > 3.0) {
                return Optional.empty();
            }
            return Optional.of(new HostContact(this, 0.25, toward, new Vec3(0.0, 1.0, 0.0)));
        }

        @Override
        public boolean covers(Vec3 point, double projectileRadius, long gameTick) {
            return false;
        }

        @Override
        public CaptureRoute route(HostContact contact, Vec3 incomingVelocity, long gameTick) {
            return null;
        }
    }
}

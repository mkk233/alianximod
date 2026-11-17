package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import com.bslx.XueXiMod.shu.technique.release.WaterReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandMode;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBandSnapshot;
import com.bslx.XueXiMod.shu.technique.release.water.ArcBounds;
import com.bslx.XueXiMod.shu.technique.release.water.ArcVec;
import com.bslx.XueXiMod.shu.technique.release.water.ProgressArcBand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 纯几何。不构造 Minecraft 世界，也不把结果说成实体集成已经通过。
 */
public final class HostGeometryCheck {
    private HostGeometryCheck() {
    }

    public static void main(String[] arguments) {
        optIn();
        wall();
        ring();
        arc();
        damageCursorUnchanged();
        System.out.println("水势偏转宿主几何检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void optIn() {
        check(BarrierDefinition.curtain().deflectsProjectiles(), "水幕工厂打开偏转");
        check(!new BarrierDefinition(3.2, 2.8, 0.25, 2.0, 80, 8, 2, 20, 0.5f).deflectsProjectiles(),
                "普通薄墙默认关闭");
        check(GuardDefinition.personalRing().deflectsProjectiles(), "水环工厂打开偏转");
        check(!new GuardDefinition("other_ring", 10, 0, 0, 20, 0.2f, 1.0, 0.1, 0.5, 0f).deflectsProjectiles(),
                "其它护体默认关闭");
        WaterReleaseDefinition plain = WaterReleaseDefinition.shot("plain_shot", Trajectory.STRAIGHT, 10, 1f, 0.2f);
        check(!plain.deflectsProjectiles(), "普通水系射击不承接");
        WaterReleaseDefinition sword = WaterReleaseDefinition.shot("blade_probe", Trajectory.STRAIGHT, 10, 1f, 0.2f).swordForm();
        check(sword.deflectsProjectiles(), "三拍水弧工厂打开偏转");
        check(WaterReleaseDefinition.barrier("curtain_probe", BarrierDefinition.curtain()).deflectsProjectiles(),
                "水幕释放读墙声明");
        check(!WaterReleaseDefinition.barrier("plain_wall",
                new BarrierDefinition(1, 1, 0.2, 1, 10, 0, 0, 20, 0.5f)).deflectsProjectiles(),
                "未打开的墙释放不承接");
        check(!IncomingPassage.letsPass(null, null), "空关系不放行");
        check(!IncomingPassage.letsPass(UUID.randomUUID(), null), "没有来者时不放行");
    }

    private static void wall() {
        AABB wall = new AABB(-0.125, 0.0, -1.6, 0.125, 2.8, 1.6);
        Optional<WallBandGeometry.WallHit> fast = WallBandGeometry.earliest(
                wall, new Vec3(-20.0, 1.0, 0.0), new Vec3(20.0, 1.0, 0.0), 0.0);
        check(fast.isPresent(), "高速箭穿过有限墙");
        check(fast.get().segmentT() > 0.4 && fast.get().segmentT() < 0.6, "高速命中取最早进入而不是终点");
        check(fast.get().unitNormal().dot(new Vec3(1.0, 0.0, 0.0)) < 0.0, "法线朝来弹一侧");

        Optional<WallBandGeometry.WallHit> inside = WallBandGeometry.earliest(
                wall, new Vec3(0.0, 1.0, 0.0), new Vec3(4.0, 1.0, 0.0), 0.0);
        check(inside.isPresent() && inside.get().segmentT() == 0.0, "从墙内出发取 t 为 0");
        check(WallBandGeometry.covers(wall, new Vec3(0.0, 1.0, 0.0), 0.0), "箭心在墙内算重叠");
        check(!WallBandGeometry.covers(wall, new Vec3(3.0, 1.0, 0.0), 0.0), "墙外的箭心不算重叠");
        check(WallBandGeometry.covers(wall, new Vec3(0.3, 1.0, 0.0), 0.2), "弹体半径仍碰到墙算重叠");

        check(WallBandGeometry.earliest(wall, new Vec3(-5.0, 4.0, 0.0), new Vec3(5.0, 4.0, 0.0), 0.0).isEmpty(),
                "擦过无限平面但高出有限墙时不相交");
        Optional<WallBandGeometry.WallHit> graze = WallBandGeometry.earliest(
                wall, new Vec3(0.0, 2.8, -3.0), new Vec3(0.0, 2.8, 3.0), 0.0);
        check(graze.isPresent(), "贴着顶边擦过仍然相交");
        check(WallBandGeometry.earliest(wall, new Vec3(0.0, 2.9, -3.0), new Vec3(0.0, 2.9, 3.0), 0.0).isEmpty(),
                "离开顶边一个缝时不相交");
        check(WallBandGeometry.earliest(wall, new Vec3(0.0, 2.9, -3.0), new Vec3(0.0, 2.9, 3.0), 0.15).isPresent(),
                "弹体半径把擦边收进墙");

        Vec3 incoming = new Vec3(3.0, 1.0, 0.0);
        Vec3 reflected = WallBandGeometry.reflect(incoming, new Vec3(-1.0, 0.0, 0.0));
        Vec3 flipped = WallBandGeometry.reflect(incoming, new Vec3(1.0, 0.0, 0.0));
        check(reflected.distanceTo(flipped) < 1.0E-9, "法线正负不影响反射");
        check(Math.abs(reflected.length() - incoming.length()) < 1.0E-9, "反射保持速度长度");
        check(reflected.dot(new Vec3(1.0, 0.0, 0.0)) < 0.0, "反射不朝原入射继续追");

        Vec3 contact = new Vec3(0.0, 1.0, 0.0);
        Vec3 clear = WallBandGeometry.nudgeClear(contact, new Vec3(-1.0, 0.0, 0.0), 0.5,
                new AABB(-0.4, 0.5, -0.2, -0.2, 1.5, 0.2));
        check(clear.x > -0.2 && clear.x <= 0.0, "出口偏移停在另一块固体之前");
        check(wall.minX == -0.125 && wall.maxY == 2.8, "偏移不改墙盒");

        double[] faces = new double[3];
        AABB[] boxes = new AABB[] {
                new AABB(-0.125, 0.0, -1.0, 0.125, 2.0, 1.0),
                new AABB(2.875, 0.0, -1.0, 3.125, 2.0, 1.0),
                new AABB(5.875, 0.0, -1.0, 6.125, 2.0, 1.0)
        };
        Vec3 from = new Vec3(-2.0, 1.0, 0.0);
        Vec3 toward = new Vec3(10.0, 1.0, 0.0);
        for (int faceIndex = 0; faceIndex < boxes.length; faceIndex++) {
            Optional<WallBandGeometry.WallHit> hit = WallBandGeometry.earliest(boxes[faceIndex], from, toward, 0.0);
            faces[faceIndex] = hit.isEmpty() ? Double.NaN : hit.get().segmentT();
        }
        check(nearestFace(faces) == 0, "多面取最早的一面");
        check(nearestFace(faces) == 0, "多面选择重复询问不变");
    }

    private static void ring() {
        Vec3 center = Vec3.ZERO;
        Vec3 worldUp = new Vec3(0.0, 1.0, 0.0);
        double major = 0.9;
        double tube = 0.06;
        check(RingBandGeometry.earliest(center, worldUp, major, tube,
                new Vec3(0.0, -2.0, 0.0), new Vec3(0.0, 2.0, 0.0), 0.0).isEmpty(),
                "沿轴穿过环心不命中");
        check(RingBandGeometry.earliest(center, worldUp, major, tube,
                new Vec3(0.2, 0.0, 0.0), new Vec3(-0.2, 0.0, 0.0), 0.0).isEmpty(),
                "环内圆盘不是盾");
        Optional<RingBandGeometry.RingHit> crossing = RingBandGeometry.earliest(center, worldUp, major, tube,
                new Vec3(0.2, 0.0, 0.0), new Vec3(2.0, 0.0, 0.0), 0.0);
        check(crossing.isPresent() && crossing.get().segmentT() > 0.05 && crossing.get().segmentT() < 0.95,
                "两端都在管外、中段穿过环带时命中");
        RingBandGeometry.PlaneBasis flat = RingBandGeometry.PlaneBasis.of(worldUp);
        Vec3 tubePoint = center.add(flat.eastAxis().scale(major));
        Optional<RingBandGeometry.RingHit> through = RingBandGeometry.earliest(center, worldUp, major, tube,
                tubePoint.add(worldUp.scale(2.0)), tubePoint.subtract(worldUp.scale(2.0)), 0.0);
        check(through.isPresent(), "穿过管壁命中");
        check(RingBandGeometry.covers(center, worldUp, major, tube, tubePoint, 0.0), "管心上的点算在环带里");
        check(!RingBandGeometry.covers(center, worldUp, major, tube, center, 0.0), "环心圆盘不算重叠");
        double expected = (2.0 - tube) / 4.0;
        check(Math.abs(through.get().segmentT() - expected) < 2.0E-3, "环带最早 t 靠近管壁");

        Vec3 tilted = new Vec3(0.0, 0.6, 0.8).normalize();
        RingBandGeometry.PlaneBasis tiltedBasis = RingBandGeometry.PlaneBasis.of(tilted);
        Vec3 tiltedPoint = center.add(tiltedBasis.eastAxis().scale(major));
        check(RingBandGeometry.earliest(center, tilted, major, tube,
                center.subtract(tilted.scale(2.0)), center.add(tilted.scale(2.0)), 0.0).isEmpty(),
                "倾斜环的空心仍然放过");
        check(RingBandGeometry.earliest(center, tilted, major, tube,
                tiltedPoint.subtract(tilted.scale(2.0)), tiltedPoint.add(tilted.scale(2.0)), 0.0).isPresent(),
                "倾斜环带按当前法线命中");

        Vec3 owner = new Vec3(0.0, 0.0, 5.0);
        double exit = RingBandGeometry.chooseExitAngle(0.0, 0.0, center, flat, major, owner);
        double again = RingBandGeometry.chooseExitAngle(0.0, 0.0, center, flat, major, owner);
        check(Math.abs(exit - again) < 1.0E-12, "出口角只由这一次随机决定");
        Vec3 outward = flat.radial(exit, 1.0).normalize();
        Vec3 exitPoint = center.add(flat.radial(exit, major));
        check(outward.dot(owner.subtract(exitPoint)) <= 1.0E-6, "出口不朝主人");

        FixedRing follow = new FixedRing(center, worldUp, center);
        Vec3 contact = tubePoint.add(worldUp.scale(tube));
        RingOrbitRoute first = new RingOrbitRoute(follow, contact, new Vec3(0.0, 0.0, 1.0), 40L, 0.2, major);
        RingOrbitRoute second = new RingOrbitRoute(follow, contact, new Vec3(0.0, 0.0, -1.0), 40L, 0.8, major);
        RouteSample early = first.sample(45L);
        check(!early.finished(), "第 6 拍之前还在环上");
        RouteSample done = first.sample(46L);
        check(done.finished(), "第 6 拍到达出口");
        check(Math.abs(done.position().subtract(center).length() - major) < 0.02, "出口落在环上");
        ReleaseSample release = first.emergencyRelease(46L);
        ReleaseSample releaseAgain = first.emergencyRelease(50L);
        check(release.position().distanceTo(releaseAgain.position()) < 1.0E-9, "正常出口重复查询不推进");
        check(release.unitDirection().distanceTo(releaseAgain.unitDirection()) < 1.0E-9, "出口方向不重抽");
        Vec3 radial = release.position().subtract(center).normalize();
        check(release.unitDirection().dot(radial) > 0.99, "出口朝径向外侧");
        check(release.unitDirection().dot(center.subtract(release.position())) < 0.0, "出口不指回环心主人");
        check(Math.abs(release.unitDirection().length() - 1.0) < 1.0E-6, "出口方向是单位向量");
        check(second.sample(40L).position().distanceTo(contact) < 0.05, "另一条路线不被第一条的进度带走");
        check(first.active(), "正常走完不是丢失宿主");

        follow.living = false;
        RingOrbitRoute broken = new RingOrbitRoute(follow, contact, new Vec3(1.0, 0.0, 0.0), 8L, 0.3, major);
        ReleaseSample emergency = broken.emergencyRelease(9L);
        ReleaseSample emergencyAgain = broken.emergencyRelease(12L);
        check(!broken.active(), "主人失效后路线结束");
        check(!broken.sample(12L).finished(), "提前失效不当成正常走完");
        check(emergency.position().distanceTo(emergencyAgain.position()) < 1.0E-9, "紧急放出重复查询不变");
        check(emergency.position().distanceTo(new Vec3(1000.0, 0.0, 0.0)) > 10.0, "失效后不追到远处");

        Vec3 blended = RingBandGeometry.blendNormal(worldUp, new Vec3(1.0, 0.0, 0.0), 0, 4, 2);
        Vec3 blendedAgain = RingBandGeometry.blendNormal(worldUp, new Vec3(1.0, 0.0, 0.0), 0, 4, 2);
        check(blended.distanceTo(blendedAgain) < 1.0E-12, "姿态插值确定");
        check(blended.y > 0.2 && blended.x > 0.2, "中途法线在两端之间");
    }

    private static void arc() {
        ArcBandSnapshot blade = blade(new ArcVec(0.0, 0.0, 0.0), 1.2, 1000L);
        check(blade != null, "径向快照可建");
        ArcSliceGeometry.ActiveSpan last = ArcSliceGeometry.activeSpan(1006L, 1000L, ArcBandSnapshot.ACTIVE_TICKS);
        check(last != null && Math.abs(last.fromProgress() - 5.0 / 6.0) < 1.0E-9
                        && Math.abs(last.toProgress() - 1.0) < 1.0E-9,
                "最后一拍活动切片是 5/6 到 1");
        check(ArcSliceGeometry.activeSpan(1007L, 1000L, 6) == null, "拖尾第一拍不再接弹");
        check(ArcSliceGeometry.activeSpan(1010L, 1000L, 6) == null, "拖尾结束仍然不接");
        check(ArcBandSnapshot.TAIL_TICKS == 4 && ArcBandSnapshot.ACTIVE_TICKS == 6, "活动 6 拍拖尾 4 拍");

        Optional<ArcSliceGeometry.ArcHit> opening = shoot(blade, 1001L, 1.0 / 12.0);
        Optional<ArcSliceGeometry.ArcHit> closing = shoot(blade, 1006L, 11.0 / 12.0);
        check(opening.isPresent(), "第一拍活动切片能挡住对准它的箭");
        check(opening.isPresent() && ArcSliceGeometry.covers(blade, opening.get().point(), 0.0, 1001L),
                "命中点在当前切片体积内");
        check(!ArcSliceGeometry.covers(blade, new Vec3(8.0, 0.0, 8.0), 0.0, 1001L), "远离弧的点不在切片内");
        check(shoot(blade, 1006L, 1.0 / 12.0).isEmpty(), "后面的切片不使用整段历史扇形");
        check(closing.isPresent(), "最后一拍切片能挡住");
        check(shoot(blade, 1007L, 11.0 / 12.0).isEmpty(), "同一几何在拖尾不再相交");
        Optional<ArcSliceGeometry.ArcHit> fast = shootFar(blade, 1001L, 1.0 / 12.0);
        check(fast.isPresent() && fast.get().segmentT() > 0.2 && fast.get().segmentT() < 0.8,
                "高速穿越取最早 t");
        check(shoot(blade, 1001L, 0.5).isEmpty(), "第一拍不包含半程扇形");
        check(shootMiss(blade, 1001L), "远离弧的线段不相交");

        ArcBounds broad = ProgressArcBand.sliceBroadBounds(blade, 0.0, 1.0);
        Vec3 corner = new Vec3(broad.highEast(), 0.0, broad.highSouth());
        Optional<ArcSliceGeometry.ArcHit> loose = ArcSliceGeometry.earliest(
                blade, corner.subtract(new Vec3(0.0, 0.4, 0.0)), corner.add(new Vec3(0.0, 0.4, 0.0)), 0.0, 1003L);
        check(loose.isEmpty(), "宽包围盒角落不当成窄相命中");

        ArcBandSnapshot higher = blade(new ArcVec(0.0, 2.0, 0.0), 1.2, 1000L);
        ArcBandSnapshot side = ArcBandSnapshot.of(
                new ArcVec(30.0, 0.0, 0.0), new ArcVec(0.0, 0.0, 1.0), new ArcVec(0.0, 1.0, 0.0), new ArcVec(-1.0, 0.0, 0.0),
                1, 2.4, 0.16, -0.2, 1.2, 1000L, 6, 4, ArcBandMode.RADIAL_BLADE);
        Vec3 shared = interior(blade, 0.42, 0.6);
        Vec3 sharedFrom = shared.subtract(new Vec3(0.0, 3.0, 0.0));
        Vec3 sharedToward = shared.add(new Vec3(0.0, 6.0, 0.0));
        double[] faces = new double[] {
                segmentOf(ArcSliceGeometry.earliest(blade, sharedFrom, sharedToward, 0.0, 1003L)),
                segmentOf(ArcSliceGeometry.earliest(higher, sharedFrom, sharedToward, 0.0, 1003L)),
                segmentOf(ArcSliceGeometry.earliest(side, sharedFrom, sharedToward, 0.0, 1003L))
        };
        check(Double.isFinite(faces[0]) && Double.isFinite(faces[1]) && faces[0] < faces[1], "两面都中时较近的更早");
        check(nearestFace(faces) == 0, "三斩面稳定选择最早的一面");
        check(nearestFace(faces) == nearestFace(faces), "三斩面重复选择不变");

        ArcBandSnapshot outer = ArcBandSnapshot.of(
                new ArcVec(0.0, 0.0, 0.0), new ArcVec(0.0, 0.0, 1.0), new ArcVec(1.0, 0.0, 0.0), new ArcVec(0.0, 1.0, 0.0),
                0, 2.4, 0.16, -0.2, 1.2, 1000L, 6, 4, ArcBandMode.OUTER_ARC);
        check(ArcSliceGeometry.earliest(outer, new Vec3(0.0, -2.0, 1.0), new Vec3(0.0, 2.0, 1.0), 0.0, 1003L).isEmpty(),
                "非径向弧不会自动接上");

        checkTangent(blade(new ArcVec(0.0, 0.0, 0.0), 1.2, 1000L), "正扫向");
        checkTangent(blade(new ArcVec(0.0, 0.0, 0.0), -1.2, 1000L), "负扫向");

        ArcBandSnapshot ride = blade(new ArcVec(0.0, 0.0, 0.0), 1.2, 200L);
        ArcVec rim = ProgressArcBand.point(ride, 0.35);
        Vec3 contact = ArcSliceGeometry.vec(ride.origin().plus(rim.minus(ride.origin()).scale(0.55)));
        ArcPresence living = new ArcPresence() {
            @Override
            public boolean present(long gameTick) {
                return true;
            }
        };
        ArcTransportRoute route = new ArcTransportRoute(ride, living, contact, 0.35, 202L);
        Vec3 previous = route.sample(202L).position();
        for (long gameTick = 203L; gameTick <= 206L; gameTick++) {
            Vec3 step = route.sample(gameTick).position();
            check(step.distanceTo(previous) < 2.0, "运输每拍不瞬移过大");
            previous = step;
        }
        RouteSample arrived = route.sample(206L);
        check(arrived.finished(), "剩余活动进度走完");
        check(arrived.position().distanceTo(ArcSliceGeometry.outerPoint(ride, 1.0)) < 1.0E-6, "终点到达外沿");
        ReleaseSample tangent = route.emergencyRelease(206L);
        check(tangent.unitDirection().distanceTo(ArcSliceGeometry.endTangent(ride)) < 1.0E-6, "末端放出切线");
        check(route.sample(210L).position().distanceTo(arrived.position()) < 1.0E-9, "走完后不进入拖尾");

        ArcTransportRoute immediate = new ArcTransportRoute(ride, living, contact, 0.92, 206L);
        RouteSample ended = immediate.sample(206L);
        check(ended.finished(), "接到活动终点时立即放出");
        check(ended.position().distanceTo(ArcSliceGeometry.outerPoint(ride, 1.0)) < 1.0E-6, "立即放出仍在外沿");
        check(immediate.sample(210L).position().distanceTo(ended.position()) < 1.0E-9, "立即放出不延长到拖尾");
        check(immediate.active(), "立即放出仍是正常结束");

        ArcPresence dying = new ArcPresence() {
            @Override
            public boolean present(long gameTick) {
                return gameTick < 81L;
            }
        };
        ArcTransportRoute aborted = new ArcTransportRoute(ride, dying, contact, 0.35, 80L);
        ReleaseSample firstStop = aborted.emergencyRelease(81L);
        ReleaseSample secondStop = aborted.emergencyRelease(84L);
        check(!aborted.active(), "提前结束不再当作活宿主");
        check(!aborted.sample(84L).finished(), "提前结束不是正常末端");
        check(firstStop.position().distanceTo(secondStop.position()) < 1.0E-9, "提前结束重复查询不变");
        check(firstStop.position().distanceTo(contact) < ride.radius(), "提前结束停在已经走到的位置附近");
    }

    private static void damageCursorUnchanged() {
        ProgressArcBand.SweepCursor opened = ProgressArcBand.advance(0.0, false, 1L, 6);
        check(opened.swept() && Math.abs(opened.fromProgress()) < 1.0E-12
                        && Math.abs(opened.toProgress() - 1.0 / 6.0) < 1.0E-12,
                "伤害推进的第一拍窗口不变");
        ProgressArcBand.SweepCursor closed = ProgressArcBand.advance(0.5, true, 4L, 6);
        check(!closed.swept() && closed.damageClosed(), "已关闭的伤害窗口不再扫");
        ArcBandSnapshot blade = blade(new ArcVec(0.0, 0.0, 0.0), 1.2, 10L);
        ArcBounds far = new ArcBounds(40.0, 40.0, 40.0, 41.0, 41.0, 41.0);
        check(!ProgressArcBand.intersectsSlice(blade, far, 0.0, 1.0), "远处盒子仍然不是命中");
    }

    private static void checkTangent(ArcBandSnapshot snapshot, String label) {
        Vec3 tangent = ArcSliceGeometry.endTangent(snapshot);
        check(tangent != null && Math.abs(tangent.length() - 1.0) < 1.0E-6, label + "切线是单位向量");
        ArcVec radial = ProgressArcBand.direction(snapshot, 1.0);
        Vec3 radialPoint = new Vec3(radial.east(), radial.above(), radial.south());
        check(Math.abs(tangent.dot(radialPoint)) < 1.0E-6, label + "切线不是径向");
        double endAngle = snapshot.startAngle() + snapshot.sweepAngle();
        double sweepSign = snapshot.sweepAngle() >= 0.0 ? 1.0 : -1.0;
        ArcVec expected = snapshot.forwardAxis().scale(-Math.sin(endAngle))
                .plus(snapshot.crossAxis().scale(Math.cos(endAngle)))
                .scale(sweepSign)
                .normalize();
        check(tangent.distanceTo(new Vec3(expected.east(), expected.above(), expected.south())) < 1.0E-9,
                label + "切线符合扫向符号");
    }

    private static Optional<ArcSliceGeometry.ArcHit> shoot(ArcBandSnapshot snapshot, long gameTick, double progress) {
        Vec3 through = interior(snapshot, progress, 0.6);
        Vec3 normal = ArcSliceGeometry.vec(snapshot.normalAxis());
        return ArcSliceGeometry.earliest(snapshot, through.subtract(normal.scale(3.0)), through.add(normal.scale(3.0)),
                0.0, gameTick);
    }

    private static Optional<ArcSliceGeometry.ArcHit> shootFar(ArcBandSnapshot snapshot, long gameTick, double progress) {
        Vec3 through = interior(snapshot, progress, 0.6);
        Vec3 normal = ArcSliceGeometry.vec(snapshot.normalAxis());
        return ArcSliceGeometry.earliest(snapshot, through.subtract(normal.scale(30.0)), through.add(normal.scale(30.0)),
                0.0, gameTick);
    }

    private static boolean shootMiss(ArcBandSnapshot snapshot, long gameTick) {
        return ArcSliceGeometry.earliest(snapshot, new Vec3(8.0, -2.0, 8.0), new Vec3(8.0, 2.0, 8.0), 0.0, gameTick).isEmpty();
    }

    private static Vec3 interior(ArcBandSnapshot snapshot, double progress, double scale) {
        ArcVec rim = ProgressArcBand.point(snapshot, progress);
        return ArcSliceGeometry.vec(snapshot.origin().plus(rim.minus(snapshot.origin()).scale(scale)));
    }

    private static double segmentOf(Optional<ArcSliceGeometry.ArcHit> hit) {
        return hit.isEmpty() ? Double.NaN : hit.get().segmentT();
    }

    private static ArcBandSnapshot blade(ArcVec origin, double sweep, long birth) {
        return ArcBandSnapshot.of(origin, new ArcVec(0.0, 0.0, 1.0), new ArcVec(1.0, 0.0, 0.0), new ArcVec(0.0, 1.0, 0.0),
                0, 2.4, 0.16, -0.2, sweep, birth, 6, 4, ArcBandMode.RADIAL_BLADE);
    }

    private static int nearestFace(double[] segmentValues) {
        int chosen = -1;
        double best = Double.POSITIVE_INFINITY;
        for (int faceIndex = 0; faceIndex < segmentValues.length; faceIndex++) {
            double segmentT = segmentValues[faceIndex];
            if (!Double.isFinite(segmentT)) {
                continue;
            }
            if (chosen < 0 || segmentT < best - 1.0E-9) {
                best = segmentT;
                chosen = faceIndex;
            }
        }
        return chosen;
    }

    private static int assertionCount;

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(label);
        }
    }

    private static final class FixedRing implements RingFollow {
        private final Vec3 center;
        private final Vec3 normal;
        private final Vec3 anchor;
        private boolean living = true;

        private FixedRing(Vec3 center, Vec3 normal, Vec3 anchor) {
            this.center = center;
            this.normal = normal;
            this.anchor = anchor;
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
            return normal;
        }

        @Override
        public Vec3 ownerAnchor() {
            return anchor;
        }
    }
}

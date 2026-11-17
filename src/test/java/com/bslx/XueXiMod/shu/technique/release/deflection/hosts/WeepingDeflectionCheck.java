package com.bslx.XueXiMod.shu.technique.release.deflection.hosts;

import com.bslx.XueXiMod.shu.technique.release.WaterSegment;
import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import net.minecraft.world.phys.Vec3;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * 垂丝偏转的纯几何和路线。直接调用生产方法，不重写落差公式，也不启动世界或实体。
 * 登记、主人离场、方块挡路、好友、费用、三次上限、重叠宿主同箭只扣一次，都要真实服务端，这里不测。
 */
public final class WeepingDeflectionCheck {
    private static int assertionCount;

    private WeepingDeflectionCheck() {
    }

    public static void main(String[] arguments) {
        orientedVolume();
        followsCurrentFoot();
        emergencyStaysPut();
        copiesDoNotShareProgress();
        sharedGatesStayDefault();
        bindingIgnoresNull();
        System.out.println("垂丝偏转几何与路线检查通过");
        System.out.println("断言 " + assertionCount);
        System.out.println("运行时缺口: 服务器登记与注销、主人离场、出口被方块挡住、好友与湿折扣与三次上限、"
                + "重叠瀑布同一支箭只收一次、近战零长度线段、Mixin 与游戏画面。");
    }

    private static void orientedVolume() {
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        Vec3 center = new Vec3(0.0, 8.0, 0.0);
        WaterSegment segment = curtain(center, ahead, 0.0, 4.0, 4.0, 2.0, 0.7, true, false);
        check(WeepingDeflectionHost.coversPoint(segment, center, 0.0), "水段中心算在体积里");
        check(!WeepingDeflectionHost.coversPoint(segment, new Vec3(0.0, 8.0, 1.0), 0.0),
                "厚度外的点即使落在粗立方里也不算水");
        check(!WeepingDeflectionHost.coversPoint(segment, new Vec3(0.0, 8.0, 0.5), 0.0), "刚超出厚度不算");
        check(WeepingDeflectionHost.coversPoint(segment, new Vec3(0.0, 8.0, 0.5), 0.2), "弹体半径仍能贴到水");
        check(!WeepingDeflectionHost.coversPoint(null, center, 0.0), "没有水段就不覆盖");

        WaterSegment shallow = curtain(center, ahead, 0.0, WaterSegment.PRESENT_GAP, 4.0, 2.0, 0.7, true, true);
        check(!WeepingDeflectionHost.usable(shallow), "浅到精确体积下限时不接弹");
        check(!WeepingDeflectionHost.coversPoint(shallow, center, 0.0), "过浅水段没有覆盖");
        WaterSegment drained = curtain(center, ahead, 0.0, 4.0, 4.0, 2.0, 0.7, false, true);
        check(WeepingDeflectionHost.intersectSegment(drained, new Vec3(0.0, 8.0, -2.0), new Vec3(0.0, 8.0, 2.0), 0.0).isEmpty(),
                "水段标记为空就没有碰撞");

        Optional<WeepingDeflectionHost.SlabHit> side = WeepingDeflectionHost.intersectSegment(
                segment, new Vec3(0.0, 8.0, -3.0), new Vec3(0.0, 8.0, 3.0), 0.0);
        check(side.isPresent(), "水平箭穿过瀑布");
        check(side.get().segmentT() > 0.0 && side.get().segmentT() < 0.5, "取进入的一面");
        check(side.get().point().z < 0.0, "接触点在来向一侧");
        check(side.get().unitNormal().z < 0.0, "法线朝来向");
        Optional<WeepingDeflectionHost.SlabHit> repeated = WeepingDeflectionHost.intersectSegment(
                segment, new Vec3(0.0, 8.0, -3.0), new Vec3(0.0, 8.0, 3.0), 0.0);
        check(Math.abs(repeated.get().segmentT() - side.get().segmentT()) < 1.0E-12, "同一段再问结果不变");

        Optional<WeepingDeflectionHost.SlabHit> inside = WeepingDeflectionHost.intersectSegment(
                segment, center, new Vec3(0.0, 8.0, 3.0), 0.0);
        check(inside.isPresent() && Math.abs(inside.get().segmentT()) < 1.0E-9, "从水里出发参数是 0");
        check(WeepingDeflectionHost.intersectSegment(
                segment, new Vec3(0.0, 20.0, -2.0), new Vec3(0.0, 20.0, 2.0), 0.0).isEmpty(),
                "高过水头的水平箭不相交");

        Optional<WeepingDeflectionHost.SlabHit> fromAbove = WeepingDeflectionHost.intersectSegment(
                segment, new Vec3(0.0, 20.0, 0.0), new Vec3(0.0, 0.0, 0.0), 0.0);
        check(fromAbove.isPresent(), "从上向下的箭打进水头");
        check(fromAbove.get().point().y > center.y, "先碰到上沿");
        check(fromAbove.get().unitNormal().y > 0.0, "上沿法线朝来向");

        WaterSegment turned = curtain(center, new Vec3(1.0, 0.0, 0.0), 0.0, 4.0, 4.0, 2.0, 0.7, true, false);
        check(WeepingDeflectionHost.coversPoint(turned, new Vec3(0.2, 8.0, 0.0), 0.0), "厚度跟着出手方向");
        check(!WeepingDeflectionHost.coversPoint(turned, new Vec3(1.0, 8.0, 0.0), 0.0), "转过身以后厚度外仍然漏过");
        check(WeepingDeflectionHost.coversPoint(turned, new Vec3(0.0, 8.0, 0.9), 0.0), "宽度在出手方向的侧面");

        WaterSegment lipOffset = new WaterSegment(
                new Vec3(5.0, 10.0, 0.0), center, ahead, 0.0, 4.0, 4.0, 2.0, 0.7, true, false);
        check(WeepingDeflectionHost.intersectSegment(
                lipOffset, new Vec3(0.0, 8.0, -2.0), new Vec3(0.0, 8.0, 2.0), 0.0).isPresent(),
                "体积跟水段中心走");
        check(WeepingDeflectionHost.intersectSegment(
                lipOffset, new Vec3(5.0, 8.0, -2.0), new Vec3(5.0, 8.0, 2.0), 0.0).isEmpty(),
                "唇口横移本身不另开一块水");
    }

    private static void followsCurrentFoot() {
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        Vec3 contact = new Vec3(0.2, 9.0, -0.1);
        WaterSegment full = curtain(new Vec3(0.0, 8.0, 0.0), ahead, 0.0, 4.0, 20.0, 2.0, 0.7, true, false);
        MutableSegment held = new MutableSegment(full);
        OpenGate gate = new OpenGate();
        long captureTick = 100L;
        WeepingDownflowRoute route = new WeepingDownflowRoute(contact, ahead, captureTick, held, gate);
        RouteSample started = route.sample(captureTick);
        check(!started.finished() && route.active(), "接触当拍还在水里走");
        check(started.position().distanceTo(contact) < 1.0E-9, "接触当拍不跳到水脚");
        check(route.sample(captureTick).position().distanceTo(started.position()) < 1.0E-9, "同一拍再问不前进一步");

        RouteSample moved = route.sample(captureTick + 1L);
        check(!moved.finished(), "第一拍还没结束");
        check(moved.position().y < contact.y && moved.position().y > WeepingDeflectionHost.boxBottom(full),
                "第一拍只向下走一截");
        check(sameHorizontal(moved.position(), contact), "下行不改水平位置");

        double footHeight = moved.position().y;
        double span = WeepingDeflectionHost.boxTop(full) - footHeight;
        WaterSegment raised = curtain(new Vec3(4.0, footHeight + span * 0.5, -2.0), new Vec3(1.0, 0.0, 0.0),
                0.0, span, 20.0, 2.0, 0.7, true, false);
        held.replace(raised);
        RouteSample foot = route.sample(captureTick + 2L);
        check(foot.finished(), "水脚抬到箭下时提前结束");
        check(Math.abs(foot.position().y - WeepingDeflectionHost.boxBottom(raised)) < 1.0E-6, "停在当前水脚");
        check(foot.position().y > WeepingDeflectionHost.boxBottom(full) + 1.0, "不落到更深的旧水脚或足端落差");
        check(sameHorizontal(foot.position(), contact), "水段中心横移不带走箭");
        ReleaseSample outlet = route.emergencyRelease(captureTick + 2L);
        check(outlet.unitDirection().distanceTo(ahead) < 1.0E-9, "出口仍是捕获时的水平朝向");
        check(Math.abs(outlet.unitDirection().y) < 1.0E-9, "出口没有向下");
        check(outlet.position().distanceTo(foot.position()) < 1.0E-9, "正常放出位置就是水脚");
        check(!raised.floorFoot(), "没有真实地面也允许在水脚出射");
    }

    private static void emergencyStaysPut() {
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        Vec3 contact = new Vec3(0.2, 12.0, -0.1);
        WaterSegment full = curtain(new Vec3(0.0, 9.0, 0.0), ahead, 0.0, 6.0, 20.0, 2.0, 0.7, true, false);
        long captureTick = 40L;
        MutableSegment held = new MutableSegment(full);
        WeepingDownflowRoute route = new WeepingDownflowRoute(contact, ahead, captureTick, held, new OpenGate());
        check(route.sample(captureTick).position().distanceTo(contact) < 1.0E-9, "长瀑布开局留在接触点");
        RouteSample moved = route.sample(captureTick + 1L);
        check(moved.position().y < contact.y && moved.position().y > WeepingDeflectionHost.boxBottom(full),
                "长瀑布第一拍不到底");

        held.replace(curtain(new Vec3(6.0, 9.0, 1.0), new Vec3(1.0, 0.0, 0.0), 0.0, 6.0, 20.0, 2.0, 0.7, true, false));
        RouteSample shifted = route.sample(captureTick + 2L);
        check(!shifted.finished() && sameHorizontal(shifted.position(), contact), "源点横移时箭仍从接触点向下");

        held.replace(curtain(new Vec3(0.0, 6.5, 0.0), ahead, 0.0, 1.0, 20.0, 2.0, 0.7, true, false));
        RouteSample overtaken = route.sample(captureTick + 3L);
        check(!overtaken.finished() && !route.active(), "上沿追过箭时路线作废");
        check(overtaken.position().distanceTo(shifted.position()) < 1.0E-9, "上沿追过时留在上一拍");
        ReleaseSample overtakenExit = route.emergencyRelease(captureTick + 3L);
        check(overtakenExit.position().distanceTo(shifted.position()) < 1.0E-9, "紧急放出不改到水脚");
        check(overtakenExit.unitDirection().distanceTo(ahead) < 1.0E-9, "紧急出口是冻结朝向");
        check(route.sample(captureTick + 4L).position().distanceTo(overtaken.position()) < 1.0E-9, "作废后再问不再往下");

        MutableSegment emptyHeld = new MutableSegment(full);
        OpenGate emptyGate = new OpenGate();
        WeepingDownflowRoute emptyRoute = new WeepingDownflowRoute(contact, ahead, captureTick, emptyHeld, emptyGate);
        RouteSample emptyMoved = emptyRoute.sample(captureTick + 1L);
        emptyHeld.replace(curtain(new Vec3(0.0, 9.0, 0.0), ahead, 0.0, 6.0, 20.0, 2.0, 0.7, false, true));
        RouteSample emptied = emptyRoute.sample(captureTick + 2L);
        check(!emptied.finished() && !emptyRoute.active(), "水没了就停");
        check(emptied.position().distanceTo(emptyMoved.position()) < 1.0E-9, "水没了不落到旧水脚");
        check(emptyRoute.emergencyRelease(captureTick + 2L).unitDirection().distanceTo(ahead) < 1.0E-9,
                "水没了仍沿冻结朝向放出");

        MutableSegment closedHeld = new MutableSegment(full);
        OpenGate closedGate = new OpenGate();
        WeepingDownflowRoute closedRoute = new WeepingDownflowRoute(contact, ahead, captureTick, closedHeld, closedGate);
        RouteSample closedMoved = closedRoute.sample(captureTick + 1L);
        closedGate.close();
        RouteSample closed = closedRoute.sample(captureTick + 2L);
        check(!closed.finished() && !closedRoute.active(), "宿主撤掉后路线作废");
        check(closed.position().distanceTo(closedMoved.position()) < 1.0E-9, "宿主撤掉时留在当前位置");
        check(closedRoute.emergencyRelease(captureTick + 2L).unitDirection().distanceTo(ahead) < 1.0E-9,
                "宿主撤掉的出口是冻结朝向");

        Vec3 footContact = new Vec3(contact.x, WeepingDeflectionHost.boxBottom(full), contact.z);
        WeepingDownflowRoute already = new WeepingDownflowRoute(footContact, ahead, captureTick, new MutableSegment(full), new OpenGate());
        RouteSample immediate = already.sample(captureTick);
        check(immediate.finished() && already.active(), "已经在水脚就当拍正常结束");
        check(Math.abs(immediate.position().y - WeepingDeflectionHost.boxBottom(full)) < 1.0E-6, "当拍结束的位置是水脚");
        check(already.emergencyRelease(captureTick).unitDirection().distanceTo(ahead) < 1.0E-9, "水脚出口水平向前");

        WeepingDownflowRoute gone = new WeepingDownflowRoute(contact, ahead, captureTick,
                new MutableSegment(curtain(new Vec3(0.0, 9.0, 0.0), ahead, 0.0, 6.0, 20.0, 2.0, 0.7, false, false)),
                new OpenGate());
        RouteSample absent = gone.sample(captureTick);
        check(!absent.finished() && !gone.active(), "捕获时已经没水就不要占着");
        check(absent.position().distanceTo(contact) < 1.0E-9, "没水时停在接触点");

        RouteSample finished = routeAt(full, contact, ahead, captureTick).sample(captureTick + WeepingDownflowRoute.FLOW_TICKS);
        check(finished.finished(), "最多六拍后结束");
        check(Math.abs(finished.position().y - WeepingDeflectionHost.boxBottom(full)) < 1.0E-6, "六拍时落在捕获时的水脚");
        check(sameHorizontal(finished.position(), contact), "六拍仍然不改水平位置");
    }

    private static void copiesDoNotShareProgress() {
        Vec3 ahead = new Vec3(0.0, 0.0, 1.0);
        WaterSegment full = curtain(new Vec3(0.0, 9.0, 0.0), ahead, 0.0, 6.0, 6.0, 2.0, 0.7, true, false);
        Vec3 firstContact = new Vec3(0.2, 12.0, -0.1);
        Vec3 secondContact = new Vec3(-0.4, 11.0, 0.3);
        long captureTick = 7L;
        WeepingDownflowRoute first = routeAt(full, firstContact, ahead, captureTick);
        WeepingDownflowRoute second = routeAt(full, secondContact, ahead, captureTick);
        RouteSample advanced = first.sample(captureTick + 3L);
        RouteSample untouched = second.sample(captureTick);
        check(advanced.position().y < firstContact.y, "第一份已经向下");
        check(untouched.position().distanceTo(secondContact) < 1.0E-9, "另一份仍停在自己的接触点");
        check(Math.abs(advanced.position().x - untouched.position().x) > 0.1, "两份不共用一个位置");
    }

    private static void sharedGatesStayDefault() {
        boolean meleeOverride = false;
        boolean eligibilityOverride = false;
        for (Method method : WeepingDeflectionHost.class.getDeclaredMethods()) {
            if (method.getName().equals("intersectMelee")) {
                meleeOverride = true;
            }
            if (method.getName().equals("eligible")) {
                eligibilityOverride = true;
            }
        }
        check(!meleeOverride, "垂丝不覆盖近战");
        check(!eligibilityOverride, "垂丝不另写好友和费用资格");
        check(WeepingDownflowRoute.FLOW_TICKS == 6, "下行最多六拍");
        check(WeepingDeflectionHost.STABLE_GENERATION == 0L, "宿主代次固定为 0");
    }

    private static void bindingIgnoresNull() {
        WeepingDeflectionBinding.update(null, null);
        WeepingDeflectionBinding.remove(null);
        check(true, "空实体不登记");
    }

    private static WeepingDownflowRoute routeAt(WaterSegment segment, Vec3 contact, Vec3 ahead, long captureTick) {
        return new WeepingDownflowRoute(contact, ahead, captureTick, new MutableSegment(segment), new OpenGate());
    }

    private static boolean sameHorizontal(Vec3 point, Vec3 contact) {
        return Math.abs(point.x - contact.x) < 1.0E-9 && Math.abs(point.z - contact.z) < 1.0E-9;
    }

    private static WaterSegment curtain(Vec3 center, Vec3 ahead, double upperDrop, double lowerDrop, double footDrop,
                                        double across, double thick, boolean present, boolean floorFoot) {
        double middle = (upperDrop + lowerDrop) * 0.5;
        Vec3 lip = new Vec3(center.x, center.y + middle, center.z);
        return new WaterSegment(lip, center, ahead, upperDrop, lowerDrop, footDrop, across, thick, present, floorFoot);
    }

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(label);
        }
    }

    private static final class MutableSegment implements WeepingDownflowRoute.SegmentSupply {
        private WaterSegment segment;

        private MutableSegment(WaterSegment segment) {
            this.segment = segment;
        }

        private void replace(WaterSegment segment) {
            this.segment = segment;
        }

        @Override
        public WaterSegment current() {
            return segment;
        }
    }

    private static final class OpenGate implements WeepingDownflowRoute.FlowWindow {
        private boolean flowing = true;

        private void close() {
            this.flowing = false;
        }

        @Override
        public boolean open(long gameTick) {
            return flowing;
        }
    }
}

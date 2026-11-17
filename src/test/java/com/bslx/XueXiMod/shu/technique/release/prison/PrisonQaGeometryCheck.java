package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.release.deflection.ReleaseSample;
import com.bslx.XueXiMod.shu.technique.release.deflection.RouteSample;
import com.bslx.XueXiMod.shu.technique.release.prison.deflection.PrisonDeflectionHost;
import com.bslx.XueXiMod.shu.technique.release.prison.deflection.PrisonReflectRoute;
import com.bslx.XueXiMod.shu.technique.release.prison.deflection.PrisonShellGeometry;

import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Optional;
import java.util.UUID;

/**
 * 直接调用壳、出射和向内反射。不复制几何，不构造生物。
 */
public final class PrisonQaGeometryCheck {
    private static int assertionCount;
    private static int failures;

    private PrisonQaGeometryCheck() {
    }

    public static void main(String[] arguments) {
        shell();
        leaving();
        reflection();
        identity();
        System.out.println("EXECUTED shell-leaving-reflection");
        System.out.println("未执行 eligible 和 PrisonStrike：需要攻击者实体，不构造假实体");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void shell() {
        AABB body = new AABB(0.0D, 64.0D, 0.0D, 1.0D, 66.0D, 1.0D);
        AABB outer = PrisonShell.expand(body);
        check(outer != null, "外扩壳是空的");
        if (outer == null) {
            return;
        }
        check(near(outer.minX, body.minX - PrisonShell.PADDING), "壳没有按 0.20 外扩");
        check(near(outer.maxY, body.maxY + PrisonShell.PADDING), "壳顶部没有外扩");
        AABB inner = PrisonShell.innerOf(outer);
        check(inner != null && near(inner.minX, body.minX) && near(inner.maxZ, body.maxZ),
                "内层没有收回同一圈填充");
        check(PrisonShell.expand(null) == null && PrisonShell.innerOf(null) == null, "空盒子没有保持空");
    }

    private static void leaving() {
        AABB shell = new AABB(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 2.0D);
        Vec3 inside = new Vec3(1.0D, 1.0D, 1.0D);
        Vec3 outside = new Vec3(5.0D, 1.0D, 1.0D);
        Optional<PrisonShellGeometry.ShellHit> outward = PrisonShellGeometry.leaving(shell, inside, outside, 0.0D);
        check(outward.isPresent(), "壳内向壳外没有交点");
        if (outward.isPresent()) {
            PrisonShellGeometry.ShellHit hit = outward.get();
            check(hit.inwardNormal().x < -0.5D, "向东离开时内法线没有指回壳内，实际 " + hit.inwardNormal());
            check(hit.point().x > 1.5D && hit.point().x <= 2.0D + 1.0E-6D, "出射点不在东面，实际 " + hit.point());
        }
        Optional<PrisonShellGeometry.ShellHit> inward = PrisonShellGeometry.leaving(shell, outside, inside, 0.0D);
        check(inward.isEmpty(), "壳外向壳内被当成了被困者的出射");
        check(PrisonShellGeometry.leaving(shell, inside, inside, 0.0D).isEmpty(), "零位移被当成出射");
        check(PrisonShellGeometry.leaving(null, inside, outside, 0.0D).isEmpty(), "空壳给出了交点");
        Vec3 deep = new Vec3(1.0D, 1.0D, 1.0D);
        Vec3 face = new Vec3(2.0D, 1.0D, 1.0D);
        check(!PrisonShellGeometry.touches(shell, deep, 0.1D), "盒心被当成贴壳");
        check(PrisonShellGeometry.touches(shell, face, 0.0D), "壳面没有被当成接触");
    }

    private static void reflection() {
        Vec3 inwardNormal = new Vec3(-1.0D, 0.0D, 0.0D);
        Vec3 alreadyInward = PrisonShellGeometry.reflectInward(new Vec3(-2.0D, 0.0D, 0.0D), inwardNormal);
        check(near(alreadyInward.x, -2.0D) && near(alreadyInward.y, 0.0D), "已经朝内的速度被再弹了一次");
        Vec3 bounced = PrisonShellGeometry.reflectInward(new Vec3(2.0D, 0.0D, 0.0D), inwardNormal);
        check(near(bounced.x, -2.0D) && near(bounced.y, 0.0D), "朝外的速度没有按原长度折回壳内，实际 " + bounced);
        AABB shell = new AABB(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 2.0D);
        Vec3 contact = new Vec3(2.0D, 1.0D, 1.0D);
        Vec3 released = PrisonShellGeometry.releaseInside(shell, contact, inwardNormal, bounced.normalize(), 0.1D);
        check(inside(shell, released), "释放点跑到了壳外 " + released);
        check(!PrisonShellGeometry.touches(shell, released, 0.1D), "释放点仍贴着壳 " + released);
        PrisonReflectRoute route = new PrisonReflectRoute(contact, bounced);
        RouteSample first = route.sample(0L);
        RouteSample later = route.sample(40L);
        check(first.finished() && later.finished(), "反射路线没有当拍结束");
        check(near(first.position().x, later.position().x) && near(first.position().y, later.position().y)
                        && near(first.position().z, later.position().z),
                "两个时刻的落点不同，路线还在卷绕");
        ReleaseSample emergency = route.emergencyRelease(3L);
        check(emergency.unitDirection().dot(inwardNormal) > 0.5D, "紧急放出没有朝壳内");
        check(route.active(), "反射路线构造后不是活动的");
    }

    private static void identity() {
        UUID prisoner = UUID.randomUUID();
        UUID caster = UUID.randomUUID();
        PrisonView view = new PrisonView(UUID.randomUUID(), 1L, caster, prisoner, null, Vec3.ZERO,
                new AABB(0.0D, 0.0D, 0.0D, 1.0D, 2.0D, 1.0D), 2, 0L, 20L);
        try {
            PrisonDeflectionHost host = new PrisonDeflectionHost(view);
            check(host.confines(prisoner), "被困者没有被认成牢内攻击者");
            check(!host.confines(caster), "施术者被认成了被困者");
            check(!host.confines(null), "空身份被关进了牢");
            check(!host.confines(UUID.randomUUID()), "外人被认成了被困者");
            System.out.println("EXECUTED PrisonDeflectionHost.confines");
        } catch (LinkageError failure) {
            System.out.println("UNEXECUTED confines 需要游戏引导 " + failure.getClass().getName());
        } catch (RuntimeException failure) {
            check(false, "confines 抛出 " + failure.getClass().getName() + " " + failure.getMessage());
        }
    }

    private static boolean inside(AABB shell, Vec3 point) {
        return point.x >= shell.minX && point.x <= shell.maxX
                && point.y >= shell.minY && point.y <= shell.maxY
                && point.z >= shell.minZ && point.z <= shell.maxZ;
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) < 1.0E-6D;
    }

    private static void check(boolean passed, String message) {
        assertionCount++;
        if (!passed) {
            failures++;
            System.out.println("失败 " + message);
        }
    }
}

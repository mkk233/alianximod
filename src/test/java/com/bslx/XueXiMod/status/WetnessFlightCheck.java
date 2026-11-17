package com.bslx.XueXiMod.status;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.List;

/**
 * 调用生产的追加下降和体素裁短。不断言手写的第二套碰撞。
 * 不构造世界，也不跑恼鬼或末影龙实体。
 */
public final class WetnessFlightCheck {
    private static int checks;
    private static int failures;

    private WetnessFlightCheck() {}

    public static void main(String[] arguments) {
        tiers();
        volumes();
        vexEndpoint();
        parts();
        guards();
        eligibility();
        System.out.println("断言 " + checks + " 失败 " + failures);
        System.out.println("实体路径 未运行");
        System.out.println("方块标签选择 未运行");
        System.out.println("完整变换 未验证");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void tiers() {
        check(WetnessFlight.extraDown(1, 0.2D) == 0.0D, "一级没有追加");
        check(WetnessFlight.extraDown(2, 0.2D) == 0.0D, "二级没有追加");
        check(WetnessFlight.extraDown(1, -0.5D) == 0.0D, "一级更快的竖直也不追加");
        check(WetnessFlight.extraDown(2, -0.5D) == 0.0D, "二级更快的竖直也不追加");
        double slowExtra = WetnessFlight.extraDown(3, 0.2D);
        double fastExtra = WetnessFlight.extraDown(4, 0.2D);
        near(0.2D + slowExtra, WetnessFlight.SLOW_LIMIT, "三级从 0.2 追加后应到缓慢上限");
        near(0.2D + fastExtra, WetnessFlight.FAST_LIMIT, "四级从 0.2 追加后应到快速上限");
        near(slowExtra, -0.28D, "三级追加");
        near(fastExtra, -0.50D, "四级追加");
        check(WetnessFlight.extraDown(3, -0.5D) == 0.0D, "三级已经更快时追加为 0");
        check(WetnessFlight.extraDown(4, -0.5D) == 0.0D, "四级已经更快时追加为 0");
        System.out.println("数值 三级追加 " + slowExtra + " 四级追加 " + fastExtra);
    }

    private static void volumes() {
        VoxelShape cube = Shapes.block();
        double requested = -0.30D;
        double fullClip = WetnessFlightClip.clipExtra(column(1.0D + 0.05D), requested, List.of(cube));
        near(fullClip, -0.05D, "整块上方 0.05");
        VoxelShape slab = Shapes.box(0.0D, 0.0D, 0.0D, 1.0D, 0.5D, 1.0D);
        double slabClip = WetnessFlightClip.clipExtra(column(0.5D + 0.05D), requested, List.of(slab));
        near(slabClip, -0.05D, "半砖上方 0.05");
        AABB touching = column(1.0D);
        check(!Shapes.joinIsNotEmpty(Shapes.create(touching), cube, BooleanOp.AND), "贴面不是体素重叠");
        double touchClip = WetnessFlightClip.clipExtra(touching, requested, List.of(cube));
        check(touchClip == 0.0D, "贴面不能再下降，实际 " + touchClip);
        AABB buried = new AABB(0.2D, 0.2D, 0.2D, 0.8D, 1.2D, 0.8D);
        check(Shapes.joinIsNotEmpty(Shapes.create(buried), cube, BooleanOp.AND), "夹具应真实重叠");
        double buriedClip = WetnessFlightClip.clipExtra(buried, requested, List.of(cube));
        double collideWitness = Shapes.collide(
                net.minecraft.core.Direction.Axis.Y, buried, List.of(cube), requested);
        check(buriedClip == 0.0D, "重叠必须停在原地，实际 " + buriedClip);
        System.out.println("重叠对照 collide=" + collideWitness + " clip=" + buriedClip);
        VoxelShape pillars = Shapes.or(
                Shapes.box(0.0D, 0.0D, 0.0D, 0.2D, 1.0D, 1.0D),
                Shapes.box(0.8D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
        AABB corridor = new AABB(0.3D, 1.05D, 0.2D, 0.7D, 2.05D, 0.8D);
        AABB swept = corridor.expandTowards(0.0D, requested, 0.0D);
        check(swept.intersects(pillars.bounds()), "夹缝扫过的外包围盒相交，界限 " + pillars.bounds());
        check(!Shapes.joinIsNotEmpty(Shapes.create(corridor), pillars, BooleanOp.AND), "夹缝起点没有咬住柱子");
        double corridorClip = WetnessFlightClip.clipExtra(corridor, requested, List.of(pillars));
        near(corridorClip, requested, "夹缝不能按包围盒挡住");
        AABB striking = new AABB(0.0D, 1.05D, 0.2D, 0.4D, 2.05D, 0.8D);
        double strikingClip = WetnessFlightClip.clipExtra(striking, requested, List.of(pillars));
        near(strikingClip, -0.05D, "撞上左柱应留下 0.05");
        AABB above = column(1.05D);
        check(WetnessFlightClip.clipExtra(above, requested, List.of()) == requested, "空集合保留追加");
        check(WetnessFlightClip.clipExtra(above, requested, null) == requested, "空引用保留追加");
        check(WetnessFlightClip.clipExtra(above, requested, List.of(Shapes.empty())) == requested, "空形状保留追加");
        check(WetnessFlightClip.clipExtra(above, 0.2D, List.of(cube)) == 0.0D, "向上的追加必须是 0");
        check(WetnessFlightClip.clipExtra(null, requested, List.of(cube)) == 0.0D, "空盒子必须是 0");
        double mixed = WetnessFlightClip.clipExtra(above, requested, List.of(Shapes.empty(), cube));
        near(mixed, -0.05D, "空形状不能抵消整块");
        System.out.println("体积 整块 " + fullClip + " 半砖 " + slabClip + " 贴面 " + touchClip
                + " 夹缝 " + corridorClip + " 撞柱 " + strikingClip);
    }

    private static void vexEndpoint() {
        double extra = WetnessFlight.extraDown(3, 0.2D);
        AABB destination = new AABB(0.2D, 0.25D, 0.2D, 0.8D, 1.25D, 0.8D);
        VoxelShape ground = Shapes.box(0.0D, -1.0D, 0.0D, 1.0D, 0.0D, 1.0D);
        double allowed = WetnessFlightClip.clipExtra(destination, extra, List.of(ground));
        near(allowed, -0.25D, "恼鬼终点底 0.25 对地面只许再降 0.25");
        near(0.2D + allowed, -0.05D, "原竖直 0.2 加上裁短结果");
        AABB phased = new AABB(0.2D, 0.2D, 0.2D, 0.8D, 1.2D, 0.8D);
        double phasedClip = WetnessFlightClip.clipExtra(phased, extra, List.of(Shapes.block()));
        check(phasedClip == 0.0D, "水平穿模时追加竖直必须为 0，实际 " + phasedClip);
        System.out.println("恼鬼终点 追加 " + extra + " 裁短 " + allowed + " 穿模 " + phasedClip);
    }

    private static void parts() {
        double requested = -0.30D;
        VoxelShape cube = Shapes.block();
        double headAllowed = WetnessFlightClip.clipExtra(column(1.0D + 0.02D), requested, List.of(cube));
        double bodyAllowed = WetnessFlightClip.clipExtra(column(1.0D + 0.10D), requested, List.of(cube));
        double neckAllowed = WetnessFlightClip.clipExtra(column(1.0D + 0.20D), requested, List.of(cube));
        double shared = WetnessFlightClip.strictestExtra(requested, headAllowed, neckAllowed, bodyAllowed);
        near(headAllowed, -0.02D, "头的空隙");
        near(bodyAllowed, -0.10D, "身的空隙");
        near(neckAllowed, -0.20D, "颈的空隙");
        check(shared == headAllowed, "最严应是头的裁短，实际 " + shared + " 头 " + headAllowed);
        double unused = WetnessFlightClip.rejectedDown(requested, shared);
        near(unused, -0.28D, "未采用的追加只是差额，不是上抬");
        AABB buried = new AABB(0.2D, 0.2D, 0.2D, 0.8D, 1.2D, 0.8D);
        double buriedAllowed = WetnessFlightClip.clipExtra(buried, requested, List.of(cube));
        check(buriedAllowed == 0.0D, "埋入的部件裁短应为 0，实际 " + buriedAllowed);
        double blocked = WetnessFlightClip.strictestExtra(requested, headAllowed, buriedAllowed, neckAllowed);
        check(blocked == 0.0D, "任一部位重叠时整条追加为 0，实际 " + blocked);
        check(WetnessFlightClip.strictestExtra(requested, -0.50D) == requested, "最严不能比请求下降得更多");
        System.out.println("部件 头 " + headAllowed + " 身 " + bodyAllowed + " 颈 " + neckAllowed
                + " 最严 " + shared + " 未采用 " + unused);
    }

    private static void guards() {
        WetnessFlight.beginDragonStep(null);
        WetnessFlight.beforeTravel(null);
        Vec3 sample = new Vec3(3.0D, 0.2D, -4.0D);
        Vec3 vexReturned = WetnessFlight.adjustVexMove(null, MoverType.SELF, sample);
        Vec3 dragonReturned = WetnessFlight.adjustDragonMove(null, MoverType.SELF, sample);
        check(vexReturned == sample && vexReturned.x == 3.0D && vexReturned.z == -4.0D,
                "空恼鬼应原样返回向量");
        check(dragonReturned == sample, "空末影龙应原样返回向量");
        List<VoxelShape> missingLevel = WetnessFlightClip.dragonObstacles(
                null, null, new AABB(0.0D, 0.0D, 0.0D, 1.0D, 1.0D, 1.0D));
        check(missingLevel.isEmpty(), "没有世界时障碍为空，实际 " + missingLevel.size());
        System.out.println("空守卫已执行。恼鬼裁短、龙部件下移和方块标签循环没有实体或世界可跑。");
    }

    private static void eligibility() {
        check(WetnessFlight.descentEligible(true, false, false, false, false, false, false),
                "玩家标志为真、人在空中、没有乘客落地和流体时，现在应允许下降");
        check(WetnessFlight.descentEligible(false, false, false, false, false, false, false),
                "非飞行生物在同样的空中条件下现在应允许下降");
        check(!WetnessFlight.descentEligible(false, true, false, false, true, true, true),
                "乘客仍然不允许下降");
        check(!WetnessFlight.descentEligible(false, false, true, false, true, true, true),
                "落地仍然不允许下降");
        check(!WetnessFlight.descentEligible(false, false, false, true, true, true, true),
                "实际流体里仍然不允许下降");
        check(WetnessFlight.extraDown(1, 0.2D) == 0.0D && WetnessFlight.extraDown(2, 0.2D) == 0.0D,
                "一级和二级的追加量变了");
        System.out.println("资格 玩家和非飞行空中为允许，乘客落地流体仍拒绝。实体黑名单没有构造生物。");
    }

    private static AABB column(double bottom) {
        return new AABB(0.2D, bottom, 0.2D, 0.8D, bottom + 1.0D, 0.8D);
    }

    private static void near(double seen, double expected, String detail) {
        check(Math.abs(seen - expected) < 1.0E-9D, detail + " 实际 " + seen + " 期望 " + expected);
    }

    private static void check(boolean condition, String detail) {
        checks++;
        if (!condition) {
            failures++;
            System.out.println("失败[" + checks + "] " + detail);
        }
    }
}

package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.hitbox.SizedBoxHitbox;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 垂丝相位、足迹裁切和精确盒子。直接调用生产方法。
 * 不启动世界，因此不覆盖方块枚举、粒子和实体缓存。
 */
public final class WeepingFlowGeometryCheck {
    private static final Vec3 SOUTH = new Vec3(0.0, 0.0, 1.0);
    private static final double FOOT = 5.62;

    private static int assertionCount;

    private WeepingFlowGeometryCheck() {
    }

    public static void main(String[] programArguments) {
        frozenNumbers();
        lipStaysOverSource();
        phaseGrowsHoldsThenDrains();
        centerDoesNotLean();
        flatGroundLands();
        sourceInsideEmpties();
        openAirAndLimitAreNotGround();
        stepAndNarrowObstacle();
        diagonalCornerIsOutside();
        oversizeDoesNotSampleThrough();
        noMinimumFakeHeight();
        splashUsesTheRealFoot();
        widthChargeStaysFull();
        emptySegmentIsFinite();
        exactBoxSkipsPreviousCenter();
        System.out.println("垂丝几何检查通过");
        System.out.println("断言 " + assertionCount);
        System.out.println("未跑：世界碰撞形状枚举、水花粒子、实体弱缓存、命中实体上的 contains");
    }

    private static void frozenNumbers() {
        check(WeepingSettings.FILL_TICKS == 8, "填充是 8 拍");
        check(WeepingSettings.DRAIN_START == 32, "满流保持到 32 拍");
        check(WeepingSettings.LIFETIME == 40, "寿命是 40 拍");
        check(Math.abs(WeepingSettings.FALL_DISTANCE_LIMIT - 6.0) < 1.0E-12, "最大落差是 6");
        check(Math.abs(WeepingCurtain.LIP_LIFT - 4.0) < 1.0E-12, "唇口抬高 4");
    }

    private static void lipStaysOverSource() {
        Vec3 lip = WeepingCurtain.lipOf(new Vec3(2.0, 1.62, 3.0));
        check(Math.abs(lip.x - 2.0) < 1.0E-12, "唇口不往前移");
        check(Math.abs(lip.z - 3.0) < 1.0E-12, "唇口不横移");
        check(Math.abs(lip.y - 5.62) < 1.0E-12, "眼睛加 4 得到 5.62");
    }

    private static void phaseGrowsHoldsThenDrains() {
        Vec3 lip = new Vec3(3.0, 10.0, 4.0);
        WaterSegment closed = flowing(lip, 0);
        check(!closed.present(), "0 拍还没有水");
        check(closed.activeHeight() == 0.0, "0 拍高度是 0");
        check(samePoint(closed.center(), lip), "0 拍中心还在唇口");

        WaterSegment growing = flowing(lip, 4);
        check(growing.present(), "4 拍水头已经下来");
        check(Math.abs(growing.upperDrop()) < 1.0E-12, "填充时上沿留在唇口");
        check(Math.abs(growing.lowerDrop() - FOOT * 0.5) < 1.0E-9, "4 拍下沿走到一半");
        check(Math.abs(growing.center().y - (lip.y - FOOT * 0.25)) < 1.0E-9, "填充中心在上下沿中间");

        WaterSegment full = flowing(lip, WeepingSettings.FILL_TICKS);
        check(full.present() && full.floorFoot(), "8 拍整段在");
        check(Math.abs(full.lowerDrop() - FOOT) < 1.0E-12, "8 拍下沿到达足端");
        check(Math.abs(full.upperDrop()) < 1.0E-12, "8 拍上沿仍在唇口");
        check(Math.abs(full.center().y + full.activeHeight() * 0.5 - lip.y) < 1.0E-9, "满流上沿贴着唇口");
        check(Math.abs(full.center().y - full.activeHeight() * 0.5 - (lip.y - FOOT)) < 1.0E-9, "满流下沿在足端");

        WaterSegment held = flowing(lip, WeepingSettings.DRAIN_START);
        check(held.present(), "32 拍仍是满流");
        check(Math.abs(held.upperDrop()) < 1.0E-12 && Math.abs(held.lowerDrop() - FOOT) < 1.0E-12, "32 拍还没开始收");

        WaterSegment draining = flowing(lip, 36);
        check(draining.present(), "36 拍还剩半段");
        check(Math.abs(draining.upperDrop() - FOOT * 0.5) < 1.0E-9, "36 拍上沿下移一半");
        check(Math.abs(draining.lowerDrop() - FOOT) < 1.0E-9, "36 拍下沿仍在足端");
        check(Math.abs(draining.center().y + draining.activeHeight() * 0.5 - (lip.y - FOOT * 0.5)) < 1.0E-9,
                "排空时可见上沿跟着下移");

        WaterSegment ended = flowing(lip, WeepingSettings.LIFETIME);
        check(!ended.present(), "40 拍不再有水");
        check(ended.activeHeight() == 0.0, "40 拍不留最低高度");
        check(Math.abs(ended.upperDrop() - ended.lowerDrop()) < 1.0E-12, "40 拍上下沿重合");
        check(Math.abs(ended.center().y - (lip.y - FOOT)) < 1.0E-9, "收干后中心落到足端高度但没有体积");
    }

    private static void centerDoesNotLean() {
        Vec3 lip = new Vec3(3.0, 10.0, 4.0);
        Vec3 pitched = new Vec3(0.0, -2.0, 2.0);
        WaterSegment segment = WeepingCurtain.flowSegment(lip, pitched, FOOT, true, 0.5, 0.35, 16);
        check(Math.abs(segment.center().x - lip.x) < 1.0E-12, "俯仰不把帘心往东推");
        check(Math.abs(segment.center().z - lip.z) < 1.0E-12, "俯仰不把帘心往南推");
        check(Math.abs(segment.ahead().y) < 1.0E-12, "朝向没有垂直分量");
        check(segment.ahead().z > 0.99, "朝向落在水平南方");
        check(Math.abs(segment.ahead().lengthSqr() - 1.0) < 1.0E-9, "朝向是单位向量");
        WaterSegment east = flowing(lip, 16);
        check(Math.abs(east.center().x - lip.x) < 1.0E-12, "朝东时中心也不前倾");
        check(Math.abs(east.center().z - lip.z) < 1.0E-12, "朝东时横向中心留在唇口");
    }

    private static void flatGroundLands() {
        Vec3 lip = new Vec3(0.0, FOOT, 0.0);
        AABB ground = new AABB(-4.0, -2.0, -4.0, 4.0, 0.0, 4.0);
        WeepingCurtain.FootClip clip = clip(lip, SOUTH, 0.5, 0.35, List.of(ground));
        check(clip.floorFoot(), "5.62 的平地是朝上地面");
        check(!clip.sourceInside() && !clip.overBudget(), "平地没有堵住唇口");
        check(Math.abs(clip.footDrop() - FOOT) < 1.0E-9, "落差就是 5.62");
        WaterSegment segment = WeepingCurtain.segmentFor(clip, lip, SOUTH, 0.5, 0.35, WeepingSettings.FILL_TICKS);
        check(segment.present() && segment.floorFoot(), "落地后满流仍在");
        check(Math.abs(segment.activeHeight() - FOOT) < 1.0E-9, "满流高度等于落差");
    }

    private static void sourceInsideEmpties() {
        Vec3 lip = new Vec3(0.0, 5.5, 0.0);
        AABB solid = new AABB(-1.0, 5.0, -1.0, 1.0, 6.0, 1.0);
        AABB floor = new AABB(-4.0, -1.0, -4.0, 4.0, 0.0, 4.0);
        WeepingCurtain.FootClip clip = clip(lip, SOUTH, 0.5, 0.35, List.of(floor, solid));
        check(clip.sourceInside(), "唇口在实心里");
        check(!clip.floorFoot() && clip.footDrop() == 0.0, "源内实心不是落地");
        WaterSegment hidden = WeepingCurtain.segmentFor(clip, lip, SOUTH, 0.5, 0.35, WeepingSettings.FILL_TICKS);
        check(!hidden.present() && hidden.activeHeight() == 0.0, "源内实心没有水体");
        check(!hidden.floorFoot(), "空段不报告地面");

        AABB mouth = new AABB(-0.1, 5.0, -0.2, 0.4, 7.0, 0.2);
        WeepingCurtain.FootClip cut = clip(new Vec3(0.0, 6.0, 0.0), SOUTH, 0.5, 0.35, List.of(mouth));
        check(cut.sourceInside(), "切过唇口平面的墙算实心");

        AABB ceiling = new AABB(-1.0, 10.2, -1.0, 1.0, 12.0, 1.0);
        WeepingCurtain.FootClip under = clip(new Vec3(0.0, 10.0, 0.0), SOUTH, 0.5, 0.35, List.of(ceiling));
        check(!under.sourceInside() && !under.floorFoot(), "头顶的方块不变成地面");
        check(Math.abs(under.footDrop() - WeepingSettings.FALL_DISTANCE_LIMIT) < 1.0E-12, "只有天花板时落到最大落差");
    }

    private static void openAirAndLimitAreNotGround() {
        Vec3 lip = new Vec3(0.0, 40.0, 0.0);
        WeepingCurtain.FootClip air = clip(lip, SOUTH, 0.5, 0.35, List.of());
        check(!air.floorFoot() && !air.sourceInside(), "没有方块就不是地面");
        check(Math.abs(air.footDrop() - WeepingSettings.FALL_DISTANCE_LIMIT) < 1.0E-12, "空中停在最大落差");
        AABB atLimit = new AABB(-2.0, 0.0, -2.0, 2.0, 34.0, 2.0);
        WeepingCurtain.FootClip ended = clip(lip, SOUTH, 0.5, 0.35, List.of(atLimit));
        check(!ended.floorFoot(), "刚好 6 格的末端不是地面");
        check(Math.abs(ended.footDrop() - WeepingSettings.FALL_DISTANCE_LIMIT) < 1.0E-9, "末端仍是最大落差");
        AABB insideLimit = new AABB(-2.0, 0.0, -2.0, 2.0, 34.2, 2.0);
        WeepingCurtain.FootClip landed = clip(lip, SOUTH, 0.5, 0.35, List.of(insideLimit));
        check(landed.floorFoot(), "比 6 格更近的盒顶是地面");
        check(Math.abs(landed.footDrop() - 5.8) < 1.0E-9, "落差停在盒顶");
    }

    private static void stepAndNarrowObstacle() {
        Vec3 lip = new Vec3(0.0, 10.0, 0.0);
        AABB lowerStep = new AABB(-2.0, 0.0, -2.0, 2.0, 5.0, 2.0);
        AABB upperStep = new AABB(-2.0, 0.0, -2.0, 0.0, 7.0, 2.0);
        WeepingCurtain.FootClip stepped = clip(lip, SOUTH, 2.0, 0.35, List.of(lowerStep, upperStep));
        check(stepped.floorFoot(), "台阶顶是地面");
        check(Math.abs(stepped.footDrop() - 3.0) < 1.0E-9, "整帘取更高的那一级");

        AABB tooth = new AABB(2.55, 0.0, -0.1, 2.85, 9.0, 0.1);
        WeepingCurtain.FootClip narrow = clip(new Vec3(0.0, 12.0, 0.0), SOUTH, 8.0, 0.4, List.of(tooth));
        check(narrow.floorFoot(), "宽帘里的窄障碍不能漏过");
        check(Math.abs(narrow.footDrop() - 3.0) < 1.0E-9, "窄障碍把落差收到它的顶");

        AABB beside = new AABB(0.25, 0.0, -1.0, 1.25, 11.0, 1.0);
        WeepingCurtain.FootClip missed = clip(new Vec3(0.0, 12.0, 0.0), SOUTH, 0.5, 0.35, List.of(beside));
        check(!missed.floorFoot(), "只贴着外侧面的墙不裁短");
        check(Math.abs(missed.footDrop() - WeepingSettings.FALL_DISTANCE_LIMIT) < 1.0E-12, "贴面墙外仍是最大落差");

        AABB intruding = new AABB(0.0, 0.0, -1.0, 1.0, 11.0, 1.0);
        WeepingCurtain.FootClip cut = clip(new Vec3(0.0, 12.0, 0.0), SOUTH, 0.5, 0.35, List.of(intruding));
        check(cut.floorFoot() && Math.abs(cut.footDrop() - 1.0) < 1.0E-9, "伸进足迹的墙按盒顶截断");
    }

    private static void diagonalCornerIsOutside() {
        Vec3 lip = new Vec3(0.0, 16.0, 0.0);
        Vec3 diagonal = new Vec3(1.0, 0.0, 1.0);
        AABB corner = new AABB(0.15, 10.0, 0.15, 0.35, 14.0, 0.35);
        WeepingCurtain.FootClip outside = clip(lip, diagonal, 0.4, 0.4, List.of(corner));
        check(!outside.floorFoot() && !outside.sourceInside(), "轴向包围盒角落但帘外的方块不算");
        check(Math.abs(outside.footDrop() - WeepingSettings.FALL_DISTANCE_LIMIT) < 1.0E-12, "帘外角落不改变落差");
        AABB onCurtain = new AABB(-0.05, 10.0, -0.05, 0.05, 14.0, 0.05);
        WeepingCurtain.FootClip inside = clip(lip, diagonal, 0.4, 0.4, List.of(onCurtain));
        check(inside.floorFoot() && Math.abs(inside.footDrop() - 2.0) < 1.0E-9, "斜帘中心的方块仍要截断");
    }

    private static void oversizeDoesNotSampleThrough() {
        Vec3 lip = new Vec3(0.0, 20.0, 0.0);
        long calmCells = WeepingCurtain.footprintCellCount(lip, SOUTH, 0.5, 0.35);
        long hugeCells = WeepingCurtain.footprintCellCount(lip, SOUTH, 80.0, 40.0);
        check(calmCells > 0 && calmCells <= WeepingCurtain.FOOTPRINT_CELL_BUDGET, "普通帘在预算内");
        check(hugeCells > WeepingCurtain.FOOTPRINT_CELL_BUDGET, "超大足迹超过预算");
        AABB solid = new AABB(-1.0, 19.0, -1.0, 1.0, 21.0, 1.0);
        WeepingCurtain.FootClip huge = clip(lip, SOUTH, 80.0, 40.0, List.of(solid));
        check(huge.overBudget(), "超预算要记下来");
        check(!huge.sourceInside() && !huge.floorFoot(), "超预算时不拿部分格子继续判");
        check(huge.cellCount() == hugeCells, "记下的格子数就是足迹边界");
        check(WeepingCurtain.footprintBudgetRejected(), "最近一次超预算能被读到");
        check(WeepingCurtain.notedFootprintCells() == hugeCells, "最近一次格子数与足迹一致");
        WaterSegment hidden = WeepingCurtain.segmentFor(huge, lip, SOUTH, 80.0, 40.0, 8);
        check(!hidden.present() && hidden.activeHeight() == 0.0, "超预算整段留空，不穿实心");
        WeepingCurtain.FootClip calm = clip(lip, SOUTH, 0.5, 0.35, List.of());
        check(!calm.overBudget() && !WeepingCurtain.footprintBudgetRejected(), "随后的普通帘清掉超预算标记");
    }

    private static void noMinimumFakeHeight() {
        Vec3 lip = new Vec3(0.0, 10.0, 0.0);
        AABB almost = new AABB(-1.0, 0.0, -1.0, 1.0, 9.98, 1.0);
        WeepingCurtain.FootClip tiny = clip(lip, SOUTH, 0.5, 0.35, List.of(almost));
        check(tiny.floorFoot() && Math.abs(tiny.footDrop() - 0.02) < 1.0E-9, "0.02 的缝仍按真实盒顶");
        WaterSegment gone = WeepingCurtain.segmentFor(tiny, lip, SOUTH, 0.5, 0.35, WeepingSettings.FILL_TICKS);
        check(!gone.present() && gone.activeHeight() == 0.0, "小于开度的缝不保底成水");
        AABB shortFall = new AABB(-1.0, 0.0, -1.0, 1.0, 9.9, 1.0);
        WeepingCurtain.FootClip kept = clip(lip, SOUTH, 0.5, 0.35, List.of(shortFall));
        WaterSegment thin = WeepingCurtain.segmentFor(kept, lip, SOUTH, 0.5, 0.35, WeepingSettings.FILL_TICKS);
        check(thin.present() && Math.abs(thin.activeHeight() - 0.1) < 1.0E-9, "0.1 的水保持 0.1，不抬到 0.2");
    }

    private static void splashUsesTheRealFoot() {
        Vec3 lip = new Vec3(2.0, FOOT, -1.0);
        AABB ground = new AABB(-4.0, -2.0, -4.0, 4.0, 0.0, 4.0);
        WeepingCurtain.FootClip landed = clip(lip, new Vec3(1.0, 0.0, 0.0), 0.5, 0.35, List.of(ground));
        WaterSegment full = WeepingCurtain.segmentFor(landed, lip, new Vec3(1.0, 0.0, 0.0), 0.5, 0.35, WeepingSettings.FILL_TICKS);
        check(WeepingCurtain.splashes(WeepingSettings.FILL_TICKS, landed, full), "水头到达地面才溅");
        Vec3 foot = WeepingCurtain.footPoint(full);
        check(Math.abs(foot.x - lip.x) < 1.0E-12 && Math.abs(foot.z - lip.z) < 1.0E-12, "水花在唇口正下方");
        check(Math.abs(foot.y) < 1.0E-9, "水花高度是真实足端");
        WeepingCurtain.FootClip air = clip(lip, SOUTH, 0.5, 0.35, List.of());
        WaterSegment hanging = WeepingCurtain.segmentFor(air, lip, SOUTH, 0.5, 0.35, WeepingSettings.FILL_TICKS);
        check(hanging.present() && !hanging.floorFoot(), "没有地面时水还在，但不算落地");
        check(!WeepingCurtain.splashes(WeepingSettings.FILL_TICKS, air, hanging), "没有地面不溅水花");
        check(!WeepingCurtain.splashes(WeepingSettings.DRAIN_START, landed, full), "满流之后不再补一次水花");
        WaterSegment blocked = WaterSegment.absent(lip, SOUTH, 0.5, 0.35);
        check(!WeepingCurtain.splashes(WeepingSettings.FILL_TICKS, landed, blocked), "没有水体不溅");
    }

    private static void widthChargeStaysFull() {
        check(Math.abs(WeepingCurtain.sheetAcross(0.5) - 0.5) < 1.0E-12, "基准宽是 0.5");
        check(Math.abs(WeepingCurtain.sheetThick(0.5) - 0.35) < 1.0E-12, "基准厚是 0.35");
        check(Math.abs(WeepingCurtain.sheetAcross(1.0) - 1.0) < 1.0E-12, "加宽后整宽保留");
        check(Math.abs(WeepingCurtain.sheetThick(1.0) - 0.7) < 1.0E-12, "厚度按宽度放大");
        check(Math.abs(WeepingCurtain.sheetAcross(0.05) - 0.05) < 1.0E-12, "过窄仍保留原来的宽");
        check(Math.abs(WeepingCurtain.sheetThick(0.05) - 0.35) < 1.0E-12, "过窄用基准厚度");
        check(Math.abs(WeepingCurtain.sheetAcross(Double.NaN) - 0.5) < 1.0E-12, "非法宽度回到基准");
        double chargedAcross = WeepingCurtain.sheetAcross(1.0);
        double chargedThick = WeepingCurtain.sheetThick(1.0);
        WaterSegment charged = WeepingCurtain.flowSegment(
                new Vec3(0.0, 8.0, 0.0), SOUTH, FOOT, true, chargedAcross, chargedThick, 16);
        check(Math.abs(charged.across() - 1.0) < 1.0E-12, "水段记下的是整宽");
        check(Math.abs(charged.thick() - 0.7) < 1.0E-12, "水段记下的是整厚");
        Vec3 side = new Vec3(0.4, 8.0 - FOOT * 0.5, 0.0);
        check(!SizedBoxHitbox.holdsPoint(charged.center(), SOUTH, 0.35, 0.5, FOOT, side), "基准半宽不包含 0.4");
        check(SizedBoxHitbox.holdsPoint(charged.center(), SOUTH, charged.thick(), charged.across(), FOOT, side),
                "加宽后的整宽包含 0.4");
    }

    private static void emptySegmentIsFinite() {
        WaterSegment empty = WeepingCurtain.active(null);
        check(empty == WaterSegment.EMPTY, "没有缓存时返回同一条有限空段");
        check(!empty.present() && !empty.floorFoot(), "空段没有水也没有地面");
        check(empty.footDrop() == 0.0 && empty.activeHeight() == 0.0, "空段不是探地得来的 6 格");
        check(finiteSegment(empty), "空段每个数都有限");
        check(empty.ahead().lengthSqr() > 0.5, "空段朝向可用");
        WeepingCurtain.forget(null);
        check(WeepingCurtain.active(null) == WaterSegment.EMPTY, "忘掉空键之后仍是空段");
    }

    private static void exactBoxSkipsPreviousCenter() {
        Vec3 current = new Vec3(0.0, 1.0, 0.0);
        Vec3 previous = new Vec3(0.0, 1.0, 0.7);
        AABB body = new AABB(-0.2, 0.5, 0.5, 0.2, 1.5, 0.9);
        Vec3 bodyCenter = body.getCenter();
        boolean narrowHit = SizedBoxHitbox.holdsPoint(current, SOUTH, 0.35, 0.5, 2.0, bodyCenter);
        boolean sweptHit = SizedBoxHitbox.sweptSegmentHits(body, previous, current);
        check(!narrowHit, "当前薄盒碰不到前倾出去的旧中心");
        check(sweptHit, "旧线段仍然能扫到那个身体");
        check(!SizedBoxHitbox.acceptContact(true, 2.0, narrowHit, sweptHit), "精确体积不吃上一帘心");
        check(SizedBoxHitbox.acceptContact(false, 2.0, narrowHit, sweptHit), "非精确体积仍保留扫掠");
        check(SizedBoxHitbox.acceptContact(false, 0.01, false, true), "非精确的矮盒子仍走旧扫掠");
        check(!SizedBoxHitbox.acceptContact(true, 0.05, true, true), "精确高度不到 0.05 不算命中");
        check(SizedBoxHitbox.acceptContact(true, 2.0, true, false), "精确体积的当前盒命中有效");
        check(SizedBoxHitbox.holdsPoint(current, SOUTH, 0.35, 0.5, 2.0, current), "中心在当前盒内");
        check(SizedBoxHitbox.holdsPoint(current, SOUTH, 0.35, 0.5, 2.0, new Vec3(0.25, 1.0, 0.0)), "半宽边界仍算在里面");
        check(!SizedBoxHitbox.holdsPoint(current, SOUTH, 0.35, 0.5, 2.0, new Vec3(0.26, 1.0, 0.0)), "超出半宽不算");
    }

    private static WaterSegment flowing(Vec3 lip, int age) {
        return WeepingCurtain.flowSegment(lip, new Vec3(1.0, 0.0, 0.0), FOOT, true, 0.5, 0.35, age);
    }

    private static WeepingCurtain.FootClip clip(Vec3 lip, Vec3 look, double across, double thick, List<AABB> boxes) {
        return WeepingCurtain.clipFootprint(lip, look, across, thick, boxes);
    }

    private static boolean samePoint(Vec3 left, Vec3 right) {
        return Math.abs(left.x - right.x) < 1.0E-12
                && Math.abs(left.y - right.y) < 1.0E-12
                && Math.abs(left.z - right.z) < 1.0E-12;
    }

    private static boolean finiteSegment(WaterSegment segment) {
        return finitePoint(segment.lip())
                && finitePoint(segment.center())
                && finitePoint(segment.ahead())
                && Double.isFinite(segment.upperDrop())
                && Double.isFinite(segment.lowerDrop())
                && Double.isFinite(segment.footDrop())
                && Double.isFinite(segment.across())
                && Double.isFinite(segment.thick())
                && Double.isFinite(segment.activeHeight());
    }

    private static boolean finitePoint(Vec3 point) {
        return point != null && Double.isFinite(point.x) && Double.isFinite(point.y) && Double.isFinite(point.z);
    }

    private static void check(boolean condition, String message) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(message);
        }
    }
}

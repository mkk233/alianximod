package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierBody;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierDefinition;
import com.bslx.XueXiMod.shu.technique.release.barrier.BarrierService;
import com.bslx.XueXiMod.shu.technique.release.domain.AxisBoxGeometry;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainDefinition;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainGeometry;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainInstance;
import com.bslx.XueXiMod.shu.technique.release.domain.DomainService;
import com.bslx.XueXiMod.shu.technique.release.field.FieldSpawner;
import com.bslx.XueXiMod.shu.technique.release.field.FieldZones;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardDefinition;
import com.bslx.XueXiMod.shu.technique.release.guard.GuardStates;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * 领域、护体、屏障、场地的生产消费：BASE、二倍、四倍各乘一次，刷新可恢复，锚点和同步读同一份派生值。
 */
public final class ReleaseContainerEnergyCheck {
    private static int assertionCount;
    private static int failures;

    private ReleaseContainerEnergyCheck() {
    }

    public static void main(String[] arguments) {
        domain();
        guard();
        barrier();
        field();
        System.out.println("未执行 屏障碰撞预检、真实玩家刷新、场地进世界：这些要服务器");
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void domain() {
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        Vec3 feet = new Vec3(10.0, 64.0, -3.0);
        DomainGeometry placed = AxisBoxGeometry.atFeet(feet, 7.0, 7.0, 8.0);
        DomainGeometry baseBox = DomainService.enlarge(placed, baseline);
        DomainGeometry twiceBox = DomainService.enlarge(placed, doubled);
        DomainGeometry fourBox = DomainService.enlarge(placed, quadrupled);
        DomainGeometry illegalBox = DomainService.enlarge(placed, new ReleaseInput(Float.NaN));
        DomainGeometry absentBox = DomainService.enlarge(placed, null);
        AABB placedBounds = placed.bounds();
        AABB baseBounds = baseBox.bounds();
        check(sameBox(baseBounds, placedBounds), "BASE 领域盒偏离原放置");
        check(sameBox(illegalBox.bounds(), placedBounds), "非法倍率没有退回 1");
        check(sameBox(absentBox.bounds(), placedBounds), "空输入没有退回 BASE");
        check(baseBounds.minY == feet.y, "BASE 底面不在地面");
        check(twiceBox.bounds().minY == feet.y, "二倍底面离开地面");
        check(fourBox.bounds().minY == feet.y, "四倍底面离开地面");
        check(near(midAcross(twiceBox.bounds()), feet.x), "二倍水平锚点漂移");
        check(near(midDepth(twiceBox.bounds()), feet.z), "二倍深度锚点漂移");
        check(near(midAcross(fourBox.bounds()), feet.x), "四倍水平锚点漂移");
        check(near(across(twiceBox.bounds()), across(baseBounds) * 2.0), "二倍宽度没有只乘一次");
        check(near(depthOf(twiceBox.bounds()), depthOf(baseBounds) * 2.0), "二倍深度没有只乘一次");
        check(near(upward(twiceBox.bounds()), upward(baseBounds) * 2.0), "二倍高度没有只乘一次");
        check(near(across(fourBox.bounds()), across(baseBounds) * 4.0), "四倍宽度不对");
        check(near(upward(fourBox.bounds()), upward(baseBounds) * 4.0), "四倍高度不对");
        Vec3 caster = new Vec3(0.0, 64.0, 0.0);
        Vec3 contact = new Vec3(5.0, 70.0, 3.0);
        DomainGeometry shifted = DomainService.placedGeometry(
                AxisBoxGeometry.atFeet(caster, 7.0, 7.0, 8.0), contact, caster, doubled);
        check(shifted.bounds().minY == contact.y, "接触点平移后底面没有留在新地面");
        check(near(midAcross(shifted.bounds()), contact.x), "接触点平移后水平锚点不对");
        check(near(midDepth(shifted.bounds()), contact.z), "接触点平移后深度锚点不对");
        check(near(across(shifted.bounds()), 14.0), "平移后再放大不是一次");
        check(near(upward(shifted.bounds()), 16.0), "平移后高度不是一次二倍");
        DomainDefinition mist = DomainDefinition.mistRain();
        int mistLife = mist.lifetimeTicks();
        check(mistLife == 200, "雾雨寿命基线不是 200");
        check(mist.initialCost() == 40 && mist.upkeepCost() == 8 && mist.upkeepPeriod() == 20, "雾雨费用被改了");
        check(DomainService.frozenLife(mist, baseline) == mistLife, "BASE 领域寿命被改写");
        int once = DomainService.frozenLife(mist, doubled);
        check(once == doubled.scaleDuration(mistLife), "领域寿命没有走 scaleDuration");
        check(once != doubled.scaleDuration(once), "领域寿命被乘了两次");
        check(mist.lifetimeTicks() == mistLife, "放大改写了注册领域寿命");
        int fourLife = DomainService.frozenLife(mist, quadrupled);
        check(fourLife == quadrupled.scaleDuration(mistLife), "四倍领域寿命不对");
        check(fourLife > once, "四倍寿命没有长过二倍");
        DomainInstance.FrozenClock clock = DomainInstance.FrozenClock.freeze(1_000, 9_000L, once);
        check(clock.expiresAt == ReleaseInput.expiryTick(1_000, once), "领域到期刻和缩放寿命不同源");
        check(clock.visualEnd == ReleaseInput.expiryGameTime(9_000L, once), "视觉结束没有走 expiryGameTime");
        check(DomainService.withinLife(1_000, clock.expiresAt), "刚放下就被判到期");
        check(!DomainService.withinLife(clock.expiresAt, clock.expiresAt), "到期刻仍算存活");
        int started = Integer.MAX_VALUE - 10;
        DomainInstance.FrozenClock wrapped = DomainInstance.FrozenClock.freeze(started, 50L, 40);
        check(wrapped.expiresAt == ReleaseInput.expiryTick(started, 40), "环绕到期没有走助手");
        check(DomainService.withinLife(started, wrapped.expiresAt), "环绕后大小比较会立刻到期");
        check(started >= wrapped.expiresAt, "这组刻没有环绕，看不出 hasExpired 和 >= 的差别");
        check(!DomainService.withinLife(wrapped.expiresAt, wrapped.expiresAt), "环绕到期刻没有结束");
        long saturated = DomainInstance.FrozenClock.freeze(1, Long.MAX_VALUE - 5L, 100).visualEnd;
        check(saturated == Long.MAX_VALUE, "游戏时间到期没有饱和");
        check(saturated == ReleaseInput.expiryGameTime(Long.MAX_VALUE - 5L, 100), "视觉饱和和助手不一致");
        DomainInstance.FrozenClock paused = DomainInstance.FrozenClock.freeze(80, 90L, 0);
        check(paused.expiresAt == 80 && paused.visualEnd == 90L, "非正寿命被送进了到期助手");
        DomainInstance.FrozenClock negative = DomainInstance.FrozenClock.freeze(80, 90L, -5);
        check(negative.expiresAt == 75 && negative.visualEnd == 85L, "负寿命语义变了");
        DomainGeometry pressure = DomainDefinition.feetBox(new Vec3(1.0, 10.0, 1.0), 7.0, 7.0, 3.0);
        DomainGeometry pressureFour = DomainService.enlarge(pressure, quadrupled);
        check(pressureFour.bounds().minY == 10.0, "深压底面被抬起");
        check(near(upward(pressureFour.bounds()), 12.0), "深压高度不是四倍");
        DomainDefinition pressureDefinition = DomainDefinition.deepPressure();
        check(pressureDefinition.lifetimeTicks() == 200, "深压寿命基线变了");
        check(pressureDefinition.initialCost() == 40, "深压费用变了");
    }

    private static void guard() {
        GuardDefinition registered = GuardDefinition.personalRing();
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        int animation = 40;
        int upkeep = 120;
        GuardStates.RefreshCarry first = GuardStates.retune(registered, baseline, 100, animation, upkeep, 1);
        GuardStates.RefreshCarry boosted = GuardStates.retune(registered, doubled, 130, first.animationStart, first.nextUpkeepAt, first.revision);
        GuardStates.RefreshCarry restored = GuardStates.retune(registered, baseline, 160, boosted.animationStart, boosted.nextUpkeepAt, boosted.revision);
        GuardStates.RefreshCarry quadrupledCarry = GuardStates.retune(registered, quadrupled, 180, animation, upkeep, 1);
        check(registered.radius() == 0.9, "水环半径基线变了");
        check(registered.band() == 0.12, "水环带宽基线变了");
        check(registered.bodyHeight() == 0.55, "身高比基线变了");
        check(registered.lifetimeTicks() == 200, "水环寿命基线变了");
        check(registered.spawnCost() == 8 && registered.upkeepCost() == 2 && registered.upkeepPeriod() == 20, "水环费用或维护周期变了");
        check(registered.unloadRatio() == 0.20f, "卸力比变了");
        check(near(first.derived.radius(), registered.radius()), "普通刷新半径变了");
        check(near(boosted.derived.radius(), registered.radius() * 2.0), "二倍半径不对");
        check(near(restored.derived.radius(), registered.radius()), "回到普通后半径没有恢复");
        check(near(quadrupledCarry.derived.radius(), registered.radius() * 4.0), "四倍半径不对");
        check(near(boosted.derived.band(), registered.band() * 2.0), "二倍带宽不对");
        check(near(restored.derived.band(), registered.band()), "回到普通后带宽没有恢复");
        check(near(quadrupledCarry.derived.band(), registered.band() * 4.0), "四倍带宽不对");
        check(boosted.derived.bodyHeight() == registered.bodyHeight(), "二倍身高比被乘了");
        check(restored.derived.bodyHeight() == registered.bodyHeight(), "恢复后身高比变了");
        check(boosted.derived.spawnCost() == 8 && boosted.derived.upkeepCost() == 2 && boosted.derived.upkeepPeriod() == 20, "费用或维护周期被乘了");
        check(boosted.derived.unloadRatio() == registered.unloadRatio(), "卸力被乘了");
        check(first.animationStart == animation && boosted.animationStart == animation && restored.animationStart == animation, "刷新重置了动画起点");
        check(first.nextUpkeepAt == upkeep && boosted.nextUpkeepAt == upkeep && restored.nextUpkeepAt == upkeep, "刷新打乱了维护节拍");
        check(first.revision == 2 && boosted.revision == 3 && restored.revision == 4, "刷新没有递增修订");
        int once = doubled.scaleDuration(registered.lifetimeTicks());
        check(boosted.derived.lifetimeTicks() == once, "护体寿命没有走一次 scaleDuration");
        check(once != doubled.scaleDuration(once), "护体寿命被乘了两次");
        check(quadrupledCarry.derived.lifetimeTicks() == quadrupled.scaleDuration(registered.lifetimeTicks()), "四倍护体寿命不对");
        check(boosted.expiresAt == GuardStates.expiryOf(130, once), "刷新到期和派生寿命不同源");
        check(boosted.expiresAt == ReleaseInput.expiryTick(130, once), "刷新到期没有走助手");
        check(GuardStates.withinLife(130, boosted.expiresAt), "刷新后立刻到期");
        check(!GuardStates.withinLife(boosted.expiresAt, boosted.expiresAt), "护体到期刻仍算存活");
        check(restored.derived.lifetimeTicks() == baseline.scaleDuration(registered.lifetimeTicks()), "回到普通后寿命没有恢复");
        float syncedRadius = (float) boosted.derived.radius();
        float syncedBand = (float) boosted.derived.band();
        float syncedBody = (float) boosted.derived.bodyHeight();
        check(Math.abs(syncedRadius - (float) (registered.radius() * 2.0)) < 1.0E-4f, "同步半径不是二倍派生值");
        check(Math.abs(syncedBand - (float) (registered.band() * 2.0)) < 1.0E-4f, "同步带宽不是二倍派生值");
        check(syncedBody == (float) registered.bodyHeight(), "同步身高比被乘了");
        check(near(boosted.derived.band() * 0.5, registered.band() * 0.5 * 2.0), "偏转半带宽没有跟着派生带宽");
        GuardDefinition compounded = boosted.derived.withInput(doubled);
        check(near(compounded.radius(), registered.radius() * 4.0), "对派生结果再乘应变成四倍");
        check(!near(boosted.derived.radius(), compounded.radius()), "生产刷新若从已放大定义再乘就会变成四倍");
        int started = Integer.MAX_VALUE - 10;
        int wrapped = GuardStates.expiryOf(started, 40);
        check(wrapped == ReleaseInput.expiryTick(started, 40), "护体环绕到期没有走助手");
        check(GuardStates.withinLife(started, wrapped), "护体 living/live 在环绕处会误判");
        check(!GuardStates.withinLife(wrapped, wrapped), "护体环绕到期刻没有结束");
        GuardDefinition dead = new GuardDefinition("probe", 0, 8, 2, 20, 0.2f, 1.0, 0.2, 0.55, 0f, true);
        GuardDefinition deadDerived = dead.withInput(quadrupled);
        check(deadDerived.lifetimeTicks() == 0, "非正护体寿命被延长了");
        check(GuardStates.expiryOf(100, deadDerived.lifetimeTicks()) == 100, "非正护体寿命进了助手");
        check(registered.radius() == 0.9, "派生之后注册半径变了");
    }

    private static void barrier() {
        BarrierDefinition registered = BarrierDefinition.curtain();
        double widthBefore = registered.width();
        double forwardBefore = registered.forward();
        int lifeBefore = registered.lifetimeTicks();
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        Vec3 feet = new Vec3(4.0, 70.0, -2.0);
        Vec3 look = new Vec3(0.0, 0.0, 1.0);
        var legacy = BarrierService.placeAt(feet, look, 0f, registered);
        var base = BarrierService.placeAt(feet, look, 0f, registered, ReleaseInput.BASE);
        var twice = BarrierService.placeAt(feet, look, 0f, registered, doubled);
        var four = BarrierService.placeAt(feet, look, 0f, registered, quadrupled);
        check(sameBox(legacy.box(), base.box()), "旧 placeAt 和 BASE 不一致");
        check(registered.width() == widthBefore && registered.forward() == forwardBefore, "放墙改写了注册定义");
        check(registered.lifetimeTicks() == lifeBefore && lifeBefore == 80, "墙寿命基线变了");
        check(base.box().minY == feet.y && twice.box().minY == feet.y && four.box().minY == feet.y, "墙底离开脚底");
        check(near(midDepth(base.box()) - feet.z, forwardBefore), "BASE 前向距离变了");
        check(near(midDepth(twice.box()) - feet.z, forwardBefore), "放大后前向距离变了");
        check(near(midDepth(four.box()) - feet.z, forwardBefore), "四倍前向距离变了");
        check(near(midAcross(twice.box()), feet.x), "放大后横向锚点变了");
        check(near(across(twice.box()), across(base.box()) * 2.0), "墙宽没有只乘一次");
        check(near(upward(twice.box()), upward(base.box()) * 2.0), "墙高没有只乘一次");
        check(near(depthOf(twice.box()), depthOf(base.box()) * 2.0), "墙厚没有只乘一次");
        check(near(across(four.box()), across(base.box()) * 4.0), "四倍墙宽不对");
        check(near(upward(four.box()), upward(base.box()) * 4.0), "四倍墙高不对");
        check(near(depthOf(four.box()), depthOf(base.box()) * 4.0), "四倍墙厚不对");
        BarrierDefinition once = registered.withInput(doubled);
        BarrierDefinition again = once.withInput(doubled);
        check(once.lifetimeTicks() == doubled.scaleDuration(lifeBefore), "墙寿命没有走一次 scaleDuration");
        check(once.lifetimeTicks() != doubled.scaleDuration(once.lifetimeTicks()), "墙寿命被乘了两次");
        check(near(across(twice.box()), once.width()), "墙盒和派生宽不同源");
        check(near(upward(twice.box()), once.height()), "墙盒和派生高不同源");
        check(near(depthOf(twice.box()), once.thickness()), "墙盒和派生厚不同源");
        check(!near(across(twice.box()), again.width()), "墙盒吃了两次倍率");
        check(once.forward() == forwardBefore, "派生前向变了");
        check(once.spawnCost() == 8 && once.upkeepCost() == 2 && once.upkeepPeriod() == 20, "墙费用或维护周期被乘了");
        check(once.attenuation() == 0.5f, "衰减被乘了");
        int lifeEnd = ReleaseInput.expiryTick(200, once.lifetimeTicks());
        check(BarrierBody.withinLife(lifeEnd - 1, lifeEnd), "墙到期前就拆了");
        check(!BarrierBody.withinLife(lifeEnd, lifeEnd), "墙到期刻仍活着");
        int started = Integer.MAX_VALUE - 10;
        int wrapped = ReleaseInput.expiryTick(started, 40);
        check(BarrierBody.withinLife(started, wrapped), "墙 tick 在环绕处会误判");
        check(!BarrierBody.withinLife(wrapped, wrapped), "墙环绕到期没有结束");
    }

    private static void field() {
        ReleaseDefinition synthetic = WaterReleaseDefinition.zone("synthetic_container_field", 4.5f, 200, 3, 1);
        float radiusBefore = synthetic.fieldRadius();
        int lifeBefore = synthetic.fieldLifetimeTicks();
        ReleaseInput baseline = ReleaseInput.BASE;
        ReleaseInput doubled = new ReleaseInput(2f);
        ReleaseInput quadrupled = new ReleaseInput(4f);
        FieldZones.PlannedZone base = FieldZones.plan(synthetic.fieldRadius(), synthetic.fieldLifetimeTicks(), baseline, 500);
        FieldZones.PlannedZone twice = FieldZones.plan(synthetic.fieldRadius(), synthetic.fieldLifetimeTicks(), doubled, 500);
        FieldZones.PlannedZone four = FieldZones.plan(synthetic.fieldRadius(), synthetic.fieldLifetimeTicks(), quadrupled, 500);
        check(synthetic.fieldRadius() == radiusBefore, "计划改写了合成定义半径");
        check(synthetic.fieldLifetimeTicks() == lifeBefore, "计划改写了合成定义寿命");
        check(near(base.radius, radiusBefore), "BASE 场地半径变了");
        check(base.lifetimeTicks == lifeBefore, "BASE 场地寿命变了");
        check(near(twice.radius, radiusBefore * 2.0), "二倍场地半径不对");
        check(near(four.radius, radiusBefore * 4.0), "四倍场地半径不对");
        check(twice.lifetimeTicks == doubled.scaleDuration(lifeBefore), "场地寿命没有走一次 scaleDuration");
        check(twice.lifetimeTicks != doubled.scaleDuration(twice.lifetimeTicks), "场地寿命被乘了两次");
        check(four.lifetimeTicks == quadrupled.scaleDuration(lifeBefore), "四倍场地寿命不对");
        check(near(twice.burstReach, FieldZones.burstReach(twice.radius)), "终爆没有从放大后的主半径派生");
        check(twice.expireTick == ReleaseInput.expiryTick(500, twice.lifetimeTicks), "场地到期和寿命不同源");
        check(FieldZones.withinLife(500, twice.expireTick), "场地刚放就到期");
        check(!FieldZones.withinLife(twice.expireTick, twice.expireTick), "场地到期刻仍活着");
        FieldZones.PlannedZone small = FieldZones.plan(1.0, 80, quadrupled, 10);
        double clampedThenScaled = FieldZones.burstReach(1.0) * 4.0;
        check(near(small.radius, 4.0), "小场地四倍半径不对");
        check(near(small.burstReach, FieldZones.burstReach(small.radius)), "小场地终爆不是主半径的一次派生");
        check(!near(small.burstReach, clampedThenScaled), "终爆先钳制再乘倍率");
        check(near(small.radius + 1.0, 5.0), "查询外扩不是冻结半径加一");
        check(FieldZones.scaledLifetime(0, quadrupled) == 200, "非正场地寿命没有保持 200 回退");
        check(FieldZones.scaledLifetime(0, quadrupled) != quadrupled.scaleDuration(200), "回退的 200 又被乘了一次");
        check(FieldZones.scaledLifetime(-4, quadrupled) == 200, "负场地寿命语义变了");
        FieldZones.PlannedZone absentLife = FieldZones.plan(radiusBefore, 0, quadrupled, 10);
        check(absentLife.lifetimeTicks == 200, "计划把非正寿命乘进了四倍");
        check(near(absentLife.radius, radiusBefore * 4.0), "非正寿命把半径也停掉了");
        Vec3 anchor = FieldSpawner.anchorAt(new Vec3(3.0, 64.0, 5.0), 1.8);
        check(anchor.x == 3.0 && anchor.z == 5.0, "场地锚点水平位置变了");
        check(near(anchor.y, 64.9), "场地锚点不在身体中点");
        FieldSpawner.releaseAttached(null, synthetic, 1, false, null, false, quadrupled);
        FieldSpawner.releaseFromChain(null, List.of(), 0, synthetic, 1, quadrupled);
        check(synthetic.fieldRadius() == radiusBefore, "空玩家释放改写了定义");
        int started = Integer.MAX_VALUE - 10;
        FieldZones.PlannedZone wrapped = FieldZones.plan(radiusBefore, 40, baseline, started);
        check(wrapped.lifetimeTicks == baseline.scaleDuration(40), "BASE 短寿命被改写");
        check(wrapped.expireTick == ReleaseInput.expiryTick(started, wrapped.lifetimeTicks), "场地环绕到期没有走助手");
        check(FieldZones.withinLife(started, wrapped.expireTick), "场地 tick 在环绕处会误判");
        check(!FieldZones.withinLife(wrapped.expireTick, wrapped.expireTick), "场地环绕到期没有结束");
    }

    private static double across(AABB box) {
        return box.maxX - box.minX;
    }

    private static double upward(AABB box) {
        return box.maxY - box.minY;
    }

    private static double depthOf(AABB box) {
        return box.maxZ - box.minZ;
    }

    private static double midAcross(AABB box) {
        return (box.minX + box.maxX) * 0.5;
    }

    private static double midDepth(AABB box) {
        return (box.minZ + box.maxZ) * 0.5;
    }

    private static boolean sameBox(AABB left, AABB right) {
        return left.minX == right.minX && left.minY == right.minY && left.minZ == right.minZ
                && left.maxX == right.maxX && left.maxY == right.maxY && left.maxZ == right.maxZ;
    }

    private static boolean near(double actual, double expected) {
        return Math.abs(actual - expected) <= 1.0E-6;
    }

    private static void check(boolean passed, String detail) {
        assertionCount += 1;
        if (!passed) {
            failures += 1;
            System.out.println("FAIL " + detail);
        }
    }
}

package com.bslx.XueXiMod.shu.technique.release.deflection;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * 不启动 Forge。报价、抑制栈、占用和内存账在这里跑。
 * 真实玩家水势和维度卸载要有服务端，这里不造。
 */
public final class DeflectionCoreCheck {
    private DeflectionCoreCheck() {
    }

    public static void main(String[] arguments) {
        quotes();
        scope();
        System.out.println("报价和抑制栈通过");
        hostsAndBook();
        System.out.println("水势偏转共享底座检查通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void quotes() {
        DeflectionEconomy.Quote below = DeflectionEconomy.quote(19f, 5f, 0, false);
        check(below.outcome() == DeflectionEconomy.Quote.Outcome.NONE && below.cost() == 0f && below.remaining() == 19f,
                "19 点不能启动");
        DeflectionEconomy.Quote meleeBelow = DeflectionEconomy.quote(19f, 100f, 0, true);
        check(meleeBelow.outcome() == DeflectionEconomy.Quote.Outcome.NONE, "近战 19 点不能启动");
        DeflectionEconomy.Quote meleeExact = DeflectionEconomy.quote(20f, Float.NaN, 3, true);
        check(meleeExact.outcome() == DeflectionEconomy.Quote.Outcome.FULL && meleeExact.cost() == 20f
                && meleeExact.remaining() == 0f && meleeExact.spendsFullAttempt(), "近战刚好 20，忽略伤害和完整次数");
        DeflectionEconomy.Quote meleeRoom = DeflectionEconomy.quote(40f, 10f, -1, true);
        check(meleeRoom.outcome() == DeflectionEconomy.Quote.Outcome.FULL && meleeRoom.cost() == 20f
                && meleeRoom.remaining() == 20f, "近战不看负的完整次数");

        DeflectionEconomy.Quote smallDamage = DeflectionEconomy.quote(20f, 7f, 0, false);
        check(smallDamage.outcome() == DeflectionEconomy.Quote.Outcome.FULL && smallDamage.cost() == 7f
                && smallDamage.remaining() == 13f, "伤害小于 20 时只扣伤害");
        DeflectionEconomy.Quote largerPool = DeflectionEconomy.quote(40f, 7f, 0, false);
        check(largerPool.cost() == 7f && largerPool.remaining() == 33f, "足额水势仍然只扣伤害");
        DeflectionEconomy.Quote exact = DeflectionEconomy.quote(30f, 30f, 0, false);
        check(exact.outcome() == DeflectionEconomy.Quote.Outcome.FULL && exact.cost() == 30f && exact.remaining() == 0f,
                "水势等于伤害时完整偏转");
        DeflectionEconomy.Quote underpaid = DeflectionEconomy.quote(25f, 30f, 0, false);
        check(underpaid.outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL && underpaid.cost() == 25f
                && underpaid.remaining() == 0f && !underpaid.spendsFullAttempt(), "水势不足时耗尽且不计完整次数");
        DeflectionEconomy.Quote stillPartial = DeflectionEconomy.quote(25f, 40f, 2, false);
        check(stillPartial.outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL && !stillPartial.spendsFullAttempt(),
                "已有两次完整偏转时部分卸力仍不计次");
        DeflectionEconomy.Quote roomLeft = DeflectionEconomy.quote(100f, 10f, 2, false);
        check(roomLeft.outcome() == DeflectionEconomy.Quote.Outcome.FULL, "第三次之前仍可完整偏转");
        DeflectionEconomy.Quote capped = DeflectionEconomy.quote(100f, 10f, 3, false);
        check(capped.outcome() == DeflectionEconomy.Quote.Outcome.NONE && capped.cost() == 0f && capped.remaining() == 100f,
                "满三次不再卸力");
        DeflectionEconomy.Quote cappedPartial = DeflectionEconomy.quote(25f, 40f, 3, false);
        check(cappedPartial.outcome() == DeflectionEconomy.Quote.Outcome.NONE, "满三次连部分卸力也不做");
        DeflectionEconomy.Quote negativeCount = DeflectionEconomy.quote(40f, 10f, -1, false);
        check(negativeCount.outcome() == DeflectionEconomy.Quote.Outcome.NONE, "负的完整次数拒绝");

        check(DeflectionEconomy.quote(50f, 0f, 0, false).outcome() == DeflectionEconomy.Quote.Outcome.NONE, "0 伤害拒绝");
        check(DeflectionEconomy.quote(50f, -3f, 0, false).outcome() == DeflectionEconomy.Quote.Outcome.NONE, "负伤害拒绝");
        DeflectionEconomy.Quote missing = DeflectionEconomy.quote(50f, Float.NaN, 0, false);
        check(missing.outcome() == DeflectionEconomy.Quote.Outcome.NONE && missing.remaining() == 50f, "伤害非数拒绝");
        DeflectionEconomy.Quote brokenFunds = DeflectionEconomy.quote(Float.NaN, 10f, 0, false);
        check(brokenFunds.outcome() == DeflectionEconomy.Quote.Outcome.NONE && brokenFunds.cost() == 0f
                && brokenFunds.remaining() == 0f, "水势非数拒绝");
        check(DeflectionEconomy.quote(Float.POSITIVE_INFINITY, 10f, 0, false).outcome() == DeflectionEconomy.Quote.Outcome.NONE,
                "无限水势拒绝");
        check(DeflectionEconomy.quote(-5f, 10f, 0, true).outcome() == DeflectionEconomy.Quote.Outcome.NONE
                && DeflectionEconomy.quote(-5f, 10f, 0, true).remaining() == 0f, "负水势拒绝");
    }

    private static void scope() {
        Object victim = new Object();
        Object source = new Object();
        Object otherVictim = new Object();
        Object otherSource = new Object();
        MitigationCreditScope.Token outer = MitigationCreditScope.open(victim, source, true);
        try {
            check(MitigationCreditScope.suppressesMitigated(victim, source), "partial 压住这对引用");
            check(!MitigationCreditScope.suppressesMitigated(otherVictim, source), "换受害者不抑制");
            check(!MitigationCreditScope.suppressesMitigated(victim, otherSource), "换伤害来源不抑制");
            boolean copied = MitigationCreditScope.suppressesMitigated(victim, source);
            MitigationCreditScope.Token inner = MitigationCreditScope.open(victim, otherSource, false);
            try {
                check(MitigationCreditScope.suppressesMitigated(victim, source), "内层不关掉外层");
                check(!MitigationCreditScope.suppressesMitigated(victim, otherSource), "非 partial 不抑制");
            } finally {
                inner.close();
            }
            check(copied, "抄走的布尔在关闭前已经是真");
        } finally {
            outer.close();
        }
        check(!MitigationCreditScope.suppressesMitigated(victim, source), "关闭后线程栈不再抑制");
        MitigationCreditScope.Token absent = MitigationCreditScope.open(null, source, true);
        try {
            check(!MitigationCreditScope.suppressesMitigated(victim, source), "空受害者不开栈");
        } finally {
            absent.close();
        }
        check(MitigationCreditScope.allowsMitigatedCredit(5f, false, false), "普通减伤仍入账");
        check(!MitigationCreditScope.allowsMitigatedCredit(5f, true, false), "溺水仍抑制减伤");
        check(!MitigationCreditScope.allowsMitigatedCredit(5f, false, true), "partial 抑制减伤");
        check(!MitigationCreditScope.allowsMitigatedCredit(0f, false, false), "没有减伤不入账");
        check(!MitigationCreditScope.allowsMitigatedCredit(Float.NaN, false, false), "非数减伤不入账");
    }

    private static void hostsAndBook() {
        chooseNearest();
        reserveFailure();
        closeRefunds();
        commitOnce();
        hostInactive();
        registryDropped();
        sameArrowCompetition();
        differentArrows();
        partialDoesNotRequote();
        releaseGate();
        registryStopClears();
        closedKeyStillBlocks();
        activeContactCannotRetire();
        retireIsIdempotent();
        retireAttackLeavesOpenProjectile();
        projectileEndClearsOnlyThatArrow();
        retireAttackLeavesGate();
    }

    private static void chooseNearest() {
        reset();
        StubHost nearHost = stub(2L);
        StubHost farHost = stub(9L);
        StubHost idleHost = stub(1L);
        idleHost.live = false;
        Optional<HostContact> chosen = DeflectionHosts.choose(List.of(
                touch(farHost, 0.40, 3.0),
                touch(nearHost, 0.10, 1.0),
                touch(idleHost, 0.01, 0.0),
                touch(farHost, 1.50, 0.0),
                touch(farHost, Double.NaN, 0.0)));
        check(chosen.isPresent() && chosen.get().host() == nearHost, "更小的线段参数胜出，停用和越界丢掉");
        StubHost left = stub(2L);
        StubHost right = stub(9L);
        Optional<HostContact> tied = DeflectionHosts.choose(List.of(
                touch(right, 0.25, 2.0),
                touch(left, 0.25 + 1.0E-12, 1.0)));
        check(tied.isPresent() && tied.get().host() == left, "平局走较小的宿主身份");
        check(DeflectionHosts.choose(List.of()).isEmpty(), "没有候选就是空");
        check(DeflectionHosts.choose(null).isEmpty(), "空名单就是空");
    }

    private static void reserveFailure() {
        reset();
        StubHost host = stub(3L);
        MemoryPurse purse = new MemoryPurse(80f);
        purse.forceReject = true;
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0),
                AttackKey.projectile(new UUID(3L, 1L), 1), 10f, 0, false, purse, false, 4L);
        check(!opened.acquired() && !opened.passedOver() && opened.quote().engages(), "预留失败仍带着原报价");
        check(purse.balance == 80f && purse.reserved == 0f && purse.commitCalls == 0, "预留失败不扣款");
        opened.commit();
        opened.close();
        check(purse.balance == 80f && purse.releaseCalls == 0 && !opened.committed(), "失败单提交和关闭都不动账");
    }

    private static void closeRefunds() {
        reset();
        StubHost host = stub(4L);
        MemoryPurse purse = new MemoryPurse(40f);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0),
                AttackKey.projectile(new UUID(4L, 1L), 1), 10f, 0, false, purse, false, 4L);
        check(opened.acquired() && purse.balance == 40f && purse.reserved == 10f, "预留先占住，余额还在");
        opened.close();
        opened.close();
        check(purse.balance == 40f && purse.reserved == 0f && purse.releaseCalls == 1, "关闭退回，重复关闭不再退");
        opened.commit();
        check(!opened.committed() && purse.commitCalls == 0 && purse.balance == 40f, "关闭后不能再提交");
    }

    private static void commitOnce() {
        reset();
        StubHost host = stub(5L);
        MemoryPurse purse = new MemoryPurse(40f);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0),
                AttackKey.projectile(new UUID(5L, 1L), 1), 10f, 0, false, purse, false, 4L);
        opened.commit();
        opened.commit();
        check(opened.committed() && purse.commitCalls == 1 && purse.balance == 30f && purse.reserved == 0f,
                "提交只扣一次");
        opened.close();
        check(purse.balance == 30f && purse.releaseCalls == 0, "已提交的关闭不再退款");
    }

    private static void hostInactive() {
        reset();
        StubHost host = stub(6L);
        host.live = false;
        MemoryPurse purse = new MemoryPurse(40f);
        DeflectionService.Transaction blocked = buy(host, touch(host, 0.2, 0.0),
                AttackKey.projectile(new UUID(6L, 1L), 1), 10f, 0, false, purse, false, 4L);
        check(!blocked.acquired() && purse.reserveCalls == 0, "宿主失效时不预留");
        host.live = true;
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 1.0),
                AttackKey.projectile(new UUID(6L, 1L), 2), 10f, 0, false, purse, false, 4L);
        host.live = false;
        opened.commit();
        check(!opened.committed() && purse.balance == 40f && purse.reserved == 10f, "提交前宿主失效就不扣");
        opened.close();
        check(purse.reserved == 0f && purse.balance == 40f, "失效提交之后关闭会退回");
    }

    private static void registryDropped() {
        reset();
        StubHost host = stub(7L);
        DeflectionHosts.registerUnchecked(host);
        MemoryPurse purse = new MemoryPurse(40f);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0),
                AttackKey.projectile(new UUID(7L, 1L), 1), 10f, 0, false, purse, true, 4L);
        check(opened.acquired(), "已注册宿主可以收购");
        DeflectionHosts.unregister(host.key());
        opened.commit();
        check(!opened.committed() && purse.balance == 40f, "注销后提交不扣款");
        opened.close();
        check(purse.reserved == 0f && purse.balance == 40f, "注销后的预留在关闭时退回");
    }

    private static void sameArrowCompetition() {
        reset();
        StubHost first = stub(8L);
        StubHost second = stub(18L);
        UUID arrowOne = new UUID(8L, 4L);
        MemoryPurse firstPurse = new MemoryPurse(40f);
        MemoryPurse secondPurse = new MemoryPurse(40f);
        DeflectionService.Transaction held = buy(first, touch(first, 0.2, 0.0),
                AttackKey.projectile(arrowOne, 1), 10f, 0, false, firstPurse, false, 4L);
        DeflectionService.Transaction otherSerial = buy(first, touch(first, 0.3, 1.0),
                AttackKey.projectile(arrowOne, 2), 10f, 0, false, firstPurse, false, 4L);
        DeflectionService.Transaction rival = buy(second, touch(second, 0.1, 2.0),
                AttackKey.projectile(arrowOne, 1), 10f, 0, false, secondPurse, false, 4L);
        check(held.acquired() && !otherSerial.acquired() && otherSerial.passedOver(), "占用期间同一支箭的新序号也不接");
        check(!rival.acquired() && rival.passedOver() && secondPurse.reserveCalls == 0, "第二宿主不预留");
        check(firstPurse.reserved == 10f && secondPurse.balance == 40f, "只有先手占住预留");
        held.close();
        otherSerial.close();
        rival.close();
        DeflectionService.Transaction after = buy(second, touch(second, 0.1, 2.0),
                AttackKey.projectile(arrowOne, 3), 10f, 0, false, secondPurse, false, 4L);
        check(after.acquired() && secondPurse.reserved == 10f, "先手关闭后另一宿主可以接新序号");
        after.close();
    }

    private static void differentArrows() {
        reset();
        StubHost first = stub(11L);
        StubHost second = stub(12L);
        MemoryPurse firstPurse = new MemoryPurse(50f);
        MemoryPurse secondPurse = new MemoryPurse(50f);
        DeflectionService.Transaction left = buy(first, touch(first, 0.2, 0.0),
                AttackKey.projectile(new UUID(11L, 1L), 1), 8f, 0, false, firstPurse, false, 2L);
        DeflectionService.Transaction right = buy(second, touch(second, 0.2, 4.0),
                AttackKey.projectile(new UUID(12L, 1L), 1), 9f, 0, false, secondPurse, false, 2L);
        check(left.acquired() && right.acquired(), "同一 tick 的两支箭各自收购");
        check(firstPurse.reserved == 8f && secondPurse.reserved == 9f, "两支箭各自预留");
        left.close();
        right.close();
    }

    private static void partialDoesNotRequote() {
        reset();
        StubHost host = stub(13L);
        MemoryPurse purse = new MemoryPurse(25f);
        UUID arrowOne = new UUID(13L, 2L);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.4, 0.0),
                AttackKey.projectile(arrowOne, 1), 40f, 0, false, purse, false, 6L);
        check(opened.acquired() && opened.quote().outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL
                && !opened.quote().spendsFullAttempt(), "部分卸力报价成功");
        opened.commit();
        check(opened.committed() && purse.balance == 0f && purse.commitCalls == 1, "部分卸力扣掉全部可用水势");
        opened.close();
        purse.restore(40f);
        int reserves = purse.reserveCalls;
        DeflectionService.Transaction repeat = buy(host, touch(host, 0.4, 0.1),
                AttackKey.projectile(arrowOne, 1), 10f, 0, false, purse, false, 7L);
        check(!repeat.acquired() && repeat.passedOver() && purse.reserveCalls == reserves && purse.balance == 40f,
                "同一接触序号不重复报价");
        DeflectionService.Transaction fresh = buy(host, touch(host, 0.4, 5.0),
                AttackKey.projectile(arrowOne, 2), 10f, 0, false, purse, false, 8L);
        check(fresh.acquired() && fresh.quote().spendsFullAttempt() && fresh.quote().cost() == 10f,
                "新序号重新报价，服务本身不把部分卸力记成完整次数");
        fresh.close();
        repeat.close();
    }

    private static void releaseGate() {
        reset();
        StubHost host = stub(14L);
        MemoryPurse purse = new MemoryPurse(100f);
        UUID arrowOne = new UUID(14L, 5L);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0),
                AttackKey.projectile(arrowOne, 1), 10f, 0, false, purse, false, 5L);
        opened.commit();
        opened.close();
        check(purse.balance == 90f, "完整偏转提交后余额减少");
        DeflectionService.Transaction near = buy(host, touch(host, 0.2, 0.2),
                AttackKey.projectile(arrowOne, 2), 10f, 0, false, purse, false, 5L);
        check(!near.acquired() && near.passedOver() && purse.balance == 90f && purse.reserved == 0f,
                "同一 tick 靠近放出点不再接回");
        DeflectionService.Transaction away = buy(host, touch(host, 0.2, 3.0),
                AttackKey.projectile(arrowOne, 3), 10f, 0, false, purse, false, 5L);
        check(away.acquired(), "同一 tick 离开闸门后可以再接");
        away.close();
        check(purse.balance == 90f && purse.reserved == 0f, "未提交的第二次收购会退回");
        DeflectionService.Transaction later = buy(host, touch(host, 0.2, 0.2),
                AttackKey.projectile(arrowOne, 4), 10f, 0, false, purse, false, 6L);
        check(later.acquired() && later.quote().cost() == 10f, "下一 tick 闸门失效，次数仍由调用方传入");
        later.close();
    }

    private static void registryStopClears() {
        reset();
        StubHost host = stub(15L);
        DeflectionHosts.registerUnchecked(host);
        check(DeflectionHosts.find(host.key()) == host, "未检查线程的登记能查到");
        DeflectionHosts.onLevelUnload(null);
        check(DeflectionHosts.find(host.key()) == host, "空卸载不清注册表");
        DeflectionHosts.onServerStopping();
        check(DeflectionHosts.find(host.key()) == null, "停服丢掉全部宿主");
    }

    private static void closedKeyStillBlocks() {
        reset();
        StubHost host = stub(21L);
        MemoryPurse purse = new MemoryPurse(40f);
        AttackKey contact = AttackKey.projectile(new UUID(21L, 1L), 1);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0), contact, 10f, 0, false, purse, false, 4L);
        opened.commit();
        opened.close();
        check(DeflectionService.remembers(contact), "关闭后同一接触仍防重");
        int reserves = purse.reserveCalls;
        DeflectionService.Transaction repeat = buy(host, touch(host, 0.2, 0.4), contact, 10f, 0, false, purse, false, 4L);
        check(!repeat.acquired() && repeat.passedOver() && purse.reserveCalls == reserves && purse.balance == 30f,
                "未清理时同一接触不重复扣");
        repeat.close();
    }

    private static void activeContactCannotRetire() {
        reset();
        StubHost host = stub(22L);
        StubHost rival = stub(23L);
        MemoryPurse purse = new MemoryPurse(40f);
        MemoryPurse rivalPurse = new MemoryPurse(40f);
        AttackKey contact = AttackKey.projectile(new UUID(22L, 2L), 1);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0), contact, 10f, 0, false, purse, false, 4L);
        check(!DeflectionService.retireAttack(contact), "占用中不能清理");
        check(purse.reserved == 10f && purse.balance == 40f && !DeflectionService.remembers(contact), "拒绝清理时不改预留");
        opened.commit();
        check(!DeflectionService.retireAttack(contact) && DeflectionService.remembers(contact), "未关闭时不能抹掉防重");
        check(purse.balance == 30f && purse.commitCalls == 1, "拒绝清理不退已提交的水势");
        DeflectionService.Transaction blocked = buy(rival, touch(rival, 0.1, 1.0), contact, 10f, 0, false, rivalPurse, false, 4L);
        check(!blocked.acquired() && blocked.passedOver() && rivalPurse.reserveCalls == 0, "清理失败后占用还在");
        opened.close();
        blocked.close();
        check(DeflectionService.remembers(contact) && purse.balance == 30f, "关闭只放占用，不放防重");
    }

    private static void retireIsIdempotent() {
        reset();
        StubHost host = stub(24L);
        MemoryPurse purse = new MemoryPurse(40f);
        AttackKey swing = AttackKey.melee(new UUID(24L, 8L), new UUID(24L, 9L), 11L);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0), swing, Float.NaN, 3, true, purse, false, 4L);
        opened.commit();
        opened.close();
        check(purse.balance == 20f && DeflectionService.remembers(swing), "近战提交后记着这次接触");
        check(DeflectionService.retireAttack(swing), "接触结束后可以清理");
        check(!DeflectionService.remembers(swing), "清理后这次接触不再留下");
        check(DeflectionService.retireAttack(swing) && DeflectionService.retireAttack(null), "重复清理和空键都成功");
        check(purse.balance == 20f && purse.reserved == 0f && purse.commitCalls == 1, "清理不碰余额和预留");
    }

    private static void retireAttackLeavesOpenProjectile() {
        reset();
        StubHost host = stub(27L);
        MemoryPurse purse = new MemoryPurse(80f);
        UUID arrowOne = new UUID(27L, 3L);
        AttackKey finished = AttackKey.projectile(arrowOne, 1);
        AttackKey current = AttackKey.projectile(arrowOne, 2);
        AttackKey waiting = AttackKey.projectile(arrowOne, 3);
        DeflectionService.Transaction done = buy(host, touch(host, 0.2, 0.0), finished, 10f, 0, false, purse, false, 5L);
        done.commit();
        done.close();
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 4.0), current, 10f, 0, false, purse, false, 5L);
        check(DeflectionService.retireAttack(finished) && !DeflectionService.remembers(finished), "已关闭的旧序号可以单独清掉");
        DeflectionService.Transaction blocked = buy(host, touch(host, 0.2, 6.0), waiting, 10f, 0, false, purse, false, 5L);
        check(opened.acquired() && !blocked.acquired() && blocked.passedOver() && purse.reserved == 10f,
                "旧序号清理后，这支箭的当前占用还在");
        opened.close();
        blocked.close();
    }

    private static void projectileEndClearsOnlyThatArrow() {
        reset();
        StubHost host = stub(25L);
        MemoryPurse purse = new MemoryPurse(100f);
        UUID arrowOne = new UUID(25L, 1L);
        UUID arrowTwo = new UUID(25L, 2L);
        AttackKey firstSerial = AttackKey.projectile(arrowOne, 1);
        AttackKey secondSerial = AttackKey.projectile(arrowOne, 2);
        AttackKey otherArrow = AttackKey.projectile(arrowTwo, 1);
        AttackKey swing = AttackKey.melee(new UUID(25L, 3L), new UUID(25L, 4L), 6L);
        DeflectionService.Transaction first = buy(host, touch(host, 0.2, 0.0), firstSerial, 10f, 0, false, purse, false, 5L);
        first.commit();
        first.close();
        DeflectionService.Transaction second = buy(host, touch(host, 0.3, 4.0), secondSerial, 10f, 0, false, purse, false, 5L);
        check(!DeflectionService.retireProjectile(arrowOne), "还有未关闭接触时不能清整支箭");
        check(DeflectionService.remembers(firstSerial) && DeflectionService.departureGateCount(arrowOne) == 1,
                "拒绝清理时防重和闸门都还在");
        second.commit();
        second.close();
        DeflectionService.Transaction other = buy(host, touch(host, 0.2, 8.0), otherArrow, 10f, 0, false, purse, false, 5L);
        other.commit();
        other.close();
        DeflectionService.Transaction melee = buy(host, touch(host, 0.2, 0.0), swing, 4f, 0, true, purse, false, 5L);
        melee.commit();
        melee.close();
        check(purse.balance == 50f, "四次提交后余额是 50");
        check(DeflectionService.retireProjectile(arrowOne), "箭的接触都关闭后可以整支清理");
        check(!DeflectionService.remembers(firstSerial) && !DeflectionService.remembers(secondSerial), "这支箭的防重都去掉");
        check(DeflectionService.departureGateCount(arrowOne) == 0, "这支箭的离体闸门去掉");
        check(DeflectionService.remembers(otherArrow) && DeflectionService.departureGateCount(arrowTwo) == 1, "另一支箭还在");
        check(DeflectionService.remembers(swing), "近战防重还在");
        int reserves = purse.reserveCalls;
        DeflectionService.Transaction otherRepeat = buy(host, touch(host, 0.2, 8.2), otherArrow, 10f, 0, false, purse, false, 5L);
        DeflectionService.Transaction meleeRepeat = buy(host, touch(host, 0.2, 0.2), swing, 4f, 0, true, purse, false, 5L);
        check(!otherRepeat.acquired() && otherRepeat.passedOver() && !meleeRepeat.acquired() && meleeRepeat.passedOver(),
                "其它箭和近战仍防重");
        check(purse.reserveCalls == reserves && purse.balance == 50f, "清理这支箭不扣别人的账");
        check(DeflectionService.retireProjectile(arrowOne) && DeflectionService.retireProjectile(null), "整支箭清理可以重复");
        otherRepeat.close();
        meleeRepeat.close();
    }

    private static void retireAttackLeavesGate() {
        reset();
        StubHost host = stub(26L);
        MemoryPurse purse = new MemoryPurse(100f);
        UUID arrowOne = new UUID(26L, 5L);
        AttackKey firstSerial = AttackKey.projectile(arrowOne, 1);
        DeflectionService.Transaction opened = buy(host, touch(host, 0.2, 0.0), firstSerial, 10f, 0, false, purse, false, 5L);
        opened.commit();
        opened.close();
        check(DeflectionService.departureGateCount(arrowOne) == 1, "完整偏转留下离体闸门");
        check(DeflectionService.retireAttack(firstSerial) && !DeflectionService.remembers(firstSerial), "结束后只清这次接触");
        check(DeflectionService.departureGateCount(arrowOne) == 1 && purse.balance == 90f, "清理接触不删闸门也不退款");
        AttackKey laterSerial = AttackKey.projectile(arrowOne, 2);
        DeflectionService.Transaction near = buy(host, touch(host, 0.2, 0.2), laterSerial, 10f, 0, false, purse, false, 5L);
        check(!near.acquired() && near.passedOver() && purse.balance == 90f && purse.reserved == 0f, "闸门在清理接触后仍然挡住同 tick");
        near.close();
    }

    private static DeflectionService.Transaction buy(StubHost host, HostContact contact, AttackKey attackKey,
                                                     float direct, int fullCount, boolean isMelee, MemoryPurse purse,
                                                     boolean registryRequired, long gameTick) {
        return DeflectionService.open(host, contact, attackKey, direct, fullCount, isMelee, purse,
                registryRequired, gameTick);
    }

    private static HostContact touch(StubHost host, double segmentT, double east) {
        return new HostContact(host, segmentT, new Vec3(east, 0.0, 0.0), new Vec3(0.0, 1.0, 0.0));
    }

    private static StubHost stub(long generation) {
        return new StubHost(new HostKey(new UUID(generation, 7L), generation), new UUID(generation, 9L));
    }

    private static void reset() {
        DeflectionHosts.onServerStopping();
    }

    private static int assertionCount;

    private static void check(boolean condition, String detail) {
        assertionCount++;
        if (!condition) {
            throw new IllegalStateException(detail);
        }
    }

    private static final class StubHost implements DeflectionHost {
        private final HostKey hostKey;
        private final UUID owner;
        private boolean live = true;

        private StubHost(HostKey hostKey, UUID owner) {
            this.hostKey = hostKey;
            this.owner = owner;
        }

        @Override
        public UUID ownerId() {
            return this.owner;
        }

        @Override
        public HostKey key() {
            return this.hostKey;
        }

        @Override
        public ServerLevel level() {
            return null;
        }

        @Override
        public boolean active() {
            return this.live;
        }

        @Override
        public Optional<HostContact> intersect(Vec3 from, Vec3 toward, double projectileRadius, long gameTick) {
            return Optional.empty();
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

    private static final class MemoryPurse implements MomentumPurse {
        private float balance;
        private float reserved;
        private int reserveCalls;
        private int commitCalls;
        private int releaseCalls;
        private boolean forceReject;

        private MemoryPurse(float balance) {
            this.balance = balance;
        }

        private void restore(float next) {
            this.balance = next;
            this.reserved = 0f;
        }

        @Override
        public float available() {
            float funds = this.balance - this.reserved;
            if (!Float.isFinite(funds) || funds <= 0f) {
                return 0f;
            }
            return funds;
        }

        @Override
        public Object reserve(float amount) {
            this.reserveCalls++;
            float funds = this.balance - this.reserved;
            if (this.forceReject || !(amount > 0f) || !Float.isFinite(amount) || amount > funds + 0.001f) {
                return null;
            }
            this.reserved += amount;
            return new MemoryHold(amount);
        }

        @Override
        public boolean commit(Object reservation) {
            if (!(reservation instanceof MemoryHold hold) || hold.state != MemoryHold.State.RESERVED) {
                return false;
            }
            hold.state = MemoryHold.State.COMMITTED;
            this.reserved -= hold.amount;
            this.balance -= hold.amount;
            this.commitCalls++;
            return true;
        }

        @Override
        public void release(Object reservation) {
            if (!(reservation instanceof MemoryHold hold) || hold.state != MemoryHold.State.RESERVED) {
                return;
            }
            hold.state = MemoryHold.State.RELEASED;
            this.reserved -= hold.amount;
            this.releaseCalls++;
        }
    }

    private static final class MemoryHold {
        private enum State {
            RESERVED,
            COMMITTED,
            RELEASED
        }

        private final float amount;
        private State state = State.RESERVED;

        private MemoryHold(float amount) {
            this.amount = amount;
        }
    }
}

import com.bslx.XueXiMod.combat.counter.CounterAdmission;
import com.bslx.XueXiMod.combat.counter.CounterArmResult;
import com.bslx.XueXiMod.combat.counter.CounterHurtFrame;
import com.bslx.XueXiMod.combat.counter.CounterLedger;
import com.bslx.XueXiMod.combat.counter.CounterLedger.HurtMitigation;
import com.bslx.XueXiMod.combat.counter.CounterLedger.HurtOffer;
import com.bslx.XueXiMod.combat.counter.CounterLedger.OwnerReadiness;
import com.bslx.XueXiMod.combat.counter.CounterLedger.PlannedStrike;
import com.bslx.XueXiMod.combat.counter.CounterLedger.StrikeFacts;
import com.bslx.XueXiMod.combat.counter.CounterWindowSpec;
import com.bslx.XueXiMod.combat.counter.ManaRestoreMath;

import java.util.List;

/**
 * 通用回蓝和一次迎击的纯账本检查。不读木系资源，不启动 Forge，也不调用原版 hurt。
 */
public final class CounterEffectChecks {
    private static int checks;
    private static int failures;

    private CounterEffectChecks() {
    }

    public static void main(String[] arguments) {
        checkMana();
        checkSpec();
        checkWindow();
        checkOutcomes();
        checkStrikeGate();
        System.out.println("断言 " + checks + " 失败 " + failures);
        System.out.println("范围 纯账本与补蓝计算");
        System.out.println("未覆盖 木系资源、Forge 事件顺序、Mixin 字节码、原版 hurt、吸收结算、盾牌、PVP、游戏");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void checkMana() {
        int nearCeiling = Integer.MAX_VALUE - 5;
        expect(ManaRestoreMath.accepted(nearCeiling, Integer.MAX_VALUE, 100) == 5, "接近上限只补剩余");
        expect(ManaRestoreMath.accepted(Integer.MAX_VALUE - 20, Integer.MAX_VALUE, Integer.MAX_VALUE) == 20,
                "请求值本身很大时也不按 int 相加");
        expect(ManaRestoreMath.accepted(1, Integer.MAX_VALUE, Integer.MAX_VALUE) == Integer.MAX_VALUE - 1,
                "房间比请求小");
        expect(ManaRestoreMath.accepted(Integer.MAX_VALUE, Integer.MAX_VALUE, 1) == 0, "已满不加");
        expect(ManaRestoreMath.accepted(12, 10, 4) == 0, "当前高于上限不加");
        expect(ManaRestoreMath.accepted(4, 10, 0) == 0, "零请求不加");
        expect(ManaRestoreMath.accepted(4, 10, -3) == 0, "负请求不加");
        expect(ManaRestoreMath.accepted(4, 10, 3) == 3, "普通补蓝");
    }

    private static void checkSpec() {
        expect(CounterWindowSpec.tryOf(0, 0.2f, 1.0f, CounterAdmission.MELEE) == null, "窗口时长要为正");
        expect(CounterWindowSpec.tryOf(8, 1.0f, 1.0f, CounterAdmission.MELEE) == null, "减伤比例不能到 1");
        expect(CounterWindowSpec.tryOf(8, -0.1f, 1.0f, CounterAdmission.MELEE) == null, "减伤比例不能为负");
        expect(CounterWindowSpec.tryOf(8, Float.NaN, 1.0f, CounterAdmission.MELEE) == null, "减伤比例要有限");
        expect(CounterWindowSpec.tryOf(8, 0.2f, -1.0f, CounterAdmission.MELEE) == null, "反击伤害不能为负");
        expect(CounterWindowSpec.tryOf(8, 0.2f, Float.POSITIVE_INFINITY, CounterAdmission.DIRECT) == null,
                "反击伤害要有限");
        expect(CounterWindowSpec.tryOf(8, 0.0f, 0.0f, CounterAdmission.DIRECT) != null, "零减伤和零反击可以装");
        expect(CounterWindowSpec.tryOf(8, 0.2f, 1.0f, null) == null, "准入不能空");
    }

    private static void checkWindow() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 20, 0.25f, 4.0f);
        expect(ledger.arm("owner", CounterWindowSpec.tryOf(5, 0.1f, 9.0f, CounterAdmission.MELEE))
                == CounterArmResult.REJECTED_OCCUPIED, "已有窗口拒绝覆盖");
        expect(ledger.active("owner"), "拒绝覆盖后原窗口还在");
        expect(ledger.cancel("owner"), "取消会摘窗口");
        expect(!ledger.active("owner"), "取消后不再活动");
        expect(ledger.arm(null, CounterWindowSpec.tryOf(5, 0.1f, 1.0f, CounterAdmission.MELEE))
                == CounterArmResult.REJECTED_INVALID, "空主人拒绝");

        CounterLedger shortWindow = armed("owner", CounterAdmission.MELEE, 2, 0.0f, 1.0f);
        shortWindow.tick();
        expect(shortWindow.active("owner"), "两拍窗口第一拍还在");
        shortWindow.tick();
        expect(!shortWindow.active("owner"), "超时摘窗口");

        CounterLedger paused = armed("owner", CounterAdmission.MELEE, 1, 0.0f, 1.0f);
        CounterHurtFrame openFrame = paused.enterHurt();
        expect(paused.noteHurt(openFrame, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "预留中");
        paused.tick();
        expect(paused.active("owner"), "调用还在栈上时这一拍不超时");
        paused.abortHurt(openFrame);
        paused.tick();
        expect(!paused.active("owner"), "解除预留后超时仍会摘掉");
    }

    private static void checkOutcomes() {
        checkReduction();
        checkIgnored("取消的事件", HurtOffer.melee("owner", "attacker", 6.0f).withCanceled(true), true, false);
        checkIgnored("非正伤害", HurtOffer.melee("owner", "attacker", 0.0f), true, false);
        checkIgnored("反击来源", HurtOffer.melee("owner", "attacker", 6.0f).withCounterSource(true), true, false);
        checkIgnored("绕过无敌", HurtOffer.melee("owner", "attacker", 6.0f).withBypass(true), true, false);
        checkIgnored("不是敌对", HurtOffer.melee("owner", "attacker", 6.0f).withHostile(false), true, false);
        checkMeleeRejectsProjectile();
        checkDirectAcceptsProjectile();
        acceptUntilFinish();
        checkDamageCancelKeepsWindow();
        checkHurtCancelKeepsWindow();
        checkMissingDamageKeepsWindow();
        checkAbsorptionConsumes();
        checkOneSuccess();
        checkScopeBlocks();
        checkNestedAbort();
        checkOwnerUnfit();
        checkOwnerUnknown();
        checkClearDropsStrike();
        checkServerStopClearsAll();
        checkTimeoutKeepsOtherStrike();
    }

    private static void checkReduction() {
        CounterLedger ledger = armed("owner", CounterAdmission.MELEE, 30, 0.25f, 4.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        HurtMitigation mitigation = ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 8.0f));
        expect(mitigation.reserved(), "合格伤害会预留");
        expect(mitigation.amount() == 6.0f, "按 1 减比例缩小当前伤害");
        HurtMitigation second = ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 8.0f));
        expect(!second.reserved(), "同一次调用只预留一次");
        ledger.abortHurt(frame);
        expect(ledger.active("owner"), "异常解除后窗口还在");
        expect(ledger.drainStrikes().isEmpty(), "异常解除不排队");
    }

    private static void checkIgnored(String label, HurtOffer offer, boolean accepted, boolean damageCanceled) {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.25f, 4.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        expect(!ledger.noteHurt(frame, offer).reserved(), label + "不占窗口");
        if (damageCanceled) {
            ledger.noteDamage(frame, true);
        }
        ledger.finishHurt(frame, accepted, OwnerReadiness.READY);
        expect(ledger.active("owner"), label + "后窗口还在");
        expect(ledger.drainStrikes().isEmpty(), label + "不排队");
    }

    private static void checkMeleeRejectsProjectile() {
        CounterLedger ledger = armed("owner", CounterAdmission.MELEE, 30, 0.0f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        HurtOffer projectile = HurtOffer.melee("owner", "attacker", 5.0f).withMelee(false).withOwnedProjectile(true);
        expect(!ledger.noteHurt(frame, projectile).reserved(), "近战窗口不收弹体");
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.active("owner"), "弹体未消耗近战窗口");
    }

    private static void checkDirectAcceptsProjectile() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.5f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        HurtOffer projectile = HurtOffer.melee("owner", "attacker", 4.0f).withMelee(false).withOwnedProjectile(true);
        HurtMitigation mitigation = ledger.noteHurt(frame, projectile);
        expect(mitigation.reserved(), "直接攻击窗口收有主弹体");
        expect(mitigation.amount() == 2.0f, "弹体同样减伤");
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.drainStrikes().size() == 1, "弹体成功会排队");
    }

    private static void checkDamageCancelKeepsWindow() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 3.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        expect(ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 5.0f)).reserved(), "伤害取消前已预留");
        ledger.noteDamage(frame, true);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.active("owner"), "伤害事件取消不消费");
        expect(ledger.drainStrikes().isEmpty(), "伤害事件取消不排队");
    }

    private static void checkHurtCancelKeepsWindow() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 3.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        expect(ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 5.0f)).reserved(), "受伤取消前已预留");
        ledger.noteHurtCanceled(frame);
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.active("owner"), "受伤事件取消不消费");
        expect(ledger.drainStrikes().isEmpty(), "受伤事件取消不排队");
    }

    private static void checkMissingDamageKeepsWindow() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 3.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        expect(ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 5.0f)).reserved(), "没走到伤害事件前已预留");
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.active("owner"), "hurt 返回真但没有伤害事件时不消费");
        expect(ledger.drainStrikes().isEmpty(), "没走到伤害事件不排队");
    }

    private static void checkAbsorptionConsumes() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 3.5f);
        CounterHurtFrame frame = ledger.enterHurt();
        expect(ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 5.0f)).reserved(), "吸收前伤害为正");
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        List<PlannedStrike> due = ledger.drainStrikes();
        expect(!ledger.active("owner"), "吸收生命仍消费窗口");
        expect(due.size() == 1, "吸收生命仍排队一次");
        expect(due.get(0).damage() == 3.5f, "排队的是窗口上的反击伤害");
        expect(due.get(0).attackerKey().equals("attacker"), "反击对象是攻击者");
    }

    private static void checkOneSuccess() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame first = ledger.enterHurt();
        ledger.noteHurt(first, HurtOffer.melee("owner", "attacker", 4.0f));
        ledger.noteDamage(first, false);
        ledger.finishHurt(first, true, OwnerReadiness.READY);
        CounterHurtFrame second = ledger.enterHurt();
        expect(!ledger.noteHurt(second, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "成功后同刻不能再用");
        ledger.noteDamage(second, false);
        ledger.finishHurt(second, true, OwnerReadiness.READY);
        expect(ledger.drainStrikes().size() == 1, "只排队一次");
    }

    private static void checkScopeBlocks() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        ledger.openCounterScope();
        CounterHurtFrame frame = ledger.enterHurt();
        expect(!ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "反击调用标记中不预留");
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        ledger.closeCounterScope();
        expect(ledger.active("owner"), "递归期间不消费");
        CounterHurtFrame later = ledger.enterHurt();
        expect(ledger.noteHurt(later, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "标记结束后可以预留");
        ledger.abortHurt(later);
    }

    private static void checkNestedAbort() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame outer = ledger.enterHurt();
        CounterHurtFrame inner = ledger.enterHurt();
        expect(ledger.noteHurt(inner, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "内层可以预留");
        ledger.abortHurt(inner);
        expect(ledger.current() == outer, "内层异常后外层还在");
        expect(ledger.active("owner"), "内层失败不消费");
        expect(ledger.noteHurt(outer, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "外层随后可以预留");
        ledger.noteDamage(outer, false);
        ledger.finishHurt(outer, true, OwnerReadiness.READY);
        expect(ledger.drainStrikes().size() == 1, "外层成功只排队一次");
    }

    private static void checkOwnerUnfit() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 4.0f));
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.UNFIT);
        expect(!ledger.active("owner"), "主人已死时关掉窗口");
        expect(ledger.drainStrikes().isEmpty(), "主人已死不排队反击");
    }

    private static void checkOwnerUnknown() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 4.0f));
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.UNKNOWN);
        expect(ledger.active("owner"), "查不到主人时不消费");
        expect(ledger.drainStrikes().isEmpty(), "查不到主人时不排队");
    }

    private static void checkClearDropsStrike() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 4.0f));
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        ledger.clearOwner("owner");
        expect(ledger.drainStrikes().isEmpty(), "死亡登出或换维清掉已排队的反击");
        expect(!ledger.active("owner"), "生命周期清掉窗口");
    }

    private static void checkTimeoutKeepsOtherStrike() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 4.0f));
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.arm("owner", CounterWindowSpec.tryOf(1, 0.0f, 9.0f, CounterAdmission.MELEE))
                == CounterArmResult.ARMED, "成功后可以再装一扇新窗口");
        ledger.tick();
        expect(!ledger.active("owner"), "新窗口自己超时");
        List<PlannedStrike> due = ledger.drainStrikes();
        expect(due.size() == 1 && due.get(0).damage() == 2.0f, "新窗口超时不取消上一扇已经排好的反击");
    }

    private static void checkServerStopClearsAll() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 2.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 4.0f));
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, true, OwnerReadiness.READY);
        expect(ledger.arm("other", CounterWindowSpec.tryOf(12, 0.0f, 5.0f, CounterAdmission.MELEE))
                == CounterArmResult.ARMED, "停服前另一扇窗口还在");
        CounterHurtFrame stuck = ledger.enterHurt();
        expect(ledger.noteHurt(stuck, HurtOffer.melee("other", "attacker", 3.0f)).reserved(), "停服前预留还在");
        ledger.openCounterScope();
        ledger.clearAll();
        expect(!ledger.active("owner"), "停服后原主人没有窗口");
        expect(!ledger.active("other"), "停服不看玩家列表也清掉另一扇窗口");
        expect(ledger.current() == null, "停服清掉当前线程上的残留帧");
        expect(!ledger.counterScopeOpen(), "停服清掉当前线程上的反击深度");
        expect(ledger.drainStrikes().isEmpty(), "停服清掉待反击");
        ledger.noteDamage(stuck, false);
        ledger.finishHurt(stuck, true, OwnerReadiness.READY);
        expect(ledger.drainStrikes().isEmpty(), "清掉之后旧预留不能再排队");
        CounterHurtFrame later = ledger.enterHurt();
        expect(!ledger.noteHurt(later, HurtOffer.melee("owner", "attacker", 4.0f)).reserved(), "清后旧窗口不能再预留");
        ledger.noteDamage(later, false);
        ledger.finishHurt(later, true, OwnerReadiness.READY);
        expect(ledger.drainStrikes().isEmpty(), "清后不能再触发反击");
        ledger.clearAll();
        expect(!ledger.active("owner") && !ledger.active("other"), "重复停止仍然是空的");
        expect(ledger.drainStrikes().isEmpty(), "重复停止不会生出反击");
        expect(ledger.current() == null, "重复停止不会留下帧");
        expect(ledger.arm("owner", CounterWindowSpec.tryOf(8, 0.0f, 1.0f, CounterAdmission.DIRECT))
                == CounterArmResult.ARMED, "清空后可以重新装窗");
    }

    private static void checkStrikeGate() {
        StrikeFacts ready = new StrikeFacts();
        ready.ownerAlive = true;
        ready.sameDimension = true;
        ready.attackerAlive = true;
        ready.hostile = true;
        ready.damage = 2.0f;
        expect(CounterLedger.strikeReady(ready), "主人目标同维且敌对时可以打");
        ready.damage = 0.0f;
        expect(!CounterLedger.strikeReady(ready), "零反击不调用 hurt");
        ready.damage = 2.0f;
        ready.ownerAlive = false;
        expect(!CounterLedger.strikeReady(ready), "主人已死不再打");
        ready.ownerAlive = true;
        ready.sameDimension = false;
        expect(!CounterLedger.strikeReady(ready), "换维后不再打");
        ready.sameDimension = true;
        ready.hostile = false;
        expect(!CounterLedger.strikeReady(ready), "不再敌对就不再打");
        ready.hostile = true;
        ready.attackerSpectator = true;
        expect(!CounterLedger.strikeReady(ready), "旁观攻击者不打");
    }

    private static CounterLedger armed(String owner, CounterAdmission admission, int ticks, float ratio, float damage) {
        CounterLedger ledger = new CounterLedger();
        CounterWindowSpec spec = CounterWindowSpec.tryOf(ticks, ratio, damage, admission);
        expect(spec != null, "参数 " + admission);
        expect(ledger.arm(owner, spec) == CounterArmResult.ARMED, "装上 " + owner);
        return ledger;
    }

    private static CounterLedger acceptUntilFinish() {
        CounterLedger ledger = armed("owner", CounterAdmission.DIRECT, 30, 0.0f, 3.0f);
        CounterHurtFrame frame = ledger.enterHurt();
        expect(ledger.noteHurt(frame, HurtOffer.melee("owner", "attacker", 5.0f)).reserved(), "失败路径已预留");
        ledger.noteDamage(frame, false);
        ledger.finishHurt(frame, false, OwnerReadiness.READY);
        expect(ledger.active("owner"), "hurt 没接受时不消费");
        expect(ledger.drainStrikes().isEmpty(), "hurt 没接受时不排队");
        return ledger;
    }

    private static void expect(boolean condition, String label) {
        checks += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}

package com.bslx.XueXiMod.shu.technique.release.deflection;

import com.bslx.XueXiMod.shu.technique.release.deflection.melee.MeleeLine;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowBook;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowDamageFreeze;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowSegmentOrder;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowSupport;
import com.bslx.XueXiMod.shu.technique.release.deflection.projectile.ArrowTickState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.projectile.SpectralArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.phys.Vec3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

/**
 * 不启动 Forge。报价冻结、线段次序、次数和失败回退在这里跑。
 * 真实箭实体、注入和水族路线要有服务端，这里不造。
 */
public final class DeflectionProjectileCheck {
    private DeflectionProjectileCheck() {
    }

    public static void main(String[] arguments) throws Exception {
        freeze();
        order();
        sameSerialStops();
        ledger();
        meleeLine();
        support();
        ownership();
        System.out.println("箭报价、反射次序和回退通过");
        System.out.println("断言 " + assertionCount);
    }

    private static void freeze() {
        int[] calls = new int[1];
        float plain = ArrowDamageFreeze.freeze(3.0D, 2.0D, false, bound -> {
            calls[0]++;
            return 0;
        }, 1.5F);
        check(calls[0] == 0, "非暴击不能抽随机");
        check(Math.abs(plain - 7.5F) < 0.001F, "领域加值保持浮点");
        float critical = ArrowDamageFreeze.freeze(3.0D, 2.0D, true, bound -> bound - 1, 0.5F);
        check(Math.abs(critical - 10.5F) < 0.001F, "暴击按原版上界加回且不截断领域");
        check(ArrowDamageFreeze.ceilBase(1.2D, 2.0D) == 3, "速度乘基础伤害后向上取整");
        check(ArrowDamageFreeze.applyCrit(3, 1) == 4, "暴击附加量加回基数");
    }

    private static void order() throws Exception {
        check(ArrowSegmentOrder.waterEarlier(0.2D, false, 0.5D, true, -1.0D), "水早于实体");
        check(!ArrowSegmentOrder.waterEarlier(0.8D, false, 0.5D, true, -1.0D), "实体更早时不能越过去接");
        check(!ArrowSegmentOrder.waterEarlier(0.999999D, true, 2.0D, false, -1.0D), "贴着方块面不算早于方块");
        check(ArrowSegmentOrder.waterEarlier(0.2D, false, 1.0D, false, -1.0D), "没有活体目标时水体仍可接");
        check(!ArrowSegmentOrder.waterEarlier(0.2D, false, 0.8D, true, 0.2D), "已经处理过的水不能再追认");
        double parameter = ArrowSegmentOrder.parameter(0.0D, 0.0D, 0.0D, 10.0D, 0.0D, 0.0D, 4.0D, 0.0D, 0.0D);
        check(Math.abs(parameter - 0.4D) < 1.0E-9D, "线段参数按投影");

        ArrowSegmentOrder.Passage pierce = ArrowSegmentOrder.passage(
                new double[]{0.1D, 0.5D}, false, 0.8D, true, 0.3D);
        int[] pierceAdmitted = pierce.admitted();
        check(pierceAdmitted.length == 1 && pierceAdmitted[0] == 1 && !pierce.blocked(),
                "已处理前的水跳过，后方仍早于实体的水可以考虑");

        ArrowSegmentOrder.Passage late = ArrowSegmentOrder.passage(
                new double[]{0.85D}, false, 0.8D, true, 0.3D);
        check(late.admitted().length == 0 && late.blocked(), "晚于当前合法实体的水被挡住");

        ArrowSegmentOrder.Passage mixed = ArrowSegmentOrder.passage(
                new double[]{0.1D, 0.5D, 0.85D}, false, 0.8D, true, 0.3D);
        int[] mixedAdmitted = mixed.admitted();
        check(mixedAdmitted.length == 1 && mixedAdmitted[0] == 1 && mixed.blocked(),
                "先考虑0.5，到实体才停，不把已处理界限改成实体");

        double gap = ArrowSegmentOrder.EARLIER_GAP;
        check(ArrowSegmentOrder.classify(0.3D + gap, false, 0.8D, true, 0.3D) == ArrowSegmentOrder.PLACE_HANDLED,
                "正好落在已处理间隙内继续跳过");
        check(ArrowSegmentOrder.classify(Math.nextUp(0.3D + gap), false, 0.8D, true, 0.3D) == ArrowSegmentOrder.PLACE_OPEN,
                "刚过已处理间隙可以考虑");
        check(ArrowSegmentOrder.classify(0.8D, false, 0.8D, true, -1.0D) == ArrowSegmentOrder.PLACE_OBSTACLE,
                "与实体同一参数不算更早");
        check(ArrowSegmentOrder.classify(0.8D - gap, false, 0.8D, true, -1.0D) == ArrowSegmentOrder.PLACE_OBSTACLE,
                "只差一个间隙仍不算严格更早");
        check(ArrowSegmentOrder.classify(Math.nextDown(0.8D - gap), false, 0.8D, true, -1.0D) == ArrowSegmentOrder.PLACE_OPEN,
                "严格早于实体间隙才可考虑");
        check(ArrowSegmentOrder.classify(1.0D - gap, true, 2.0D, false, -1.0D) == ArrowSegmentOrder.PLACE_OBSTACLE,
                "贴着方块终点是障碍");
        check(ArrowSegmentOrder.classify(Math.nextDown(1.0D - gap), true, 2.0D, false, -1.0D) == ArrowSegmentOrder.PLACE_OPEN,
                "方块间隙之前仍可考虑");
        check(ArrowSegmentOrder.classify(Double.NaN, false, 0.8D, true, 0.3D) == ArrowSegmentOrder.PLACE_SKIP,
                "无效参数跳过");
        ArrowSegmentOrder.Passage invalidFirst = ArrowSegmentOrder.passage(
                new double[]{Double.NaN, 0.5D}, false, 0.8D, true, 0.3D);
        int[] invalidAdmitted = invalidFirst.admitted();
        check(invalidAdmitted.length == 1 && invalidAdmitted[0] == 1 && !invalidFirst.blocked(),
                "无效候选不挡住后方水");

        String flight = Files.readString(Path.of(
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowFlight.java"),
                StandardCharsets.UTF_8);
        check(flight.contains("ArrowSegmentOrder.passage(") && !flight.contains("waterEarlier"),
                "飞行循环使用同一套遍历，不再把已处理前段当成障碍打断");
        check(flight.contains("STOP_HERE"), "部分卸力和已结算仍然停在这一次接触");
    }

    private static void sameSerialStops() {
        DeflectionService.clearBook();
        CompoundTag tag = new CompoundTag();
        int serial = ArrowBook.contactSerial(tag);
        int fullCount = ArrowBook.fullCount(tag);
        UUID arrowId = UUID.randomUUID();
        AttackKey sameKey = AttackKey.projectile(arrowId, serial);
        double[] waterTimes = new double[]{0.4D, 0.6D};
        ArrowSegmentOrder.Passage passage = ArrowSegmentOrder.passage(waterTimes, false, 0.9D, true, -1.0D);
        int[] admitted = passage.admitted();
        check(admitted.length == 2 && admitted[0] == 0 && admitted[1] == 1 && !passage.blocked(),
                "同一接触上的两处水都会进入遍历");
        LedgerPurse purse = new LedgerPurse(25f);
        CheckHost firstHost = new CheckHost(new HostKey(UUID.randomUUID(), 1L), UUID.randomUUID());
        CheckHost secondHost = new CheckHost(new HostKey(UUID.randomUUID(), 2L), UUID.randomUUID());
        HostContact firstContact = new HostContact(firstHost, waterTimes[admitted[0]],
                new Vec3(0.4D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D));
        HostContact secondContact = new HostContact(secondHost, waterTimes[admitted[1]],
                new Vec3(4.0D, 0.0D, 0.0D), new Vec3(0.0D, 1.0D, 0.0D));
        DeflectionService.Transaction opened = DeflectionService.open(firstHost, firstContact, sameKey,
                40f, fullCount, false, purse, false, 3L);
        check(opened.acquired() && opened.quote().outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL
                && !opened.quote().spendsFullAttempt(), "第一处水部分卸力成交");
        opened.commit();
        check(opened.committed() && purse.commitCalls == 1 && purse.balance == 0f, "部分卸力只扣一次");
        opened.close();
        purse.restore(40f);
        int reserves = purse.reserveCalls;
        DeflectionService.Transaction again = DeflectionService.open(secondHost, secondContact, sameKey,
                40f, fullCount, false, purse, false, 4L);
        check(!again.acquired() && again.passedOver() && purse.reserveCalls == reserves && purse.balance == 40f,
                "同一接触序号再次进入不产生第二次成交");
        again.close();
        check(DeflectionService.remembers(sameKey), "成交后的防重还在，没有清理这次接触");
        check(ArrowBook.contactSerial(tag) == serial && ArrowBook.fullCount(tag) == fullCount,
                "重复进入不拨号也不加完整次数");
        DeflectionService.clearBook();
    }

    private static void ledger() {
        CompoundTag tag = new CompoundTag();
        DeflectionEconomy.Quote partial = DeflectionEconomy.quote(25.0F, 30.4F, 0, false);
        check(partial.outcome() == DeflectionEconomy.Quote.Outcome.PARTIAL, "水势不足是部分卸力");
        check(ArrowBook.applyCommit(tag, partial, 30.4F, 1, true), "提交成功才写剩余");
        check(Math.abs(ArrowBook.remaining(tag) - 5.4F) < 0.001F, "永久削弱保留小数");
        check(ArrowBook.fullCount(tag) == 0, "部分卸力不计完整次数");
        check(tag.getInt(ArrowBook.PARTIAL_AT) == 1, "记下这次部分接触");
        float before = ArrowBook.remaining(tag);
        int countBefore = ArrowBook.fullCount(tag);
        check(!ArrowBook.applyCommit(tag, partial, 30.4F, 1, false), "未提交不能改账");
        check(ArrowBook.remaining(tag) == before && ArrowBook.fullCount(tag) == countBefore, "失败后剩余和次数都不变");
        DeflectionEconomy.Quote none = DeflectionEconomy.quote(19.0F, 30.0F, 0, false);
        check(!ArrowBook.applyCommit(tag, none, 30.0F, 1, true), "没成交的报价即使标记提交也不落账");
        DeflectionEconomy.Quote full = DeflectionEconomy.quote(20.0F, 5.4F, 0, false);
        check(full.spendsFullAttempt(), "足额才占一次完整");
        check(ArrowBook.applyCommit(tag, full, 5.4F, 2, true), "完整偏转提交");
        check(ArrowBook.fullCount(tag) == 1, "只有完整提交后次数加一");
        check(Math.abs(ArrowBook.remaining(tag) - 5.4F) < 0.001F, "完整偏转不把剩余伤害打成零");
        tag.putBoolean(ArrowBook.CAPTURED, true);
        ArrowBook.markSafeRelease(tag);
        check(!ArrowBook.captured(tag), "安全放出清掉捕获");
        check(ArrowBook.fullCount(tag) == 1 && Math.abs(ArrowBook.remaining(tag) - 5.4F) < 0.001F, "放出不再扣款也不加次数");

        ArrowBook.stayContact(tag);
        ArrowBook.stayContact(tag);
        check(ArrowBook.contactSerial(tag) == 0, "仍在水里不增加接触序号");
        UUID contactArrow = UUID.randomUUID();
        com.bslx.XueXiMod.shu.technique.release.deflection.HostKey latched =
                new com.bslx.XueXiMod.shu.technique.release.deflection.HostKey(contactArrow, 1L);
        ArrowBook.latchExit(tag, latched);
        ArrowBook.leaveContact(tag, contactArrow);
        check(ArrowBook.contactSerial(tag) == 1, "离开水体后序号加一");
        check(latched.equals(ArrowBook.exitLatch(tag)), "放出闩在离开序号之后还在");
        check(ArrowBook.fullCount(tag) == 1 && Math.abs(ArrowBook.remaining(tag) - 5.4F) < 0.001F, "退出闩不改伤害和次数");
        ArrowBook.clearExit(tag);
        check(ArrowBook.exitLatch(tag) == null, "箭体出界后清掉退出闩");
        ArrowBook.leaveContact(tag, contactArrow);
        check(ArrowBook.contactSerial(tag) == 1, "已经在外面就不再加");

        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        ArrowBook.appendExcluded(tag, first);
        ArrowBook.appendExcluded(tag, first);
        ArrowBook.appendExcluded(tag, second);
        check(ArrowBook.excludes(tag, first) && ArrowBook.excludes(tag, second), "保护名单追加且不重复");
        check(!ArrowBook.excludes(tag, UUID.randomUUID()), "名单外的目标不被排除");
        long[] pairs = tag.getLongArray(com.bslx.XueXiMod.shu.technique.release.deflection.DeflectionMarks.EXCLUDE_OWNERS);
        check(pairs.length == 4, "每个身份占高低两位");

        UUID arrowId = UUID.randomUUID();
        ArrowTickState.armSuppress(arrowId);
        check(ArrowTickState.consumeSuppress(arrowId), "本次后续伤害消耗抑制");
        check(!ArrowTickState.consumeSuppress(arrowId), "下一次命中不再继承这次抑制");
        ArrowTickState.clear();
    }

    private static void meleeLine() {
        double[] nearest = MeleeLine.closest(0.0D, 0.0D, 0.0D, 2.0D, 0.0D, 0.0D, 3.0D, 1.0D, 1.0D);
        check(nearest[0] == 2.0D && nearest[1] == 0.0D && nearest[2] == 0.0D, "攻击线落到身体最近点");
        double span = nearest[0] * nearest[0] + nearest[1] * nearest[1] + nearest[2] * nearest[2];
        check(MeleeLine.usable(span), "身体外的攻击线可用");
        double[] inside = MeleeLine.closest(2.5D, 0.5D, 0.5D, 2.0D, 0.0D, 0.0D, 3.0D, 1.0D, 1.0D);
        double insideSpan = (inside[0] - 2.5D) * (inside[0] - 2.5D)
                + (inside[1] - 0.5D) * (inside[1] - 0.5D)
                + (inside[2] - 0.5D) * (inside[2] - 0.5D);
        check(!MeleeLine.usable(insideSpan), "点在身体内部时没有全身免疫线");
    }

    private static void support() {
        check(ArrowSupport.supportsType(Arrow.class), "普通箭和药箭");
        check(ArrowSupport.supportsType(SpectralArrow.class), "光灵箭");
        check(!ArrowSupport.supportsType(ThrownTrident.class), "三叉戟不在首版");
        check(!ArrowSupport.supportsType(AbstractArrow.class), "未知父类不默认纳入");
    }

    private static void ownership() throws Exception {
        String[] files = new String[]{
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowFlight.java",
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowCaptureBook.java",
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/projectile/ArrowBook.java",
                "src/main/java/com/bslx/XueXiMod/shu/technique/release/deflection/melee/MeleeDeflection.java",
                "src/main/java/com/bslx/XueXiMod/mixin/ArrowDeflectionMixin.java",
                "src/main/java/com/bslx/XueXiMod/mixin/MobMeleeDeflectionMixin.java"
        };
        String[] banned = new String[]{
                "MomentumPurse",
                "registerUnchecked",
                "DeflectionService.open",
                "clearBook",
                "forgetHosts",
                "DeflectionHosts.find",
                "DeflectionHosts.choose"
        };
        for (String relative : files) {
            String text = Files.readString(Path.of(relative), StandardCharsets.UTF_8);
            for (String token : banned) {
                if (text.contains(token)) {
                    throw new AssertionError(relative + " 调用了内部接缝 " + token);
                }
            }
        }
    }

    private static int assertionCount;

    private static void check(boolean condition, String label) {
        assertionCount++;
        if (!condition) {
            throw new AssertionError(label);
        }
    }

    private static final class CheckHost implements DeflectionHost {
        private final HostKey hostKey;
        private final UUID owner;

        private CheckHost(HostKey hostKey, UUID owner) {
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
            return true;
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

    private static final class LedgerPurse implements MomentumPurse {
        private float balance;
        private float reserved;
        private int reserveCalls;
        private int commitCalls;

        private LedgerPurse(float balance) {
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
            if (!(amount > 0f) || !Float.isFinite(amount) || amount > funds + 0.001f) {
                return null;
            }
            this.reserved += amount;
            return new LedgerHold(amount);
        }

        @Override
        public boolean commit(Object reservation) {
            if (!(reservation instanceof LedgerHold hold) || hold.state != LedgerHold.Stage.RESERVED) {
                return false;
            }
            hold.state = LedgerHold.Stage.COMMITTED;
            this.reserved -= hold.amount;
            this.balance -= hold.amount;
            this.commitCalls++;
            return true;
        }

        @Override
        public void release(Object reservation) {
            if (!(reservation instanceof LedgerHold hold) || hold.state != LedgerHold.Stage.RESERVED) {
                return;
            }
            hold.state = LedgerHold.Stage.RELEASED;
            this.reserved -= hold.amount;
        }
    }

    private static final class LedgerHold {
        private enum Stage {
            RESERVED,
            COMMITTED,
            RELEASED
        }

        private final float amount;
        private Stage state = Stage.RESERVED;

        private LedgerHold(float amount) {
            this.amount = amount;
        }
    }
}

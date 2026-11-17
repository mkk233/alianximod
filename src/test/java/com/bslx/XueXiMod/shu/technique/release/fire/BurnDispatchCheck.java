package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** 伤害回调里的新计划和丢弃。不创建实体，也不启动游戏。 */
public final class BurnDispatchCheck {
    private static final UUID CAST = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa1");
    private static final UUID CASTER = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa2");
    private static final UUID TARGET = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaa3");
    private static int assertionCount;
    private static int failures;

    private BurnDispatchCheck() {
    }

    public static void main(String[] arguments) {
        section("回调挂载", BurnDispatchCheck::scheduleDuringStrike);
        section("回调丢弃", BurnDispatchCheck::discardDuringStrike);
        section("死亡", BurnDispatchCheck::deathDuringStrike);
        section("首段等待", BurnDispatchCheck::firstSegmentWaits);
        System.out.println("断言 " + assertionCount + " 失败 " + failures);
        System.out.println("未执行：实体 NBT、死亡事件、真实 hurt。没有启动游戏。");
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static void scheduleDuringStrike() {
        LedgerMemory memory = duePair();
        BurnDispatch.tick(memory, pulse -> {
            if (memory.strikes == 0) {
                BurnLedger fresh = memory.read();
                boolean admitted = fresh.admit(schedule("gamma", 4), identity(UUID.randomUUID()));
                memory.write(fresh);
                check(admitted, "回调能在已提交账本上挂新轨道");
            }
            memory.strikes += 1;
        }, memory, 1L);
        check(memory.strikes == 2, "本次已经到期的两段都打出");
        List<BurnTrackState> stored = memory.read().export();
        check(stored.size() == 1 && "gamma".equals(stored.get(0).effectKey()), "回调挂上的轨道还在");
        check(stored.get(0).ticksUntilNext() == 4 && stored.get(0).segmentCursor() == 0, "新轨道同一次 tick 不推进");
    }

    private static void discardDuringStrike() {
        LedgerMemory memory = duePair();
        long generation = memory.generation();
        BurnDispatch.tick(memory, pulse -> {
            if (memory.strikes == 0) {
                memory.discard();
            }
            memory.strikes += 1;
        }, memory, 1L);
        check(memory.strikes == 1, "丢弃后剩余段不再打");
        check(memory.read().trackCount() == 0, "旧账本没有把计划写回去");
        check(memory.generation() == generation + 1L, "丢弃推进世代");
    }

    private static void deathDuringStrike() {
        LedgerMemory memory = duePair();
        BurnDispatch.tick(memory, pulse -> {
            memory.alive = false;
            memory.discard();
            memory.strikes += 1;
        }, memory, 1L);
        check(memory.strikes == 1 && memory.read().trackCount() == 0, "死亡清账后剩余段不复活计划");
    }

    private static void firstSegmentWaits() {
        LedgerMemory memory = new LedgerMemory();
        BurnLedger ledger = memory.read();
        check(ledger.admit(schedule("delta", 4), identity(CAST)), "挂上间隔 4 的轨道");
        memory.write(ledger);
        BurnDispatch.tick(memory, pulse -> memory.strikes += 1, memory, 1L);
        List<BurnTrackState> stored = memory.read().export();
        check(memory.strikes == 0 && stored.size() == 1 && stored.get(0).ticksUntilNext() == 3,
                "首段仍等满间隔，失败与否都不在这次补打");
    }

    private static LedgerMemory duePair() {
        LedgerMemory memory = new LedgerMemory();
        BurnLedger first = memory.read();
        check(first.admit(schedule("alpha", 1), identity(CAST)), "第一段到期轨道");
        memory.write(first);
        BurnLedger second = memory.read();
        check(second.admit(schedule("beta", 1), identity(UUID.randomUUID())), "第二段到期轨道");
        memory.write(second);
        return memory;
    }

    private static BurnSchedule schedule(String effectKey, int intervalTicks) {
        Optional<BurnComponent> component = BurnComponent.of(2.0d, 0.0d);
        Optional<BurnSchedule> schedule = BurnSchedule.of(effectKey, false, intervalTicks, List.of(component.orElseThrow()));
        return schedule.orElseThrow();
    }

    private static BurnIdentity identity(UUID castId) {
        return new BurnIdentity(castId, CASTER, TARGET, 10, 1, 0, 0.0d);
    }

    private static final class LedgerMemory implements BurnDispatch.Memory, BurnDispatch.FuelPurse {
        private BurnLedger ledger = new BurnLedger();
        private long generation;
        private boolean alive = true;
        private int strikes;
        private int balance = 100;

        @Override
        public int balance() {
            return this.balance;
        }

        @Override
        public void commitSpent(int spent) {
            if (spent <= 0) {
                return;
            }
            if (spent >= this.balance) {
                this.balance = 0;
                return;
            }
            this.balance -= spent;
        }

        @Override
        public BurnLedger read() {
            return BurnLedger.restore(this.ledger.export());
        }

        @Override
        public void write(BurnLedger ledger) {
            this.ledger = BurnLedger.restore(ledger.export());
        }

        @Override
        public long generation() {
            return this.generation;
        }

        @Override
        public void discard() {
            this.ledger = new BurnLedger();
            this.generation += 1L;
        }

        @Override
        public boolean alive() {
            return this.alive;
        }
    }

    private static void section(String title, Runnable body) {
        int before = failures;
        body.run();
        System.out.println(title + (failures == before ? " 通过" : " 失败"));
    }

    private static void check(boolean condition, String label) {
        assertionCount += 1;
        if (!condition) {
            failures += 1;
            System.out.println("失败 " + label);
        }
    }
}

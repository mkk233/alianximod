package com.bslx.XueXiMod.shu.technique.release.fire;

/** 先提交余额和账本，再逐段打击。回调里的新计划和丢弃都不再被旧账本盖回去。 */
public final class BurnDispatch {
    private BurnDispatch() {
    }

    public interface Memory {
        BurnLedger read();

        void write(BurnLedger ledger);

        long generation();

        void discard();

        boolean alive();
    }

    public interface Strike {
        void hit(BurnPulse pulse);
    }

    /** 余额只在打击前回写。回调里的新生产和新层写在这次提交之后，不能被旧余额盖掉。 */
    public interface FuelPurse {
        int balance();

        void commitSpent(int spent);
    }

    public static void tick(Memory memory, Strike strike, FuelPurse purse, long turn) {
        if (memory == null || strike == null || purse == null) {
            return;
        }
        if (!memory.alive()) {
            memory.discard();
            return;
        }
        BurnLedger ledger = memory.read();
        if (ledger.trackCount() == 0) {
            return;
        }
        int opening = purse.balance();
        PumpResult result = ledger.pump(opening, turn);
        purse.commitSpent(result.spent());
        memory.write(ledger);
        long generation = memory.generation();
        for (BurnPulse pulse : result.pulses()) {
            if (!memory.alive() || memory.generation() != generation) {
                return;
            }
            strike.hit(pulse);
        }
    }

    /** 只结算等待为 0 的首段。没有这种层时不写回，也不推进其他轨道。 */
    public static void settleOpening(Memory memory, Strike strike, FuelPurse purse, long turn) {
        if (memory == null || strike == null || purse == null || !memory.alive()) {
            return;
        }
        BurnLedger ledger = memory.read();
        if (ledger.trackCount() == 0) {
            return;
        }
        int balance = purse.balance();
        PumpResult result = ledger.settleOpening(balance, turn);
        if (result == null) {
            return;
        }
        purse.commitSpent(result.spent());
        memory.write(ledger);
        long generation = memory.generation();
        for (BurnPulse pulse : result.pulses()) {
            if (!memory.alive() || memory.generation() != generation) {
                return;
            }
            strike.hit(pulse);
        }
    }
}

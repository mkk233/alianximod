package com.bslx.XueXiMod.combat.counter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 迎击窗口的纯账本。一次 hurt 调用对应一帧；取消、失败或伤害事件没走到就解除预留。
 * 成功先关掉窗口，再把反击放进队列，等调用方在 tick 末执行。
 */
public final class CounterLedger {
    private final Map<String, Window> windows = new HashMap<>();
    private final ThreadLocal<ArrayDeque<CounterHurtFrame>> callStack = ThreadLocal.withInitial(ArrayDeque::new);
    private final ThreadLocal<int[]> counterDepth = ThreadLocal.withInitial(() -> new int[1]);
    private final List<PlannedStrike> pendingStrikes = new ArrayList<>();
    private long nextGeneration = 1L;

    public CounterArmResult arm(String ownerKey, CounterWindowSpec spec) {
        if (ownerKey == null || ownerKey.isEmpty() || spec == null) {
            return CounterArmResult.REJECTED_INVALID;
        }
        if (this.windows.containsKey(ownerKey)) {
            return CounterArmResult.REJECTED_OCCUPIED;
        }
        long generation = this.nextGeneration;
        this.nextGeneration += 1L;
        this.windows.put(ownerKey, new Window(ownerKey, generation, spec));
        return CounterArmResult.ARMED;
    }

    public boolean cancel(String ownerKey) {
        if (ownerKey == null || ownerKey.isEmpty()) {
            return false;
        }
        Window removed = this.windows.remove(ownerKey);
        boolean droppedStrike = dropPending(ownerKey, null);
        return removed != null || droppedStrike;
    }

    public boolean active(String ownerKey) {
        return ownerKey != null && this.windows.containsKey(ownerKey);
    }

    public void clearOwner(String ownerKey) {
        cancel(ownerKey);
    }

    /** 停服时清掉全部内存窗口、待反击和当前线程上的残留帧。不读玩家列表。 */
    public void clearAll() {
        this.windows.clear();
        this.pendingStrikes.clear();
        ArrayDeque<CounterHurtFrame> stack = this.callStack.get();
        while (!stack.isEmpty()) {
            CounterHurtFrame frame = stack.removeFirst();
            frame.open = false;
            frame.reserved = false;
        }
        this.counterDepth.get()[0] = 0;
    }

    public CounterHurtFrame enterHurt() {
        CounterHurtFrame frame = new CounterHurtFrame();
        this.callStack.get().addFirst(frame);
        return frame;
    }

    public CounterHurtFrame current() {
        ArrayDeque<CounterHurtFrame> stack = this.callStack.get();
        if (stack.isEmpty()) {
            return null;
        }
        return stack.peekFirst();
    }

    public HurtMitigation noteHurt(CounterHurtFrame frame, HurtOffer offer) {
        if (frame == null || offer == null || frame != current() || !frame.open || frame.reserved) {
            return HurtMitigation.none();
        }
        if (offer.canceled) {
            frame.hurtCanceled = true;
            return HurtMitigation.none();
        }
        if (counterScopeOpen() || offer.counterSource || offer.bypassInvulnerability) {
            return HurtMitigation.none();
        }
        if (!Float.isFinite(offer.amount) || offer.amount <= 0.0f || !offer.hostile) {
            return HurtMitigation.none();
        }
        if (offer.ownerKey == null || offer.attackerKey == null) {
            return HurtMitigation.none();
        }
        Window window = this.windows.get(offer.ownerKey);
        if (window == null || window.reservedFrame != null) {
            return HurtMitigation.none();
        }
        if (!admissionAllows(window.admission, offer)) {
            return HurtMitigation.none();
        }
        float reduced = offer.amount * (1.0f - window.reductionRatio);
        if (!Float.isFinite(reduced) || reduced < 0.0f) {
            return HurtMitigation.none();
        }
        frame.reserved = true;
        frame.ownerKey = offer.ownerKey;
        frame.attackerKey = offer.attackerKey;
        frame.dimensionKey = offer.dimensionKey;
        frame.counterDamage = window.counterDamage;
        frame.generation = window.generation;
        window.reservedFrame = frame;
        return HurtMitigation.held(reduced);
    }

    public void noteHurtCanceled(CounterHurtFrame frame) {
        if (frame == null || frame != current() || !frame.open) {
            return;
        }
        frame.hurtCanceled = true;
    }

    public void noteDamage(CounterHurtFrame frame, boolean canceled) {
        if (frame == null || frame != current() || !frame.open) {
            return;
        }
        frame.damageReached = true;
        if (canceled) {
            frame.damageCanceled = true;
        }
    }

    public void finishHurt(CounterHurtFrame frame, boolean accepted, OwnerReadiness readiness) {
        detach(frame, true, accepted, readiness == null ? OwnerReadiness.UNKNOWN : readiness);
    }

    public void abortHurt(CounterHurtFrame frame) {
        detach(frame, false, false, OwnerReadiness.UNKNOWN);
    }

    public void tick() {
        List<String> expired = new ArrayList<>();
        ArrayDeque<CounterHurtFrame> stack = this.callStack.get();
        for (Window window : this.windows.values()) {
            CounterHurtFrame stuck = window.reservedFrame;
            if (stuck != null) {
                if (stuck.open && stack.contains(stuck)) {
                    continue;
                }
                stuck.reserved = false;
                window.reservedFrame = null;
            }
            window.remainingTicks -= 1;
            if (window.remainingTicks <= 0) {
                expired.add(window.ownerKey);
            }
        }
        for (String ownerKey : expired) {
            Window window = this.windows.remove(ownerKey);
            if (window != null) {
                dropPending(ownerKey, window.generation);
            }
        }
    }

    public List<PlannedStrike> drainStrikes() {
        if (this.pendingStrikes.isEmpty()) {
            return List.of();
        }
        List<PlannedStrike> due = List.copyOf(this.pendingStrikes);
        this.pendingStrikes.clear();
        return due;
    }

    public void openCounterScope() {
        this.counterDepth.get()[0] += 1;
    }

    public void closeCounterScope() {
        int[] depth = this.counterDepth.get();
        if (depth[0] > 0) {
            depth[0] -= 1;
        }
    }

    public boolean counterScopeOpen() {
        return this.counterDepth.get()[0] > 0;
    }

    public static boolean strikeReady(StrikeFacts facts) {
        if (facts == null || !facts.ownerAlive || facts.ownerRemoved || facts.ownerSpectator || !facts.sameDimension) {
            return false;
        }
        if (!facts.attackerAlive || facts.attackerRemoved || facts.attackerSpectator || !facts.hostile) {
            return false;
        }
        return Float.isFinite(facts.damage) && facts.damage > 0.0f;
    }

    private void detach(CounterHurtFrame frame, boolean commit, boolean accepted, OwnerReadiness readiness) {
        if (frame == null) {
            return;
        }
        List<CounterHurtFrame> popped = popThrough(frame);
        if (popped.isEmpty()) {
            settle(frame, false, false, OwnerReadiness.UNKNOWN);
            return;
        }
        for (int step = 0; step < popped.size(); step += 1) {
            CounterHurtFrame poppedFrame = popped.get(step);
            boolean selected = poppedFrame == frame;
            OwnerReadiness selectedReadiness = selected ? readiness : OwnerReadiness.UNKNOWN;
            settle(poppedFrame, commit && selected, accepted, selectedReadiness);
        }
    }

    private List<CounterHurtFrame> popThrough(CounterHurtFrame frame) {
        ArrayDeque<CounterHurtFrame> stack = this.callStack.get();
        boolean found = false;
        for (CounterHurtFrame stacked : stack) {
            if (stacked == frame) {
                found = true;
                break;
            }
        }
        List<CounterHurtFrame> popped = new ArrayList<>();
        if (!found) {
            return popped;
        }
        while (!stack.isEmpty()) {
            CounterHurtFrame top = stack.removeFirst();
            popped.add(top);
            if (top == frame) {
                break;
            }
        }
        return popped;
    }

    private void settle(CounterHurtFrame frame, boolean commit, boolean accepted, OwnerReadiness readiness) {
        frame.open = false;
        if (!frame.reserved) {
            return;
        }
        Window window = this.windows.get(frame.ownerKey);
        if (window == null || window.reservedFrame != frame || window.generation != frame.generation) {
            frame.reserved = false;
            return;
        }
        boolean damageOk = frame.damageReached && !frame.damageCanceled && !frame.hurtCanceled;
        boolean consume = commit && accepted && damageOk
                && (readiness == OwnerReadiness.READY || readiness == OwnerReadiness.UNFIT);
        if (!consume) {
            window.reservedFrame = null;
            frame.reserved = false;
            return;
        }
        this.windows.remove(frame.ownerKey);
        frame.reserved = false;
        if (readiness != OwnerReadiness.READY) {
            return;
        }
        this.pendingStrikes.add(new PlannedStrike(
                frame.ownerKey,
                frame.attackerKey,
                frame.dimensionKey,
                frame.counterDamage,
                frame.generation));
    }

    private boolean dropPending(String ownerKey, Long windowGeneration) {
        boolean dropped = false;
        for (int step = this.pendingStrikes.size() - 1; step >= 0; step -= 1) {
            PlannedStrike strike = this.pendingStrikes.get(step);
            if (!ownerKey.equals(strike.ownerKey())) {
                continue;
            }
            if (windowGeneration != null && strike.generation() != windowGeneration.longValue()) {
                continue;
            }
            this.pendingStrikes.remove(step);
            dropped = true;
        }
        return dropped;
    }

    private static boolean admissionAllows(CounterAdmission admission, HurtOffer offer) {
        if (admission == CounterAdmission.MELEE) {
            return offer.melee;
        }
        if (admission == CounterAdmission.OWNED_PROJECTILE) {
            return offer.ownedProjectile;
        }
        if (admission == CounterAdmission.DIRECT) {
            return offer.melee || offer.ownedProjectile;
        }
        return false;
    }

    public enum OwnerReadiness {
        READY,
        UNFIT,
        UNKNOWN
    }

    public static final class HurtOffer {
        private final String ownerKey;
        private final String attackerKey;
        private final String dimensionKey;
        private final float amount;
        private final boolean canceled;
        private final boolean melee;
        private final boolean ownedProjectile;
        private final boolean counterSource;
        private final boolean bypassInvulnerability;
        private final boolean hostile;

        private HurtOffer(String ownerKey, String attackerKey, String dimensionKey, float amount, boolean canceled,
                          boolean melee, boolean ownedProjectile, boolean counterSource, boolean bypassInvulnerability,
                          boolean hostile) {
            this.ownerKey = ownerKey;
            this.attackerKey = attackerKey;
            this.dimensionKey = dimensionKey;
            this.amount = amount;
            this.canceled = canceled;
            this.melee = melee;
            this.ownedProjectile = ownedProjectile;
            this.counterSource = counterSource;
            this.bypassInvulnerability = bypassInvulnerability;
            this.hostile = hostile;
        }

        public static HurtOffer of(String ownerKey, String attackerKey, String dimensionKey, float amount,
                                   boolean canceled, boolean melee, boolean ownedProjectile, boolean counterSource,
                                   boolean bypassInvulnerability, boolean hostile) {
            return new HurtOffer(ownerKey, attackerKey, dimensionKey, amount, canceled, melee, ownedProjectile,
                    counterSource, bypassInvulnerability, hostile);
        }

        public static HurtOffer melee(String ownerKey, String attackerKey, float amount) {
            return of(ownerKey, attackerKey, "minecraft:overworld", amount, false, true, false, false, false, true);
        }

        public HurtOffer withAmount(float amount) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, amount, this.canceled, this.melee,
                    this.ownedProjectile, this.counterSource, this.bypassInvulnerability, this.hostile);
        }

        public HurtOffer withCanceled(boolean canceled) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, this.amount, canceled, this.melee,
                    this.ownedProjectile, this.counterSource, this.bypassInvulnerability, this.hostile);
        }

        public HurtOffer withCounterSource(boolean counterSource) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, this.amount, this.canceled, this.melee,
                    this.ownedProjectile, counterSource, this.bypassInvulnerability, this.hostile);
        }

        public HurtOffer withHostile(boolean hostile) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, this.amount, this.canceled, this.melee,
                    this.ownedProjectile, this.counterSource, this.bypassInvulnerability, hostile);
        }

        public HurtOffer withMelee(boolean melee) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, this.amount, this.canceled, melee,
                    this.ownedProjectile, this.counterSource, this.bypassInvulnerability, this.hostile);
        }

        public HurtOffer withOwnedProjectile(boolean ownedProjectile) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, this.amount, this.canceled, this.melee,
                    ownedProjectile, this.counterSource, this.bypassInvulnerability, this.hostile);
        }

        public HurtOffer withBypass(boolean bypassInvulnerability) {
            return copy(this.ownerKey, this.attackerKey, this.dimensionKey, this.amount, this.canceled, this.melee,
                    this.ownedProjectile, this.counterSource, bypassInvulnerability, this.hostile);
        }

        private static HurtOffer copy(String ownerKey, String attackerKey, String dimensionKey, float amount,
                                      boolean canceled, boolean melee, boolean ownedProjectile, boolean counterSource,
                                      boolean bypassInvulnerability, boolean hostile) {
            return new HurtOffer(ownerKey, attackerKey, dimensionKey, amount, canceled, melee, ownedProjectile,
                    counterSource, bypassInvulnerability, hostile);
        }
    }

    public static final class HurtMitigation {
        private final boolean reserved;
        private final float amount;

        private HurtMitigation(boolean reserved, float amount) {
            this.reserved = reserved;
            this.amount = amount;
        }

        public boolean reserved() {
            return this.reserved;
        }

        public float amount() {
            return this.amount;
        }

        static HurtMitigation none() {
            return new HurtMitigation(false, 0.0f);
        }

        static HurtMitigation held(float amount) {
            return new HurtMitigation(true, amount);
        }
    }

    public static final class PlannedStrike {
        private final String ownerKey;
        private final String attackerKey;
        private final String dimensionKey;
        private final float damage;
        private final long generation;

        private PlannedStrike(String ownerKey, String attackerKey, String dimensionKey, float damage, long generation) {
            this.ownerKey = ownerKey;
            this.attackerKey = attackerKey;
            this.dimensionKey = dimensionKey;
            this.damage = damage;
            this.generation = generation;
        }

        public String ownerKey() {
            return this.ownerKey;
        }

        public String attackerKey() {
            return this.attackerKey;
        }

        public String dimensionKey() {
            return this.dimensionKey;
        }

        public float damage() {
            return this.damage;
        }

        public long generation() {
            return this.generation;
        }
    }

    public static final class StrikeFacts {
        public boolean ownerAlive;
        public boolean ownerRemoved;
        public boolean ownerSpectator;
        public boolean sameDimension;
        public boolean attackerAlive;
        public boolean attackerRemoved;
        public boolean attackerSpectator;
        public boolean hostile;
        public float damage;
    }

    private static final class Window {
        private final String ownerKey;
        private final long generation;
        private final float reductionRatio;
        private final float counterDamage;
        private final CounterAdmission admission;
        private int remainingTicks;
        private CounterHurtFrame reservedFrame;

        private Window(String ownerKey, long generation, CounterWindowSpec spec) {
            this.ownerKey = ownerKey;
            this.generation = generation;
            this.reductionRatio = spec.reductionRatio();
            this.counterDamage = spec.counterDamage();
            this.admission = spec.admission();
            this.remainingTicks = spec.windowTicks();
        }
    }
}

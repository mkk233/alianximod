package com.bslx.XueXiMod.shu.technique.release.fire;

import com.bslx.XueXiMod.status.fire.seed.FuelShare;

import com.bslx.XueXiMod.shu.technique.release.firecast.FireConsumerPlans;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

/** 一个目标上的燃烧账。预付轨道只播已买的段；共享轨道按段争用调用方给出的余额。 */
public final class BurnLedger {
    private static final AtomicLong NEXT_LAYER = new AtomicLong(1L);

    private final ArrayList<Track> tracks = new ArrayList<>();
    private int droppedOnRestore;

    public int trackCount() {
        return this.tracks.size();
    }

    public int droppedOnRestore() {
        return this.droppedOnRestore;
    }

    public boolean duplicate(UUID castId, UUID targetId, String effectKey) {
        if (castId == null || targetId == null || effectKey == null) {
            return false;
        }
        for (Track track : this.tracks) {
            if (effectKey.equals(track.effectKey) && castId.equals(track.castId) && targetId.equals(track.targetId)) {
                return true;
            }
        }
        return false;
    }

    public static boolean canSeal(BurnSchedule schedule, double furnaceBonus) {
        return seal(schedule, furnaceBonus) != null;
    }

    public boolean admit(BurnSchedule schedule, BurnIdentity identity) {
        if (schedule == null || identity == null) {
            return false;
        }
        List<Double> sealed = seal(schedule, identity.furnaceBonus());
        if (sealed == null || sealed.isEmpty()) {
            return false;
        }
        SharedSpec spec = SharedSpec.of(schedule.intervalTicks(), sealed.size(), FireLimits.STANDARD_SEGMENT_DEMAND,
                sealed.get(0), identity.tiers()).orElse(null);
        if (spec == null) {
            return false;
        }
        return this.admitShared(schedule.effectKey(), spec, identity.castId(), identity.casterId(), identity.targetId(),
                identity.amount(), identity.remainder(), identity.furnaceBonus(), 0L);
    }

    public boolean admitShared(String effectKey, SharedSpec spec, UUID castId, UUID casterId, UUID targetId,
                               int observedBalance, int observedRemainder, double furnaceBonus, long layerIdentity) {
        return this.acceptShared(effectKey, spec, castId, casterId, targetId, observedBalance, observedRemainder,
                furnaceBonus, layerIdentity, false, false);
    }

    /** 与 admitShared 相同，但到期分账的余数走轮换。旧调用不进入这条路径。 */
    public boolean admitSharedRotating(String effectKey, SharedSpec spec, UUID castId, UUID casterId, UUID targetId,
                                       int observedBalance, int observedRemainder, double furnaceBonus,
                                       long layerIdentity) {
        return this.acceptShared(effectKey, spec, castId, casterId, targetId, observedBalance, observedRemainder,
                furnaceBonus, layerIdentity, false, true);
    }

    /**
     * 燎原当拍首段。等待记 0，供 {@link #settleOpening} 单独结算。
     * 默认 {@link #admitShared} 仍从完整间隔开始等，不改其他术式的首段延迟。
     */
    public boolean admitSharedOpening(String effectKey, SharedSpec spec, UUID castId, UUID casterId, UUID targetId,
                                      int observedBalance, int observedRemainder, double furnaceBonus, long layerIdentity) {
        return this.acceptShared(effectKey, spec, castId, casterId, targetId, observedBalance, observedRemainder,
                furnaceBonus, layerIdentity, true, false);
    }

    private boolean acceptShared(String effectKey, SharedSpec spec, UUID castId, UUID casterId, UUID targetId,
                                 int observedBalance, int observedRemainder, double furnaceBonus, long layerIdentity,
                                 boolean openingDue, boolean rotateRemainder) {
        if (spec == null || !BurnSchedule.validKey(effectKey) || castId == null || targetId == null) {
            return false;
        }
        if (!Double.isFinite(furnaceBonus) || furnaceBonus < 0.0d) {
            return false;
        }
        if (observedBalance < 0 || observedBalance > FireLimits.SEED_CAP || observedRemainder < 0) {
            return false;
        }
        if (this.tracks.size() >= FireLimits.PLAN_CAP || this.duplicate(castId, targetId, effectKey)) {
            return false;
        }
        long identity = layerIdentity == 0L ? NEXT_LAYER.getAndIncrement() : layerIdentity;
        int initialWait = openingDue ? 0 : spec.intervalTicks();
        double snapshot = spec.dynamicDemand() ? spec.furnaceSnapshot() : furnaceBonus;
        this.tracks.add(Track.shared(effectKey, castId, casterId, targetId, observedBalance, spec.observedTiers(),
                observedRemainder, snapshot, spec, identity, initialWait, rotateRemainder));
        return true;
    }

    /** 只推进已经预付的旧轨道。共享轨道不在这里白拿伤害。 */
    public List<BurnPulse> advance() {
        ArrayList<BurnPulse> pulses = new ArrayList<>();
        ArrayList<Track> retained = new ArrayList<>();
        for (Track track : this.tracks) {
            if (track.shared) {
                retained.add(track);
                continue;
            }
            if (keepPrepaid(track, pulses)) {
                retained.add(track);
            }
        }
        this.tracks.clear();
        this.tracks.addAll(retained);
        return List.copyOf(pulses);
    }

    public PumpResult pump(int balance, long turn) {
        int working = balance < 0 ? 0 : balance;
        int count = this.tracks.size();
        boolean[] dueShared = new boolean[count];
        int dueCount = 0;
        for (int cursor = 0; cursor < count; cursor++) {
            Track track = this.tracks.get(cursor);
            if (!track.shared || track.ticksUntilNext > 1) {
                continue;
            }
            dueShared[cursor] = true;
            dueCount += 1;
        }
        int[] shareByIndex = new int[count];
        int spent = 0;
        if (dueCount > 0 && working > 0) {
            int[] demands = new int[dueCount];
            long[] identities = new long[dueCount];
            int[] slots = new int[dueCount];
            int fill = 0;
            boolean rotateAll = true;
            for (int cursor = 0; cursor < count; cursor++) {
                if (!dueShared[cursor]) {
                    continue;
                }
                slots[fill] = cursor;
                Track due = this.tracks.get(cursor);
                int demand = due.segmentDemand;
                if (due.dynamicDemand) {
                    demand = FireConsumerPlans.skyDemand(working, due.segmentLimit - due.segmentCursor);
                }
                demands[fill] = demand;
                identities[fill] = this.tracks.get(cursor).layerIdentity;
                if (!this.tracks.get(cursor).rotateRemainder) {
                    rotateAll = false;
                }
                fill += 1;
            }
            int[] shares = rotateAll
                    ? RemainderRotation.share(working, demands, identities, turn)
                    : FuelShare.divide(working, demands, identities, turn);
            for (int cursor = 0; cursor < dueCount; cursor++) {
                shareByIndex[slots[cursor]] = shares[cursor];
                spent += shares[cursor];
            }
            working -= spent;
        }
        boolean fuelGone = working <= 0;
        ArrayList<BurnPulse> pulses = new ArrayList<>();
        ArrayList<Track> retained = new ArrayList<>();
        for (int cursor = 0; cursor < count; cursor++) {
            Track track = this.tracks.get(cursor);
            if (!track.shared) {
                if (keepPrepaid(track, pulses)) {
                    retained.add(track);
                }
                continue;
            }
            if (track.dynamicDemand) {
                if (!dueShared[cursor]) {
                    track.ticksUntilNext -= 1;
                    retained.add(track);
                    continue;
                }
                int share = shareByIndex[cursor];
                double scaled = FireConsumerPlans.skyDamage(working + spent, share, track.furnaceBonus);
                if (FireLayer.acceptableSettled(scaled)) {
                    pulses.add(new BurnPulse(track.effectKey, track.casterId, track.castId, scaled,
                            track.segmentCursor, track.segmentLimit));
                }
                track.segmentCursor += 1;
                if (track.segmentCursor < track.segmentLimit) {
                    track.ticksUntilNext = track.intervalTicks;
                    retained.add(track);
                }
                continue;
            }
            if (fuelGone && !dueShared[cursor]) {
                continue;
            }
            if (!dueShared[cursor]) {
                track.ticksUntilNext -= 1;
                retained.add(track);
                continue;
            }
            int share = shareByIndex[cursor];
            if (share <= 0) {
                if (working > 0) {
                    track.ticksUntilNext = track.intervalTicks;
                    retained.add(track);
                }
                continue;
            }
            double scaled = track.fullSegmentDamage * (double) share / (double) track.segmentDemand;
            if (FireLayer.acceptableSettled(scaled)) {
                pulses.add(new BurnPulse(track.effectKey, track.casterId, track.castId, scaled, track.segmentCursor,
                        track.segmentLimit));
            }
            track.segmentCursor += 1;
            boolean finished = track.segmentCursor >= track.segmentLimit || working <= 0;
            if (!finished) {
                track.ticksUntilNext = track.intervalTicks;
                retained.add(track);
            }
        }
        this.tracks.clear();
        this.tracks.addAll(retained);
        return new PumpResult(working, spent, pulses);
    }

    /**
     * 只结算等待为 0 的共享层，并走同一次 {@link FuelShare#divide}。
     * 不递减其他轨道，也不推进预付轨道。没有这种层时返回 null，账本不动。
     * 余额被本批花尽时，其余未到期共享层按既有 pump 的耗尽规则丢弃。
     */
    public PumpResult settleOpening(int balance, long turn) {
        int working = balance < 0 ? 0 : balance;
        int count = this.tracks.size();
        boolean[] openingDue = new boolean[count];
        int dueCount = 0;
        for (int cursor = 0; cursor < count; cursor++) {
            Track track = this.tracks.get(cursor);
            if (track.shared && track.ticksUntilNext == 0) {
                openingDue[cursor] = true;
                dueCount += 1;
            }
        }
        if (dueCount == 0) {
            return null;
        }
        int[] shareByIndex = new int[count];
        int spent = 0;
        if (working > 0) {
            int[] demands = new int[dueCount];
            long[] identities = new long[dueCount];
            int[] slots = new int[dueCount];
            int fill = 0;
            for (int cursor = 0; cursor < count; cursor++) {
                if (!openingDue[cursor]) {
                    continue;
                }
                slots[fill] = cursor;
                Track due = this.tracks.get(cursor);
                int demand = due.segmentDemand;
                if (due.dynamicDemand) {
                    demand = FireConsumerPlans.skyDemand(working, due.segmentLimit - due.segmentCursor);
                }
                demands[fill] = demand;
                identities[fill] = this.tracks.get(cursor).layerIdentity;
                fill += 1;
            }
            int[] shares = FuelShare.divide(working, demands, identities, turn);
            for (int cursor = 0; cursor < dueCount; cursor++) {
                shareByIndex[slots[cursor]] = shares[cursor];
                spent += shares[cursor];
            }
            working -= spent;
        }
        boolean fuelGone = working <= 0;
        ArrayList<BurnPulse> pulses = new ArrayList<>();
        ArrayList<Track> retained = new ArrayList<>();
        for (int cursor = 0; cursor < count; cursor++) {
            Track track = this.tracks.get(cursor);
            if (!track.shared) {
                retained.add(track);
                continue;
            }
            if (track.dynamicDemand) {
                if (!openingDue[cursor]) {
                    retained.add(track);
                    continue;
                }
                int share = shareByIndex[cursor];
                double scaled = FireConsumerPlans.skyDamage(working + spent, share, track.furnaceBonus);
                if (FireLayer.acceptableSettled(scaled)) {
                    pulses.add(new BurnPulse(track.effectKey, track.casterId, track.castId, scaled,
                            track.segmentCursor, track.segmentLimit));
                }
                track.segmentCursor += 1;
                if (track.segmentCursor < track.segmentLimit) {
                    track.ticksUntilNext = track.intervalTicks;
                    retained.add(track);
                }
                continue;
            }
            if (!openingDue[cursor]) {
                if (!fuelGone) {
                    retained.add(track);
                }
                continue;
            }
            int share = shareByIndex[cursor];
            if (share <= 0) {
                if (working > 0) {
                    track.ticksUntilNext = track.intervalTicks;
                    retained.add(track);
                }
                continue;
            }
            double scaled = track.fullSegmentDamage * (double) share / (double) track.segmentDemand;
            if (FireLayer.acceptableSettled(scaled)) {
                pulses.add(new BurnPulse(track.effectKey, track.casterId, track.castId, scaled, track.segmentCursor,
                        track.segmentLimit));
            }
            track.segmentCursor += 1;
            boolean finished = track.segmentCursor >= track.segmentLimit || working <= 0;
            if (!finished) {
                track.ticksUntilNext = track.intervalTicks;
                retained.add(track);
            }
        }
        this.tracks.clear();
        this.tracks.addAll(retained);
        return new PumpResult(working, spent, pulses);
    }

    public void clear() {
        this.tracks.clear();
    }

    public List<BurnTrackState> export() {
        ArrayList<BurnTrackState> states = new ArrayList<>();
        for (Track track : this.tracks) {
            states.add(track.export());
        }
        return List.copyOf(states);
    }

    public static BurnLedger restore(List<BurnTrackState> stored) {
        BurnLedger ledger = new BurnLedger();
        if (stored == null || stored.isEmpty()) {
            return ledger;
        }
        int seen = 0;
        for (BurnTrackState state : stored) {
            seen += 1;
            if (seen > FireLimits.PLAN_CAP) {
                ledger.droppedOnRestore += stored.size() - FireLimits.PLAN_CAP;
                break;
            }
            if (ledger.tracks.size() >= FireLimits.PLAN_CAP) {
                ledger.droppedOnRestore += 1;
                continue;
            }
            Track track = Track.restore(state);
            if (track == null || ledger.duplicate(track.castId, track.targetId, track.effectKey)) {
                ledger.droppedOnRestore += 1;
                continue;
            }
            ledger.tracks.add(track);
        }
        return ledger;
    }

    private static boolean keepPrepaid(Track track, List<BurnPulse> pulses) {
        if (track.ticksUntilNext > 1) {
            track.ticksUntilNext -= 1;
            return true;
        }
        if (track.segmentCursor < 0 || track.segmentCursor >= track.damages.size()) {
            return false;
        }
        double damage = track.damages.get(track.segmentCursor);
        int ordinal = track.segmentCursor;
        track.segmentCursor += 1;
        pulses.add(new BurnPulse(track.effectKey, track.casterId, track.castId, damage, ordinal, track.damages.size()));
        if (track.segmentCursor < track.damages.size()) {
            track.ticksUntilNext = track.intervalTicks;
            return true;
        }
        return false;
    }

    private static List<Double> seal(BurnSchedule schedule, double furnaceBonus) {
        if (schedule == null || !identityUsableFurnace(furnaceBonus)) {
            return null;
        }
        ArrayList<Double> sealed = new ArrayList<>();
        for (BurnComponent component : schedule.components()) {
            double damage = FireLayer.combine(component.base(), component.resourceAboveOne(), furnaceBonus);
            if (!FireLayer.acceptableDamage(damage)) {
                return null;
            }
            sealed.add(damage);
        }
        if (sealed.isEmpty() || sealed.size() > FireLimits.SEGMENT_CAP) {
            return null;
        }
        return List.copyOf(sealed);
    }

    private static boolean identityUsableFurnace(double furnaceBonus) {
        return Double.isFinite(furnaceBonus) && furnaceBonus >= 0.0d;
    }

    private static final class Track {
        private final boolean shared;
        private final String effectKey;
        private final boolean targetExclusive;
        private final UUID castId;
        private final UUID casterId;
        private final UUID targetId;
        private final int amount;
        private final int tiers;
        private final int remainder;
        private final double furnaceBonus;
        private final int intervalTicks;
        private final List<Double> damages;
        private final int segmentDemand;
        private final double fullSegmentDamage;
        private final int segmentLimit;
        private final long layerIdentity;
        private final boolean rotateRemainder;
        private final boolean dynamicDemand;
        private int ticksUntilNext;
        private int segmentCursor;

        private Track(boolean shared, String effectKey, boolean targetExclusive, UUID castId, UUID casterId,
                      UUID targetId, int amount, int tiers, int remainder, double furnaceBonus, int intervalTicks,
                      int ticksUntilNext, int segmentCursor, List<Double> damages, int segmentDemand,
                      double fullSegmentDamage, int segmentLimit, long layerIdentity, boolean rotateRemainder,
                      boolean dynamicDemand) {
            this.shared = shared;
            this.effectKey = effectKey;
            this.targetExclusive = targetExclusive;
            this.castId = castId;
            this.casterId = casterId;
            this.targetId = targetId;
            this.amount = amount;
            this.tiers = tiers;
            this.remainder = remainder;
            this.furnaceBonus = furnaceBonus;
            this.intervalTicks = intervalTicks;
            this.ticksUntilNext = ticksUntilNext;
            this.segmentCursor = segmentCursor;
            this.damages = damages;
            this.segmentDemand = segmentDemand;
            this.fullSegmentDamage = fullSegmentDamage;
            this.segmentLimit = segmentLimit;
            this.layerIdentity = layerIdentity;
            this.rotateRemainder = rotateRemainder;
            this.dynamicDemand = dynamicDemand;
        }

        private static Track shared(String effectKey, UUID castId, UUID casterId, UUID targetId, int observedBalance,
                                    int observedTiers, int observedRemainder, double furnaceBonus, SharedSpec spec,
                                    long layerIdentity, int initialWait, boolean rotateRemainder) {
            return new Track(true, effectKey, false, castId, casterId, targetId, observedBalance, observedTiers,
                    observedRemainder, furnaceBonus, spec.intervalTicks(), initialWait, 0, List.of(),
                    spec.segmentDemand(), spec.fullSegmentDamage(), spec.segmentLimit(), layerIdentity,
                    rotateRemainder, spec.dynamicDemand());
        }

        private static Track restore(BurnTrackState state) {
            if (state == null || !BurnSchedule.validKey(state.effectKey()) || state.castId() == null
                    || state.targetId() == null) {
                return null;
            }
            if (state.sharedFuel()) {
                return restoreShared(state);
            }
            return restorePrepaid(state);
        }

        private static Track restorePrepaid(BurnTrackState state) {
            if (state.intervalTicks() < FireLimits.INTERVAL_FLOOR || state.intervalTicks() > FireLimits.INTERVAL_CAP) {
                return null;
            }
            if (state.amount() <= 0 || state.amount() > FireLimits.SEED_CAP || state.tiers() < 0
                    || state.remainder() < 0) {
                return null;
            }
            if (state.tiers() != state.amount() / 10 || state.remainder() != state.amount() - state.tiers() * 10) {
                return null;
            }
            if (!Double.isFinite(state.furnaceBonus()) || state.furnaceBonus() < 0.0d) {
                return null;
            }
            List<Double> damages = state.damages();
            if (damages.isEmpty() || damages.size() > FireLimits.SEGMENT_CAP) {
                return null;
            }
            if (state.segmentCursor() < 0 || state.segmentCursor() >= damages.size()) {
                return null;
            }
            ArrayList<Double> accepted = new ArrayList<>();
            for (Double damage : damages) {
                if (damage == null || !FireLayer.acceptableSettled(damage)) {
                    return null;
                }
                accepted.add(damage);
            }
            int wait = state.ticksUntilNext();
            if (wait < 0 || wait > state.intervalTicks()) {
                wait = state.intervalTicks();
            }
            return new Track(false, state.effectKey(), state.targetExclusive(), state.castId(), state.casterId(),
                    state.targetId(), state.amount(), state.tiers(), state.remainder(), state.furnaceBonus(),
                    state.intervalTicks(), wait, state.segmentCursor(), List.copyOf(accepted), 0, 0.0d,
                    accepted.size(), 0L, false, false);
        }

        private static Track restoreShared(BurnTrackState state) {
            if (state.intervalTicks() < FireLimits.INTERVAL_FLOOR || state.intervalTicks() > FireLimits.INTERVAL_CAP) {
                return null;
            }
            boolean dynamic = state.dynamicDemand();
            if (dynamic) {
                if (state.segmentDemand() != 0 || state.fullSegmentDamage() != 0.0d) {
                    return null;
                }
            } else if (state.segmentDemand() <= 0 || state.segmentDemand() > FireLimits.DEMAND_CAP) {
                return null;
            }
            if (state.segmentLimit() <= 0 || state.segmentLimit() > FireLimits.SEGMENT_CAP) {
                return null;
            }
            if (state.segmentCursor() < 0 || state.segmentCursor() >= state.segmentLimit()) {
                return null;
            }
            if (!dynamic && !FireLayer.acceptableSettled(state.fullSegmentDamage())) {
                return null;
            }
            if (state.tiers() < 0 || state.tiers() > FireLimits.TIER_CAP) {
                return null;
            }
            if (!Double.isFinite(state.furnaceBonus()) || state.furnaceBonus() < 0.0d) {
                return null;
            }
            int wait = state.ticksUntilNext();
            if (wait < 0 || wait > state.intervalTicks()) {
                wait = state.intervalTicks();
            }
            return new Track(true, state.effectKey(), false, state.castId(), state.casterId(), state.targetId(),
                    state.amount(), state.tiers(), state.remainder(), state.furnaceBonus(), state.intervalTicks(),
                    wait, state.segmentCursor(), List.of(), state.segmentDemand(), state.fullSegmentDamage(),
                    state.segmentLimit(), state.layerIdentity(),
                    ResourceEffectKeys.KINDLE.equals(state.effectKey()), dynamic);
        }

        private BurnTrackState export() {
            if (!this.shared) {
                return new BurnTrackState(this.effectKey, this.targetExclusive, this.castId, this.casterId, this.targetId,
                        this.amount, this.tiers, this.remainder, this.furnaceBonus, this.intervalTicks,
                        this.ticksUntilNext, this.segmentCursor, this.damages);
            }
            ArrayList<Double> shown = new ArrayList<>();
            if (!this.dynamicDemand) {
                for (int cursor = 0; cursor < this.segmentLimit; cursor++) {
                    shown.add(this.fullSegmentDamage);
                }
            }
            return new BurnTrackState(this.effectKey, false, this.castId, this.casterId, this.targetId, this.amount,
                    this.tiers, this.remainder, this.furnaceBonus, this.intervalTicks, this.ticksUntilNext,
                    this.segmentCursor, shown, FuelMode.SHARED, this.segmentDemand, this.fullSegmentDamage,
                    this.segmentLimit, this.layerIdentity, this.dynamicDemand ? SharedSpec.DYNAMIC_CEILING
                    : SharedSpec.FIXED_FULL, this.furnaceBonus);
        }
    }
}

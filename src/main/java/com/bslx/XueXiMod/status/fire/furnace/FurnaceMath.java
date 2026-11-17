package com.bslx.XueXiMod.status.fire.furnace;

/**
 * 炉心纯数值。热量用千分位整数进出，避免 0.002 连加漂移，也避免把离线间隔乘进热量。
 */
public final class FurnaceMath {
    public static final int CURRENT_VERSION = 1;
    public static final double HEAT_UNIT = 0.001D;
    public static final int NORMAL_RATE_UNITS = 2;
    public static final int OVERDRIVE_RATE_UNITS = 3;
    public static final int NORMAL_CEILING_UNITS = 300;
    public static final int OVERDRIVE_CEILING_UNITS = 600;
    public static final int NORMAL_TICKS_TO_CEILING = 150;
    public static final int OVERDRIVE_TICKS_TO_CEILING = 200;
    public static final int UPGRADE_TICKS_TO_CEILING = 100;

    public enum Tier {
        NORMAL,
        OVERDRIVE
    }

    public enum Action {
        ENTER_NORMAL,
        ENTER_OVERDRIVE,
        END,
        COOL
    }

    public enum Outcome {
        ENTERED_NORMAL,
        ENTERED_OVERDRIVE,
        UPGRADED,
        ENDED,
        COOLED,
        REJECT_ALREADY_NORMAL,
        REJECT_ALREADY_OVERDRIVE,
        REJECT_CANNOT_DOWNGRADE,
        REJECT_INACTIVE,
        REJECT_NULL,
        REJECT_CLIENT,
        REJECT_BLOCKED,
        REJECT_THREAD;

        public boolean succeeded() {
            return this == ENTERED_NORMAL
                    || this == ENTERED_OVERDRIVE
                    || this == UPGRADED
                    || this == ENDED
                    || this == COOLED;
        }
    }

    public static boolean allowsServerWork(boolean serverPresent, boolean sameThread) {
        return serverPresent && sameThread;
    }

    public static final class Transition {
        private final Outcome outcome;
        private final Tier tier;
        private final double bonus;
        private final int lastTick;

        public Transition(Outcome outcome, Tier tier, double bonus, int lastTick) {
            this.outcome = outcome;
            this.tier = tier;
            this.bonus = bonus;
            this.lastTick = lastTick;
        }

        public Outcome outcome() {
            return this.outcome;
        }

        public Tier tier() {
            return this.tier;
        }

        public double bonus() {
            return this.bonus;
        }

        public int lastTick() {
            return this.lastTick;
        }
    }

    public static final class Sanitized {
        private final double bonus;
        private final int amplifier;
        private final boolean zeroedBonus;
        private final boolean repairedAmplifier;
        private final boolean rejectedVersion;

        public Sanitized(double bonus, int amplifier, boolean zeroedBonus, boolean repairedAmplifier,
                         boolean rejectedVersion) {
            this.bonus = bonus;
            this.amplifier = amplifier;
            this.zeroedBonus = zeroedBonus;
            this.repairedAmplifier = repairedAmplifier;
            this.rejectedVersion = rejectedVersion;
        }

        public double bonus() {
            return this.bonus;
        }

        public int amplifier() {
            return this.amplifier;
        }

        public boolean zeroedBonus() {
            return this.zeroedBonus;
        }

        public boolean repairedAmplifier() {
            return this.repairedAmplifier;
        }

        public boolean rejectedVersion() {
            return this.rejectedVersion;
        }
    }

    private FurnaceMath() {
    }

    /** 不是正数收成普通档，其余收成过载档。 */
    public static int legalAmplifier(int amplifier) {
        return amplifier <= 0 ? 0 : 1;
    }

    /** 非正放大器是普通，其余是过载。 */
    public static Tier tierOfAmplifier(int amplifier) {
        return amplifier <= 0 ? Tier.NORMAL : Tier.OVERDRIVE;
    }

    /** 这一档的热量上限，单位是千分位。 */
    public static int ceilingUnits(Tier tier) {
        return tier == Tier.OVERDRIVE ? OVERDRIVE_CEILING_UNITS : NORMAL_CEILING_UNITS;
    }

    /** 这一档在线每拍增加的千分位。 */
    public static int rateUnits(Tier tier) {
        return tier == Tier.OVERDRIVE ? OVERDRIVE_RATE_UNITS : NORMAL_RATE_UNITS;
    }

    /** 这一档的热量上限。 */
    public static double ceiling(Tier tier) {
        return fromUnits(ceilingUnits(tier));
    }

    /** 这一档在线每拍增加的热量。 */
    public static double rate(Tier tier) {
        return fromUnits(rateUnits(tier));
    }

    /** 千分位换成热量。 */
    public static double fromUnits(int heatUnits) {
        return heatUnits / 1000.0D;
    }

    /** 热量换成千分位。非有限或非正数是 0，超过过载上限的结果另有更大夹限。 */
    public static int toUnits(double bonus) {
        if (!Double.isFinite(bonus) || bonus <= 0.0D) {
            return 0;
        }
        long scaled = Math.round(bonus / HEAT_UNIT);
        if (scaled >= OVERDRIVE_CEILING_UNITS) {
            return (int) Math.min(scaled, 1_000_000L);
        }
        return (int) scaled;
    }

    /** 版本损坏、非有限值或负数归零；高于当前档上限只钳制。 */
    public static Sanitized sanitize(int version, int amplifier, double bonus) {
        boolean rejectedVersion = version != CURRENT_VERSION;
        boolean brokenBonus = !Double.isFinite(bonus) || bonus < 0.0D;
        boolean zeroedBonus = rejectedVersion || brokenBonus;
        int legal = legalAmplifier(amplifier);
        boolean repairedAmplifier = amplifier != legal;
        int heatUnits = 0;
        if (!zeroedBonus) {
            heatUnits = toUnits(bonus);
            int ceilingUnits = ceilingUnits(tierOfAmplifier(legal));
            if (heatUnits > ceilingUnits) {
                heatUnits = ceilingUnits;
            }
        }
        return new Sanitized(fromUnits(heatUnits), legal, zeroedBonus, repairedAmplifier, rejectedVersion);
    }

    public static double step(double bonus, Tier tier) {
        if (tier == null) {
            return 0.0D;
        }
        int heatUnits = toUnits(bonus);
        int ceilingUnits = ceilingUnits(tier);
        if (heatUnits >= ceilingUnits) {
            return fromUnits(ceilingUnits);
        }
        int nextUnits = heatUnits + rateUnits(tier);
        if (nextUnits > ceilingUnits) {
            nextUnits = ceilingUnits;
        }
        return fromUnits(nextUnits);
    }

    /**
     * 在线只走一拍。elapsedTicks 大于 1 仍只加一拍，小于等于 0 不涨。
     * 登录重设时基之后，离线间隔不会被乘进来。
     */
    public static double advanceOnline(double bonus, Tier tier, long elapsedTicks) {
        if (tier == null || elapsedTicks <= 0L) {
            return bonus;
        }
        return step(bonus, tier);
    }

    /** 没有炉心时产种倍率为 1。过载固定 2.2；普通按热量从 1.12 走到 1.60。 */
    public static double seedMultiplier(Tier tier, double bonus) {
        if (tier == null) {
            return 1.0D;
        }
        if (tier == Tier.OVERDRIVE) {
            return 2.2D;
        }
        double ratio = toUnits(bonus) / (double) NORMAL_CEILING_UNITS;
        if (ratio < 0.0D) {
            ratio = 0.0D;
        }
        if (ratio > 1.0D) {
            ratio = 1.0D;
        }
        return 1.12D + 0.48D * ratio;
    }

    /** 没有炉心时水冰承伤倍率为 1。普通 1.4，过载 1.7。 */
    public static double waterIceMultiplier(Tier tier) {
        if (tier == null) {
            return 1.0D;
        }
        if (tier == Tier.OVERDRIVE) {
            return 1.7D;
        }
        return 1.4D;
    }

    /** 普通治疗保留一半，过载保留三成。没有炉心时不改。 */
    public static double healFactor(Tier tier) {
        if (tier == null) {
            return 1.0D;
        }
        if (tier == Tier.OVERDRIVE) {
            return 0.3D;
        }
        return 0.5D;
    }

    /** 普通生命上限保留 0.75，过载保留 0.6。 */
    public static double healthFactor(Tier tier) {
        return tier == Tier.OVERDRIVE ? 0.6D : 0.75D;
    }

    /** 普通护甲保留一半，过载保留四分之一。 */
    public static double armorFactor(Tier tier) {
        return tier == Tier.OVERDRIVE ? 0.25D : 0.5D;
    }

    /** 韧性保留比例和护甲相同。 */
    public static double toughnessFactor(Tier tier) {
        return armorFactor(tier);
    }

    /** 把保留比例换成乘法总量修饰器的增量。 */
    public static double totalModifier(double factor) {
        return factor - 1.0D;
    }

    /** 存档主人标识和当前玩家不同。任一标识为空时不判成别人的存档。 */
    public static boolean foreignOwner(String storedUuid, String playerUuid) {
        if (storedUuid == null || storedUuid.isEmpty() || playerUuid == null || playerUuid.isEmpty()) {
            return false;
        }
        return !storedUuid.equals(playerUuid);
    }

    /** 结束和冷却要求已经有炉心。普通不能降档；普通进过载保留热量，直接进过载从 0 开始。 */
    public static Transition transition(Tier current, double bonus, int lastTick, Action action) {
        if (action == Action.END || action == Action.COOL) {
            if (current == null) {
                return new Transition(Outcome.REJECT_INACTIVE, null, bonus, lastTick);
            }
            Outcome outcome = action == Action.COOL ? Outcome.COOLED : Outcome.ENDED;
            return new Transition(outcome, null, 0.0D, 0);
        }
        if (action == Action.ENTER_NORMAL) {
            if (current == Tier.OVERDRIVE) {
                return new Transition(Outcome.REJECT_CANNOT_DOWNGRADE, current, bonus, lastTick);
            }
            if (current == Tier.NORMAL) {
                return new Transition(Outcome.REJECT_ALREADY_NORMAL, current, bonus, lastTick);
            }
            return new Transition(Outcome.ENTERED_NORMAL, Tier.NORMAL, 0.0D, lastTick);
        }
        if (current == Tier.OVERDRIVE) {
            return new Transition(Outcome.REJECT_ALREADY_OVERDRIVE, current, bonus, lastTick);
        }
        if (current == Tier.NORMAL) {
            return new Transition(Outcome.UPGRADED, Tier.OVERDRIVE, bonus, lastTick);
        }
        return new Transition(Outcome.ENTERED_OVERDRIVE, Tier.OVERDRIVE, 0.0D, lastTick);
    }
}

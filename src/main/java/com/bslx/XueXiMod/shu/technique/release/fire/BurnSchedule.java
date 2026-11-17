package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

/** 不可变燃烧计划。不记录术式编号，只记录效果键、排他、间隔和分量。 */
public final class BurnSchedule {
    private static final Pattern KEY = Pattern.compile("[a-z0-9_.:\\-]{1," + FireLimits.KEY_CAP + "}");

    private final String effectKey;
    private final boolean targetExclusive;
    private final int intervalTicks;
    private final List<BurnComponent> components;

    private BurnSchedule(String effectKey, boolean targetExclusive, int intervalTicks, List<BurnComponent> components) {
        this.effectKey = effectKey;
        this.targetExclusive = targetExclusive;
        this.intervalTicks = intervalTicks;
        this.components = components;
    }

    public static boolean validKey(String effectKey) {
        return effectKey != null && KEY.matcher(effectKey).matches();
    }

    public static Optional<BurnSchedule> of(String effectKey, boolean targetExclusive, int intervalTicks,
                                            List<BurnComponent> components) {
        Optional<SegmentPlan> plan = SegmentPlan.of(intervalTicks, components);
        if (!validKey(effectKey) || plan.isEmpty()) {
            return Optional.empty();
        }
        SegmentPlan accepted = plan.get();
        return Optional.of(new BurnSchedule(effectKey, targetExclusive, accepted.intervalTicks(), accepted.components()));
    }

    public String effectKey() {
        return this.effectKey;
    }

    public boolean targetExclusive() {
        return this.targetExclusive;
    }

    public int intervalTicks() {
        return this.intervalTicks;
    }

    public List<BurnComponent> components() {
        return this.components;
    }
}

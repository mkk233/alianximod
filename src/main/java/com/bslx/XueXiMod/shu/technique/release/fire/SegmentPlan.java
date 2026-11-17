package com.bslx.XueXiMod.shu.technique.release.fire;

import java.util.List;
import java.util.Optional;

/** 与快照档数相关的间隔和分量。效果键由调用方另给，便于先做冲突预检。 */
public final class SegmentPlan {
    private final int intervalTicks;
    private final List<BurnComponent> components;

    private SegmentPlan(int intervalTicks, List<BurnComponent> components) {
        this.intervalTicks = intervalTicks;
        this.components = components;
    }

    public static Optional<SegmentPlan> of(int intervalTicks, List<BurnComponent> components) {
        if (intervalTicks < FireLimits.INTERVAL_FLOOR || intervalTicks > FireLimits.INTERVAL_CAP) {
            return Optional.empty();
        }
        if (components == null || components.isEmpty() || components.size() > FireLimits.SEGMENT_CAP) {
            return Optional.empty();
        }
        for (BurnComponent component : components) {
            if (component == null) {
                return Optional.empty();
            }
        }
        return Optional.of(new SegmentPlan(intervalTicks, List.copyOf(components)));
    }

    public int intervalTicks() {
        return this.intervalTicks;
    }

    public List<BurnComponent> components() {
        return this.components;
    }
}

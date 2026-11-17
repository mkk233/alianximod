package com.bslx.XueXiMod.shu.technique.release.batch;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** 环带用的中心排除名单。名单外的目标仍按自己的命中判断。 */
public final class ImpactPartition {
    private final Set<UUID> excludedTargets;

    private ImpactPartition(Set<UUID> excludedTargets) {
        this.excludedTargets = excludedTargets;
    }

    public static ImpactPartition none() {
        return new ImpactPartition(Set.of());
    }

    /** 排除这一个中心目标。 */
    public static ImpactPartition excluding(UUID targetId) {
        if (targetId == null) {
            return none();
        }
        return new ImpactPartition(Set.of(targetId));
    }

    /** 排除这一组中心目标。 */
    public static ImpactPartition excludingAll(Set<UUID> targetIds) {
        if (targetIds == null || targetIds.isEmpty()) {
            return none();
        }
        return new ImpactPartition(Set.copyOf(targetIds));
    }

    /** 这个目标是否在环带中心排除名单里。 */
    public boolean excludes(UUID targetId) {
        return targetId != null && excludedTargets.contains(targetId);
    }

    public Set<UUID> excludedTargets() {
        return Collections.unmodifiableSet(new HashSet<>(excludedTargets));
    }
}

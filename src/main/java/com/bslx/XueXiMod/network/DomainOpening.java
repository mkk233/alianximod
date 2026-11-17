package com.bslx.XueXiMod.network;

import com.bslx.XueXiMod.shu.technique.release.bind.BindingLayout;
import com.bslx.XueXiMod.shu.technique.release.bind.BoundReleasePlan;

import java.util.List;
import java.util.UUID;

/** 同一次按键共用的捕获布局、投影步下标和声明后缀。 */
final class DomainOpening {
    private final BindingLayout layout;
    private final List<Integer> projectedStepIndexes;
    private final UUID releaseIdentity;

    DomainOpening(BindingLayout layout, List<Integer> projectedStepIndexes, UUID releaseIdentity) {
        this.layout = layout;
        this.projectedStepIndexes = projectedStepIndexes == null ? List.of() : List.copyOf(projectedStepIndexes);
        this.releaseIdentity = releaseIdentity;
    }

    UUID releaseIdentity() {
        return releaseIdentity;
    }

    String laneToken() {
        return this.releaseIdentity == null ? "none" : this.releaseIdentity.toString();
    }

    BoundReleasePlan planFor(int localActionIndex) {
        if (layout == null || layout.failed()) {
            return null;
        }
        int originalIndex = layout.originalFromStep(projectedStepIndexes, localActionIndex);
        if (originalIndex < 0) {
            return null;
        }
        return layout.planAtDomain(originalIndex);
    }

    String claimFor(BoundReleasePlan plan) {
        if (plan == null || releaseIdentity == null) {
            return null;
        }
        return "bind:" + plan.originalDomainIndex() + ":" + releaseIdentity;
    }
}

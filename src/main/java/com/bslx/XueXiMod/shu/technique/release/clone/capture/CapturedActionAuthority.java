package com.bslx.XueXiMod.shu.technique.release.clone.capture;

import java.util.Objects;
import java.util.UUID;

/**
 * 一张已经转到行动阶段的部署授权。没有实体，也没有租约对象。
 */
public record CapturedActionAuthority(
        long sessionToken,
        UUID ownerId,
        UUID rootEventId,
        UUID deploymentId,
        long actionToken,
        int adapterVersion) {

    public CapturedActionAuthority {
        Objects.requireNonNull(ownerId, "ownerId");
        Objects.requireNonNull(rootEventId, "rootEventId");
        Objects.requireNonNull(deploymentId, "deploymentId");
    }
}

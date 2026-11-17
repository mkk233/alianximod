package com.bslx.XueXiMod.shu.technique.release.clone.core;

import java.util.UUID;

final class CapturedDeployment {
    final DeploymentLease lease;
    final UUID actorId;
    final CloneRuntimeState body;
    boolean detachedHeld;

    CapturedDeployment(DeploymentLease lease, CloneRuntimeState body) {
        this.lease = lease;
        this.actorId = body.actorId;
        this.body = body;
    }
}

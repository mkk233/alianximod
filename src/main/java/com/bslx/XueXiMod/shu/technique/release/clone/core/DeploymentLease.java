package com.bslx.XueXiMod.shu.technique.release.clone.core;

import java.util.UUID;

public final class DeploymentLease {
    private final UUID deploymentId;
    private final UUID ownerId;
    private final long sessionToken;
    private Phase phase;
    private boolean released;

    DeploymentLease(UUID deploymentId, UUID ownerId, long sessionToken) {
        if (deploymentId == null || ownerId == null || sessionToken == 0L) {
            throw new IllegalArgumentException("部署租约不完整");
        }
        this.deploymentId = deploymentId;
        this.ownerId = ownerId;
        this.sessionToken = sessionToken;
        this.phase = Phase.BODY;
    }

    public UUID deploymentId() {
        return this.deploymentId;
    }

    public UUID ownerId() {
        return this.ownerId;
    }

    public long sessionToken() {
        return this.sessionToken;
    }

    public Phase phase() {
        return this.phase;
    }

    public boolean released() {
        return this.released;
    }

    boolean transferToTicket() {
        if (this.released || this.phase != Phase.BODY) {
            return false;
        }
        this.phase = Phase.TICKET;
        return true;
    }

    boolean transferToVolume() {
        if (this.released || this.phase != Phase.TICKET) {
            return false;
        }
        this.phase = Phase.VOLUME;
        return true;
    }

    boolean release() {
        if (this.released) {
            return false;
        }
        this.released = true;
        this.phase = Phase.RELEASED;
        return true;
    }

    public enum Phase {
        BODY,
        TICKET,
        VOLUME,
        RELEASED
    }
}

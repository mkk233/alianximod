package com.bslx.XueXiMod.shu.technique.release.clone.core;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

final class HurtAttempt {
    final CloneHandle handle;
    final DeploymentLease lease;
    final Vec3 eyeOrigin;
    final Vec3 lookUnit;
    @Nullable
    final UUID sourceId;
    final long transactionSerial;
    private boolean deferralOpen = true;
    private boolean pendingDeath;
    private boolean pendingKilledRemoval;
    private boolean hardCancelled;

    private HurtAttempt(CloneHandle handle, DeploymentLease lease, Vec3 eyeOrigin, Vec3 lookUnit,
                        @Nullable UUID sourceId, long transactionSerial) {
        this.handle = handle;
        this.lease = lease;
        this.eyeOrigin = eyeOrigin;
        this.lookUnit = lookUnit;
        this.sourceId = sourceId;
        this.transactionSerial = transactionSerial;
    }

    static HurtAttempt open(CloneRuntimeState state, CloneEntity actor, @Nullable UUID sourceId) {
        long serial = state.hurtSerial + 1L;
        state.hurtSerial = serial;
        Vec3 eyes = actor.getEyePosition();
        Vec3 look = state.frozenLook == null ? actor.getLookAngle() : state.frozenLook;
        return new HurtAttempt(state.handle(actor.syncedSessionToken()), state.deploymentLease, eyes, look,
                sourceId, serial);
    }

    boolean deferralOpen() {
        return this.deferralOpen;
    }

    void closeDeferral() {
        this.deferralOpen = false;
    }

    void notePendingDeath() {
        this.pendingDeath = true;
    }

    boolean pendingDeath() {
        return this.pendingDeath;
    }

    void notePendingKilledRemoval() {
        this.pendingKilledRemoval = true;
    }

    boolean pendingKilledRemoval() {
        return this.pendingKilledRemoval;
    }

    void noteHardCancel() {
        this.hardCancelled = true;
    }

    boolean hardCancelled() {
        return this.hardCancelled;
    }
}

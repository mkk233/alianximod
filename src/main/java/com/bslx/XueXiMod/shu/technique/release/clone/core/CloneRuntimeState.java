package com.bslx.XueXiMod.shu.technique.release.clone.core;

import com.bslx.XueXiMod.shu.technique.release.clone.CloneBalance;
import com.bslx.XueXiMod.shu.technique.release.cooperative.api.DeploymentSource;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class CloneRuntimeState {
    final UUID actorId;
    final UUID ownerId;
    final long actorGeneration;
    final CloneSpec spec;
    final ResourceKey<Level> dimension;
    final long spawnedTick;
    final int staggerSlot;
    final List<UUID> leases = new ArrayList<>();

    @Nullable
    CloneEntity actor;
    CloneLifeState life = CloneLifeState.ACTIVE;
    CloneResponseState response = CloneResponseState.READY;
    @Nullable
    CloneActionRequest pending;
    long cooldownReadyTick;
    boolean budgetSpent;
    long actionSequence;
    long occupiedToken;
    boolean executing;
    boolean commitLine;
    boolean cleaning;
    boolean endCounted;
    boolean prepareCancelled;
    long rejectionSerial = Long.MIN_VALUE;
    long hurtSerial;
    @Nullable
    DeploymentLease deploymentLease;
    @Nullable
    CapturedAttackSnapshot capturedSnapshot;
    boolean cosmeticOnly;
    @Nullable
    Vec3 frozenLook;
    @Nullable
    HurtAttempt hurtAttempt;
    boolean leaseTransferred;
    /** 与旧 pending 互斥的协同占用。非空时响应必须是 RESERVED，手动和受击都不能再占这具身体。 */
    @Nullable
    CloneCooperationHold cooperationHold;
    @Nullable
    DeploymentSource deploymentSource;
    /** 部署时冻结的内部响应冷却。协同结束不再重读配置。 */
    int frozenResponseCooldownTicks = CloneBalance.PULSE_COOLDOWN_TICKS;
    @Nullable
    UUID activePlanId;

    CloneRuntimeState(UUID actorId, UUID ownerId, long actorGeneration, CloneSpec spec,
                      ResourceKey<Level> dimension, long spawnedTick, int staggerSlot) {
        this.actorId = actorId;
        this.ownerId = ownerId;
        this.actorGeneration = actorGeneration;
        this.spec = spec;
        this.dimension = dimension;
        this.spawnedTick = spawnedTick;
        this.staggerSlot = staggerSlot;
    }

    public CloneHandle handle(long sessionToken) {
        return new CloneHandle(sessionToken, this.dimension, this.actorId, this.actorGeneration);
    }

    public CloneSpec spec() {
        return this.spec;
    }

    public CloneResponseState response() {
        return this.response;
    }

    public boolean budgetSpent() {
        return this.budgetSpent;
    }

    boolean noteRejection(long serial) {
        if (this.rejectionSerial == serial) {
            return false;
        }
        this.rejectionSerial = serial;
        return true;
    }
}

package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

/** 一次释放会话里的目标占用。未提交时 close 放人；已提交时 close 不破牢。 */
public final class PrisonReservation implements AutoCloseable {
    private final UUID operationId;
    private final ServerPlayer caster;
    private final LivingEntity target;
    private final boolean acquired;
    private final String reason;
    private boolean closed;
    private boolean committed;
    private PrisonCommitResult outcome;

    PrisonReservation(UUID operationId, ServerPlayer caster, LivingEntity target, boolean acquired, String reason) {
        this.operationId = operationId;
        this.caster = caster;
        this.target = target;
        this.acquired = acquired;
        this.reason = reason == null ? "" : reason;
    }

    public boolean acquired() {
        return this.acquired;
    }

    public String reason() {
        return this.reason;
    }

    public PrisonCommitResult commit(EnchantmentSnapshot contact) {
        if (this.outcome != null) {
            return this.outcome;
        }
        if (!this.acquired) {
            this.outcome = PrisonCommitResult.fail(this.reason.isEmpty() ? "rejected" : this.reason);
            return this.outcome;
        }
        if (this.closed) {
            this.outcome = PrisonCommitResult.fail("closed");
            return this.outcome;
        }
        this.outcome = PrisonRuntime.commit(this, contact);
        return this.outcome;
    }

    @Override
    public void close() {
        if (this.closed) {
            return;
        }
        this.closed = true;
        PrisonRuntime.close(this);
    }

    UUID operationId() {
        return this.operationId;
    }

    ServerPlayer caster() {
        return this.caster;
    }

    LivingEntity target() {
        return this.target;
    }

    boolean wasCommitted() {
        return this.committed;
    }

    void markCommitted() {
        this.committed = true;
    }
}

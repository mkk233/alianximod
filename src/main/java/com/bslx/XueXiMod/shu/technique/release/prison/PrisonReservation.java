package com.bslx.XueXiMod.shu.technique.release.prison;

import com.bslx.XueXiMod.shu.technique.enchant.EnchantmentSnapshot;
import com.bslx.XueXiMod.shu.technique.release.ReleaseInput;
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
    private final ReleaseInput releaseInput;
    private boolean closed;
    private boolean committed;
    private PrisonCommitResult outcome;

    PrisonReservation(UUID operationId, ServerPlayer caster, LivingEntity target, boolean acquired, String reason,
                      ReleaseInput releaseInput) {
        this.operationId = operationId;
        this.caster = caster;
        this.target = target;
        this.acquired = acquired;
        this.reason = reason == null ? "" : reason;
        this.releaseInput = releaseInput == null ? ReleaseInput.BASE : releaseInput;
    }

    public ReleaseInput releaseInput() {
        return this.releaseInput;
    }

    public boolean acquired() {
        return this.acquired;
    }

    public String reason() {
        return this.reason;
    }

    public PrisonCommitResult commit(EnchantmentSnapshot contact) {
        return this.commit(contact, this.releaseInput);
    }

    /** 同一预约只提交一次。未占用或已关闭直接失败，不建牢、不退款。 */
    public PrisonCommitResult commit(EnchantmentSnapshot contact, ReleaseInput offeredInput) {
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
        this.outcome = PrisonRuntime.commit(this, contact, offeredInput);
        return this.outcome;
    }

    @Override
    /** 未提交时放掉目标占用。已提交的牢仍然留下。 */
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

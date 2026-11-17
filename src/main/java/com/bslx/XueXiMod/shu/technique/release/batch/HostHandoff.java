package com.bslx.XueXiMod.shu.technique.release.batch;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/**
 * 交给后续资源或载体的根上下文。只含主人、维度、根施法、分支、事件点和方向。
 * 不复制父分支的修饰附魔。
 */
public final class HostHandoff {
    private final UUID rootCastId;
    private final UUID ownerId;
    private final String dimensionId;
    private final int branchOrdinal;
    private final int copyOrdinal;
    private final HitClock hitClock;
    private final Vec3 eventPoint;
    private final Vec3 direction;
    private final boolean childInstance;

    public HostHandoff(UUID rootCastId, ServerPlayer owner, int branchOrdinal, int copyOrdinal, HitClock hitClock,
                       Vec3 eventPoint, Vec3 direction, boolean childInstance) {
        this.rootCastId = rootCastId;
        this.ownerId = owner == null ? null : owner.getUUID();
        this.dimensionId = owner == null || owner.serverLevel() == null
                ? ""
                : owner.serverLevel().dimension().location().toString();
        this.branchOrdinal = branchOrdinal;
        this.copyOrdinal = copyOrdinal;
        this.hitClock = hitClock;
        this.eventPoint = eventPoint;
        this.direction = direction;
        this.childInstance = childInstance;
    }

    public UUID rootCastId() {
        return rootCastId;
    }

    public UUID ownerId() {
        return ownerId;
    }

    public String dimensionId() {
        return dimensionId;
    }

    public int branchOrdinal() {
        return branchOrdinal;
    }

    public int copyOrdinal() {
        return copyOrdinal;
    }

    public HitClock hitClock() {
        return hitClock;
    }

    public Vec3 eventPoint() {
        return eventPoint;
    }

    public Vec3 direction() {
        return direction;
    }

    public boolean childInstance() {
        return childInstance;
    }
}

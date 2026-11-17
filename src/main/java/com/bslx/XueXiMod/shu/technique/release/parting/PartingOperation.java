package com.bslx.XueXiMod.shu.technique.release.parting;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import com.bslx.XueXiMod.shu.technique.release.momentum.WaterMomentum;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 一次分水从预留到提交的状态。复制份登记到同一个操作上。
 */
public final class PartingOperation {
    private final PartingOperationId operationId;
    private final UUID playerId;
    private final ResourceKey<Level> dimension;
    private final WaterMomentum.Hold hold;
    private final List<TechniqueHitEntity> attachments = new ArrayList<>();
    private final Set<UUID> captured = new LinkedHashSet<>();
    private final List<PartingGeometrySnapshot> snapshots = new ArrayList<>();
    private boolean cancelled;
    private boolean charged;
    private boolean scanned;

    PartingOperation(PartingOperationId operationId, UUID playerId, ResourceKey<Level> dimension, WaterMomentum.Hold hold) {
        this.operationId = operationId;
        this.playerId = playerId;
        this.dimension = dimension;
        this.hold = hold;
    }

    PartingOperationId operationId() {
        return operationId;
    }

    UUID playerId() {
        return playerId;
    }

    ResourceKey<Level> dimension() {
        return dimension;
    }

    WaterMomentum.Hold hold() {
        return hold;
    }

    boolean cancelled() {
        return cancelled;
    }

    boolean charged() {
        return charged;
    }

    boolean scanned() {
        return scanned;
    }

    void markCharged() {
        charged = true;
    }

    void markScanned() {
        scanned = true;
    }

    void attach(TechniqueHitEntity hitEntity) {
        if (cancelled || charged) {
            return;
        }
        for (TechniqueHitEntity existing : attachments) {
            if (existing.getUUID().equals(hitEntity.getUUID())) {
                return;
            }
        }
        attachments.add(hitEntity);
    }

    boolean capture(UUID entityId, PartingGeometrySnapshot snapshot) {
        if (cancelled || !captured.add(entityId)) {
            return false;
        }
        snapshots.add(snapshot);
        return true;
    }

    List<PartingGeometrySnapshot> snapshots() {
        return snapshots;
    }

    /** 还活着、又还没冻结几何的实体。去掉已经移除的。 */
    boolean waitingForActivation() {
        boolean waiting = false;
        for (TechniqueHitEntity hitEntity : attachments) {
            if (hitEntity.isRemoved()) {
                continue;
            }
            if (!captured.contains(hitEntity.getUUID())) {
                waiting = true;
            }
        }
        return waiting;
    }

    boolean hasLivingAttachment() {
        for (TechniqueHitEntity hitEntity : attachments) {
            if (!hitEntity.isRemoved()) {
                return true;
            }
        }
        return false;
    }

    void cancel() {
        cancelled = true;
        WaterMomentum.release(hold);
    }
}

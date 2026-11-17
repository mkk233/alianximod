package com.bslx.XueXiMod.network;

import net.minecraft.world.phys.Vec3;

import java.util.UUID;

/** 一次合法接触的冻结快照。A 和 B 都从这里生成，不再跟着人走。 */
public final class ActionContact {
    private final UUID targetId;
    private final int contactTick;
    private final Vec3 point;
    private final Vec3 direction;
    private final UUID groupId;

    public ActionContact(UUID targetId, int contactTick, Vec3 point, Vec3 direction, UUID groupId) {
        this.targetId = targetId;
        this.contactTick = contactTick;
        this.point = point;
        this.direction = direction;
        this.groupId = groupId;
    }

    public UUID targetId() {
        return targetId;
    }

    public int contactTick() {
        return contactTick;
    }

    public Vec3 point() {
        return point;
    }

    public Vec3 direction() {
        return direction;
    }

    public UUID groupId() {
        return groupId;
    }
}

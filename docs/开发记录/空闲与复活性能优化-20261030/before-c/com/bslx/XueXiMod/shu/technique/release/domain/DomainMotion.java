package com.bslx.XueXiMod.shu.technique.release.domain;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 完全进入后按轴裁切，只挡住穿出的那一面。传送不走这里，也不把人拉回旧位置。 */
public final class DomainMotion {
    private static final ConcurrentHashMap<UUID, Vec3> ALLOWED = new ConcurrentHashMap<>();

    private DomainMotion() {
    }

    public static Vec3 clip(Entity mover, Vec3 movement) {
        if (mover == null || movement == null || mover.isPassenger() || mover.isSpectator() || mover.noPhysics) {
            return movement;
        }
        if (movement.lengthSqr() <= 1.0E-8) {
            return movement;
        }
        List<DomainBound> domains = relevant(mover);
        if (domains.isEmpty()) {
            return movement;
        }
        Entity root = mover.getRootVehicle();
        AABB body = union(root);
        Vec3 clipped = movement;
        boolean held = false;
        for (DomainBound domain : domains) {
            if (!domain.active() || domain.boundary() == DomainBoundary.OPEN || groupPermitted(domain, root) || !domain.geometry().fits(body)) {
                continue;
            }
            if (!domain.geometry().fullyContains(body)) {
                continue;
            }
            held = true;
            clipped = domain.geometry().clipInside(body, clipped);
        }
        if (held) {
            ALLOWED.put(mover.getUUID(), mover.position().add(clipped));
        }
        return clipped;
    }

    public static void finish(Entity mover) {
        if (mover == null) {
            return;
        }
        Vec3 allowed = ALLOWED.remove(mover.getUUID());
        if (allowed == null) {
            return;
        }
        Entity root = mover.getRootVehicle();
        AABB body = union(root);
        boolean escaped = false;
        for (DomainBound domain : relevant(mover)) {
            if (!domain.active() || domain.boundary() == DomainBoundary.OPEN || groupPermitted(domain, root) || !domain.geometry().fits(body)) {
                continue;
            }
            if (!domain.geometry().fullyContains(body)) {
                escaped = true;
            }
        }
        if (!escaped) {
            return;
        }
        mover.setPos(allowed.x, allowed.y, allowed.z);
    }

    public static void forget(Entity mover) {
        if (mover != null) {
            ALLOWED.remove(mover.getUUID());
        }
    }

    private static boolean groupPermitted(DomainBound domain, Entity root) {
        if (!memberPermitted(domain, root)) {
            return false;
        }
        for (Entity passenger : root.getPassengers()) {
            if (!groupPermitted(domain, passenger)) {
                return false;
            }
        }
        return true;
    }

    private static boolean memberPermitted(DomainBound domain, Entity member) {
        return domain.passes(member);
    }

    private static AABB union(Entity root) {
        AABB body = root.getBoundingBox();
        for (Entity passenger : root.getPassengers()) {
            body = body.minmax(union(passenger));
        }
        return body;
    }

    private static List<DomainBound> relevant(Entity mover) {
        List<DomainBound> found = new ArrayList<>();
        if (mover.level().isClientSide()) {
            found.addAll(DomainClientBridge.targets(mover));
            return found;
        }
        for (DomainInstance domain : DomainService.active()) {
            if (domain.active() && domain.hostLevel() == mover.level()) {
                found.add(domain);
            }
        }
        return found;
    }
}

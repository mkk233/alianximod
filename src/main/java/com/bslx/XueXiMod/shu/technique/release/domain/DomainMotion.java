package com.bslx.XueXiMod.shu.technique.release.domain;

import com.bslx.XueXiMod.shu.technique.release.domain.motion.FlowMotionIntents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 领域普通移动裁切服务。完全进入后按轴裁切，只挡住穿出的那一面；传送不走这里，也不把人拉回旧位置。
 * 球形场地不封锁进出，护体跟着主人，都不使用这份封闭裁切。
 */
public final class DomainMotion {
    private static final ConcurrentHashMap<UUID, Vec3> ALLOWED = new ConcurrentHashMap<>();
    private static final List<DomainBound> SHARED_EMPTY = List.of();

    private DomainMotion() {
    }

    /** 裁切会穿出封闭领域的位移。乘客、旁观、无碰撞或开放边界原样返回，不扣资源。 */
    public static Vec3 clip(Entity mover, Vec3 movement) {
        if (mover == null || movement == null) {
            return movement;
        }
        if (mover.isPassenger() || mover.isSpectator() || mover.noPhysics) {
            return movement;
        }
        Vec3 shifted = FlowMotionIntents.compose(mover, movement);
        if (shifted == null) {
            shifted = movement;
        }
        if (shifted.lengthSqr() <= 1.0E-8D) {
            return shifted;
        }
        List<DomainBound> domains = relevant(mover);
        if (domains.isEmpty()) {
            return shifted;
        }
        Entity root = mover.getRootVehicle();
        AABB body = union(root);
        Vec3 clipped = shifted;
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

    /** 移动后复核身体。仍被封闭领域完整包含时不改位置，逃出时拉回本拍允许点。 */
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

    /** 丢掉这个移动者的允许落点。空实体忽略，不补偿位置。 */
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
        if (mover.level().isClientSide()) {
            List<DomainBound> clientTargets = DomainClientBridge.targets(mover);
            if (clientTargets.isEmpty()) {
                return SHARED_EMPTY;
            }
            return clientTargets;
        }
        List<DomainBound> found = new ArrayList<>();
        for (DomainInstance domain : DomainService.active()) {
            if (domain.active() && domain.hostLevel() == mover.level()) {
                found.add(domain);
            }
        }
        return found;
    }
}

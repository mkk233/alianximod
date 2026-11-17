package com.bslx.XueXiMod.shu.technique.release.statusquery;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.EnderDragonPart;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 同维度活体扫描。末影龙部位收成主体后再按 UUID 去重；距离看主体碰撞箱，不看部位箱。
 * 返回顺序只按距离平方和 UUID，档位排序由调用方用资源快照自己排。
 */
public final class StatusScan {
    private StatusScan() {
    }

    public static List<StatusCandidate> collect(ServerPlayer caster, StatusScanRequest request) {
        if (caster == null || request == null || !caster.isAlive() || caster.isSpectator() || caster.isRemoved()) {
            return List.of();
        }
        if (!(caster.level() instanceof ServerLevel level)) {
            return List.of();
        }
        MinecraftServer host = level.getServer();
        if (host == null || !host.isSameThread()) {
            return List.of();
        }
        Vec3 origin = caster.position();
        if (origin == null || !Double.isFinite(origin.x) || !Double.isFinite(origin.y) || !Double.isFinite(origin.z)) {
            return List.of();
        }
        ResourceStatuses.registerBuiltins();
        AABB search = new AABB(origin, origin).inflate(request.radius());
        List<Entity> found = level.getEntities(caster, search, StatusScan::listed);
        Map<UUID, LivingEntity> subjects = new LinkedHashMap<>();
        for (Entity candidate : found) {
            LivingEntity subject = resolveSubject(candidate);
            if (!selectable(caster, subject) || subject.level() != level) {
                continue;
            }
            UUID subjectId = subject.getUUID();
            if (subjectId == null) {
                continue;
            }
            subjects.putIfAbsent(subjectId, subject);
        }
        List<StatusCandidate> accepted = new ArrayList<>();
        for (LivingEntity subject : subjects.values()) {
            AABB bounds = subject.getBoundingBox();
            if (bounds == null) {
                continue;
            }
            Vec3 closest = BlockOcclusion.closestPoint(origin, bounds);
            double distanceSquared = origin.distanceToSqr(closest);
            if (!BlockOcclusion.withinRadius(distanceSquared, request.radius())) {
                continue;
            }
            if (!request.fullCircle()) {
                Vec3 look = caster.getLookAngle();
                if (!BlockOcclusion.inFront(caster.getEyePosition(), closest, look)) {
                    continue;
                }
            }
            if (!request.relation().accepts(caster, subject)) {
                continue;
            }
            if (request.blockOcclusion() && !BlockOcclusion.clearOfBlocks(level, caster, closest)) {
                continue;
            }
            if (!request.query().matches(subject)) {
                continue;
            }
            accepted.add(new StatusCandidate(
                    subject,
                    subject.getUUID(),
                    distanceSquared,
                    ResourceStatuses.readAll(subject),
                    StatusQuery.activeEffects(subject)));
        }
        accepted.sort((left, right) -> snapshotOrder(
                left.closestDistanceSquared(),
                left.subjectId(),
                right.closestDistanceSquared(),
                right.subjectId()));
        return List.copyOf(accepted);
    }

    static int snapshotOrder(double leftDistance, UUID leftId, double rightDistance, UUID rightId) {
        int distanceOrder = Double.compare(leftDistance, rightDistance);
        if (distanceOrder != 0) {
            return distanceOrder;
        }
        return leftId.compareTo(rightId);
    }

    private static boolean listed(Entity candidate) {
        if (candidate == null || candidate.isSpectator() || !candidate.isAlive()) {
            return false;
        }
        return candidate instanceof LivingEntity || candidate instanceof EnderDragonPart;
    }

    private static LivingEntity resolveSubject(Entity candidate) {
        if (candidate instanceof EnderDragonPart part) {
            return part.parentMob;
        }
        if (candidate instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    private static boolean selectable(ServerPlayer caster, LivingEntity subject) {
        if (caster == null || subject == null) {
            return false;
        }
        if (!subject.isAlive() || subject.isSpectator() || subject.isRemoved()) {
            return false;
        }
        if (subject == caster) {
            return false;
        }
        UUID subjectId = subject.getUUID();
        return subjectId != null && !subjectId.equals(caster.getUUID());
    }
}

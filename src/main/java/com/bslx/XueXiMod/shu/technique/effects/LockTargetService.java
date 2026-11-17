package com.bslx.XueXiMod.shu.technique.effects;

import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.batch.SpecialBalance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 玩家到生物的标记表。绑定施法帧前解析，后续按玩家标识取回。不改弹道、生成点或伤害。 */
public final class LockTargetService {
    static final double RANGE = 20.0;
    static final double SIDE_DISTANCE_UPPER_BOUND = 8.0;

    /** 把视线距离和侧向距离登记成索敌平衡项。 */
    public static void declareBalance() {
        SpecialBalance.publish(SpecialBalance.blocks(
                SpecialBalance.blocks(SpecialBalance.begin("universal_lock_target"),
                        BalanceKeys.SEARCH_RANGE, RANGE, "索敌视线距离"),
                BalanceKeys.SCAN_RANGE, SIDE_DISTANCE_UPPER_BOUND, "索敌侧向距离"));
    }

    private static double lookRange() {
        return SpecialBalance.number("universal_lock_target", BalanceKeys.SEARCH_RANGE, RANGE);
    }

    private static double sideRange() {
        return SpecialBalance.number("universal_lock_target", BalanceKeys.SCAN_RANGE, SIDE_DISTANCE_UPPER_BOUND);
    }

    private static final Map<UUID, Integer> MARKS = new ConcurrentHashMap<>();

    private LockTargetService() {
    }

    /** 找到则写入玩家标记并返回目标；找不到则清掉旧标记并返回空。 */
    public static LivingEntity resolve(ServerPlayer player) {
        LivingEntity target = findTarget(player);
        if (target != null) {
            mark(player.getUUID(), target.getId());
        } else {
            clear(player.getUUID());
        }
        return target;
    }

    /** 记下玩家标识对应的实体编号，覆盖旧标记。 */
    public static void mark(UUID playerId, int entityId) {
        MARKS.put(playerId, entityId);
    }

    /** 去掉该玩家的标记。没有标记时不做处理。 */
    public static void clear(UUID playerId) {
        MARKS.remove(playerId);
    }

    /** 取回仍存活的生物。标记缺失、实体不存在或已死亡时清标记并返回空。 */
    public static LivingEntity getMarkedTarget(ServerLevel level, UUID playerId) {
        Integer entityId = MARKS.get(playerId);
        if (entityId == null) {
            return null;
        }
        Entity entity = level.getEntity(entityId);
        if (!(entity instanceof LivingEntity living) || !living.isAlive()) {
            MARKS.remove(playerId);
            return null;
        }
        return living;
    }

    private static LivingEntity findTarget(ServerPlayer player) {
        Vec3 eye = player.getEyePosition();
        double lookRange = lookRange();
        double sideRange = sideRange();
        Vec3 end = eye.add(player.getLookAngle().scale(lookRange));
        AABB box = new AABB(eye, end).inflate(sideRange);
        List<LivingEntity> candidates = LivingCandidateQueries.targetQuery(player.level(), box,
                entity -> entity != player && entity.isAlive());
        LivingEntity best = null;
        double bestDistSqr = Double.MAX_VALUE;
        for (LivingEntity candidate : candidates) {
            Vec3 center = candidate.getBoundingBox().getCenter();
            double distSqr = distanceSqrToSegment(center, eye, end);
            if (distSqr > sideRange * sideRange) {
                continue;
            }
            if (distSqr < bestDistSqr) {
                bestDistSqr = distSqr;
                best = candidate;
            }
        }
        return best;
    }

    private static double distanceSqrToSegment(Vec3 point, Vec3 start, Vec3 end) {
        double abx = end.x - start.x;
        double aby = end.y - start.y;
        double abz = end.z - start.z;
        double apx = point.x - start.x;
        double apy = point.y - start.y;
        double apz = point.z - start.z;
        double abSqr = abx * abx + aby * aby + abz * abz;
        double projection = (apx * abx + apy * aby + apz * abz) / abSqr;
        projection = Math.max(0.0, Math.min(1.0, projection));
        double deltaX = point.x - (start.x + projection * abx);
        double deltaY = point.y - (start.y + projection * aby);
        double deltaZ = point.z - (start.z + projection * abz);
        return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ;
    }
}

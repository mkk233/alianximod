package com.bslx.XueXiMod.shu.technique.release.motion;

import com.bslx.XueXiMod.shu.compute.ComputeCalls;
import com.bslx.XueXiMod.shu.compute.JavaReference;
import com.bslx.XueXiMod.shu.technique.query.LivingCandidateQueries;
import com.bslx.XueXiMod.shu.technique.release.aim.LaunchAim;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Comparator;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * 单个弹道的找目标和限幅转向。自动瞄准只活在当前调用，没有全局目标账本。
 * 不移动命中体，也不改无敌帧。
 */
public final class TrackingSupport {
    /** 只活在当前这一拍的运动调用里，用完就清，不能留到下一 tick。 */
    private static final ThreadLocal<LaunchAim> BOUND_AIM = new ThreadLocal<>();

    /** 现有每拍转向上限。转向算法本身不变。 */
    public static double turnLimitRadians() {
        return JavaReference.turnLimitRadians();
    }

    private TrackingSupport() {}

    /** 只有自动持续的瞄准才绑上。其他值会清掉上一拍残留。 */
    public static void bindAutomaticAim(@Nullable LaunchAim aim) {
        if (aim != null && aim.automaticLifetime()) {
            BOUND_AIM.set(aim);
        } else {
            BOUND_AIM.remove();
        }
    }

    /** 运动调用结束时清掉线程上的瞄准，避免下一拍误用。 */
    public static void clearAutomaticAim() {
        BOUND_AIM.remove();
    }

    @Nullable
    /**
     * 自动持续只认绑定瞄准找到的目标。否则先保留原锁定，失效后在 5 格内重新找最近活体。
     * 同距离时取较小实体编号。
     */
    public static LivingEntity resolveTarget(ServerLevel level, Vec3 position,
                                             @Nullable UUID lockedTarget, Predicate<LivingEntity> eligible) {
        Predicate<LivingEntity> valid = target -> !target.isRemoved() && target.isAlive()
                && !target.isSpectator() && target.level() == level && eligible.test(target);
        LaunchAim boundAim = BOUND_AIM.get();
        if (boundAim != null && boundAim.automaticLifetime()) {
            LivingEntity localTarget = boundAim.findTarget(level);
            if (localTarget != null && valid.test(localTarget)) {
                return localTarget;
            }
            return null;
        }
        if (lockedTarget != null) {
            Entity current = level.getEntity(lockedTarget);
            if (current instanceof LivingEntity living && valid.test(living)) {
                return living;
            }
        }
        AABB area = new AABB(position, position).inflate(5.0);
        return LivingCandidateQueries.entityQuery(level, area, valid).stream()
                .filter(target -> target.getBoundingBox().getCenter().distanceToSqr(position) <= 25.0)
                .min(Comparator.comparingDouble((LivingEntity target) ->
                        target.getBoundingBox().getCenter().distanceToSqr(position))
                        .thenComparingInt(Entity::getId))
                .orElse(null);
    }

    /** 转向幅度交给公共计算，这里不另改上限。 */
    public static Vec3 turnToward(Vec3 direction, Vec3 desiredDirection) {
        ComputeCalls.turnToward(direction, desiredDirection);
        return ComputeCalls.turnedDirection();
    }
}

package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.compute.ComputeCalls;
import com.bslx.XueXiMod.shu.compute.JavaReference;
import com.bslx.XueXiMod.shu.technique.release.batch.MotionProfile;
import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

/**
 * 运动策略共用的采样。给出位置和朝向，也可以在函数弹道上重配体积。
 * 不创建命中体，不判定命中，也不改无敌帧或账本。
 */
public final class TrajectorySampler {

    private TrajectorySampler() {}

    public record Sample(Vec3 position, Vec3 direction) {}

    /** 按进度返回函数路径上的位置。 */
    public interface TrajectoryFunction {
        Vec3 position(Vec3 origin, Vec3 forward, double speed, double progress, Vec3 targetPoint);
    }

    /** 函数路径在采样后调整实体体积。 */
    public interface VolumeFit {
        void fit(TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint);
    }

    private static final Map<String, TrajectoryFunction> FUNCTIONS = new HashMap<>();
    private static final Map<String, VolumeFit> VOLUME_FITS = new HashMap<>();

    /** 同名函数直接替换。空编号也会按传入值登记。 */
    public static void registerFunction(String functionId, TrajectoryFunction function) {
        FUNCTIONS.put(functionId, function);
    }

    /** 同名体积适配直接替换。没有登记时采样仍可继续。 */
    public static void registerVolume(String functionId, VolumeFit volumeFit) {
        VOLUME_FITS.put(functionId, volumeFit);
    }

    /** 缺登记、实体或世界时什么都不做。 */
    public static void fitVolume(String functionId, TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint) {
        VolumeFit volumeFit = VOLUME_FITS.get(functionId);
        if (volumeFit != null && entity != null && level != null) {
            volumeFit.fit(entity, level, groundPoint);
        }
    }

    /**
     * 弹道点按已过拍数计算，超过运动拍数后停在最后一拍。
     * 水平上限大于 0 时只收回东西和南北距离，高度保持弹道结果。
     */
    public static Sample sampleBallistic(Vec3 origin, Vec3 initialVelocity, double gravity, int ageTicks,
                                         int motionTicks, double horizontalLimit) {
        Vec3 start = origin == null ? Vec3.ZERO : origin;
        Vec3 velocity = initialVelocity == null ? new Vec3(0.0, 1.0, 0.0) : initialVelocity;
        int elapsed = ageTicks;
        if (motionTicks > 0 && elapsed > motionTicks) {
            elapsed = motionTicks;
        }
        JavaReference.BallisticPoint ballistic = JavaReference.ballisticPoint(
                start.x, start.y, start.z, velocity.x, velocity.y, velocity.z, gravity, elapsed);
        Vec3 position = new Vec3(ballistic.positionEast(), ballistic.positionUp(), ballistic.positionSouth());
        Vec3 direction = new Vec3(ballistic.directionEast(), ballistic.directionUp(), ballistic.directionSouth());
        if (horizontalLimit > 0.0D) {
            double east = position.x - start.x;
            double south = position.z - start.z;
            double horizontal = Math.sqrt(east * east + south * south);
            if (horizontal > horizontalLimit && horizontal > 1.0E-8D) {
                double scale = horizontalLimit / horizontal;
                position = new Vec3(start.x + east * scale, position.y, start.z + south * scale);
            }
        }
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = new Vec3(0.0, -1.0, 0.0);
        }
        return new Sample(position, direction.normalize());
    }

    /** 在现有俯仰上再加角度，然后乘速度。几乎竖直时改用固定的水平分量。 */
    public static Vec3 pitchedVelocity(Vec3 forward, double speed, double pitchDegrees) {
        Vec3 unit = normalized(forward);
        if (unit == null) {
            unit = new Vec3(0.0, 0.0, 1.0);
        }
        double horizontalSpan = Math.sqrt(unit.x * unit.x + unit.z * unit.z);
        double pitch = Math.atan2(unit.y, horizontalSpan) + Math.toRadians(pitchDegrees);
        double pitchedCos = Math.cos(pitch);
        double pitchedSin = Math.sin(pitch);
        Vec3 pitched;
        if (horizontalSpan < 1.0E-8D) {
            pitched = new Vec3(0.0, unit.y >= 0.0D ? pitchedSin : -pitchedSin, pitchedCos);
        } else {
            pitched = new Vec3(unit.x / horizontalSpan * pitchedCos, pitchedSin, unit.z / horizontalSpan * pitchedCos);
        }
        return pitched.scale(speed);
    }

    /** 只有一份时垂直向上。多份按序号绕冻结轴均分，轴太陡时改用东西向作参考。 */
    public static Vec3 upwellVelocity(Vec3 frozenForward, double speed, double coneDegrees, int copyOrdinal, int spreadCount) {
        double tilt = spreadCount <= 1 ? 0.0D : Math.toRadians(coneDegrees);
        double yaw = spreadCount <= 1 ? 0.0D : (Math.PI * 2.0D * copyOrdinal / spreadCount);
        double localEast = Math.sin(tilt) * Math.cos(yaw);
        double localUp = Math.cos(tilt);
        double localSouth = Math.sin(tilt) * Math.sin(yaw);
        if (spreadCount <= 1) {
            return new Vec3(0.0D, speed, 0.0D);
        }
        Vec3 axis = normalized(frozenForward);
        if (axis == null) {
            axis = new Vec3(0.0, 1.0, 0.0);
        }
        Vec3 worldUp = Math.abs(axis.y) > 0.9D ? new Vec3(1.0, 0.0, 0.0) : new Vec3(0.0, 1.0, 0.0);
        Vec3 side = axis.cross(worldUp);
        if (side.lengthSqr() < 1.0E-8D) {
            side = new Vec3(1.0, 0.0, 0.0);
        } else {
            side = side.normalize();
        }
        Vec3 lifted = axis.cross(side).normalize();
        Vec3 direction = axis.scale(localUp).add(side.scale(localEast)).add(lifted.scale(localSouth));
        if (direction.lengthSqr() < 1.0E-8D) {
            direction = axis;
        }
        return direction.normalize().scale(speed);
    }

    /** 空参数先成无参数，再按弹道取已确认值。 */
    public static MotionProfile profileOrApproved(MotionProfile profile, Trajectory trajectory) {
        MotionProfile resolved = profile == null ? MotionProfile.NONE : profile;
        return resolved.resolve(trajectory);
    }

    /**
     * 函数路径用登记函数；没有登记时继续走公共闭合采样。
     * 函数朝向取当前点与稍早一点的差，差太小就沿用前向。
     */
    public static Sample sample(Trajectory trajectory, Vec3 origin, Vec3 forward, double speed,
                                int lifetimeTicks, int ageTicks, Vec3 targetPoint,
                                boolean followCurrentLook, Vec3 currentLook, String functionId) {
        if (trajectory == Trajectory.FUNCTION) {
            double progress = lifetimeTicks <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, ageTicks / (double) lifetimeTicks));
            Vec3 forwardUnit = normalized(forward);
            if (forwardUnit == null) {
                forwardUnit = new Vec3(0.0, 0.0, 1.0);
            }
            TrajectoryFunction function = FUNCTIONS.get(functionId);
            if (function != null) {
                Vec3 position = function.position(origin, forwardUnit, speed, progress, targetPoint);
                double earlier = Math.max(0.0, progress - 0.04);
                Vec3 before = function.position(origin, forwardUnit, speed, earlier, targetPoint);
                Vec3 direction = normalized(position.subtract(before));
                if (direction == null) {
                    direction = forwardUnit;
                }
                return new Sample(position, direction);
            }
        }
        ComputeCalls.sampleClosed(trajectory, origin, forward, speed, lifetimeTicks, ageTicks,
                targetPoint, followCurrentLook, currentLook);
        return new Sample(ComputeCalls.sampledPosition(trajectory, origin), ComputeCalls.sampledDirection());
    }

    private static Vec3 normalized(Vec3 vector) {
        if (vector == null || vector.lengthSqr() < 1.0E-8) {
            return null;
        }
        return vector.normalize();
    }
}

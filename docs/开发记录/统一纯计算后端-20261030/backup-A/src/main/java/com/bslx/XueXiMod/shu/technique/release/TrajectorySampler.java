package com.bslx.XueXiMod.shu.technique.release;

import com.bslx.XueXiMod.shu.technique.release.entity.TechniqueHitEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class TrajectorySampler {

    private TrajectorySampler() {}

    public record Sample(Vec3 position, Vec3 direction) {}

    public interface TrajectoryFunction {
        Vec3 position(Vec3 origin, Vec3 forward, double speed, double progress, Vec3 targetPoint);
    }

    public interface VolumeFit {
        void fit(TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint);
    }

    private static final Map<String, TrajectoryFunction> FUNCTIONS = new HashMap<>();
    private static final Map<String, VolumeFit> VOLUME_FITS = new HashMap<>();

    public static void registerFunction(String functionId, TrajectoryFunction function) {
        FUNCTIONS.put(functionId, function);
    }

    public static void registerVolume(String functionId, VolumeFit volumeFit) {
        VOLUME_FITS.put(functionId, volumeFit);
    }

    public static void fitVolume(String functionId, TechniqueHitEntity entity, ServerLevel level, Vec3 groundPoint) {
        VolumeFit volumeFit = VOLUME_FITS.get(functionId);
        if (volumeFit != null && entity != null && level != null) {
            volumeFit.fit(entity, level, groundPoint);
        }
    }

    public static Sample sample(Trajectory trajectory, Vec3 origin, Vec3 forward, double speed,
                                int lifetimeTicks, int ageTicks, Vec3 targetPoint,
                                boolean followCurrentLook, Vec3 currentLook, String functionId) {
        double progress = lifetimeTicks <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, ageTicks / (double) lifetimeTicks));

        Vec3 fwd = normalized(forward);
        if (fwd == null) {
            fwd = new Vec3(0.0, 0.0, 1.0);
        }

        switch (trajectory) {
            case STRAIGHT -> {
                if (targetPoint != null) {
                    Vec3 direction = normalized(targetPoint.subtract(origin));
                    if (direction == null) {
                        direction = fwd;
                    }
                    return new Sample(origin.add(direction.scale(speed * ageTicks)), direction);
                }
                return new Sample(origin.add(fwd.scale(speed * ageTicks)), fwd);
            }
            case DROP -> {
                // 只改高度。水平位置留在索敌已经选定的那一点，避免整片雨被收成一条线。
                double fall = 0.5 * 0.08 * ageTicks * ageTicks;
                return new Sample(new Vec3(origin.x, origin.y - fall, origin.z), new Vec3(0.0, -1.0, 0.0));
            }
            case FOLLOW_LOOK -> {
                Vec3 direction = normalized(currentLook);
                if (direction == null) {
                    direction = fwd;
                }
                if (targetPoint != null) {
                    Vec3 targetDirection = normalized(targetPoint.subtract(origin));
                    if (targetDirection != null) {
                        direction = targetDirection;
                    } else if (currentLook != null) {
                        direction = currentLook;
                    } else {
                        direction = fwd;
                    }
                }
                return new Sample(origin, direction);
            }
            case VORTEX -> {
                // 龙卷风：从上往下旋，半径收到 0，之后停在落点直到寿命结束。
                Vec3 landing = origin;
                double mouthRadius = 5.0;
                double columnHeight = 12.0;
                double turnCount = 3.0;
                int spiralTicks = Math.max(8, (lifetimeTicks * 2) / 3);
                if (ageTicks >= spiralTicks) {
                    Vec3 hold = normalized(new Vec3(fwd.x, 0.0, fwd.z));
                    if (hold == null) {
                        hold = new Vec3(0.0, 0.0, 1.0);
                    }
                    return new Sample(landing, hold);
                }
                double spiralProgress = ageTicks / (double) spiralTicks;
                double phase = Math.atan2(fwd.z, fwd.x);
                double angle = phase + spiralProgress * turnCount * Math.PI * 2.0;
                double radius = mouthRadius * (1.0 - spiralProgress);
                double height = columnHeight * (1.0 - spiralProgress);
                double spinRate = turnCount * Math.PI * 2.0 / spiralTicks;
                double shrinkRate = -mouthRadius / spiralTicks;
                double dropRate = -columnHeight / spiralTicks;
                Vec3 position = new Vec3(
                        landing.x + Math.cos(angle) * radius,
                        landing.y + height,
                        landing.z + Math.sin(angle) * radius);
                Vec3 tangent = new Vec3(
                        -Math.sin(angle) * radius * spinRate + Math.cos(angle) * shrinkRate,
                        dropRate,
                        Math.cos(angle) * radius * spinRate + Math.sin(angle) * shrinkRate);
                Vec3 direction = normalized(tangent);
                if (direction == null) {
                    direction = new Vec3(0.0, -1.0, 0.0);
                }
                return new Sample(position, direction);
            }
            case FUNCTION -> {
                TrajectoryFunction function = FUNCTIONS.get(functionId);
                if (function != null) {
                    Vec3 position = function.position(origin, fwd, speed, progress, targetPoint);
                    double earlier = Math.max(0.0, progress - 0.04);
                    Vec3 before = function.position(origin, fwd, speed, earlier, targetPoint);
                    Vec3 direction = normalized(position.subtract(before));
                    if (direction == null) {
                        direction = fwd;
                    }
                    return new Sample(position, direction);
                }
                return new Sample(origin.add(fwd.scale(speed * ageTicks)), fwd);
            }
            default -> {
                return new Sample(origin.add(fwd.scale(speed * ageTicks)), fwd);
            }
        }
    }

    private static Vec3 normalized(Vec3 vector) {
        if (vector == null || vector.lengthSqr() < 1.0E-8) {
            return null;
        }
        return vector.normalize();
    }
}

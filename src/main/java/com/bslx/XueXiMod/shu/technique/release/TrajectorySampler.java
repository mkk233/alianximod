package com.bslx.XueXiMod.shu.technique.release;

import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;

public final class TrajectorySampler {

    private TrajectorySampler() {}

    public record Sample(Vec3 position, Vec3 direction) {}

    public interface TrajectoryFunction {
        Vec3 position(Vec3 origin, Vec3 forward, double speed, double t, Vec3 targetPoint);
    }

    private static final Map<String, TrajectoryFunction> FUNCTIONS = new HashMap<>();

    public static void registerFunction(String id, TrajectoryFunction function) {
        FUNCTIONS.put(id, function);
    }

    public static Sample sample(Trajectory trajectory, Vec3 origin, Vec3 forward, double speed,
                                int lifetimeTicks, int ageTicks, Vec3 targetPoint,
                                boolean followCurrentLook, Vec3 currentLook, String functionId) {
        double t = lifetimeTicks <= 0 ? 0.0 : Math.min(1.0, Math.max(0.0, ageTicks / (double) lifetimeTicks));

        Vec3 fwd = normalized(forward);
        if (fwd == null) {
            fwd = new Vec3(0.0, 0.0, 1.0);
        }

        switch (trajectory) {
            case STRAIGHT -> {
                if (targetPoint != null) {
                    Vec3 dir = normalized(targetPoint.subtract(origin));
                    if (dir == null) {
                        dir = fwd;
                    }
                    return new Sample(origin.add(dir.scale(speed * ageTicks)), dir);
                }
                return new Sample(origin.add(fwd.scale(speed * ageTicks)), fwd);
            }
            case DROP -> {
                // 铁球自由落体：水平速度不再保持。暂定加速度 0.08 格/tick²，与原版生物重力同量级。
                double fall = 0.5 * 0.08 * ageTicks * ageTicks;
                double x = origin.x;
                double z = origin.z;
                if (targetPoint != null) {
                    x = targetPoint.x;
                    z = targetPoint.z;
                } else {
                    Vec3 horiz = normalized(new Vec3(fwd.x, 0.0, fwd.z));
                    if (horiz != null) {
                        x += horiz.x;
                        z += horiz.z;
                    }
                }
                return new Sample(new Vec3(x, origin.y - fall, z), new Vec3(0.0, -1.0, 0.0));
            }
            case FOLLOW_LOOK -> {
                Vec3 dir = normalized(currentLook);
                if (dir == null) {
                    dir = fwd;
                }
                if (targetPoint != null) {
                    Vec3 td = normalized(targetPoint.subtract(origin));
                    if (td != null) {
                        dir = td;
                    } else if (currentLook != null) {
                        dir = currentLook;
                    } else {
                        dir = fwd;
                    }
                }
                return new Sample(origin, dir);
            }
            case FUNCTION -> {
                TrajectoryFunction function = FUNCTIONS.get(functionId);
                if (function != null) {
                    Vec3 pos = function.position(origin, fwd, speed, t, targetPoint);
                    Vec3 dir = normalized(pos.subtract(origin));
                    if (dir == null) {
                        dir = fwd;
                    }
                    return new Sample(pos, dir);
                }
                return new Sample(origin.add(fwd.scale(speed * ageTicks)), fwd);
            }
            default -> {
                return new Sample(origin.add(fwd.scale(speed * ageTicks)), fwd);
            }
        }
    }

    private static Vec3 normalized(Vec3 v) {
        if (v == null || v.lengthSqr() < 1.0E-8) {
            return null;
        }
        return v.normalize();
    }
}

package com.bslx.XueXiMod.shu.technique.release.guard;

import net.minecraft.world.phys.Vec3;

/**
 * 护体环面法线服务。基础倾角、受击和预测互相独立，抬头低头不改世界水平。
 * 领域裁切盒子位移，球形场地不改环面姿态。
 */
public final class GuardPose {
    public static final int NONE = 0;
    public static final int MELEE = 1;
    public static final int ARROW = 2;
    public static final int CAUSE_NONE = 0;
    public static final int CAUSE_PREVIEW = 1;
    public static final int CAUSE_HIT = 2;
    public static final int ENTER_TICKS = 3;
    public static final int HOLD_TICKS = 6;
    public static final int RETURN_TICKS = 6;
    private static final double LEAN_DEGREES = 50.0;

    private GuardPose() {
    }

    /** 由基础倾角生成环面法线。不读取世界状态，不扣资源。 */
    public static Vec3 baseNormal(float tiltDegrees) {
        double radians = Math.toRadians(tiltDegrees);
        return new Vec3(0.0, Math.cos(radians), Math.sin(radians));
    }

    /** 返回世界水平法线。抬头低头不改变这份方向。 */
    public static Vec3 worldLevel() {
        return new Vec3(0.0, 1.0, 0.0);
    }

    /** 朝来向倾斜，表示卸力。不改箭的碰撞。 */
    public static Vec3 leanToward(Vec3 incoming) {
        Vec3 flat = incoming == null ? Vec3.ZERO : new Vec3(incoming.x, 0.0, incoming.z);
        if (flat.lengthSqr() < 1.0E-6) {
            return worldLevel();
        }
        flat = flat.normalize();
        double radians = Math.toRadians(LEAN_DEGREES);
        return new Vec3(flat.x * Math.sin(radians), Math.cos(radians), flat.z * Math.sin(radians));
    }
}

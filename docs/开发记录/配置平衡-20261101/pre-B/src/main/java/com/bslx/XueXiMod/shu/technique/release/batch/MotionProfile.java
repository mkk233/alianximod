package com.bslx.XueXiMod.shu.technique.release.batch;

/**
 * 抛物线和喷涌的运动参数。没有声明时，对应弹道使用这里的首版常数，不退回直线。
 */
public final class MotionProfile {
    public enum Kind {
        NONE,
        PARABOLA,
        UPWELL
    }

    public static final double APPROVED_GRAVITY = 0.08D;
    public static final MotionProfile NONE = new MotionProfile(Kind.NONE, 0.0D, 0.0D, 0.0D, 0.0D, 0, 0.0D);

    private final Kind kind;
    private final double speed;
    private final double gravity;
    private final double pitchDegrees;
    private final double coneDegrees;
    private final int motionTicks;
    private final double horizontalLimit;

    private MotionProfile(Kind kind, double speed, double gravity, double pitchDegrees, double coneDegrees,
                          int motionTicks, double horizontalLimit) {
        this.kind = kind;
        this.speed = speed;
        this.gravity = gravity;
        this.pitchDegrees = pitchDegrees;
        this.coneDegrees = coneDegrees;
        this.motionTicks = motionTicks;
        this.horizontalLimit = horizontalLimit;
    }

    public static MotionProfile parabola(double speed, double gravity, double pitchDegrees, int motionTicks) {
        return new MotionProfile(Kind.PARABOLA, speed, gravity, pitchDegrees, 0.0D, motionTicks, 0.0D);
    }

    public static MotionProfile upwell(double speed, double gravity, double coneDegrees, int motionTicks,
                                       double horizontalLimit) {
        return new MotionProfile(Kind.UPWELL, speed, gravity, 0.0D, coneDegrees, motionTicks, horizontalLimit);
    }

    /** 陨岩首版：初速 0.8，上仰 15 度，重力 0.08，最长 100 刻。 */
    public static MotionProfile approvedParabola() {
        return parabola(0.8D, APPROVED_GRAVITY, 15.0D, 100);
    }

    /** 喷涌首版：初速 1，重力 0.08，半角 25 度，水平 32 格，最长 100 刻。 */
    public static MotionProfile approvedUpwell() {
        return upwell(1.0D, APPROVED_GRAVITY, 25.0D, 100, 32.0D);
    }

    public Kind kind() {
        return kind;
    }

    public double speed() {
        return speed;
    }

    public double gravity() {
        return gravity;
    }

    public double pitchDegrees() {
        return pitchDegrees;
    }

    public double coneDegrees() {
        return coneDegrees;
    }

    public int motionTicks() {
        return motionTicks;
    }

    public double horizontalLimit() {
        return horizontalLimit;
    }

    public MotionProfile resolve(com.bslx.XueXiMod.shu.technique.release.Trajectory trajectory) {
        if (kind != Kind.NONE) {
            return this;
        }
        if (trajectory == com.bslx.XueXiMod.shu.technique.release.Trajectory.PARABOLA) {
            return approvedParabola();
        }
        if (trajectory == com.bslx.XueXiMod.shu.technique.release.Trajectory.UPWELL) {
            return approvedUpwell();
        }
        return this;
    }
}

package com.bslx.XueXiMod.shu.technique.release.batch;

import com.bslx.XueXiMod.carrier.BatchCarrierIds;
import com.bslx.XueXiMod.shu.balance.BalanceBound;
import com.bslx.XueXiMod.shu.balance.BalanceKeys;
import com.bslx.XueXiMod.shu.balance.BalanceUnit;
import com.bslx.XueXiMod.shu.balance.RawDefault;
import com.bslx.XueXiMod.shu.technique.release.ReleaseDefinition;
import com.bslx.XueXiMod.shu.technique.release.ReleaseRegistry;

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
    public static final double PARABOLA_SPEED = 0.8D;
    public static final int PARABOLA_TICKS = 100;
    public static final double UPWELL_SPEED = 1.0D;
    public static final int UPWELL_TICKS = 100;
    public static final double UPWELL_REACH = 32.0D;
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

    /** 抛物线参数。锥角和水平限制留空。 */
    public static MotionProfile parabola(double speed, double gravity, double pitchDegrees, int motionTicks) {
        return new MotionProfile(Kind.PARABOLA, speed, gravity, pitchDegrees, 0.0D, motionTicks, 0.0D);
    }

    /** 喷涌参数。仰角留空。 */
    public static MotionProfile upwell(double speed, double gravity, double coneDegrees, int motionTicks,
                                       double horizontalLimit) {
        return new MotionProfile(Kind.UPWELL, speed, gravity, 0.0D, coneDegrees, motionTicks, horizontalLimit);
    }

    /** 陨岩首版：初速 0.8，上仰 15 度，重力 0.08，最长 100 刻。初速和时长跟陨岩登记是同一对键。 */
    public static MotionProfile approvedParabola() {
        ReleaseDefinition definition = ReleaseRegistry.get(BatchCarrierIds.METEORITE);
        double speed = definition == null ? PARABOLA_SPEED : definition.speed();
        int life = definition == null ? PARABOLA_TICKS : definition.lifetimeTicks();
        return parabola(speed, APPROVED_GRAVITY, 15.0D, life);
    }

    /** 喷涌首版：初速 1，重力 0.08，半角 25 度，水平 32 格，最长 100 刻。 */
    public static MotionProfile approvedUpwell() {
        return upwell(
                SpecialBalance.number(BatchCarrierIds.UPWELL, BalanceKeys.MOTION_SPEED, UPWELL_SPEED),
                APPROVED_GRAVITY,
                25.0D,
                SpecialBalance.integer(BatchCarrierIds.UPWELL, BalanceKeys.DURATION_TICKS, UPWELL_TICKS),
                SpecialBalance.number(BatchCarrierIds.UPWELL, BalanceKeys.SPREAD_RANGE, UPWELL_REACH));
    }

    /** 登记喷涌的初速、时长和水平距离。 */
    public static void declareSpecialBalances() {
        SpecialBalance.publish(SpecialBalance.speed(SpecialBalance.begin(BatchCarrierIds.UPWELL), (float) UPWELL_SPEED, "喷涌初速")
                .integerParameter(BalanceKeys.DURATION_TICKS, RawDefault.literal(UPWELL_TICKS),
                        BalanceBound.positiveTicks(), BalanceUnit.TICKS, "喷涌时长")
                .numberParameter(BalanceKeys.SPREAD_RANGE, RawDefault.literal(UPWELL_REACH),
                        BalanceBound.nonNegativeSize(), BalanceUnit.BLOCKS, "喷涌水平距离"));
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

    /** 自己已有种类时保持。否则按弹道取陨岩或喷涌首版。 */
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

package com.bslx.XueXiMod.shu.technique.release.domain.motion;

/**
 * 发给客户端的运动快照。旧领域保持零值，外观渲染以后再读这些数；快照本身没有生命周期。
 * 球形场地和护体都不携带旋流或气泡运动帧。
 */
public final class MotionFrame {
    public static final MotionFrame NONE = new MotionFrame(0, false, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D, 0.0D);

    private final int kindOrdinal;
    private final boolean lifted;
    private final double attraction;
    private final double pull;
    private final double orbit;
    private final double rise;
    private final double travelSpeed;
    private final double bodyRadius;

    /** 冻结一次运动数值。不校验范围，不扣资源。 */
    public MotionFrame(int kindOrdinal, boolean lifted, double attraction, double pull, double orbit, double rise,
                       double travelSpeed, double bodyRadius) {
        this.kindOrdinal = kindOrdinal;
        this.lifted = lifted;
        this.attraction = attraction;
        this.pull = pull;
        this.orbit = orbit;
        this.rise = rise;
        this.travelSpeed = travelSpeed;
        this.bodyRadius = bodyRadius;
    }

    public int kindOrdinal() {
        return kindOrdinal;
    }

    public boolean lifted() {
        return lifted;
    }

    public double attraction() {
        return attraction;
    }

    public double pull() {
        return pull;
    }

    public double orbit() {
        return orbit;
    }

    public double rise() {
        return rise;
    }

    public double travelSpeed() {
        return travelSpeed;
    }

    public double bodyRadius() {
        return bodyRadius;
    }

    /** 把序号还原成运动种类。越界时退回无运动。 */
    public VolumeMotionKind kind() {
        VolumeMotionKind[] kinds = VolumeMotionKind.values();
        if (kindOrdinal < 0 || kindOrdinal >= kinds.length) {
            return VolumeMotionKind.NONE;
        }
        return kinds[kindOrdinal];
    }
}

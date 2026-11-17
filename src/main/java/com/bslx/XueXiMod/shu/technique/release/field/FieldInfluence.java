package com.bslx.XueXiMod.shu.technique.release.field;

import com.bslx.XueXiMod.shu.technique.release.Trajectory;
import net.minecraft.world.phys.Vec3;

/**
 * 施法瞬间读到的场地影响快照。伤害按份数乘，其余效果只取一份；快照不管理场地寿命。
 * 领域只加一份最大附加伤害，护体按自身比例卸伤，都不累加这份场地层数。
 */
public final class FieldInfluence {
    public static final FieldInfluence NONE = new FieldInfluence(0, null, false, false, 0f, 0f, 0, false, null);

    public final int occupied;
    public final Trajectory attachedPath;
    public final boolean attachedLock;
    public final boolean attachedTracking;
    public final float healAmount;
    public final float pullStrength;
    public final int wetnessGrant;
    public final boolean purify;
    public final Vec3 pullAnchor;

    /** 冻结一次读取结果。不校验数值，不扣资源。 */
    FieldInfluence(int occupied, Trajectory attachedPath, boolean attachedLock, boolean attachedTracking, float healAmount,
                   float pullStrength, int wetnessGrant, boolean purify, Vec3 pullAnchor) {
        this.occupied = occupied;
        this.attachedPath = attachedPath;
        this.attachedLock = attachedLock;
        this.attachedTracking = attachedTracking;
        this.healAmount = healAmount;
        this.pullStrength = pullStrength;
        this.wetnessGrant = wetnessGrant;
        this.purify = purify;
        this.pullAnchor = pullAnchor;
    }

    /** 一块是 1 倍，两块是 2 倍。不在场地里也是 1 倍。 */
    public float damageScale() {
        if (this.occupied <= 1) {
            return 1f;
        }
        return this.occupied;
    }
}

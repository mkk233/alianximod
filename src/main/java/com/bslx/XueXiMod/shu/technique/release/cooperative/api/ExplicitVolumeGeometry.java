package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

import net.minecraft.world.phys.Vec3;

/** 显式体积。原点和对向已经是这一名演员的最终值，里面没有新的伤害单价。 */
public record ExplicitVolumeGeometry(Vec3 origin, Vec3 direction, Vec3 bladeNormal, float roll, boolean pose3d,
                                     double boxDepth, double boxWidth, double boxThickness) {
    public ExplicitVolumeGeometry {
        origin = FrozenValues.finite(origin, "体积原点");
        direction = FrozenValues.unit(direction, "体积方向");
        bladeNormal = FrozenValues.unit(bladeNormal, "体积刀面法线");
        if (!Float.isFinite(roll)) {
            throw new IllegalArgumentException("体积滚转必须有限");
        }
        if (!legalSize(boxDepth) || !legalSize(boxWidth) || !legalSize(boxThickness)) {
            throw new IllegalArgumentException("体积尺寸必须是不超过 512 的有限正数");
        }
    }

    private static boolean legalSize(double size) {
        return Double.isFinite(size) && size > 0.0D && size <= CooperativeStructureLimits.BOX_SIZE_CAP;
    }
}

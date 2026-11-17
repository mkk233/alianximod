package com.bslx.XueXiMod.shu.technique.release.cooperative.api;

/** 适配器在准备时一次读入的尺寸、寿命和倾角。横劈填自己的盒配置，不抄斜斩的键。 */
public record AdapterVolumeSpec(double boxDepth, double boxWidth, double boxThickness, int lifetimeTicks,
                                double tiltDegrees) {
    public AdapterVolumeSpec {
        if (!legalSize(boxDepth) || !legalSize(boxWidth) || !legalSize(boxThickness)) {
            throw new IllegalArgumentException("斩击尺寸必须是不超过 512 的有限正数");
        }
        if (lifetimeTicks < 1) {
            throw new IllegalArgumentException("斩击寿命至少为 1 刻");
        }
        if (!Double.isFinite(tiltDegrees)
                || tiltDegrees < CooperativeStructureLimits.TILT_LOW
                || tiltDegrees > CooperativeStructureLimits.TILT_HIGH) {
            throw new IllegalArgumentException("刀面倾角超出 -80 到 80 度");
        }
    }

    private static boolean legalSize(double size) {
        return Double.isFinite(size) && size > 0.0D && size <= CooperativeStructureLimits.BOX_SIZE_CAP;
    }
}

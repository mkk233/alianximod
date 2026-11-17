package com.bslx.XueXiMod.shu.technique.release.water;

/** 冻结弧的有限体积。旧串没有这个字段时按外环解。 */
public enum ArcBandMode {
    OUTER_ARC,
    RADIAL_BLADE;

    public static ArcBandMode fromPacked(String token) {
        if (token == null || token.isEmpty()) {
            return null;
        }
        for (ArcBandMode candidate : values()) {
            if (candidate.name().equals(token)) {
                return candidate;
            }
        }
        return null;
    }
}

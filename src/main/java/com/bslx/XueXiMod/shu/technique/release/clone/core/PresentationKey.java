package com.bslx.XueXiMod.shu.technique.release.clone.core;

public enum PresentationKey {
    LEGACY_HUMANOID,
    WATER_OUTLINE;

    public static PresentationKey fromOrdinal(int ordinal) {
        PresentationKey[] keys = values();
        if (ordinal < 0 || ordinal >= keys.length) {
            return LEGACY_HUMANOID;
        }
        return keys[ordinal];
    }
}

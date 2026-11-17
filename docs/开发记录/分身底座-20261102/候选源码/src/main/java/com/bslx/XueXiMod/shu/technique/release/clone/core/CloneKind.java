package com.bslx.XueXiMod.shu.technique.release.clone.core;

public enum CloneKind {
    PHANTOM,
    NORMAL;

    public static CloneKind fromOrdinal(int ordinal) {
        CloneKind[] kinds = values();
        if (ordinal < 0 || ordinal >= kinds.length) {
            return PHANTOM;
        }
        return kinds[ordinal];
    }
}

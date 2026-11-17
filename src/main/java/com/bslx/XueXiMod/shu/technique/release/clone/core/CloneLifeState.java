package com.bslx.XueXiMod.shu.technique.release.clone.core;

public enum CloneLifeState {
    ACTIVE,
    ENDING,
    ENDED;

    public static CloneLifeState fromOrdinal(int ordinal) {
        CloneLifeState[] states = values();
        if (ordinal < 0 || ordinal >= states.length) {
            return ENDED;
        }
        return states[ordinal];
    }
}

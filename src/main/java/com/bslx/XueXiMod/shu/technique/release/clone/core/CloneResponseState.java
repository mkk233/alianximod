package com.bslx.XueXiMod.shu.technique.release.clone.core;

public enum CloneResponseState {
    READY,
    RESERVED,
    COMMITTING,
    COOLDOWN,
    SPENT;

    public static CloneResponseState fromOrdinal(int ordinal) {
        CloneResponseState[] states = values();
        if (ordinal < 0 || ordinal >= states.length) {
            return READY;
        }
        return states[ordinal];
    }
}

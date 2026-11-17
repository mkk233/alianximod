package com.bslx.XueXiMod.carrier;

/** 载体视觉的画法。隐藏项只在服务端跑留焰，客户端不画。 */
public enum BatchVisualShape {
    BILLBOARD,
    RING,
    METEOR_BLOCK,
    HIDDEN;

    public static BatchVisualShape fromOrdinal(int ordinal) {
        BatchVisualShape[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return HIDDEN;
        }
        return values[ordinal];
    }
}
